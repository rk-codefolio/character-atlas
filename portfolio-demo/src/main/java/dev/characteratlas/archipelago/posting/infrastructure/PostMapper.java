package dev.characteratlas.archipelago.posting.infrastructure;

import dev.characteratlas.archipelago.posting.domain.PostDraft;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface PostMapper {
    @Select("""
            SELECT post.post_id, post.title, post.body, post.details::text, post.created_at, post.updated_at,
                   array_to_json(ARRAY(
                       SELECT character.display_name
                       FROM jsonb_array_elements_text(
                           CASE WHEN jsonb_typeof(post.details->'relatedCharacters') = 'array'
                                THEN post.details->'relatedCharacters'
                                ELSE jsonb_build_array(post.details->>'relatedCharacter') END
                       ) WITH ORDINALITY AS related(id, position)
                       JOIN catalog_character character ON character.character_id = related.id
                       ORDER BY related.position
                   ))::text
            FROM review_post post WHERE post.kind = #{kind} AND post.subject_id = #{subject}
            ORDER BY post.created_at DESC, post.post_id
            """)
    List<PostRow> list(@Param("kind") String kind, @Param("subject") String subject);

    @Select("SELECT EXISTS (SELECT 1 FROM catalog_character WHERE character_id = #{id})")
    boolean characterExists(@Param("id") String id);

    @Insert("""
            INSERT INTO review_post (post_id, kind, subject_id, title, body, details)
            VALUES (#{id}, #{kind}, #{subject}, #{title}, #{body}, CAST(#{details} AS jsonb))
            """)
    void insert(PostDraft draft);

    @Select(value = """
            UPDATE review_post p SET title = #{title}, body = #{body}, details = CAST(#{details} AS jsonb),
                updated_at = now()
            WHERE post_id = #{id} AND kind = #{kind} AND subject_id = #{subject}
            RETURNING to_jsonb(p)::text
            """, affectData = true)
    String replace(PostDraft draft);

    @Select("SELECT to_jsonb(p)::text FROM review_post p WHERE post_id = #{id} FOR UPDATE")
    String lock(@Param("id") UUID id);

    @Delete("DELETE FROM review_post WHERE post_id = #{id}")
    void delete(@Param("id") UUID id);

    record PostRow(UUID id, String title, String body, String details, OffsetDateTime createdAt,
                   OffsetDateTime updatedAt, String relatedNames) {}
}
