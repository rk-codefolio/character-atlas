package dev.characteratlas.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  /** 全条件を満たす候補を用意し、ANDで明示一致が候補より先に並ぶことを確認する。 */
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

  /** 明示と関連候補が混在する条件をORで検索し、明示一致が優先されることを確認する。 */
  @Test
  void orNeedsOnlyOneDeclaredHitToRankAheadOfCandidates() {
    var result = catalog.search(Set.of(SHIELD, HEAL), MatchMode.OR);
    assertEquals(
        List.of("z", "c", "a", "b"), result.stream().map(row -> row.character().id()).toList());
    assertFalse(result.getFirst().candidateOnly());
    assertTrue(result.getLast().candidateOnly());
  }

  /** 全角の名前と能力値の欠損を用意し、正規化と両方向での欠損末尾を確認する。 */
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

  /** 図鑑順位を付けたデータで一覧と空条件検索を実行し、図鑑順を確認する。 */
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

  /** 条件なしと未知のIDで検索し、全件表示と入力拒否を確認する。 */
  @Test
  void emptySearchListsEveryoneAndUnknownTermIsRejected() {
    assertEquals(4, catalog.search(Set.of(), MatchMode.AND).size());
    assertThrows(
        IllegalArgumentException.class, () -> catalog.search(Set.of("typo"), MatchMode.OR));
    assertTrue(catalog.detail("absent").isEmpty());
  }

  /** 同名の別IDを束ねてAND検索し、グループ内のいずれかの一致を確認する。 */
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
