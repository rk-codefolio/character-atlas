package dev.characteratlas.archipelago.posting.application;

import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostStore {
    List<Post> list(PostKind kind, String subject);
    boolean characterExists(String id);
    void insert(PostDraft draft);
    Optional<String> replace(PostDraft draft);
    Optional<String> lock(UUID id);
    void delete(UUID id);
}
