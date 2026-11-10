package dev.characteratlas.archipelago.moderation.application;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 通報を表す。 */
public record Report(
    /* 識別子。 */ UUID id, /* 投稿のId。 */
    UUID postId, /* 通報理由。 */
    String reason, /* 通報の補足。 */
    String note, /* 読み取り時点で固定した図鑑データ。 */
    String snapshot,
    /* createdのAt。 */ OffsetDateTime createdAt, /* resolvedのAt。 */
    OffsetDateTime resolvedAt, /* notifiedのAt。 */
    OffsetDateTime notifiedAt) {}
