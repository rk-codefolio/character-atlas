package dev.characteratlas.archipelago;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
class ReviewPostIndex {
    private final JdbcTemplate jdbc;
    private final SearchContentOptions options;

    ReviewPostIndex(JdbcTemplate jdbc, SearchContentOptions options) {
        this.jdbc = jdbc;
        this.options = options;
    }

    Set<String> taggedCharacters(List<String> contentIds) {
        var ids = new HashSet<String>();
        var allowed = options.available().stream().map(ReviewOptions.Content::id).collect(java.util.stream.Collectors.toSet());
        for (String contentId : new HashSet<>(contentIds)) {
            if (!allowed.contains(contentId)) {
                throw new IllegalArgumentException("コンテンツが見つかりません。");
            }
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
