package dev.characteratlas.archipelago.moderation.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** Discord OAuthのセッションとロールを外部送信なしで検証する。 */
class DiscordModerationAccessTest {
  /** 対象は認可コールバック。オーナー応答を模擬し、セッションの確立と照合値の再利用拒否を期待する。 */
  @Test
  void authorizesOwnerAndConsumesState() {
    // モックサーバーを付けたDiscord用HTTPビルダー。
    var builder = RestClient.builder().baseUrl("https://discord.com");
    // Discordのトークン交換と本人情報を返すモック。
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://discord.com/api/v10/oauth2/token"))
        .andRespond(withSuccess("{\"access_token\":\"sample-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://discord.com/api/v10/users/@me"))
        .andRespond(withSuccess("{\"id\":\"1111\"}", MediaType.APPLICATION_JSON));
    // オーナーとモデレーターを明示した認証処理。
    var access =
        new DiscordModerationAccess(
            "http://localhost:3000", "1234", "sample", "1111", "2222", builder.build());
    // 認可開始と応答で使う同じブラウザセッション。
    var request = new MockHttpServletRequest();
    // 認可URLに埋め込まれた一度限りの照合値。
    var state =
        UriComponentsBuilder.fromUri(access.start(request))
            .build()
            .getQueryParams()
            .getFirst("state");
    assertFalse(access.access(request).owner());
    assertEquals(
        "http://localhost:3000/?admin=1", access.finish(request, state, "code").toString());
    assertTrue(access.access(request).owner());
    assertEquals("1111", access.requireOwner(request));
    assertEquals(
        "http://localhost:3000/?admin=1&auth=expired",
        access.finish(request, state, "code").toString());
    server.verify();
  }

  /** 対象はキャンセル時の認可。正しい照合値と空のコードを渡し、管理権限を付けずに終了する。 */
  @Test
  void handlesCanceledAuthorization() {
    // 有効な設定で初期化した認証処理。
    var access =
        new DiscordModerationAccess("http://localhost:3000", "1234", "sample", "1111", "2222");
    // 認可応答を受け取るリクエスト。
    var request = new MockHttpServletRequest();
    // 認可を始めた時点の照合値。
    var state =
        UriComponentsBuilder.fromUri(access.start(request))
            .build()
            .getQueryParams()
            .getFirst("state");
    assertEquals(
        "http://localhost:3000/?admin=1&auth=cancelled",
        access.finish(request, state, null).toString());
    assertFalse(access.access(request).canModerate());
  }
}
