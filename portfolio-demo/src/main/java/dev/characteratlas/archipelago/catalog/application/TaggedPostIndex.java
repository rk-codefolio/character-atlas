package dev.characteratlas.archipelago.catalog.application;

import java.util.List;
import java.util.Set;

public interface TaggedPostIndex {
    Set<String> taggedCharacters(List<String> contentIds);
}
