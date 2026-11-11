package dev.characteratlas.archipelago.moderation.infrastructure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** 通報通知の成功と再試行の処理を模擬応答で検証する。 */
class DiscordReportNotificationsTest {
  /** 検証用の通知先。 */
  private static final String WEBHOOK = "https://discord.com/api/webhooks/1/sample";

  /** 対象は通知の状態遷移。成功と429を順に返し、送信済み記録と指定待ち時間での再試行を期待する。 */
  @Test
  void deliversReportsAndHonorsRetryAfter() {
    // 通知対象の識別子。
    var report = UUID.randomUUID();
    // 送信待ちの通報を返す永続化ポートの代替。
    var mapper = mock(ModerationMapper.class);
    when(mapper.next()).thenReturn(new PendingNotification(report, 0));
    // 通知を実際に送信せず応答だけを返すクライアント設定。
    var builder = RestClient.builder();
    // 成功とレート制限の応答を管理するモック。
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(WEBHOOK + "?wait=true")).andRespond(withSuccess());
    server
        .expect(requestTo(WEBHOOK + "?wait=true"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "75.2"));
    // 設定済みの通報通知処理。
    var worker =
        new DiscordReportNotifications(
            mapper, WEBHOOK, "http://localhost:3000", "1111", "2222", builder.build());
    worker.deliverNext();
    worker.deliverNext();
    verify(mapper).delivered(report);
    verify(mapper).retry(report, 76);
    server.verify();
  }
}
