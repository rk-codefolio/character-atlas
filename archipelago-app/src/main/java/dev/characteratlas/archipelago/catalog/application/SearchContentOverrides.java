package dev.characteratlas.archipelago.catalog.application;

import java.util.Map;

/** 検索のコンテンツのOverridesを表す。 */
public interface SearchContentOverrides {
  /** 永続化されたデータから読み取りモデルを組み立てる。 */
  Map<String, Boolean> load();

  /** ロックするを扱う。 */
  void lock();

  /** 保存するを扱う。 */
  void save(String id, boolean visible);
}
