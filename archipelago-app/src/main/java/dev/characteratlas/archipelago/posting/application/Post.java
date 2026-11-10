package dev.characteratlas.archipelago.posting.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 投稿を表す。 */
public record Post(
    /* 識別子。 */ UUID id, /* 投稿タイトル。 */
    String title, /* 投稿本文。 */
    String body, /* 投稿の種類別詳細。 */
    String details, /* createdのAt。 */
    OffsetDateTime createdAt,
    /* updatedのAt。 */ OffsetDateTime updatedAt, /* 関連のキャラクターの名称一覧。 */
    List<String> relatedCharacterNames) {}
