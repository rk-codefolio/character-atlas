package dev.characteratlas.application;

import java.util.List;
import java.util.Set;

/** 検索の結果を表す。 */
public record SearchResult(
    /* 対象キャラクター。 */ CharacterSummary character, /* スキル単位の一致結果。 */
    List<Hit> hits,
    /* matchedのTerms。 */ Set<String> matchedTerms, /* 関連候補のOnly。 */
    boolean candidateOnly) {}
