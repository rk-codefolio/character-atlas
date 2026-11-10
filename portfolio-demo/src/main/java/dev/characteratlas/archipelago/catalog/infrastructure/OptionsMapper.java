package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions.EffectTip;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface OptionsMapper {
    @Select("SELECT role, character_id FROM voyager_role ORDER BY role, character_id")
    List<RoleRow> roles();

    @Select("SELECT DISTINCT label, kind FROM voyager_effect_tip ORDER BY label, kind")
    List<KindRow> kinds();

    @Select("SELECT character_id FROM voyager_growth WHERE advanced ORDER BY character_id")
    List<String> advanced();

    @Select("SELECT term_id, label, level, description FROM voyager_effect_tip WHERE term_id = #{id} ORDER BY level")
    List<EffectTip> effects(@Param("id") String id);

    @Select("SELECT skill_id, level, description FROM voyager_skill_level WHERE character_id = #{id} ORDER BY skill_id, level")
    List<LevelRow> levels(@Param("id") String id);

    @Select("SELECT skill_id, passive, cooldown FROM voyager_skill_meta WHERE character_id = #{id}")
    List<MetaRow> metadata(@Param("id") String id);

    @Select("SELECT EXISTS (SELECT 1 FROM voyager_growth WHERE character_id = #{id} AND awakened)")
    boolean awakened(@Param("id") String id);

    record RoleRow(String role, String characterId) {}
    record KindRow(String label, String kind) {}
    record LevelRow(String skillId, int level, String description) {}
    record MetaRow(String skillId, boolean passive, Integer cooldown) {}
}
