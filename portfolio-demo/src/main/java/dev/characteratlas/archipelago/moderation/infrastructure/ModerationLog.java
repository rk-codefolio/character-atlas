package dev.characteratlas.archipelago.moderation.infrastructure;

import dev.characteratlas.archipelago.moderation.application.AuditLog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ModerationLog implements AuditLog {
    private final JdbcTemplate jdbc;

    public ModerationLog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(String actor, String action, String target, String before, String after) {
        jdbc.update("""
                INSERT INTO moderation_log (actor_id, action, target_id, before_value, after_value)
                VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
                """, actor, action, target, before, after);
    }

    public List<Entry> entries(int offset) {
        return jdbc.query("""
                SELECT log_id, recorded_at, actor_id, action, target_id, before_value::text, after_value::text
                FROM moderation_log ORDER BY log_id DESC LIMIT 50 OFFSET ?
                """, (row, index) -> new Entry(row.getLong(1), row.getObject(2, OffsetDateTime.class), row.getString(3),
                row.getString(4), row.getString(5), row.getString(6), row.getString(7)), offset);
    }

}
