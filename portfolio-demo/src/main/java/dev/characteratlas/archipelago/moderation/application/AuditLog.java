package dev.characteratlas.archipelago.moderation.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface AuditLog {
    void append(String actor, String action, String target, String before, String after);
    List<Entry> entries(int offset);

    record Entry(long id, OffsetDateTime at, String actor, String action, String target, String before, String after) {}
}
