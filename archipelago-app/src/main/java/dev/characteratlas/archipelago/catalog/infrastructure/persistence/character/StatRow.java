package dev.characteratlas.archipelago.catalog.infrastructure.persistence.character;

import java.math.BigDecimal;

/** 能力値のDB行を表す。 */
public record StatRow(
    /* キャラクターのId。 */ String characterId, /* 対象の能力値項目。 */
    String stat, /* 検証や変換の対象値。 */
    BigDecimal value) {}
