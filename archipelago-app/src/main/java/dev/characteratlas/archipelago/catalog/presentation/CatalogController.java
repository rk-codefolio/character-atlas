package dev.characteratlas.archipelago.catalog.presentation;

import dev.characteratlas.application.search.CharacterSummary;
import dev.characteratlas.application.search.Direction;
import dev.characteratlas.application.search.MatchMode;
import dev.characteratlas.application.search.SearchResult;
import dev.characteratlas.archipelago.catalog.application.CatalogQueries;
import dev.characteratlas.archipelago.catalog.application.progress.EffectTip;
import dev.characteratlas.archipelago.catalog.application.progress.Progress;
import dev.characteratlas.domain.character.Stat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 図鑑のREST APIを表す。 */
@RestController
@RequestMapping("/api")
class CatalogController {
  /** 参照処理。 */
  private final CatalogQueries queries;

  /** 図鑑のREST APIを初期化する。 */
  CatalogController(CatalogQueries queries) {
    this.queries = queries;
  }

  /** charactersを扱う。 */
  @GetMapping("/characters")
  List<CharacterSummary> characters(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Stat stat,
      @RequestParam(defaultValue = "DESC") Direction direction,
      @RequestParam(defaultValue = "true") boolean awakened,
      @RequestParam(defaultValue = "ASC") Direction archiveDirection) {
    return queries.characters(name, stat, direction, awakened, archiveDirection);
  }

  /** キャラクターを扱う。 */
  @GetMapping("/characters/{id}")
  dev.characteratlas.domain.character.CharacterProfile character(@PathVariable String id) {
    return queries.character(id);
  }

  /** 育成情報を扱う。 */
  @GetMapping("/characters/{id}/progress")
  Progress progress(@PathVariable String id) {
    return queries.progress(id);
  }

  /** termsを扱う。 */
  @GetMapping("/terms")
  List<dev.characteratlas.domain.search.SearchTerm> terms(
      @RequestParam(defaultValue = "false") boolean shared) {
    return queries.terms(shared);
  }

  /** effectsを扱う。 */
  @GetMapping("/characters/{id}/effects")
  Map<String, List<EffectTip>> effects(@PathVariable String id) {
    return queries.effects(id);
  }

  /** 検索条件を組み合わせ、根拠と図鑑順で結果を並べる。 */
  @GetMapping("/search")
  List<SearchResult> search(
      @RequestParam(required = false) Set<String> term,
      @RequestParam(required = false) List<String> group,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) List<String> content,
      @RequestParam(defaultValue = "AND") MatchMode mode) {
    return queries.search(term, group, role, content, mode);
  }
}
