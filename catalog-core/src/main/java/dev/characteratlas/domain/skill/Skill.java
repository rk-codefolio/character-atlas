package dev.characteratlas.domain.skill;

import java.util.List;

/**
 * 技の数値レベル、説明文の出所、効果の検索根拠を保持する。
 *
 * <p>説明文の出所と検索根拠は別の情報として保持し、検索時の明示一致と関連候補の判定に使用する。
 */
public record Skill(
    /* 識別子。 */ String id, /* 表示名。 */
    String name, /* レベル。 */
    int level, /* description。 */
    String description,
    /* 説明文の由来。 */ TextOrigin textOrigin, /* matches。 */
    List<Match> matches) {
  /** スキルを初期化する。 */
  public Skill {
    matches = List.copyOf(matches);
  }
}
