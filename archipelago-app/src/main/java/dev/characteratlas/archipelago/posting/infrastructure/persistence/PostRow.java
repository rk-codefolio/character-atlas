package dev.characteratlas.archipelago.posting.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 投稿のDB行を表す。 */
public record PostRow(
    /* 識別子。 */ UUID id, /* 投稿タイトル。 */
    String title, /* 投稿本文。 */
    String body, /* 投稿の種類別詳細。 */
    String details, /* createdのAt。 */
    OffsetDateTime createdAt,
    /* updatedのAt。 */ OffsetDateTime updatedAt, /* 関連の名称一覧。 */
    String relatedNames) {}
