package dev.characteratlas.application;

import dev.characteratlas.domain.Evidence;

/** 一致を表す。 */
public record Hit(
    /* スキルのId。 */ String skillId, /* スキルの名称。 */
    String skillName, /* 条件のId。 */
    String termId, /* 検索の根拠。 */
    Evidence evidence) {}
