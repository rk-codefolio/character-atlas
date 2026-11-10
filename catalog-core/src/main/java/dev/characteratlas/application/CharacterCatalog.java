package dev.characteratlas.application;

import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.domain.SearchTerm;
import dev.characteratlas.domain.SearchTermKind;
import dev.characteratlas.domain.Stat;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** キャラクターの図鑑を表す。 */
public final class CharacterCatalog {
  /** 順位のない項目を末尾に置く図鑑順。 */
  private static final Comparator<CharacterProfile> DEFAULT_ORDER =
      Comparator.comparing(
              CharacterProfile::archivePriority, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(CharacterProfile::id);

  /** 関連候補を明示一致の後に置く検索順。 */
  private static final Comparator<RankedResult> SEARCH_ORDER =
      Comparator.comparing((RankedResult row) -> row.result().candidateOnly())
          .thenComparing(RankedResult::character, DEFAULT_ORDER);

  /** 図鑑データ取得の境界。 */
  private final CatalogRepository repository;

  /** キャラクターの図鑑を初期化する。 */
  public CharacterCatalog(CatalogRepository repository) {
    this.repository = repository;
  }

  /** termsを扱う。 */
  public List<SearchTerm> terms() {
    return repository.load().terms();
  }

  /** sharedのTermsを扱う。 */
  public List<SearchTerm> sharedTerms(Set<String> additionalTags) {
    // 読み取り時点で固定した図鑑データ。
    var snapshot = repository.load();
    // 検索対象の条件一覧。
    var terms =
        snapshot.terms().stream()
            .filter(term ->
                term.kind() == SearchTermKind.EFFECT || additionalTags.contains(term.label()))
            .toList();
    // byのId。
    var byId = terms.stream().collect(Collectors.toMap(SearchTerm::id, SearchTerm::label));
    // owners。
    var owners =
        snapshot.characters().stream()
            .flatMap(character ->
                character.skills().stream()
                    .flatMap(skill -> skill.matches().stream())
                    .filter(match ->
                        byId.containsKey(match.termId()) && !match.evidence().isCandidate())
                    .map(match -> Map.entry(byId.get(match.termId()), character.id())))
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
    return terms.stream()
        .filter(term -> owners.getOrDefault(term.label(), Set.of()).size() >= 2)
        .toList();
  }

  /** 詳細を扱う。 */
  public Optional<CharacterProfile> detail(String id) {
    return repository.load().characters().stream()
        .filter(character -> character.id().equals(id))
        .findFirst();
  }

  /** 指定された条件に沿って一覧を取得する。 */
  public List<CharacterSummary> list(String name, Stat stat, Direction direction) {
    return list(name, stat, direction, false);
  }

  /** 指定された条件に沿って一覧を取得する。 */
  public List<CharacterSummary> list(
      String name, Stat stat, Direction direction, boolean awakened) {
    return list(name, stat, direction, awakened, Direction.ASC);
  }

  /** 指定された条件に沿って一覧を取得する。 */
  public List<CharacterSummary> list(
      String name, Stat stat, Direction direction, boolean awakened, Direction archiveDirection) {
    // query。
    String query = normalized(name);
    // order。
    Comparator<CharacterProfile> order =
        archiveDirection == Direction.DESC
            ? Comparator.comparing(
                    CharacterProfile::archivePriority,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CharacterProfile::id)
            : DEFAULT_ORDER;
    if (stat != null) {
      // values。
      Comparator<BigDecimal> values =
          direction == Direction.DESC ? Comparator.reverseOrder() : Comparator.naturalOrder();
      order =
          Comparator.comparing(
                  (CharacterProfile character) -> character.stats(awakened).get(stat),
                  Comparator.nullsLast(values))
              .thenComparing(DEFAULT_ORDER);
    }
    return repository.load().characters().stream()
        .filter(character -> normalized(character.name()).contains(query))
        .sorted(order)
        .map(character -> CharacterSummary.from(character, awakened))
        .toList();
  }

  /** 検索条件を組み合わせ、根拠と図鑑順で結果を並べる。 */
  public List<SearchResult> search(Set<String> terms, MatchMode mode) {
    return searchGroups(terms.stream().map(Set::of).toList(), mode);
  }

  /** 検索のGroupsを扱う。 */
  public List<SearchResult> searchGroups(List<Set<String>> groups, MatchMode mode) {
    // 図鑑データ。
    var catalog = repository.load();
    // 検索対象の条件一覧。
    var terms = groups.stream().flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    // knownのTerms。
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

  /** 結果を扱う。 */
  private static SearchResult result(
      CharacterProfile character, Set<String> terms, List<Set<String>> groups, MatchMode mode) {
    // スキル単位の一致結果。
    var hits =
        character.skills().stream()
            .flatMap(skill ->
                skill.matches().stream()
                    .filter(match -> terms.contains(match.termId()))
                    .map(match ->
                        new Hit(skill.id(), skill.name(), match.termId(), match.evidence())))
            .toList();
    // 一致した条件の識別子。
    var matched = hits.stream().map(Hit::termId).collect(Collectors.toUnmodifiableSet());
    // 明示一致。
    var declared =
        hits.stream()
            .filter(hit -> !hit.evidence().isCandidate())
            .map(Hit::termId)
            .collect(Collectors.toUnmodifiableSet());
    // 候補一致の判定。
    boolean candidate = !matched.isEmpty() && !matches(groups, declared, mode);
    return new SearchResult(CharacterSummary.from(character), hits, matched, candidate);
  }

  /** matchesを扱う。 */
  private static boolean matches(List<Set<String>> groups, Set<String> matches, MatchMode mode) {
    if (groups.isEmpty()) return true;
    return mode == MatchMode.AND
        ? groups.stream().allMatch(group -> group.stream().anyMatch(matches::contains))
        : groups.stream().anyMatch(group -> group.stream().anyMatch(matches::contains));
  }

  /** 正規化するを扱う。 */
  private static String normalized(String value) {
    return value == null
        ? ""
        : Normalizer.normalize(value.strip(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  }
}
