package dev.characteratlas.domain.character;

import dev.characteratlas.domain.skill.Skill;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 図鑑上のキャラクターの属性、育成値、技の一覧を保持する。
 *
 * <p>通常と最大覚醒後の能力値を別々に保持する。能力値のMapと技の一覧は防御的にコピーする。
 */
public record CharacterProfile(
    /* 識別子。 */ String id, /* 表示名。 */
    String name, /* attribute。 */
    String attribute, /* rarity。 */
    String rarity, /* 図鑑の優先順位。 */
    Integer archivePriority,
    /* 能力値の組み合わせ。 */ Map<Stat, BigDecimal> stats, /* 最大の覚醒後のStats。 */
    Map<Stat, BigDecimal> maxAwakenedStats, /* skills。 */
    List<Skill> skills) {
  /** キャラクターの詳細を初期化する。 */
  public CharacterProfile {
    stats = Map.copyOf(stats);
    maxAwakenedStats = Map.copyOf(maxAwakenedStats);
    skills = List.copyOf(skills);
  }

  /** キャラクターの詳細を初期化する。 */
  public CharacterProfile(
      String id,
      String name,
      String attribute,
      String rarity,
      Integer archivePriority,
      Map<Stat, BigDecimal> stats,
      List<Skill> skills) {
    this(id, name, attribute, rarity, archivePriority, stats, stats, skills);
  }

  /** statsを扱う。 */
  public Map<Stat, BigDecimal> stats(boolean awakened) {
    return awakened ? maxAwakenedStats : stats;
  }
}
