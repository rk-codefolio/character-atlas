package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.TaggedPostIndex;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.posting.infrastructure.PostSelections;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class ReviewPostIndex implements TaggedPostIndex {
    private final JdbcTemplate jdbc;

    public ReviewPostIndex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Set<String> taggedCharacters(List<String> contentIds) {
        var ids = new HashSet<String>();
        for (String contentId : new HashSet<>(contentIds)) {
            ids.addAll(jdbc.queryForList("""
                    SELECT subject_id FROM review_post WHERE kind IN ('COMMENT', 'BUILD') AND jsonb_exists(%s, ?)
                    UNION
                    SELECT related.id FROM review_post, jsonb_array_elements_text(%s) AS related(id)
                    WHERE kind = 'CONTENT' AND subject_id = ? AND related.id <> ''
                    """.formatted(PostSelections.CONTENT_TAGS, PostSelections.RELATED_CHARACTERS), String.class, contentId, contentId));
        }
        return ids;
    }

}
