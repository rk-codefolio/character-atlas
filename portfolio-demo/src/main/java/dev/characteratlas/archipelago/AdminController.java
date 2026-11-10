package dev.characteratlas.archipelago;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
class AdminController {
    private final DiscordModerationAccess access;
    private final ModerationLog log;
    private final SearchContentOptions searchContents;
    private final JdbcTemplate jdbc;

    AdminController(DiscordModerationAccess access, ModerationLog log, SearchContentOptions searchContents, JdbcTemplate jdbc) {
        this.access = access;
        this.log = log;
        this.searchContents = searchContents;
        this.jdbc = jdbc;
    }

    @GetMapping("/session")
    ResponseEntity<DiscordModerationAccess.Access> session(HttpServletRequest request) {
        return privateResponse(access.access(request));
    }

    @GetMapping("/discord/start")
    ResponseEntity<Void> start(HttpServletRequest request) {
        return redirect(access.start(request));
    }

    @GetMapping("/discord/callback")
    ResponseEntity<Void> callback(HttpServletRequest request, @RequestParam(required = false) String code,
                                  @RequestParam(required = false) String state) {
        return redirect(access.finish(request, state, code));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        access.requireOrigin(request);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/posts")
    ResponseEntity<List<PostEntry>> posts(HttpServletRequest request, @RequestParam(defaultValue = "0") int offset) {
        access.requireModerator(request);
        return privateResponse(jdbc.query("""
                SELECT p.post_id, p.kind, p.subject_id, COALESCE(c.display_name, p.subject_id),
                       p.title, left(p.body, 120), p.updated_at
                FROM review_post p
                LEFT JOIN catalog_character c ON c.character_id = p.subject_id AND p.kind <> 'CONTENT'
                ORDER BY p.updated_at DESC, p.post_id LIMIT 50 OFFSET ?
                """, (row, index) -> new PostEntry(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
                row.getString(4), row.getString(5), row.getString(6), row.getObject(7, OffsetDateTime.class)), Math.max(0, offset)));
    }

    @GetMapping("/logs")
    ResponseEntity<List<ModerationLog.Entry>> logs(HttpServletRequest request, @RequestParam(defaultValue = "0") int offset) {
        access.requireOwner(request);
        return privateResponse(log.entries(Math.max(0, offset)));
    }

    @PutMapping("/search-contents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateSearchContents(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String, Boolean> change) {
        if (change.get("visible") == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
        searchContents.change(request, id, change.get("visible"));
    }

    private static <T> ResponseEntity<T> privateResponse(T value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").location(location).build();
    }

    record PostEntry(UUID id, String kind, String subject, String subjectName, String title, String excerpt, OffsetDateTime at) {}
}
