package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.application.CatalogRepository;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQLの図鑑を表す。 */
@Repository
public class PostgresCatalog implements CatalogRepository {
  /** PostgreSQLのクエリーを実行するMapper。 */
  private final CatalogMapper mapper;

  /** PostgreSQLの図鑑を初期化する。 */
  PostgresCatalog(CatalogMapper mapper) {
    this.mapper = mapper;
  }

  /** 永続化されたデータから読み取りモデルを組み立てる。 */
  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Catalog load() {
    // 図鑑データの版。
    var revision = Optional.ofNullable(mapper.revision()).orElse("empty");
    // 検索対象の条件一覧。
    var terms =
        mapper.terms().stream()
            .map(row -> new SearchTerm(row.id(), row.label(), SearchTermKind.valueOf(row.kind())))
            .toList();
    // 能力値の組み合わせ。
    var stats = groupStats(mapper.stats());
    // 覚醒後のStats。
    var awakenedStats = groupStats(mapper.awakenedStats());
    // matches。
    var matches =
        mapper.matches().stream()
            .collect(
                Collectors.groupingBy(
                    row -> new SkillKey(row.characterId(), row.skillId()),
                    Collectors.mapping(
                        row -> new Match(row.termId(), Evidence.valueOf(row.evidence())),
                        Collectors.toList())));
    // skills。
    var skills =
        mapper.skills().stream()
            .collect(
                Collectors.groupingBy(
                    SkillRow::characterId,
                    Collectors.mapping(
                        row ->
                            new Skill(
                                row.skillId(),
                                row.name(),
                                row.level(),
                                row.description(),
                                TextOrigin.valueOf(row.origin()),
                                matches.getOrDefault(
                                    new SkillKey(row.characterId(), row.skillId()), List.of())),
                        Collectors.toList())));
    // 対象キャラクターの一覧。
    var characters =
        mapper.characters().stream()
            .map(row -> {
              // 基礎ステータス。
              var base = stats.getOrDefault(row.id(), Map.of());
              // 最大の覚醒後。
              var maxAwakened = new EnumMap<Stat, BigDecimal>(Stat.class);
              maxAwakened.putAll(base);
              maxAwakened.putAll(awakenedStats.getOrDefault(row.id(), Map.of()));
              return new CharacterProfile(
                  row.id(),
                  row.name(),
                  row.attribute(),
                  row.rarity(),
                  row.priority(),
                  base,
                  maxAwakened,
                  skills.getOrDefault(row.id(), List.of()));
            })
            .toList();
    return new Catalog(revision, characters, terms);
  }

  /** まとめるのStatsを扱う。 */
  private static Map<String, Map<Stat, BigDecimal>> groupStats(List<StatRow> rows) {
    return rows.stream()
        .collect(
            Collectors.groupingBy(
                StatRow::characterId,
                Collectors.toMap(row -> Stat.valueOf(row.stat()), StatRow::value)));
  }
}
