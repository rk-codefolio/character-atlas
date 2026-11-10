package dev.characteratlas.archipelago;

import dev.characteratlas.application.CharacterCatalog;
import dev.characteratlas.application.CharacterCatalog.Direction;
import dev.characteratlas.application.CharacterCatalog.MatchMode;
import dev.characteratlas.domain.CharacterProfile.Stat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
class CatalogController {
    private static final Set<String> CD_TERMS = Set.of("CD増加", "CD短縮");
    private final CharacterCatalog catalog;
    private final ReviewOptions options;
    private final ReviewPostIndex posts;

    CatalogController(CharacterCatalog catalog, ReviewOptions options, ReviewPostIndex posts) {
        this.catalog = catalog;
        this.options = options;
        this.posts = posts;
    }

    @GetMapping("/characters")
    List<CharacterCatalog.CharacterSummary> characters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Stat stat,
            @RequestParam(defaultValue = "DESC") Direction direction,
            @RequestParam(defaultValue = "true") boolean awakened,
            @RequestParam(defaultValue = "ASC") Direction archiveDirection) {
        return catalog.list(name, stat, direction, awakened, archiveDirection);
    }

    @GetMapping("/characters/{id}")
    dev.characteratlas.domain.CharacterProfile character(@PathVariable String id) {
        return catalog.detail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/characters/{id}/progress")
    ReviewOptions.Progress progress(@PathVariable String id) {
        return options.progress(character(id));
    }

    @GetMapping("/terms")
    List<dev.characteratlas.domain.Catalog.SearchTerm> terms(@RequestParam(defaultValue = "false") boolean shared) {
        return shared ? catalog.sharedTerms(CD_TERMS) : catalog.terms();
    }

    @GetMapping("/characters/{id}/effects")
    Map<String, List<ReviewOptions.EffectTip>> effects(@PathVariable String id) {
        return character(id).skills().stream().collect(Collectors.toMap(skill -> skill.id(), skill -> skill.matches().stream()
                .filter(match -> match.evidence() == dev.characteratlas.domain.CharacterProfile.Evidence.DECLARED_EFFECT)
                .flatMap(match -> options.effects(match.termId()).stream()).distinct().toList()));
    }

    @GetMapping("/search")
    List<CharacterCatalog.SearchResult> search(@RequestParam(required = false) Set<String> term,
                                                @RequestParam(required = false) List<String> group,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) List<String> content,
                                                @RequestParam(defaultValue = "AND") MatchMode mode) {
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
                var tagged = posts.taggedCharacters(content);
                results = results.sorted(Comparator.comparing(CharacterCatalog.SearchResult::candidateOnly)
                        .thenComparing(result -> !tagged.contains(result.character().id())));
            }
            return results.toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
