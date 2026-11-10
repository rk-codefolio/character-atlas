package dev.characteratlas.domain;

import java.util.List;

/** スキルを表す。 */
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
