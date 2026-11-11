package dev.characteratlas.archipelago.catalog.infrastructure.persistence;

import dev.characteratlas.archipelago.catalog.application.EffectTip;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 選択肢のSQL Mapperを表す。 */
@Mapper
interface OptionsMapper {
  /** rolesを扱う。 */
  @Select("SELECT role, character_id FROM voyager_role ORDER BY role, character_id")
  List<RoleRow> roles();

  /** kindsを扱う。 */
  @Select("SELECT DISTINCT label, kind FROM voyager_effect_tip ORDER BY label, kind")
  List<KindRow> kinds();

  /** advancedを扱う。 */
  @Select("SELECT character_id FROM voyager_growth WHERE advanced ORDER BY character_id")
  List<String> advanced();

  /** effectsを扱う。 */
  @Select(
      "SELECT term_id, label, level, description FROM voyager_effect_tip WHERE term_id = #{id}"
          + " ORDER BY level")
  List<EffectTip> effects(@Param("id") String id);

  /** levelsを扱う。 */
  @Select(
      "SELECT skill_id, level, description FROM voyager_skill_level WHERE character_id = #{id}"
          + " ORDER BY skill_id, level")
  List<LevelRow> levels(@Param("id") String id);

  /** 補足情報を扱う。 */
  @Select("SELECT skill_id, passive, cooldown FROM voyager_skill_meta WHERE character_id = #{id}")
  List<MetaRow> metadata(@Param("id") String id);

  /** 覚醒後を扱う。 */
  @Select("SELECT EXISTS (SELECT 1 FROM voyager_growth WHERE character_id = #{id} AND awakened)")
  boolean awakened(@Param("id") String id);
}
