package dev.characteratlas.archipelago.catalog.application;

import dev.characteratlas.application.CharacterCatalog;
import dev.characteratlas.application.CharacterCatalog.Direction;
import dev.characteratlas.application.CharacterCatalog.MatchMode;
import dev.characteratlas.domain.CharacterProfile.Stat;
import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.shared.application.UseCaseException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogQueries {
    private static final Set<String> CD_TERMS = Set.of("CD増加", "CD短縮");
    private final CharacterCatalog catalog;
    private final CatalogOptions options;
    private final TaggedPostIndex posts;
    private final SearchContentOptions searchContents;

    public CatalogQueries(CharacterCatalog catalog, CatalogOptions options, TaggedPostIndex posts, SearchContentOptions searchContents) {
        this.catalog = catalog;
        this.options = options;
        this.posts = posts;
        this.searchContents = searchContents;
    }

    public List<CharacterCatalog.CharacterSummary> characters(
            String name,
            Stat stat,
            Direction direction,
            boolean awakened,
            Direction archiveDirection) {
        return catalog.list(name, stat, direction, awakened, archiveDirection);
    }

    public dev.characteratlas.domain.CharacterProfile character(String id) {
        return catalog.detail(id).orElseThrow(() -> UseCaseException.notFound());
    }

    public CatalogOptions.Progress progress(String id) {
        return options.progress(character(id));
    }

    public List<dev.characteratlas.domain.Catalog.SearchTerm> terms(boolean shared) {
        return shared ? catalog.sharedTerms(CD_TERMS) : catalog.terms();
    }

    public Map<String, List<CatalogOptions.EffectTip>> effects(String id) {
        return character(id).skills().stream().collect(Collectors.toMap(skill -> skill.id(), skill -> skill.matches().stream()
                .filter(match -> match.evidence() == dev.characteratlas.domain.CharacterProfile.Evidence.DECLARED_EFFECT)
                .flatMap(match -> options.effects(match.termId()).stream()).distinct().toList()));
    }

    public List<CharacterCatalog.SearchResult> search(Set<String> term,
                                                List<String> group,
                                                String role,
                                                List<String> content,
                                                MatchMode mode) {
        try {
            var groups = new ArrayList<Set<String>>();
            if (term != null) term.forEach(id -> groups.add(Set.of(id)));
            if (group != null) {
                var terms = catalog.terms();
                for (String label : group) {
                    groups.add(terms.stream().filter(entry -> entry.label().equals(label))
                            .map(dev.characteratlas.domain.Catalog.SearchTerm::id).collect(Collectors.toUnmodifiableSet()));
                }
            }
            var results = catalog.searchGroups(groups, mode).stream();
            if (role != null && !role.isBlank()) {
                var members = options.get().roles().get(role);
                if (members == null) throw new IllegalArgumentException("不明な役割があります");
                results = results.filter(result -> members.contains(result.character().id()));
            }
            if (content != null && !content.isEmpty()) {
                var allowed = searchContents.available().stream().map(CatalogOptions.Content::id)
                        .collect(Collectors.toUnmodifiableSet());
                if (!allowed.containsAll(content)) throw new IllegalArgumentException("コンテンツが見つかりません。");
                var tagged = posts.taggedCharacters(content);
                results = results.sorted(Comparator.comparing(CharacterCatalog.SearchResult::candidateOnly)
                        .thenComparing(result -> !tagged.contains(result.character().id())));
            }
            return results.toList();
        } catch (IllegalArgumentException e) {
            throw UseCaseException.invalid(e.getMessage());
        }
    }
}
