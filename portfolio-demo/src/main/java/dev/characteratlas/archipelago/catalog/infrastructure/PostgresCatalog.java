package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.application.CatalogRepository;
import dev.characteratlas.domain.Catalog;
import dev.characteratlas.domain.Catalog.SearchTerm;
import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.domain.CharacterProfile.Evidence;
import dev.characteratlas.domain.CharacterProfile.Match;
import dev.characteratlas.domain.CharacterProfile.Skill;
import dev.characteratlas.domain.CharacterProfile.Stat;
import dev.characteratlas.domain.CharacterProfile.TextOrigin;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresCatalog implements CatalogRepository {
    private final CatalogMapper mapper;

    PostgresCatalog(CatalogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Catalog load() {
        var revision = Optional.ofNullable(mapper.revision()).orElse("empty");
        var terms = mapper.terms().stream().map(row ->
                new SearchTerm(row.id(), row.label(), SearchTerm.Kind.valueOf(row.kind()))).toList();
        var stats = groupStats(mapper.stats());
        var awakenedStats = groupStats(mapper.awakenedStats());
        var matches = mapper.matches().stream().collect(Collectors.groupingBy(
                row -> new SkillKey(row.characterId(), row.skillId()),
                Collectors.mapping(row -> new Match(row.termId(), Evidence.valueOf(row.evidence())), Collectors.toList())));
        var skills = mapper.skills().stream().collect(Collectors.groupingBy(CatalogMapper.SkillRow::characterId,
                Collectors.mapping(row -> new Skill(row.skillId(), row.name(), row.level(), row.description(),
                        TextOrigin.valueOf(row.origin()), matches.getOrDefault(new SkillKey(row.characterId(), row.skillId()), List.of())),
                        Collectors.toList())));
        var characters = mapper.characters().stream().map(row -> {
            var base = stats.getOrDefault(row.id(), Map.of());
            var maxAwakened = new EnumMap<Stat, BigDecimal>(Stat.class);
            maxAwakened.putAll(base);
            maxAwakened.putAll(awakenedStats.getOrDefault(row.id(), Map.of()));
            return new CharacterProfile(row.id(), row.name(), row.attribute(), row.rarity(), row.priority(),
                    base, maxAwakened, skills.getOrDefault(row.id(), List.of()));
        }).toList();
        return new Catalog(revision, characters, terms);
    }

    private static Map<String, Map<Stat, BigDecimal>> groupStats(List<CatalogMapper.StatRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(CatalogMapper.StatRow::characterId,
                Collectors.toMap(row -> Stat.valueOf(row.stat()), CatalogMapper.StatRow::value)));
    }

    private record SkillKey(String characterId, String skillId) {}
}
