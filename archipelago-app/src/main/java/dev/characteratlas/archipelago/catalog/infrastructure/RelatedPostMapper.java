package dev.characteratlas.archipelago.catalog.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 関連の投稿のSQL Mapperを表す。 */
@Mapper
interface RelatedPostMapper {
  /** taggedを扱う。 */
  @Select(
      """
      SELECT subject_id FROM review_post WHERE kind IN ('COMMENT', 'BUILD') AND jsonb_exists(
          CASE WHEN jsonb_typeof(details->'contentTags') = 'array' THEN details->'contentTags'
               ELSE jsonb_build_array(details->>'contentTag') END, #{contentId})
      UNION
      SELECT related.id FROM review_post, jsonb_array_elements_text(
          CASE WHEN jsonb_typeof(details->'relatedCharacters') = 'array' THEN details->'relatedCharacters'
               ELSE jsonb_build_array(details->>'relatedCharacter') END) AS related(id)
      WHERE kind = 'CONTENT' AND subject_id = #{contentId} AND related.id <> ''
      """)
  List<String> tagged(@Param("contentId") String contentId);
}
