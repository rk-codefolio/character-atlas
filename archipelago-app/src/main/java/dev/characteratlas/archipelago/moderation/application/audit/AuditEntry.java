package dev.characteratlas.archipelago.moderation.application.audit;

import java.time.OffsetDateTime;

/** 監査の記録を表す。 */
public record AuditEntry(
    /* 識別子。 */ long id, /* at。 */
    OffsetDateTime at, /* 操作者。 */
    String actor, /* action。 */
    String action, /* target。 */
    String target, /* 変更前。 */
    String before, /* 変更後。 */
    String after) {}
