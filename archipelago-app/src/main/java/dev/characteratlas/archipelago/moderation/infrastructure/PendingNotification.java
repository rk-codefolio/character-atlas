package dev.characteratlas.archipelago.moderation.infrastructure;

import java.util.UUID;

/** 保留中の通知を表す。 */
public record PendingNotification(/* 識別子。 */ UUID id, /* attempts。 */ int attempts) {}
