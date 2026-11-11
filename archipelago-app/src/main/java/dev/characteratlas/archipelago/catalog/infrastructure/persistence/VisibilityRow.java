package dev.characteratlas.archipelago.catalog.infrastructure.persistence;

/** 表示設定のDB行を表す。 */
public record VisibilityRow(/* 識別子。 */ String id, /* 検索候補として表示するかどうか。 */ boolean visible) {}
