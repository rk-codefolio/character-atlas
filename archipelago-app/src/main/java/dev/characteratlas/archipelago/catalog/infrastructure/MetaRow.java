package dev.characteratlas.archipelago.catalog.infrastructure;

/** MetaのDB行を表す。 */
public record MetaRow(
    /* スキルのId。 */ String skillId, /* 自動発動。 */
    boolean passive, /* 再使用までのターン数。 */
    Integer cooldown) {}
