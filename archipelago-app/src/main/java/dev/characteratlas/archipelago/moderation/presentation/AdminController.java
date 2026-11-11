package dev.characteratlas.archipelago.moderation.presentation;

import dev.characteratlas.archipelago.catalog.application.SearchContentOptions;
import dev.characteratlas.archipelago.moderation.application.AuditEntry;
import dev.characteratlas.archipelago.moderation.application.AuditLog;
import dev.characteratlas.archipelago.moderation.application.ModeratedPosts;
import dev.characteratlas.archipelago.moderation.application.PostEntry;
import dev.characteratlas.archipelago.moderation.infrastructure.DiscordModerationAccess;
import dev.characteratlas.archipelago.moderation.infrastructure.ModeratorAccess;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** AdminのREST APIを表す。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
  /** 認可情報。 */
  private final DiscordModerationAccess access;

  /** 監査記録のポート。 */
  private final AuditLog log;

  /** 検索のContents。 */
  private final SearchContentOptions searchContents;

  /** posts。 */
  private final ModeratedPosts posts;

  /** AdminのREST APIを初期化する。 */
  public AdminController(
      DiscordModerationAccess access,
      AuditLog log,
      SearchContentOptions searchContents,
      ModeratedPosts posts) {
    this.access = access;
    this.log = log;
    this.searchContents = searchContents;
    this.posts = posts;
  }

  /** セッションを扱う。 */
  @GetMapping("/session")
  public ResponseEntity<ModeratorAccess> session(HttpServletRequest request) {
    return privateResponse(access.access(request));
  }

  /** 開始するを扱う。 */
  @GetMapping("/discord/start")
  public ResponseEntity<Void> start(HttpServletRequest request) {
    return redirect(access.start(request));
  }

  /** コールバックを扱う。 */
  @GetMapping("/discord/callback")
  public ResponseEntity<Void> callback(
      HttpServletRequest request,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state) {
    return redirect(access.finish(request, state, code));
  }

  /** logoutを扱う。 */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    access.requireOrigin(request);
    // ログイン状態を保持するセッション。
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  /** postsを扱う。 */
  @GetMapping("/posts")
  public ResponseEntity<List<PostEntry>> posts(
      HttpServletRequest request, @RequestParam(defaultValue = "0") int offset) {
    access.requireModerator(request);
    return privateResponse(posts.list(Math.max(0, offset)));
  }

  /** logsを扱う。 */
  @GetMapping("/logs")
  public ResponseEntity<List<AuditEntry>> logs(
      HttpServletRequest request, @RequestParam(defaultValue = "0") int offset) {
    access.requireOwner(request);
    return privateResponse(log.entries(Math.max(0, offset)));
  }

  /** 更新するの検索のContentsを扱う。 */
  @PutMapping("/search-contents/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateSearchContents(
      HttpServletRequest request,
      @PathVariable String id,
      @RequestBody Map<String, Boolean> change) {
    if (change.get("visible") == null)
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
    access.requireOrigin(request);
    searchContents.change(access.requireOwner(request), id, change.get("visible"));
  }

  /** 非公開のレスポンスを扱う。 */
  private static <T> ResponseEntity<T> privateResponse(T value) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
  }

  /** リダイレクトを扱う。 */
  private static ResponseEntity<Void> redirect(URI location) {
    return ResponseEntity.status(HttpStatus.SEE_OTHER)
        .cacheControl(CacheControl.noStore())
        .header("Referrer-Policy", "no-referrer")
        .location(location)
        .build();
  }
}
