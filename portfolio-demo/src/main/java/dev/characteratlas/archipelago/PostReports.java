package dev.characteratlas.archipelago;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class PostReports {
    private final JdbcTemplate jdbc;
    private final DiscordModerationAccess access;
    private final ModerationLog log;

    PostReports(JdbcTemplate jdbc, DiscordModerationAccess access, ModerationLog log) {
        this.jdbc = jdbc;
        this.access = access;
        this.log = log;
    }

    @PostMapping("/posts/{id}/reports")
    ResponseEntity<Void> report(HttpServletRequest request, @PathVariable UUID id, @RequestBody Submission submission) {
        access.requireOrigin(request);
        String note = submission.note() == null ? "" : submission.note().strip();
        if (submission.reason() == null || note.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        int inserted = jdbc.update("""
                INSERT INTO post_report (report_id, post_id, reason, note, post_snapshot)
                SELECT ?, p.post_id, ?, ?, jsonb_build_object('kind', p.kind, 'subject', p.subject_id,
                    'title', p.title, 'body', p.body)
                FROM review_post p WHERE p.post_id = ?
                ON CONFLICT (post_id) WHERE resolved_at IS NULL DO NOTHING
                """, UUID.randomUUID(), submission.reason().name(), note, id);
        if (inserted == 0 && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM review_post WHERE post_id = ?)", Boolean.class, id))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/admin/reports")
    ResponseEntity<List<Report>> reports(HttpServletRequest request,
                                       @RequestParam(required = false) UUID report,
                                       @RequestParam(defaultValue = "0") int offset) {
        access.requireModerator(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(jdbc.query("""
                SELECT report_id, post_id, reason, note, post_snapshot::text, created_at, resolved_at, notified_at
                FROM post_report WHERE CAST(? AS uuid) IS NULL OR report_id = ?
                ORDER BY (resolved_at IS NOT NULL), created_at DESC, report_id LIMIT 50 OFFSET ?
                """, (row, index) -> new Report(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
                row.getString(3), row.getString(4), row.getString(5), row.getObject(6, OffsetDateTime.class),
                row.getObject(7, OffsetDateTime.class), row.getObject(8, OffsetDateTime.class)),
                report, report, Math.max(0, offset)));
    }

    @PutMapping("/admin/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void resolve(HttpServletRequest request, @PathVariable UUID id) {
        access.requireOrigin(request);
        String actor = access.requireModerator(request);
        if (jdbc.update("UPDATE post_report SET resolved_at = now() WHERE report_id = ? AND resolved_at IS NULL", id) > 0) {
            log.append(actor, "RESOLVE_REPORT", id.toString(), "{\"resolved\":false}", "{\"resolved\":true}");
        }
    }

    enum Reason { SPAM, ABUSE, OTHER }
    record Submission(Reason reason, String note) {}
    record Report(UUID id, UUID postId, String reason, String note, String snapshot,
                  OffsetDateTime createdAt, OffsetDateTime resolvedAt, OffsetDateTime notifiedAt) {}
}
