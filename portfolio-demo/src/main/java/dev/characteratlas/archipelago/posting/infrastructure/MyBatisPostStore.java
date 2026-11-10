package dev.characteratlas.archipelago.posting.infrastructure;

import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostStore;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class MyBatisPostStore implements PostStore {
    private final PostMapper mapper;
    private final ObjectMapper json;

    MyBatisPostStore(PostMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public List<Post> list(PostKind kind, String subject) {
        return mapper.list(kind.name(), subject).stream().map(row -> new Post(
                row.id(), row.title(), row.body(), row.details(), row.createdAt(), row.updatedAt(),
                List.of(json.readValue(row.relatedNames(), String[].class)))).toList();
    }

    @Override
    public boolean characterExists(String id) { return mapper.characterExists(id); }

    @Override
    public void insert(PostDraft draft) { mapper.insert(draft); }

    @Override
    public Optional<String> replace(PostDraft draft) { return Optional.ofNullable(mapper.replace(draft)); }

    @Override
    public Optional<String> lock(UUID id) { return Optional.ofNullable(mapper.lock(id)); }

    @Override
    public void delete(UUID id) { mapper.delete(id); }
}
