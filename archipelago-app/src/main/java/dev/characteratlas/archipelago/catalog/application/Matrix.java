package dev.characteratlas.archipelago.catalog.application;

import java.util.List;

/** 航路紋を表す。 */
public record Matrix(/* 表示名。 */ String name, /* thresholds。 */ List<Integer> thresholds) {}
