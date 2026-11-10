package dev.characteratlas.archipelago.moderation.infrastructure;

import dev.characteratlas.archipelago.moderation.application.ReportStore;
import dev.characteratlas.archipelago.shared.application.UseCaseException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcReportStore implements ReportStore {
    private final JdbcTemplate jdbc;

    public JdbcReportStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void add(UUID id, String reason, String note) {
        int inserted = jdbc.update("""
                INSERT INTO post_report (report_id, post_id, reason, note, post_snapshot)
                SELECT ?, p.post_id, ?, ?, jsonb_build_object('kind', p.kind, 'subject', p.subject_id,
                    'title', p.title, 'body', p.body, 'details', p.details)
                FROM review_post p WHERE p.post_id = ?
                ON CONFLICT (post_id) WHERE resolved_at IS NULL DO NOTHING
                """, UUID.randomUUID(), reason, note, id);
        if (inserted == 0 && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM review_post WHERE post_id = ?)", Boolean.class, id))) {
            throw UseCaseException.notFound();
        }
    }

    public List<Report> list(UUID report, int offset) {
        return jdbc.query("""
                SELECT report_id, post_id, reason, note, post_snapshot::text, created_at, resolved_at, notified_at
                FROM post_report WHERE CAST(? AS uuid) IS NULL OR report_id = ?
                ORDER BY (resolved_at IS NOT NULL), created_at DESC, report_id LIMIT 50 OFFSET ?
                """, (row, index) -> new Report(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
                row.getString(3), row.getString(4), row.getString(5), row.getObject(6, OffsetDateTime.class),
                row.getObject(7, OffsetDateTime.class), row.getObject(8, OffsetDateTime.class)),
                report, report, offset);
    }

    public boolean resolve(UUID id) {
        return jdbc.update("UPDATE post_report SET resolved_at = now() WHERE report_id = ? AND resolved_at IS NULL", id) > 0;
    }
}
