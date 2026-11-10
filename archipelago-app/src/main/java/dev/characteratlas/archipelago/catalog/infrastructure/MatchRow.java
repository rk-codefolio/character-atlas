package dev.characteratlas.archipelago.catalog.infrastructure;

/** 一致のDB行を表す。 */
public record MatchRow(
    /* キャラクターのId。 */ String characterId, /* スキルのId。 */
    String skillId, /* 条件のId。 */
    String termId, /* 検索の根拠。 */
    String evidence) {}
