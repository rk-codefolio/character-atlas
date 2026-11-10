package dev.characteratlas.archipelago.moderation.infrastructure;


import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class DiscordReportNotifications {
    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private final ModerationMapper mapper;
    private final URI webhook;
    private final String origin;
    private final List<String> recipients;
    private final RestClient discord;

    public DiscordReportNotifications(ModerationMapper mapper, @Value("${catalog.discord.report-webhook}") String webhook,
                               @Value("${catalog.frontend-origin}") String origin,
                               @Value("${catalog.discord.owner-id}") String owner,
                               @Value("${catalog.discord.moderator-ids}") String moderators) {
        this.mapper = mapper;
        this.origin = origin;
        this.recipients = Stream.concat(Stream.of(owner), Stream.of(moderators.split(",")))
                .map(String::strip).filter(id -> id.matches("[0-9]{1,20}")).distinct().toList();
        validateConfiguration(webhook, origin, owner, recipients);
        this.webhook = webhook.isBlank() ? null : URI.create(webhook + "?wait=true");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        discord = RestClient.builder().requestFactory(factory).build();
    }

    public static void validateConfiguration(String webhook, String origin, String owner, List<String> recipients) {
        if (!webhook.isBlank() && !webhook.matches("https://discord\\.com/api/(?:v10/)?webhooks/[0-9]{1,20}/[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid Discord report webhook URL");
        }
        if (!webhook.isBlank() && (!owner.matches("[0-9]{1,20}") || recipients.size() > 100)) {
            throw new IllegalArgumentException("Discord report notifications require a valid owner ID and at most 100 recipients");
        }
        if (!webhook.isBlank() && message(recipients, origin, new UUID(0, 0)).length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Discord report notification exceeds 2000 characters. Reduce the recipient list.");
        }
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void deliverNext() {
        if (webhook == null) return;
        var report = mapper.next();
        if (report == null) return;
        try {
            discord.post().uri(webhook).contentType(MediaType.APPLICATION_JSON)
                    .body(payload(recipients, origin, report.id())).retrieve().toBodilessEntity();
            mapper.delivered(report.id());
        } catch (RestClientException error) {
            int delay = Math.min(3600, 60 << Math.min(report.attempts(), 6));
            if (error instanceof RestClientResponseException response && response.getStatusCode().value() == 429
                    && response.getResponseHeaders() != null) {
                String retry = response.getResponseHeaders().getFirst("Retry-After");
                if (retry != null && retry.matches("[0-9]{1,7}(\\.[0-9]+)?")) {
                    delay = Math.max(delay, (int) Math.ceil(Double.parseDouble(retry)));
                }
            }
            LoggerFactory.getLogger(DiscordReportNotifications.class).warn(
                    "Discord report notification queued for retry, report={}, delay={}s", report.id(), delay);
            mapper.retry(report.id(), delay);
        }
    }

    public static Map<String, Object> payload(List<String> recipients, String origin, UUID id) {
        return Map.of("content", message(recipients, origin, id),
                "allowed_mentions", Map.of("parse", List.of(), "users", recipients));
    }

    private static String message(List<String> recipients, String origin, UUID id) {
        String mentions = String.join(" ", recipients.stream().map(user -> "<@" + user + ">").toList());
        return mentions + "\n投稿に通報が届きました。\n<" + origin + "/?admin=1&report=" + id + ">";
    }

}
