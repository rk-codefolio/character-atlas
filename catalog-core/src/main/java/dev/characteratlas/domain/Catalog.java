package dev.characteratlas.domain;

import java.util.List;

/**
 * 同じ読み取り時点に属する図鑑と検索語のスナップショット。
 *
 * <p>キャラクターと検索語の一覧は防御的にコピーし、取得元の変更が図鑑へ伝播することを防ぐ。
 */
public record Catalog(
    /* 読み取り時点で固定した図鑑データ。 */ String snapshot, /* 対象キャラクターの一覧。 */
    List<CharacterProfile> characters, /* 検索対象の条件一覧。 */
    List<SearchTerm> terms) {
  /** 図鑑を初期化する。 */
  public Catalog {
    characters = List.copyOf(characters);
    terms = List.copyOf(terms);
  }
}
