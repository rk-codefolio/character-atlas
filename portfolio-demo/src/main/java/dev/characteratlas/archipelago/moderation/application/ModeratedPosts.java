package dev.characteratlas.archipelago.moderation.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ModeratedPosts {
    List<PostEntry> list(int offset);

    public record PostEntry(UUID id, String kind, String subject, String subjectName, String title, String excerpt, OffsetDateTime at) {}
}
