package dev.characteratlas.domain;

/** 一致を表す。 */
public record Match(/* 条件のId。 */ String termId, /* 検索の根拠。 */ Evidence evidence) {}
