package dev.characteratlas.archipelago.catalog.infrastructure;

/** 役割のDB行を表す。 */
public record RoleRow(/* 役割。 */ String role, /* キャラクターのId。 */ String characterId) {}
