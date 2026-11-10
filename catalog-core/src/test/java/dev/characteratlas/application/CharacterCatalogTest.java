package dev.characteratlas.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.characteratlas.domain.Catalog;
import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.domain.Evidence;
import dev.characteratlas.domain.Match;
import dev.characteratlas.domain.SearchTerm;
import dev.characteratlas.domain.SearchTermKind;
import dev.characteratlas.domain.Skill;
import dev.characteratlas.domain.Stat;
import dev.characteratlas.domain.TextOrigin;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CharacterCatalogTest {
  private static final String SHIELD = "effect:shield";
  private static final String HEAL = "effect:heal";

  private final CharacterCatalog catalog =
      new CharacterCatalog(() ->
          new Catalog(
              "test",
              List.of(
                  character(
                      "a",
                      "Alpha",
                      30,
                      10,
                      new Match(SHIELD, Evidence.RELATED_CANDIDATE),
                      new Match(HEAL, Evidence.DECLARED_EFFECT)),
                  character(
                      "z",
                      "Zulu",
                      10,
                      20,
                      new Match(SHIELD, Evidence.DECLARED_EFFECT),
                      new Match(HEAL, Evidence.DECLARED_EFFECT)),
                  character(
                      "b", "Bravo", null, null, new Match(SHIELD, Evidence.RELATED_CANDIDATE)),
                  character("c", "Charlie", 20, 15, new Match(HEAL, Evidence.DECLARED_EFFECT))),
              List.of(
                  new SearchTerm(SHIELD, "Shield", SearchTermKind.EFFECT),
                  new SearchTerm(HEAL, "Heal", SearchTermKind.EFFECT))));

  @Test
  void andPrefersDeclaredCoverageButIncludesCandidates() {
    var result = catalog.search(Set.of(SHIELD, HEAL), MatchMode.AND);
    assertEquals(List.of("z", "a"), result.stream().map(row -> row.character().id()).toList());
    assertFalse(result.getFirst().candidateOnly());
    assertTrue(result.get(1).candidateOnly());
    assertTrue(
        result.get(1).hits().stream()
            .anyMatch(hit -> hit.evidence() == Evidence.RELATED_CANDIDATE));
  }

  @Test
  void orNeedsOnlyOneDeclaredHitToRankAheadOfCandidates() {
    var result = catalog.search(Set.of(SHIELD, HEAL), MatchMode.OR);
    assertEquals(
        List.of("z", "c", "a", "b"), result.stream().map(row -> row.character().id()).toList());
    assertFalse(result.getFirst().candidateOnly());
    assertTrue(result.getLast().candidateOnly());
  }

  @Test
  void missingStatsAreLastInBothDirectionsAndNamesAreNormalized() {
    assertEquals(
        List.of("z", "c", "a", "b"),
        catalog.list(null, Stat.SPEED, Direction.DESC).stream().map(CharacterSummary::id).toList());
    assertEquals(
        List.of("a", "c", "z", "b"),
        catalog.list(null, Stat.SPEED, Direction.ASC).stream().map(CharacterSummary::id).toList());
    assertEquals("a", catalog.list("ＡＬＰＨＡ", null, Direction.ASC).getFirst().id());
  }

  @Test
  void defaultListAndUnfilteredSearchFollowArchivePriority() {
    var expected = List.of("z", "c", "a", "b");
    assertEquals(
        expected,
        catalog.list(null, null, Direction.ASC).stream().map(CharacterSummary::id).toList());
    assertEquals(
        expected,
        catalog.search(Set.of(), MatchMode.AND).stream().map(row -> row.character().id()).toList());
  }

  @Test
  void emptySearchListsEveryoneAndUnknownTermIsRejected() {
    assertEquals(4, catalog.search(Set.of(), MatchMode.AND).size());
    assertThrows(
        IllegalArgumentException.class, () -> catalog.search(Set.of("typo"), MatchMode.OR));
    assertTrue(catalog.detail("absent").isEmpty());
  }

  @Test
  void anAliasGroupNeedsOneMatchingIdEvenInAndMode() {
    var result = catalog.searchGroups(List.of(Set.of(SHIELD, HEAL), Set.of(HEAL)), MatchMode.AND);
    assertEquals(List.of("z", "c", "a"), result.stream().map(row -> row.character().id()).toList());
    assertTrue(result.stream().noneMatch(SearchResult::candidateOnly));
  }

  private static CharacterProfile character(
      String id, String name, Integer archivePriority, Integer speed, Match... matches) {
    Map<Stat, BigDecimal> stats =
        speed == null ? Map.of() : Map.of(Stat.SPEED, BigDecimal.valueOf(speed));
    return new CharacterProfile(
        id,
        name,
        null,
        null,
        archivePriority,
        stats,
        List.of(new Skill(id + "-1", "Skill", 1, null, TextOrigin.MISSING, List.of(matches))));
  }
}
