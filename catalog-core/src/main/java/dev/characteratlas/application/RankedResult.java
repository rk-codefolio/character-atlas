package dev.characteratlas.application;

import dev.characteratlas.domain.CharacterProfile;

/** 順位付け済みの結果を表す。 */
public record RankedResult(
    /* 対象キャラクター。 */ CharacterProfile character, /* 処理結果。 */ SearchResult result) {}
