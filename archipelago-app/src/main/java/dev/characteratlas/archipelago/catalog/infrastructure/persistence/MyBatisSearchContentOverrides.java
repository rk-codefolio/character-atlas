package dev.characteratlas.archipelago.catalog.infrastructure.persistence;

import dev.characteratlas.archipelago.catalog.application.SearchContentOverrides;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** MyのBatisの検索のコンテンツのOverridesを表す。 */
@Repository
public class MyBatisSearchContentOverrides implements SearchContentOverrides {
  /** PostgreSQLのクエリーを実行するMapper。 */
  private final SearchContentMapper mapper;

  /** MyのBatisの検索のコンテンツのOverridesを初期化する。 */
  MyBatisSearchContentOverrides(SearchContentMapper mapper) {
    this.mapper = mapper;
  }

  /** 永続化されたデータから読み取りモデルを組み立てる。 */
  @Override
  public Map<String, Boolean> load() {
    return mapper.load().stream()
        .collect(Collectors.toUnmodifiableMap(VisibilityRow::id, VisibilityRow::visible));
  }

  /** ロックするを扱う。 */
  @Override
  public void lock() {
    mapper.lock();
  }

  /** 保存するを扱う。 */
  @Override
  public void save(String id, boolean visible) {
    mapper.save(id, visible);
  }
}
