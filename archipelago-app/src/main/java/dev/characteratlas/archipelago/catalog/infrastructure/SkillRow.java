package dev.characteratlas.archipelago.catalog.infrastructure;

/** スキルのDB行を表す。 */
public record SkillRow(
    /* キャラクターのId。 */ String characterId, /* スキルのId。 */
    String skillId, /* 表示名。 */
    String name, /* レベル。 */
    int level, /* description。 */
    String description, /* 許可する画面のオリジン。 */
    String origin) {}
