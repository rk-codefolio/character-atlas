package dev.characteratlas.archipelago.posting.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Post(UUID id, String title, String body, String details, OffsetDateTime createdAt,
                   OffsetDateTime updatedAt, List<String> relatedCharacterNames) {}
