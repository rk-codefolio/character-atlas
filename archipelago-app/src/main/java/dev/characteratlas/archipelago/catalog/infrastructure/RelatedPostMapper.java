package dev.characteratlas.archipelago.catalog.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 関連の投稿のSQL Mapperを表す。 */
@Mapper
interface RelatedPostMapper {
  /** taggedを扱う。 */
  List<String> tagged(@Param("contentId") String contentId);
}
