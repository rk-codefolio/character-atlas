package dev.characteratlas.archipelago.moderation.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReportStore {
    void add(UUID id, String reason, String note);
    List<Report> list(UUID report, int offset);
    boolean resolve(UUID id);

    record Report(UUID id, UUID postId, String reason, String note, String snapshot,
                  OffsetDateTime createdAt, OffsetDateTime resolvedAt, OffsetDateTime notifiedAt) {}
}
