package dev.characteratlas.application;

import dev.characteratlas.domain.Catalog;

/** 図鑑の取得境界を表す。 */
@FunctionalInterface
public interface CatalogRepository {
  /** 永続化されたデータから読み取りモデルを組み立てる。 */
  Catalog load();
}
