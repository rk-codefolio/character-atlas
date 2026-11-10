package dev.characteratlas.archipelago.catalog.application;

import dev.characteratlas.domain.CharacterProfile;
import java.util.List;

/** 図鑑の選択肢を表す。 */
public interface CatalogOptions {
  /** 取得するを扱う。 */
  Options get();

  /** isのSearchableを扱う。 */
  boolean isSearchable(String id);

  /** contentsを扱う。 */
  List<Content> contents();

  /** effectsを扱う。 */
  List<EffectTip> effects(String termId);

  /** 育成情報を扱う。 */
  Progress progress(CharacterProfile character);
}
