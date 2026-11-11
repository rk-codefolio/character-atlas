package dev.characteratlas.archipelago.catalog.infrastructure.persistence;

/** 役割のDB行を表す。 */
public record RoleRow(/* 役割。 */ String role, /* キャラクターのId。 */ String characterId) {}
