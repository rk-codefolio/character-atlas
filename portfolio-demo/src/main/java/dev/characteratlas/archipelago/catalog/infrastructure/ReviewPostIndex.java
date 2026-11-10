package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.TaggedPostIndex;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewPostIndex implements TaggedPostIndex {
    private final RelatedPostMapper mapper;

    ReviewPostIndex(RelatedPostMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Set<String> taggedCharacters(List<String> contentIds) {
        return contentIds.stream().distinct().flatMap(id -> mapper.tagged(id).stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
