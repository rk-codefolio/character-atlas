package dev.characteratlas.archipelago.catalog.presentation;

import dev.characteratlas.application.CharacterCatalog;
import dev.characteratlas.application.CharacterCatalog.Direction;
import dev.characteratlas.application.CharacterCatalog.MatchMode;
import dev.characteratlas.domain.CharacterProfile.Stat;
import dev.characteratlas.archipelago.catalog.application.CatalogQueries;
import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
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
    private final CatalogQueries queries;

    CatalogController(CatalogQueries queries) { this.queries = queries; }

    @GetMapping("/characters")
    List<CharacterCatalog.CharacterSummary> characters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Stat stat,
            @RequestParam(defaultValue = "DESC") Direction direction,
            @RequestParam(defaultValue = "true") boolean awakened,
            @RequestParam(defaultValue = "ASC") Direction archiveDirection) {
        return queries.characters(name, stat, direction, awakened, archiveDirection);
    }

    @GetMapping("/characters/{id}")
    dev.characteratlas.domain.CharacterProfile character(@PathVariable String id) {
        return queries.character(id);
    }

    @GetMapping("/characters/{id}/progress")
    CatalogOptions.Progress progress(@PathVariable String id) {
        return queries.progress(id);
    }

    @GetMapping("/terms")
    List<dev.characteratlas.domain.Catalog.SearchTerm> terms(@RequestParam(defaultValue = "false") boolean shared) {
        return queries.terms(shared);
    }

    @GetMapping("/characters/{id}/effects")
    Map<String, List<CatalogOptions.EffectTip>> effects(@PathVariable String id) {
        return queries.effects(id);
    }

    @GetMapping("/search")
    List<CharacterCatalog.SearchResult> search(@RequestParam(required = false) Set<String> term,
                                                @RequestParam(required = false) List<String> group,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) List<String> content,
                                                @RequestParam(defaultValue = "AND") MatchMode mode) {
        return queries.search(term, group, role, content, mode);
    }

}
