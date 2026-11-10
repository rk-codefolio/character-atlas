package dev.characteratlas.archipelago.catalog.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
interface CatalogMapper {
    @Select("SELECT revision FROM catalog_snapshot")
    String revision();

    @Select("SELECT term_id, label, kind FROM catalog_term ORDER BY kind, label, term_id")
    List<TermRow> terms();

    @Select("SELECT character_id, stat, value FROM catalog_stat")
    List<StatRow> stats();

    @Select("SELECT character_id, stat, value FROM catalog_awakened_stat")
    List<StatRow> awakenedStats();

    @Select("SELECT character_id, skill_id, term_id, evidence FROM catalog_skill_match ORDER BY term_id")
    List<MatchRow> matches();

    @Select("""
            SELECT character_id, skill_id, display_name, numeric_level, description, text_origin
            FROM catalog_skill ORDER BY character_id, position
            """)
    List<SkillRow> skills();

    @Select("""
            SELECT character_id, display_name, attribute, rarity, archive_priority
            FROM catalog_character ORDER BY character_id
            """)
    List<CharacterRow> characters();

    record TermRow(String id, String label, String kind) {}
    record StatRow(String characterId, String stat, BigDecimal value) {}
    record MatchRow(String characterId, String skillId, String termId, String evidence) {}
    record SkillRow(String characterId, String skillId, String name, int level, String description, String origin) {}
    record CharacterRow(String id, String name, String attribute, String rarity, Integer priority) {}
}
