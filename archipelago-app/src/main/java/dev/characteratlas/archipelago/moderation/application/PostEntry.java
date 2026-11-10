package dev.characteratlas.archipelago.moderation.application;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 投稿の記録を表す。 */
public record PostEntry(
    /* 識別子。 */ UUID id, /* 種別。 */
    String kind, /* 投稿先の識別子。 */
    String subject, /* subjectの名称。 */
    String subjectName, /* 投稿タイトル。 */
    String title, /* excerpt。 */
    String excerpt, /* at。 */
    OffsetDateTime at) {}
