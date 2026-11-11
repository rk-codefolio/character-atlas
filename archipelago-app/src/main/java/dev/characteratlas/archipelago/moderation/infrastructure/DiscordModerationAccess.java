package dev.characteratlas.archipelago.moderation.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

/** Discord OAuth2のセッションを作り、オーナーとモデレーターの権限を判定する。 */
@Component
public class DiscordModerationAccess {
  /** 認証済みユーザーを保持するセッションキー。 */
  public static final String USER_SESSION = "moderation.discordUser";

  /** 認証途中の情報を保持するセッションキー。 */
  private static final String FLOW_SESSION = "moderation.discordFlow";

  /** Discord。 */
  private static final String DISCORD = "https://discord.com";

  /** 許可する画面のオリジン。 */
  private final String origin;

  /** Discord OAuth2アプリのクライアントID。 */
  private final String clientId;

  /** 秘密情報。 */
  private final String secret;

  /** オーナーのId。 */
  private final String ownerId;

  /** 管理操作を許可したDiscordユーザーID。 */
  private final Set<String> moderatorIds;

  /** 乱数。 */
  private final SecureRandom random = new SecureRandom();

  /** Discord。 */
  private final RestClient discord;

  /** Discordのモデレーションの権限を初期化する。 */
  @Autowired
  public DiscordModerationAccess(
      @Value("${catalog.frontend-origin}") String origin,
      @Value("${catalog.discord.client-id}") String clientId,
      @Value("${catalog.discord.client-secret}") String secret,
      @Value("${catalog.discord.owner-id}") String ownerId,
      @Value("${catalog.discord.moderator-ids}") String moderatorIds) {
    this(origin, clientId, secret, ownerId, moderatorIds, createClient());
  }

  /** HTTPクライアントを受け取り、外部サービスとの接続方法を分離する。 */
  DiscordModerationAccess(
      String origin,
      String clientId,
      String secret,
      String ownerId,
      String moderatorIds,
      RestClient client) {
    this.origin = origin;
    this.clientId = clientId;
    this.secret = secret;
    this.ownerId = ownerId;
    this.moderatorIds =
        Arrays.stream(moderatorIds.split(","))
            .map(String::strip)
            .filter(DiscordModerationAccess::validId)
            .collect(Collectors.toUnmodifiableSet());
    discord = client;
  }

  /** 接続と読み取りにタイムアウトを設定したHTTPクライアントを作る。 */
  private static RestClient createClient() {
    // HTTPタイムアウトを設定するリクエストファクトリー。
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().baseUrl(DISCORD).requestFactory(factory).build();
  }

  /** 現在のセッションから管理権限を判定する。 */
  public ModeratorAccess access(HttpServletRequest request) {
    // ログイン状態を保持するセッション。
    var session = request.getSession(false);
    // 識別子。
    String id = session == null ? null : (String) session.getAttribute(USER_SESSION);
    // 認証に必要な設定が有効かどうか。
    boolean configured = enabled();
    // オーナーの識別子。
    boolean owner = configured && validId(id) && ownerId.equals(id);
    return new ModeratorAccess(
        configured, owner, configured && !owner && validId(id) && moderatorIds.contains(id));
  }

  /** モデレーター以上の権限を確認し、監査用の操作者IDを返す。 */
  public String requireModerator(HttpServletRequest request) {
    // 認可情報。
    ModeratorAccess access = access(request);
    if (!access.canModerate()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return (String) request.getSession(false).getAttribute(USER_SESSION);
  }

  /** オーナー権限を確認し、監査用の操作者IDを返す。 */
  public String requireOwner(HttpServletRequest request) {
    if (!access(request).owner()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return requireModerator(request);
  }

  /** 書き込みリクエストが設定済みの画面オリジンから届いたことを確認する。 */
  public void requireOrigin(HttpServletRequest request) {
    if (!origin.equals(request.getHeader("Origin")))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  /** 照合値をセッションに保存し、Discordの認可画面のURLを作る。 */
  public URI start(HttpServletRequest request) {
    if (!enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    // OAuth照合値の生成に使うランダムなバイト列。
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    // OAuth認証の照合値。
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    request
        .getSession()
        .setAttribute(FLOW_SESSION, new OAuthFlow(state, Instant.now().plusSeconds(300)));
    return UriComponentsBuilder.fromUriString(DISCORD + "/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("scope", "identify")
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", callback())
        .queryParam("state", state)
        .build()
        .encode()
        .toUri();
  }

  /** 認可応答を検証し、許可されたDiscordユーザーの管理セッションを確立する。 */
  public URI finish(HttpServletRequest request, String state, String code) {
    // ログイン状態を保持するセッション。
    var session = request.getSession(false);
    if (session == null || !enabled()) return landing("expired");
    // 認証の進行状態。
    OAuthFlow flow;
    synchronized (session) {
      flow = (OAuthFlow) session.getAttribute(FLOW_SESSION);
      if (flow == null || !flow.state().equals(state) || Instant.now().isAfter(flow.expires())) {
        return landing("expired");
      }
      session.removeAttribute(FLOW_SESSION);
    }
    if (code == null || code.isBlank()) return landing("cancelled");
    // 識別子。
    String id;
    try {
      // Discordのトークン交換に渡すフォーム。
      var form = new LinkedMultiValueMap<String, String>();
      form.add("client_id", clientId);
      form.add("client_secret", secret);
      form.add("grant_type", "authorization_code");
      form.add("code", code);
      form.add("redirect_uri", callback());
      // 外部サービスの認証トークン。
      JsonNode token =
          discord
              .post()
              .uri("/api/v10/oauth2/token")
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(JsonNode.class);
      // bearer。
      String bearer = token == null ? "" : token.path("access_token").asString("");
      if (bearer.isBlank()) return landing("error");
      // user。
      JsonNode user =
          discord
              .get()
              .uri("/api/v10/users/@me")
              .headers(headers -> headers.setBearerAuth(bearer))
              .retrieve()
              .body(JsonNode.class);
      id = user == null ? null : user.path("id").asString(null);
    } catch (RestClientException error) {
      return landing("error");
    }
    if (!validId(id) || !(ownerId.equals(id) || moderatorIds.contains(id)))
      return landing("denied");
    request.changeSessionId();
    session.setAttribute(USER_SESSION, id);
    return landing(null);
  }

  /** landingを扱う。 */
  private URI landing(String result) {
    return URI.create(origin + "/?admin=1" + (result == null ? "" : "&auth=" + result));
  }

  /** 有効化を扱う。 */
  private boolean enabled() {
    return validId(clientId) && !secret.isBlank() && (validId(ownerId) || !moderatorIds.isEmpty());
  }

  /** コールバックを扱う。 */
  private String callback() {
    return origin + "/api/admin/discord/callback";
  }

  /** 有効性のIdを扱う。 */
  private static boolean validId(String value) {
    return value != null && value.matches("[0-9]{1,20}");
  }
}
