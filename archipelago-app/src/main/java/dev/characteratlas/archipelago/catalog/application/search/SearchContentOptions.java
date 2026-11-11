package dev.characteratlas.archipelago.catalog.application.search;

import dev.characteratlas.archipelago.catalog.application.options.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.options.Content;
import dev.characteratlas.archipelago.moderation.application.audit.AuditLog;
import dev.characteratlas.archipelago.shared.application.error.UseCaseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 検索のコンテンツの選択肢を表す。 */
@Service
public class SearchContentOptions {
  /** overrides。 */
  private final SearchContentOverrides overrides;

  /** 図鑑の選択肢。 */
  private final CatalogOptions options;

  /** 監査記録のポート。 */
  private final AuditLog log;

  /** 検索のコンテンツの選択肢を初期化する。 */
  public SearchContentOptions(
      SearchContentOverrides overrides, CatalogOptions options, AuditLog log) {
    this.overrides = overrides;
    this.options = options;
    this.log = log;
  }

  /** availableを扱う。 */
  public List<Content> available() {
    // 表示設定。
    var visibility = overrides.load();
    return options.contents().stream()
        .filter(
            content -> visibility.getOrDefault(content.id(), options.isSearchable(content.id())))
        .toList();
  }

  /** changeを扱う。 */
  @Transactional
  public void change(String actor, String id, boolean visible) {
    if (options.contents().stream().noneMatch(content -> content.id().equals(id))) {
      throw UseCaseException.notFound();
    }
    overrides.lock();
    // 変更前。
    boolean before = available().stream().anyMatch(content -> content.id().equals(id));
    if (before == visible) return;
    overrides.save(id, visible);
    log.append(actor, "SEARCH_CONTENT", id, Boolean.toString(before), Boolean.toString(visible));
  }
}
