package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.TaggedPostIndex;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Reviewの投稿の索引を表す。 */
@Repository
public class ReviewPostIndex implements TaggedPostIndex {
  /** PostgreSQLのクエリーを実行するMapper。 */
  private final RelatedPostMapper mapper;

  /** Reviewの投稿の索引を初期化する。 */
  ReviewPostIndex(RelatedPostMapper mapper) {
    this.mapper = mapper;
  }

  /** taggedのCharactersを扱う。 */
  @Override
  public Set<String> taggedCharacters(List<String> contentIds) {
    return contentIds.stream()
        .distinct()
        .flatMap(id -> mapper.tagged(id).stream())
        .collect(Collectors.toUnmodifiableSet());
  }
}
