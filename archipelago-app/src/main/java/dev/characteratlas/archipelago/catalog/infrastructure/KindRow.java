package dev.characteratlas.archipelago.catalog.infrastructure;

/** 種別のDB行を表す。 */
public record KindRow(/* 画面に表示する名称。 */ String label, /* 種別。 */ String kind) {}
