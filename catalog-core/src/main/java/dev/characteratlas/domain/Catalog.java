package dev.characteratlas.domain;

import java.util.List;

public record Catalog(String snapshot, List<CharacterProfile> characters, List<SearchTerm> terms) {
    public Catalog {
        characters = List.copyOf(characters);
        terms = List.copyOf(terms);
    }

    public record SearchTerm(String id, String label, Kind kind) {
        public enum Kind { EFFECT, TAG }
    }
}
