package dev.characteratlas.archipelago.catalog.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 図鑑のSQL Mapperを表す。 */
@Mapper
interface CatalogMapper {
  /** revisionを扱う。 */
  @Select("SELECT revision FROM catalog_snapshot")
  String revision();

  /** termsを扱う。 */
  @Select("SELECT term_id, label, kind FROM catalog_term ORDER BY kind, label, term_id")
  List<TermRow> terms();

  /** statsを扱う。 */
  @Select("SELECT character_id, stat, value FROM catalog_stat")
  List<StatRow> stats();

  /** 覚醒後のStatsを扱う。 */
  @Select("SELECT character_id, stat, value FROM catalog_awakened_stat")
  List<StatRow> awakenedStats();

  /** matchesを扱う。 */
  @Select(
      "SELECT character_id, skill_id, term_id, evidence FROM catalog_skill_match ORDER BY term_id")
  List<MatchRow> matches();

  /** skillsを扱う。 */
  @Select(
      """
      SELECT character_id, skill_id, display_name, numeric_level, description, text_origin
      FROM catalog_skill ORDER BY character_id, position
      """)
  List<SkillRow> skills();

  /** charactersを扱う。 */
  @Select(
      """
      SELECT character_id, display_name, attribute, rarity, archive_priority
      FROM catalog_character ORDER BY character_id
      """)
  List<CharacterRow> characters();
}
