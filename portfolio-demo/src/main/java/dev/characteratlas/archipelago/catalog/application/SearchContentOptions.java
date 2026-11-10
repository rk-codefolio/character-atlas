package dev.characteratlas.archipelago.catalog.application;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.moderation.application.AuditLog;

import dev.characteratlas.archipelago.shared.application.UseCaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchContentOptions {
    private final SearchContentOverrides overrides;
    private final CatalogOptions options;
    private final AuditLog log;

    public SearchContentOptions(SearchContentOverrides overrides, CatalogOptions options, AuditLog log) {
        this.overrides = overrides;
        this.options = options;
        this.log = log;
    }

    public List<CatalogOptions.Content> available() {
        var visibility = overrides.load();
        return options.contents().stream()
                .filter(content -> visibility.getOrDefault(content.id(), options.isSearchable(content.id()))).toList();
    }

    @Transactional
    public void change(String actor, String id, boolean visible) {
        if (options.contents().stream().noneMatch(content -> content.id().equals(id))) {
            throw UseCaseException.notFound();
        }
        overrides.lock();
        boolean before = available().stream().anyMatch(content -> content.id().equals(id));
        if (before == visible) return;
        overrides.save(id, visible);
        log.append(actor, "SEARCH_CONTENT", id, Boolean.toString(before), Boolean.toString(visible));
    }
}
