package dev.characteratlas.archipelago.catalog.application;

import java.util.Map;

public interface SearchContentOverrides {
    Map<String, Boolean> load();
    void lock();
    void save(String id, boolean visible);
}
