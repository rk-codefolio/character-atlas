package dev.characteratlas.archipelago.catalog.application;

import dev.characteratlas.application.search.CharacterCatalog;
import dev.characteratlas.application.search.CharacterSummary;
import dev.characteratlas.application.search.Direction;
import dev.characteratlas.application.search.MatchMode;
import dev.characteratlas.application.search.SearchResult;
import dev.characteratlas.archipelago.catalog.application.options.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.options.Content;
import dev.characteratlas.archipelago.catalog.application.progress.EffectTip;
import dev.characteratlas.archipelago.catalog.application.progress.Progress;
import dev.characteratlas.archipelago.catalog.application.search.CatalogQueryError;
import dev.characteratlas.archipelago.catalog.application.search.SearchContentOptions;
import dev.characteratlas.archipelago.catalog.application.search.TaggedPostIndex;
import dev.characteratlas.archipelago.shared.application.error.UseCaseException;
import dev.characteratlas.domain.character.Stat;
import dev.characteratlas.domain.validation.DomainValidationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 図鑑の参照処理を表す。 */
@Service
public class CatalogQueries {
  /** 検索候補に含めるCD操作の名称。 */
  private static final Set<String> CD_TERMS = Set.of("CD増加", "CD短縮");

  /** 図鑑データ。 */
  private final CharacterCatalog catalog;

  /** 図鑑の選択肢。 */
  private final CatalogOptions options;

  /** posts。 */
  private final TaggedPostIndex posts;

  /** 検索のContents。 */
  private final SearchContentOptions searchContents;

  /** 図鑑の参照処理を初期化する。 */
  public CatalogQueries(
      CharacterCatalog catalog,
      CatalogOptions options,
      TaggedPostIndex posts,
      SearchContentOptions searchContents) {
    this.catalog = catalog;
    this.options = options;
    this.posts = posts;
    this.searchContents = searchContents;
  }

  /** charactersを扱う。 */
  public List<CharacterSummary> characters(
      String name, Stat stat, Direction direction, boolean awakened, Direction archiveDirection) {
    return catalog.list(name, stat, direction, awakened, archiveDirection);
  }

  /** キャラクターを扱う。 */
  public dev.characteratlas.domain.character.CharacterProfile character(String id) {
    return catalog.detail(id).orElseThrow(() -> UseCaseException.notFound());
  }

  /** 育成情報を扱う。 */
  public Progress progress(String id) {
    return options.progress(character(id));
  }

  /** termsを扱う。 */
  public List<dev.characteratlas.domain.search.SearchTerm> terms(boolean shared) {
    return shared ? catalog.sharedTerms(CD_TERMS) : catalog.terms();
  }

  /** effectsを扱う。 */
  public Map<String, List<EffectTip>> effects(String id) {
    return character(id).skills().stream()
        .collect(
            Collectors.toMap(
                skill -> skill.id(),
                skill ->
                    skill.matches().stream()
                        .filter(match ->
                            match.evidence()
                                == dev.characteratlas.domain.skill.Evidence.DECLARED_EFFECT)
                        .flatMap(match -> options.effects(match.termId()).stream())
                        .distinct()
                        .toList()));
  }

  /** 検索条件を組み合わせ、根拠と図鑑順で結果を並べる。 */
  public List<SearchResult> search(
      Set<String> term, List<String> group, String role, List<String> content, MatchMode mode) {
    try {
      // 照合する条件グループ。
      var groups = new ArrayList<Set<String>>();
      if (term != null) term.forEach(id -> groups.add(Set.of(id)));
      if (group != null) {
        // 検索対象の条件一覧。
        var terms = catalog.terms();
        groups.addAll(
            group.stream()
                .map(label ->
                    terms.stream()
                        .filter(entry -> entry.label().equals(label))
                        .map(entry -> entry.id())
                        .collect(Collectors.toUnmodifiableSet()))
                .toList());
      }
      // results。
      var results = catalog.searchGroups(groups, mode).stream();
      if (role != null && !role.isBlank()) {
        // members。
        var members = options.get().roles().get(role);
        if (members == null) throw new DomainValidationException(CatalogQueryError.UNKNOWN_ROLE);
        results = results.filter(result -> members.contains(result.character().id()));
      }
      if (content != null && !content.isEmpty()) {
        // 許可済み。
        var allowed =
            searchContents.available().stream()
                .map(Content::id)
                .collect(Collectors.toUnmodifiableSet());
        if (!allowed.containsAll(content))
          throw new DomainValidationException(CatalogQueryError.UNKNOWN_CONTENT);
        // tagged。
        var tagged = posts.taggedCharacters(content);
        results =
            results.sorted(
                Comparator.comparing(SearchResult::candidateOnly)
                    .thenComparing(result -> !tagged.contains(result.character().id())));
      }
      return results.toList();
    } catch (DomainValidationException e) {
      throw UseCaseException.invalid(e.error());
    }
  }
}
