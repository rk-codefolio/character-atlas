package dev.characteratlas.archipelago.posting.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 投稿のSQL Mapperを表す。 */
@Mapper
interface PostMapper {
  /** 指定された条件に沿って一覧を取得する。 */
  List<PostRow> list(@Param("kind") String kind, @Param("subject") String subject);

  /** キャラクターの存在するか確認するを扱う。 */
  @Select("SELECT EXISTS (SELECT 1 FROM catalog_character WHERE character_id = #{id})")
  boolean characterExists(@Param("id") String id);

  /** 登録するを扱う。 */
  @Insert(
      """
      INSERT INTO review_post (post_id, kind, subject_id, title, body, details)
      VALUES (#{id}, #{kind}, #{subject}, #{title}, #{body}, CAST(#{details} AS jsonb))
      """)
  void insert(PostWrite draft);

  /** 置き換えるを扱う。 */
  @Select(
      value =
          """
          UPDATE review_post p SET title = #{title}, body = #{body}, details = CAST(#{details} AS jsonb),
              updated_at = now()
          WHERE post_id = #{id} AND kind = #{kind} AND subject_id = #{subject}
          RETURNING to_jsonb(p)::text
          """,
      affectData = true)
  String replace(PostWrite draft);

  /** ロックするを扱う。 */
  @Select("SELECT to_jsonb(p)::text FROM review_post p WHERE post_id = #{id} FOR UPDATE")
  String lock(@Param("id") UUID id);

  /** 削除するを扱う。 */
  @Delete("DELETE FROM review_post WHERE post_id = #{id}")
  void delete(@Param("id") UUID id);
}
