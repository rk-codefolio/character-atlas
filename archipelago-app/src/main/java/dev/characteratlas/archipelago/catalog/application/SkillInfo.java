package dev.characteratlas.archipelago.catalog.application;

/** スキルの属性情報を表す。 */
public record SkillInfo(/* 自動発動。 */ Boolean passive, /* 再使用までのターン数。 */ Integer cooldown) {}
