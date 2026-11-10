package dev.characteratlas.archipelago.catalog.infrastructure;


import dev.characteratlas.domain.CharacterProfile;
import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReviewOptions implements CatalogOptions {
    private final JdbcTemplate jdbc;
    private final Book book;

    public ReviewOptions(JdbcTemplate jdbc, ObjectMapper mapper) throws IOException {
        this.jdbc = jdbc;
        try (var input = new ClassPathResource("archipelago-options.json").getInputStream()) {
            book = mapper.readValue(input, Book.class);
        }
    }

    public Options get() {
        var roles = new LinkedHashMap<String, List<String>>();
        jdbc.query("SELECT role, character_id FROM voyager_role ORDER BY role, character_id", row -> {
            roles.computeIfAbsent(row.getString(1), _ -> new ArrayList<>()).add(row.getString(2));
        });
        var kinds = jdbc.query("SELECT DISTINCT label, kind FROM voyager_effect_tip ORDER BY label, kind", rows -> {
            var result = new LinkedHashMap<String, String>();
            while (rows.next()) result.putIfAbsent(rows.getString(1), rows.getString(2));
            return result;
        });
        var advanced = jdbc.queryForList("SELECT character_id FROM voyager_growth WHERE advanced ORDER BY character_id", String.class);
        return new Options(book.contents(), book.shells(), book.matrices(), book.mainStats(), book.template(),
                roles, kinds, book.attributes(), advanced);
    }

    public boolean isSearchable(String id) {
        return book.searchable().contains(id);
    }

    public List<Content> contents() {
        return book.contents();
    }

    public List<EffectTip> effects(String termId) {
        return jdbc.query("""
                SELECT term_id, label, level, description FROM voyager_effect_tip
                WHERE term_id = ? ORDER BY level
                """, (row, _) -> new EffectTip(row.getString(1), row.getString(2), row.getInt(3), row.getString(4)), termId);
    }

    public Progress progress(CharacterProfile character) {
        var levels = new LinkedHashMap<String, List<SkillLevel>>();
        jdbc.query("""
                SELECT skill_id, level, description FROM voyager_skill_level
                WHERE character_id = ? ORDER BY skill_id, level
                """, row -> {
            levels.computeIfAbsent(row.getString(1), _ -> new ArrayList<>())
                    .add(new SkillLevel(row.getInt(2), row.getString(3)));
        }, character.id());
        var metadata = jdbc.query("""
                SELECT skill_id, passive, cooldown FROM voyager_skill_meta WHERE character_id = ?
                """, rows -> {
            var result = new LinkedHashMap<String, SkillInfo>();
            while (rows.next()) {
                result.put(rows.getString(1), new SkillInfo(rows.getBoolean(2), rows.getObject(3, Integer.class)));
            }
            return result;
        }, character.id());
        var awakening = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM voyager_growth WHERE character_id = ? AND awakened)
                """, Boolean.class, character.id());
        return new Progress(Boolean.TRUE.equals(awakening), book.attributes().get(character.attribute()), levels, metadata);
    }

    public record Book(List<Content> contents, Set<String> searchable, List<String> shells, List<Matrix> matrices,
                List<String> mainStats, List<String> template, Map<String, String> attributes) {}
}
