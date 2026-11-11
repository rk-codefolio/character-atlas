package dev.characteratlas.archipelago.catalog.infrastructure.persistence.visibility;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 検索のコンテンツのSQL Mapperを表す。 */
@Mapper
interface SearchContentMapper {
  /** 永続化されたデータから読み取りモデルを組み立てる。 */
  @Select("SELECT content_id, visible FROM search_content_override")
  List<VisibilityRow> load();

  /** ロックするを扱う。 */
  @Update("LOCK TABLE search_content_override IN SHARE ROW EXCLUSIVE MODE")
  void lock();

  /** 保存するを扱う。 */
  @Update(
      """
      INSERT INTO search_content_override (content_id, visible) VALUES (#{id}, #{visible})
      ON CONFLICT (content_id) DO UPDATE SET visible = EXCLUDED.visible
      """)
  void save(@Param("id") String id, @Param("visible") boolean visible);
}
