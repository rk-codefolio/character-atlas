package dev.characteratlas.archipelago.catalog.application;

import java.util.List;
import java.util.Set;

/** Taggedの投稿の索引を表す。 */
public interface TaggedPostIndex {
  /** taggedのCharactersを扱う。 */
  Set<String> taggedCharacters(List<String> contentIds);
}
