package dev.characteratlas.archipelago.catalog.infrastructure.persistence;

/** キャラクターのDB行を表す。 */
public record CharacterRow(
    /* 識別子。 */ String id, /* 表示名。 */
    String name, /* attribute。 */
    String attribute, /* rarity。 */
    String rarity, /* 優先順位。 */
    Integer priority) {}
