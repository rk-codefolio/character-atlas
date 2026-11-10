package dev.characteratlas.archipelago.catalog.infrastructure;

/** レベルのDB行を表す。 */
public record LevelRow(
    /* スキルのId。 */ String skillId, /* レベル。 */ int level, /* description。 */ String description) {}
