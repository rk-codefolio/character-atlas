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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PostgresCatalog implements CatalogRepository {
    private final JdbcTemplate jdbc;
    private final JdbcClient client;

    public PostgresCatalog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.client = JdbcClient.create(jdbc);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Catalog load() {
        String revision = client.sql("SELECT revision FROM catalog_snapshot")
                .query(String.class).optional().orElse("empty");
        List<SearchTerm> terms = jdbc.query("SELECT term_id, label, kind FROM catalog_term ORDER BY kind, label, term_id",
                (row, _) -> new SearchTerm(row.getString("term_id"), row.getString("label"),
                        SearchTerm.Kind.valueOf(row.getString("kind"))));
        var stats = loadStats("SELECT character_id, stat, value FROM catalog_stat");
        var awakenedStats = loadStats("SELECT character_id, stat, value FROM catalog_awakened_stat");
        Map<SkillKey, List<Match>> matches = new HashMap<>();
        jdbc.query("SELECT character_id, skill_id, term_id, evidence FROM catalog_skill_match ORDER BY term_id", row -> {
            var key = new SkillKey(row.getString("character_id"), row.getString("skill_id"));
            matches.computeIfAbsent(key, _ -> new ArrayList<>())
                    .add(new Match(row.getString("term_id"), Evidence.valueOf(row.getString("evidence"))));
        });

        Map<String, List<Skill>> skills = new HashMap<>();
        jdbc.query("""
                SELECT character_id, skill_id, display_name, numeric_level, description, text_origin
                FROM catalog_skill ORDER BY character_id, position
                """, row -> {
            String id = row.getString("character_id");
            String skillId = row.getString("skill_id");
            skills.computeIfAbsent(id, _ -> new ArrayList<>()).add(new Skill(
                    skillId, row.getString("display_name"), row.getInt("numeric_level"),
                    row.getString("description"), TextOrigin.valueOf(row.getString("text_origin")),
                    matches.getOrDefault(new SkillKey(id, skillId), List.of())));
        });

        List<CharacterProfile> characters = jdbc.query("""
                SELECT character_id, display_name, attribute, rarity, archive_priority FROM catalog_character ORDER BY character_id
                """, (row, _) -> {
            String id = row.getString("character_id");
            var base = stats.getOrDefault(id, Map.of());
            var maxAwakened = new EnumMap<Stat, BigDecimal>(Stat.class);
            maxAwakened.putAll(base);
            maxAwakened.putAll(awakenedStats.getOrDefault(id, Map.of()));
            return new CharacterProfile(id, row.getString("display_name"), row.getString("attribute"),
                    row.getString("rarity"), row.getObject("archive_priority", Integer.class),
                    base, maxAwakened, skills.getOrDefault(id, List.of()));
        });
        return new Catalog(revision, characters, terms);
    }

    private Map<String, Map<Stat, BigDecimal>> loadStats(String sql) {
        Map<String, Map<Stat, BigDecimal>> stats = new HashMap<>();
        jdbc.query(sql, row -> {
            stats.computeIfAbsent(row.getString("character_id"), _ -> new EnumMap<>(Stat.class))
                    .put(Stat.valueOf(row.getString("stat")), row.getBigDecimal("value"));
        });
        return stats;
    }

    private record SkillKey(String characterId, String skillId) {}
}
