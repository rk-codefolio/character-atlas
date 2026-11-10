package dev.characteratlas.archipelago.moderation.presentation;

import dev.characteratlas.archipelago.moderation.infrastructure.DiscordModerationAccess;
import dev.characteratlas.archipelago.moderation.application.AuditLog;
import dev.characteratlas.archipelago.catalog.application.SearchContentOptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import dev.characteratlas.archipelago.moderation.application.ModeratedPosts;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DiscordModerationAccess access;
    private final AuditLog log;
    private final SearchContentOptions searchContents;
    private final ModeratedPosts posts;

    public AdminController(DiscordModerationAccess access, AuditLog log, SearchContentOptions searchContents, ModeratedPosts posts) {
        this.access = access;
        this.log = log;
        this.searchContents = searchContents;
        this.posts = posts;
    }

    @GetMapping("/session")
    public ResponseEntity<DiscordModerationAccess.Access> session(HttpServletRequest request) {
        return privateResponse(access.access(request));
    }

    @GetMapping("/discord/start")
    public ResponseEntity<Void> start(HttpServletRequest request) {
        return redirect(access.start(request));
    }

    @GetMapping("/discord/callback")
    public ResponseEntity<Void> callback(HttpServletRequest request, @RequestParam(required = false) String code,
                                  @RequestParam(required = false) String state) {
        return redirect(access.finish(request, state, code));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        access.requireOrigin(request);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/posts")
    public ResponseEntity<List<ModeratedPosts.PostEntry>> posts(HttpServletRequest request, @RequestParam(defaultValue = "0") int offset) {
        access.requireModerator(request);
        return privateResponse(posts.list(Math.max(0, offset)));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog.Entry>> logs(HttpServletRequest request, @RequestParam(defaultValue = "0") int offset) {
        access.requireOwner(request);
        return privateResponse(log.entries(Math.max(0, offset)));
    }

    @PutMapping("/search-contents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSearchContents(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String, Boolean> change) {
        if (change.get("visible") == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
        access.requireOrigin(request);
        searchContents.change(access.requireOwner(request), id, change.get("visible"));
    }

    private static <T> ResponseEntity<T> privateResponse(T value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").location(location).build();
    }

}
