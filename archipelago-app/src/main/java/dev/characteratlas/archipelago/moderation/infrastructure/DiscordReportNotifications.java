package dev.characteratlas.archipelago.moderation.infrastructure;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
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

/** 保留中の通報通知をDiscordへ送信し、失敗した通知を再試行する。 */
@Component
public class DiscordReportNotifications {
  /** Discord通知本文の最大文字数。 */
  private static final int MAX_MESSAGE_LENGTH = 2_000;

  /** PostgreSQLのクエリーを実行するMapper。 */
  private final ModerationMapper mapper;

  /** 通報通知の送信先。 */
  private final URI webhook;

  /** 許可する画面のオリジン。 */
  private final String origin;

  /** 通報通知でメンションするDiscordユーザーID。 */
  private final List<String> recipients;

  /** Discord。 */
  private final RestClient discord;

  /** 通知先とメンション対象を確定し、送信クライアントを準備する。 */
  public DiscordReportNotifications(
      ModerationMapper mapper,
      @Value("${catalog.discord.report-webhook}") String webhook,
      @Value("${catalog.frontend-origin}") String origin,
      @Value("${catalog.discord.owner-id}") String owner,
      @Value("${catalog.discord.moderator-ids}") String moderators) {
    this.mapper = mapper;
    this.origin = origin;
    this.recipients =
        Stream.concat(Stream.of(owner), Stream.of(moderators.split(",")))
            .map(String::strip)
            .filter(id -> id.matches("[0-9]{1,20}"))
            .distinct()
            .toList();
    validateConfiguration(webhook, origin, owner, recipients);
    this.webhook = webhook.isBlank() ? null : URI.create(webhook + "?wait=true");
    // 生成設定。
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(8));
    discord = RestClient.builder().requestFactory(factory).build();
  }

  /** 通知先、宛先の件数、通知本文の長さを起動時に検証する。 */
  public static void validateConfiguration(
      String webhook, String origin, String owner, List<String> recipients) {
    if (!webhook.isBlank()
        && !webhook.matches(
            "https://discord\\.com/api/(?:v10/)?webhooks/[0-9]{1,20}/[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("Invalid Discord report webhook URL");
    }
    if (!webhook.isBlank() && (!owner.matches("[0-9]{1,20}") || recipients.size() > 100)) {
      throw new IllegalArgumentException(
          "Discord report notifications require a valid owner ID and at most 100 recipients");
    }
    if (!webhook.isBlank()
        && message(recipients, origin, new UUID(0, 0)).length() > MAX_MESSAGE_LENGTH) {
      throw new IllegalArgumentException(
          "Discord report notification exceeds 2000 characters. Reduce the recipient list.");
    }
  }

  /** 送信可能な通報を1件確保し、送信結果に応じて状態を更新する。 */
  @Scheduled(fixedDelay = 15000)
  @Transactional
  public void deliverNext() {
    if (webhook == null) return;
    // 対象の通報。
    var report = mapper.next();
    if (report == null) return;
    try {
      discord
          .post()
          .uri(webhook)
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload(recipients, origin, report.id()))
          .retrieve()
          .toBodilessEntity();
      mapper.delivered(report.id());
    } catch (RestClientException error) {
      // 待機時間。
      int delay = Math.min(3600, 60 << Math.min(report.attempts(), 6));
      if (error instanceof RestClientResponseException response
          && response.getStatusCode().value() == 429
          && response.getResponseHeaders() != null) {
        // 再試行する。
        String retry = response.getResponseHeaders().getFirst("Retry-After");
        if (retry != null && retry.matches("[0-9]{1,7}(\\.[0-9]+)?")) {
          delay = Math.max(delay, (int) Math.ceil(Double.parseDouble(retry)));
        }
      }
      LoggerFactory.getLogger(DiscordReportNotifications.class)
          .warn(
              "Discord report notification queued for retry, report={}, delay={}s",
              report.id(),
              delay);
      mapper.retry(report.id(), delay);
    }
  }

  /** Discordへ送る本文と許可済みメンションの指定を組み立てる。 */
  public static Map<String, Object> payload(List<String> recipients, String origin, UUID id) {
    return Map.of(
        "content",
        message(recipients, origin, id),
        "allowed_mentions",
        Map.of("parse", List.of(), "users", recipients));
  }

  /** 宛先へのメンションと通報画面のリンクを本文に組み立てる。 */
  private static String message(List<String> recipients, String origin, UUID id) {
    // Discord本文用のメンション表記。
    String mentions = String.join(" ", recipients.stream().map(user -> "<@" + user + ">").toList());
    return mentions + "\n投稿に通報が届きました。\n<" + origin + "/?admin=1&report=" + id + ">";
  }
}
