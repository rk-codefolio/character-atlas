package dev.characteratlas.application;

import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.domain.Stat;
import java.math.BigDecimal;
import java.util.Map;

/** キャラクターの概要を表す。 */
public record CharacterSummary(
    /* 識別子。 */ String id, /* 表示名。 */
    String name, /* attribute。 */
    String attribute, /* rarity。 */
    String rarity,
    /* 能力値の組み合わせ。 */ Map<Stat, BigDecimal> stats) {
  /** 変換するを扱う。 */
  static CharacterSummary from(CharacterProfile profile) {
    return from(profile, false);
  }

  /** 変換するを扱う。 */
  static CharacterSummary from(CharacterProfile profile, boolean awakened) {
    return new CharacterSummary(
        profile.id(),
        profile.name(),
        profile.attribute(),
        profile.rarity(),
        profile.stats(awakened));
  }
}
