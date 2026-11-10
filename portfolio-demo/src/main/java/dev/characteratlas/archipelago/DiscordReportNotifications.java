package dev.characteratlas.archipelago;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
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
class DiscordReportNotifications {
    private final JdbcTemplate jdbc;
    private final URI webhook;
    private final String origin;
    private final List<String> recipients;
    private final RestClient discord;

    DiscordReportNotifications(JdbcTemplate jdbc, @Value("${catalog.discord.report-webhook}") String webhook,
                               @Value("${catalog.frontend-origin}") String origin,
                               @Value("${catalog.discord.owner-id}") String owner,
                               @Value("${catalog.discord.moderator-ids}") String moderators) {
        this.jdbc = jdbc;
        this.origin = origin;
        this.recipients = Stream.concat(Stream.of(owner), Stream.of(moderators.split(",")))
                .map(String::strip).filter(id -> id.matches("[0-9]{1,20}")).distinct().toList();
        if (!webhook.isBlank() && !webhook.matches("https://discord\\.com/api/(?:v10/)?webhooks/[0-9]{1,20}/[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid Discord report webhook URL");
        }
        if (!webhook.isBlank() && (!owner.matches("[0-9]{1,20}") || recipients.size() > 100)) {
            throw new IllegalArgumentException("Discord report notifications require a valid owner ID and at most 100 recipients");
        }
        this.webhook = webhook.isBlank() ? null : URI.create(webhook + "?wait=true");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        discord = RestClient.builder().requestFactory(factory).build();
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void deliverNext() {
        if (webhook == null) return;
        var reports = jdbc.query("""
                SELECT report_id, attempts FROM post_report
                WHERE notified_at IS NULL AND resolved_at IS NULL AND next_attempt <= now()
                ORDER BY next_attempt LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (row, index) -> new Pending(row.getObject(1, UUID.class), row.getInt(2)));
        if (reports.isEmpty()) return;
        Pending report = reports.get(0);
        try {
            discord.post().uri(webhook).contentType(MediaType.APPLICATION_JSON)
                    .body(payload(recipients, origin, report.id())).retrieve().toBodilessEntity();
            jdbc.update("UPDATE post_report SET notified_at = now() WHERE report_id = ?", report.id());
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
            jdbc.update("""
                    UPDATE post_report SET attempts = attempts + 1, next_attempt = now() + ? * interval '1 second'
                    WHERE report_id = ?
                    """, delay, report.id());
        }
    }

    static Map<String, Object> payload(List<String> recipients, String origin, UUID id) {
        String mentions = String.join(" ", recipients.stream().map(user -> "<@" + user + ">").toList());
        return Map.of("content", mentions + "\n投稿に通報が届きました。\n<" + origin + "/?admin=1&report=" + id + ">",
                "allowed_mentions", Map.of("parse", List.of(), "users", recipients));
    }

    private record Pending(UUID id, int attempts) {}
}
