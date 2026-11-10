package dev.characteratlas.archipelago.moderation.infrastructure;

import dev.characteratlas.archipelago.moderation.application.ModeratedPosts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcModeratedPosts implements ModeratedPosts {
    private final JdbcTemplate jdbc;

    public JdbcModeratedPosts(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PostEntry> list(int offset) {
        return jdbc.query("""
                SELECT p.post_id, p.kind, p.subject_id, COALESCE(c.display_name, p.subject_id),
                       p.title, left(p.body, 120), p.updated_at
                FROM review_post p
                LEFT JOIN catalog_character c ON c.character_id = p.subject_id AND p.kind <> 'CONTENT'
                ORDER BY p.updated_at DESC, p.post_id LIMIT 50 OFFSET ?
                """, (row, index) -> new PostEntry(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
                row.getString(4), row.getString(5), row.getString(6), row.getObject(7, OffsetDateTime.class)), offset);
    }
}
