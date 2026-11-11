package dev.characteratlas.domain.skill;

/** 一致を表す。 */
public record Match(/* 条件のId。 */ String termId, /* 検索の根拠。 */ Evidence evidence) {}
