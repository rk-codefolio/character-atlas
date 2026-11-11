package dev.characteratlas.archipelago.catalog.infrastructure.persistence;

/** スキルのキーを表す。 */
public record SkillKey(/* キャラクターのId。 */ String characterId, /* スキルのId。 */ String skillId) {}
