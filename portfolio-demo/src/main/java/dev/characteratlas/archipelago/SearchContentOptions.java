package dev.characteratlas.archipelago;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
class SearchContentOptions {
    private final JdbcTemplate jdbc;
    private final ReviewOptions options;
    private final DiscordModerationAccess access;
    private final ModerationLog log;

    SearchContentOptions(JdbcTemplate jdbc, ReviewOptions options, DiscordModerationAccess access, ModerationLog log) {
        this.jdbc = jdbc;
        this.options = options;
        this.access = access;
        this.log = log;
    }

    List<ReviewOptions.Content> available() {
        var visibility = jdbc.query("SELECT content_id, visible FROM search_content_override", rs -> {
            var values = new java.util.HashMap<String, Boolean>();
            while (rs.next()) values.put(rs.getString(1), rs.getBoolean(2));
            return values;
        });
        return options.contents().stream()
                .filter(content -> visibility.getOrDefault(content.id(), options.isSearchable(content.id()))).toList();
    }

    @Transactional
    public void change(HttpServletRequest request, String id, boolean visible) {
        access.requireOrigin(request);
        String actor = access.requireOwner(request);
        if (options.contents().stream().noneMatch(content -> content.id().equals(id))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        jdbc.execute("LOCK TABLE search_content_override IN SHARE ROW EXCLUSIVE MODE");
        boolean before = available().stream().anyMatch(content -> content.id().equals(id));
        if (before == visible) return;
        jdbc.update("""
                INSERT INTO search_content_override (content_id, visible) VALUES (?, ?)
                ON CONFLICT (content_id) DO UPDATE SET visible = EXCLUDED.visible
                """, id, visible);
        log.append(actor, "SEARCH_CONTENT", id, Boolean.toString(before), Boolean.toString(visible));
    }
}
