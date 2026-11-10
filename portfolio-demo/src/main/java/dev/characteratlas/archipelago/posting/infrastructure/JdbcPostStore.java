package dev.characteratlas.archipelago.posting.infrastructure;

import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostStore;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPostStore implements PostStore {
    private final JdbcTemplate jdbc;
    public JdbcPostStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Post> list(PostKind kind, String subject) {
        return jdbc.query("""
                SELECT post.post_id, post.title, post.body, post.details::text, post.created_at, post.updated_at,
                       ARRAY(SELECT character.display_name FROM jsonb_array_elements_text(%s) WITH ORDINALITY AS related(id, position)
                             JOIN catalog_character character ON character.character_id = related.id ORDER BY related.position)
                FROM review_post post
                WHERE post.kind = ? AND post.subject_id = ? ORDER BY post.created_at DESC, post.post_id
                """.formatted(PostSelections.RELATED_CHARACTERS), (row, index) -> new Post(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
                row.getString(4), row.getObject(5, OffsetDateTime.class), row.getObject(6, OffsetDateTime.class),
                List.of((String[]) row.getArray(7).getArray())),
                kind.name(), subject);
    }

    @Override
    public boolean characterExists(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM catalog_character WHERE character_id = ?)", Boolean.class, id));
    }

    @Override
    public void insert(PostDraft post) {
        jdbc.update("INSERT INTO review_post (post_id, kind, subject_id, title, body, details) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))",
                post.id(), post.kind().name(), post.subject(), post.title(), post.body(), post.details());
    }

    @Override
    public Optional<String> replace(PostDraft post) {
        return jdbc.query("""
                UPDATE review_post p SET title = ?, body = ?, details = CAST(? AS jsonb), updated_at = now()
                WHERE post_id = ? AND kind = ? AND subject_id = ?
                RETURNING to_jsonb(p)::text
                """, (row, index) -> row.getString(1), post.title(), post.body(), post.details(), post.id(),
                post.kind().name(), post.subject()).stream().findFirst();
    }

    @Override
    public Optional<String> lock(UUID id) {
        return jdbc.query("SELECT to_jsonb(p)::text FROM review_post p WHERE post_id = ? FOR UPDATE",
                (row, index) -> row.getString(1), id).stream().findFirst();
    }

    @Override
    public void delete(UUID id) {
        jdbc.update("DELETE FROM review_post WHERE post_id = ?", id);
    }
}
