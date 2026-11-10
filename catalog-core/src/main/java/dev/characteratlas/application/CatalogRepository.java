package dev.characteratlas.application;

import dev.characteratlas.domain.Catalog;

@FunctionalInterface
public interface CatalogRepository {
    Catalog load();
}
