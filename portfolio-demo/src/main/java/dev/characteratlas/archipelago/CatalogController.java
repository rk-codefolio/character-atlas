package dev.characteratlas.archipelago;

import dev.characteratlas.application.CharacterCatalog;
import dev.characteratlas.application.CharacterCatalog.Direction;
import dev.characteratlas.application.CharacterCatalog.MatchMode;
import dev.characteratlas.domain.Catalog.SearchTerm;
import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.domain.CharacterProfile.Stat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
class CatalogController {
    private final CharacterCatalog catalog;

    CatalogController(CharacterCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/characters")
    List<CharacterCatalog.CharacterSummary> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Stat stat,
            @RequestParam(defaultValue = "ASC") Direction direction) {
        return catalog.list(name, stat, direction, false, direction);
    }

    @GetMapping("/characters/{id}")
    CharacterProfile detail(@PathVariable String id) {
        return catalog.detail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/terms")
    List<SearchTerm> terms() {
        return catalog.terms();
    }

    @GetMapping("/search")
    List<CharacterCatalog.SearchResult> search(
            @RequestParam(defaultValue = "AND") MatchMode mode,
            @RequestParam(required = false) Set<String> term) {
        try {
            return catalog.search(term == null ? Set.of() : term, mode);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
    }
}
