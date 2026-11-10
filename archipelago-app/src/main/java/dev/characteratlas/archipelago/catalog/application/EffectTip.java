package dev.characteratlas.archipelago.catalog.application;

/** 効果の説明を表す。 */
public record EffectTip(
    /* 識別子。 */ String id, /* 表示名。 */
    String name, /* レベル。 */
    int level, /* description。 */
    String description) {}
