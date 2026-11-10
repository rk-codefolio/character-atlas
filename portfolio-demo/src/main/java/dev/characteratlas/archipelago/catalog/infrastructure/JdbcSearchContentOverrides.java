package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.SearchContentOverrides;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class JdbcSearchContentOverrides implements SearchContentOverrides {
    private final JdbcTemplate jdbc;

    public JdbcSearchContentOverrides(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, Boolean> load() {
        return jdbc.query("SELECT content_id, visible FROM search_content_override", rs -> {
            var values = new java.util.HashMap<String, Boolean>();
            while (rs.next()) values.put(rs.getString(1), rs.getBoolean(2));
            return values;
        });
    }

    @Override
    public void lock() {
        jdbc.execute("LOCK TABLE search_content_override IN SHARE ROW EXCLUSIVE MODE");
    }

    @Override
    public void save(String id, boolean visible) {
        jdbc.update("""
                INSERT INTO search_content_override (content_id, visible) VALUES (?, ?)
                ON CONFLICT (content_id) DO UPDATE SET visible = EXCLUDED.visible
                """, id, visible);
    }
}
