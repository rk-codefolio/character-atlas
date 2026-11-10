package dev.characteratlas.domain;

/** 検索の条件を表す。 */
public record SearchTerm(
    /* 識別子。 */ String id, /* 画面に表示する名称。 */ String label, /* 種別。 */ SearchTermKind kind) {}
