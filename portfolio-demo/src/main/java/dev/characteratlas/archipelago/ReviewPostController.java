package dev.characteratlas.archipelago;

import tools.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class ReviewPostController {
    private final JdbcTemplate jdbc;
    private final ReviewOptions reviewOptions;

    private final DiscordModerationAccess access;
    private final ModerationLog log;
    private final SearchContentOptions searchContents;

    ReviewPostController(JdbcTemplate jdbc, ReviewOptions reviewOptions, DiscordModerationAccess access,
                         ModerationLog log, SearchContentOptions searchContents) {
        this.jdbc = jdbc;
        this.reviewOptions = reviewOptions;
        this.access = access;
        this.log = log;
        this.searchContents = searchContents;
    }

    @GetMapping("/review-options")
    ReviewOptions.Options options() {
        return reviewOptions.get();
    }

    @GetMapping("/search-contents")
    List<ReviewOptions.Content> searchContents() {
        return searchContents.available();
    }

    @GetMapping("/posts")
    List<Post> list(@RequestParam Kind kind, @RequestParam String subject) {
        return jdbc.query("""
                SELECT post.post_id, post.title, post.body, post.details::text, post.created_at, post.updated_at,
                       ARRAY(SELECT character.display_name FROM jsonb_array_elements_text(%s) WITH ORDINALITY AS related(id, position)
                             JOIN catalog_character character ON character.character_id = related.id ORDER BY related.position)
                FROM review_post post
                WHERE post.kind = ? AND post.subject_id = ? ORDER BY post.created_at DESC, post.post_id
                """.formatted(PostSelections.RELATED_CHARACTERS), (row, index) -> new Post(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
                row.getString(4), row.getObject(5, OffsetDateTime.class), row.getObject(6, OffsetDateTime.class),
                List.of((String[]) row.getArray(7).getArray())),
                kind.name(), subject);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    UUID create(HttpServletRequest request, @RequestBody Submission post) {
        access.requireOrigin(request);
        validate(post);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO review_post (post_id, kind, subject_id, title, body, details) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))",
                id, post.kind().name(), post.subject(), clean(post.title()), clean(post.body()), post.details().toString());
        return id;
    }

    @PutMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void edit(HttpServletRequest request, @PathVariable UUID id, @RequestBody Submission post) {
        access.requireOrigin(request);
        String actor = access.requireModerator(request);
        validate(post);
        String before = lockPost(id);
        var after = jdbc.query("""
                UPDATE review_post p SET title = ?, body = ?, details = CAST(? AS jsonb), updated_at = now()
                WHERE post_id = ? AND kind = ? AND subject_id = ?
                RETURNING to_jsonb(p)::text
                """, (row, index) -> row.getString(1), clean(post.title()), clean(post.body()),
                post.details().toString(), id, post.kind().name(), post.subject());
        if (after.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        log.append(actor, "EDIT_POST", id.toString(), before, after.get(0));
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(HttpServletRequest request, @PathVariable UUID id) {
        access.requireOrigin(request);
        String actor = access.requireModerator(request);
        String before = lockPost(id);
        jdbc.update("DELETE FROM review_post WHERE post_id = ?", id);
        log.append(actor, "DELETE_POST", id.toString(), before, null);
    }

    private String lockPost(UUID id) {
        return jdbc.query("SELECT to_jsonb(p)::text FROM review_post p WHERE post_id = ? FOR UPDATE",
                        (row, index) -> row.getString(1), id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validate(Submission post) {
        require(post.kind() != null && post.subject() != null && post.subject().matches("[a-z0-9][a-z0-9-]{0,39}")
                && post.details() != null && post.details().isObject(), "投稿先か入力項目が不正です。");
        require(clean(post.title()).length() <= 120 && clean(post.body()).length() <= 8000
                && post.details().toString().length() <= 12000, "入力が長すぎます。");
        var contentIds = reviewOptions.contents().stream().map(ReviewOptions.Content::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        require(contentIds.containsAll(selections(post.details(), "contentTags", "contentTag")),
                "コンテンツタグが見つかりません。");
        if (post.kind() == Kind.CONTENT) {
            require(contentIds.contains(post.subject()), "コンテンツを選んでください。");
            require(optionalText(post.details().path("stage"), 120), "ステージ・難易度は120文字以内で入力してください。");
            require(selections(post.details(), "relatedCharacters", "relatedCharacter").stream().allMatch(this::characterExists),
                    "関連渡り手が見つかりません。");
        } else {
            require(characterExists(post.subject()), "キャラクターが見つかりません。");
        }
        require(post.kind() == Kind.COMMENT || !clean(post.title()).isEmpty(), "タイトルを入力してください。");
        require(post.kind() == Kind.BUILD || !clean(post.body()).isEmpty(), "本文を入力してください。");
        if (post.kind() == Kind.BUILD) validateBuild(post.details());
    }

    private static List<String> selections(JsonNode details, String field, String legacyField) {
        try {
            return PostSelections.read(details, field, legacyField);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private boolean characterExists(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM catalog_character WHERE character_id = ?)", Boolean.class, id));
    }

    private void validateBuild(JsonNode details) {
        require(optionalText(details.path("targetStats"), 240), "目標ステータスは240文字以内で入力してください。");
        var options = reviewOptions.get();
        require(options.shells().contains(details.path("shell").asString("")), "航海具を選んでください。");
        var matrices = details.path("matrices");
        require(matrices.isArray() && matrices.size() >= 2 && matrices.size() <= 9, "航路紋を2種類以上選んでください。");
        var names = new java.util.HashSet<String>();
        for (JsonNode matrix : matrices) {
            String name = matrix.path("name").asString("");
            var points = matrix.path("points");
            require(names.add(name) && options.matrices().stream().anyMatch(option -> option.name().equals(name)),
                    "異なる航路紋を選んでください。");
            require(points.isMissingNode() || points.isNull() || (points.isIntegralNumber() && points.bigIntegerValue().signum() >= 0),
                    "ポイントは空欄か0以上の整数にしてください。");
        }
        var stats = details.path("mainStats");
        require(stats.isArray() && stats.size() == 3 && stats.valueStream().allMatch(stat -> options.mainStats().contains(stat.asString(""))),
                "装具スロット④・⑤・⑥を選んでください。");
    }

    private static boolean optionalText(JsonNode value, int maxLength) {
        return value.isMissingNode() || value.isNull() || value.isTextual() && value.asString().length() <= maxLength;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    enum Kind { COMMENT, BUILD, CONTENT }
    record Submission(Kind kind, String subject, String title, String body, JsonNode details) {}
    record Post(UUID id, String title, String body, String details, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                List<String> relatedCharacterNames) {}
}
