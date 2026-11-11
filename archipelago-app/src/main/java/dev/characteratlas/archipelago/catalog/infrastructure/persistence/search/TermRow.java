package dev.characteratlas.archipelago.catalog.infrastructure.persistence.search;

/** 条件のDB行を表す。 */
public record TermRow(/* 識別子。 */ String id, /* 画面に表示する名称。 */ String label, /* 種別。 */ String kind) {}
