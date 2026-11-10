package dev.characteratlas.application;

import dev.characteratlas.domain.Catalog.SearchTerm;
import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.domain.CharacterProfile.Evidence;
import dev.characteratlas.domain.CharacterProfile.Stat;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CharacterCatalog {
    private static final Comparator<CharacterProfile> DEFAULT_ORDER =
            Comparator.comparing(CharacterProfile::archivePriority, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(CharacterProfile::id);

    private static final Comparator<RankedResult> SEARCH_ORDER =
            Comparator.comparing((RankedResult row) -> row.result().candidateOnly())
                    .thenComparing(RankedResult::character, DEFAULT_ORDER);

    private final CatalogRepository repository;

    public CharacterCatalog(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<SearchTerm> terms() {
        return repository.load().terms();
    }

    public List<SearchTerm> sharedTerms(Set<String> additionalTags) {
        var snapshot = repository.load();
        var terms = snapshot.terms().stream()
                .filter(term -> term.kind() == SearchTerm.Kind.EFFECT || additionalTags.contains(term.label())).toList();
        var byId = terms.stream().collect(Collectors.toMap(SearchTerm::id, SearchTerm::label));
        var owners = new java.util.HashMap<String, Set<String>>();
        for (var character : snapshot.characters()) {
            for (var skill : character.skills()) {
                for (var match : skill.matches()) {
                    String label = byId.get(match.termId());
                    if (label != null && !match.evidence().isCandidate()) {
                        owners.computeIfAbsent(label, ignored -> new java.util.HashSet<>()).add(character.id());
                    }
                }
            }
        }
        return terms.stream().filter(term -> owners.getOrDefault(term.label(), Set.of()).size() >= 2).toList();
    }

    public Optional<CharacterProfile> detail(String id) {
        return repository.load().characters().stream().filter(character -> character.id().equals(id)).findFirst();
    }

    public List<CharacterSummary> list(String name, Stat stat, Direction direction) {
        return list(name, stat, direction, false);
    }

    public List<CharacterSummary> list(String name, Stat stat, Direction direction, boolean awakened) {
        return list(name, stat, direction, awakened, Direction.ASC);
    }

    public List<CharacterSummary> list(String name, Stat stat, Direction direction, boolean awakened, Direction archiveDirection) {
        String query = normalized(name);
        Comparator<CharacterProfile> order = archiveDirection == Direction.DESC
                ? Comparator.comparing(CharacterProfile::archivePriority, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CharacterProfile::id)
                : DEFAULT_ORDER;
        if (stat != null) {
            Comparator<BigDecimal> values = direction == Direction.DESC
                    ? Comparator.reverseOrder() : Comparator.naturalOrder();
            order = Comparator.comparing(
                    (CharacterProfile character) -> character.stats(awakened).get(stat),
                    Comparator.nullsLast(values)
            ).thenComparing(DEFAULT_ORDER);
        }
        return repository.load().characters().stream()
                .filter(character -> normalized(character.name()).contains(query))
                .sorted(order)
                .map(character -> CharacterSummary.from(character, awakened))
                .toList();
    }

    public List<SearchResult> search(Set<String> terms, MatchMode mode) {
        return searchGroups(terms.stream().map(Set::of).toList(), mode);
    }

    public List<SearchResult> searchGroups(List<Set<String>> groups, MatchMode mode) {
        var catalog = repository.load();
        var terms = groups.stream().flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
        var knownTerms = catalog.terms().stream().map(SearchTerm::id).collect(Collectors.toSet());
        if (groups.stream().anyMatch(Set::isEmpty) || !knownTerms.containsAll(terms)) {
            throw new IllegalArgumentException("不明な検索条件があります");
        }
        return catalog.characters().stream()
                .map(character -> new RankedResult(character, result(character, terms, groups, mode)))
                .filter(row -> matches(groups, row.result().matchedTerms(), mode))
                .sorted(SEARCH_ORDER)
                .map(RankedResult::result)
                .toList();
    }

    private static SearchResult result(CharacterProfile character, Set<String> terms, List<Set<String>> groups, MatchMode mode) {
        var hits = character.skills().stream().flatMap(skill -> skill.matches().stream()
                .filter(match -> terms.contains(match.termId()))
                .map(match -> new Hit(skill.id(), skill.name(), match.termId(), match.evidence())))
                .toList();
        var matched = hits.stream().map(Hit::termId).collect(Collectors.toUnmodifiableSet());
        var declared = hits.stream().filter(hit -> !hit.evidence().isCandidate())
                .map(Hit::termId).collect(Collectors.toUnmodifiableSet());
        boolean candidate = !matched.isEmpty() && !matches(groups, declared, mode);
        return new SearchResult(CharacterSummary.from(character), hits, matched, candidate);
    }

    private static boolean matches(List<Set<String>> groups, Set<String> matches, MatchMode mode) {
        if (groups.isEmpty()) return true;
        return mode == MatchMode.AND
                ? groups.stream().allMatch(group -> group.stream().anyMatch(matches::contains))
                : groups.stream().anyMatch(group -> group.stream().anyMatch(matches::contains));
    }

    private static String normalized(String value) {
        return value == null ? "" : Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    private record RankedResult(CharacterProfile character, SearchResult result) {}

    public enum Direction { ASC, DESC }
    public enum MatchMode { AND, OR }

    public record CharacterSummary(String id, String name, String attribute, String rarity,
                                   Map<Stat, BigDecimal> stats) {
        static CharacterSummary from(CharacterProfile profile) {
            return from(profile, false);
        }

        static CharacterSummary from(CharacterProfile profile, boolean awakened) {
            return new CharacterSummary(profile.id(), profile.name(), profile.attribute(),
                    profile.rarity(), profile.stats(awakened));
        }
    }

    public record Hit(String skillId, String skillName, String termId, Evidence evidence) {}

    public record SearchResult(CharacterSummary character, List<Hit> hits,
                               Set<String> matchedTerms, boolean candidateOnly) {}
}
