package dev.characteratlas.archipelago.posting.presentation;

import tools.jackson.databind.JsonNode;
import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.SearchContentOptions;
import dev.characteratlas.archipelago.moderation.infrastructure.DiscordModerationAccess;
import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostService;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReviewPostController {
    private final PostService posts;
    private final CatalogOptions reviewOptions;

    private final DiscordModerationAccess access;
    private final SearchContentOptions searchContents;

    ReviewPostController(PostService posts, CatalogOptions reviewOptions, DiscordModerationAccess access,
                         SearchContentOptions searchContents) {
        this.posts = posts;
        this.reviewOptions = reviewOptions;
        this.access = access;
        this.searchContents = searchContents;
    }

    @GetMapping("/review-options")
    CatalogOptions.Options options() {
        return reviewOptions.get();
    }

    @GetMapping("/search-contents")
    List<CatalogOptions.Content> searchContents() {
        return searchContents.available();
    }

    @GetMapping("/posts")
    List<Post> list(@RequestParam PostKind kind, @RequestParam String subject) {
        return posts.list(kind, subject);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    UUID create(HttpServletRequest request, @RequestBody Submission post) {
        access.requireOrigin(request);
        return posts.create(draft(UUID.randomUUID(), post));
    }

    @PutMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void edit(HttpServletRequest request, @PathVariable UUID id, @RequestBody Submission post) {
        access.requireOrigin(request);
        String actor = access.requireModerator(request);
        posts.edit(actor, draft(id, post));
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(HttpServletRequest request, @PathVariable UUID id) {
        access.requireOrigin(request);
        posts.remove(access.requireModerator(request), id);
    }

    private PostDraft draft(UUID id, Submission post) {
        validate(post);
        try {
            return new PostDraft(id, post.kind(), post.subject(), post.title(), post.body(), post.details().toString());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
    }

    private void validate(Submission post) {
        require(post.kind() != null && post.subject() != null && post.subject().matches("[a-z0-9][a-z0-9-]{0,39}")
                && post.details() != null && post.details().isObject(), "投稿先か入力項目が不正です。");
        var contentIds = reviewOptions.contents().stream().map(CatalogOptions.Content::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        require(contentIds.containsAll(selections(post.details(), "contentTags", "contentTag")),
                "コンテンツタグが見つかりません。");
        if (post.kind() == PostKind.CONTENT) {
            require(contentIds.contains(post.subject()), "コンテンツを選んでください。");
            require(optionalText(post.details().path("stage"), 120), "ステージ・難易度は120文字以内で入力してください。");
            require(selections(post.details(), "relatedCharacters", "relatedCharacter").stream().allMatch(posts::characterExists),
                    "関連渡り手が見つかりません。");
        } else {
            require(posts.characterExists(post.subject()), "キャラクターが見つかりません。");
        }
        if (post.kind() == PostKind.BUILD) validateBuild(post.details());
    }

    private static List<String> selections(JsonNode details, String field, String legacyField) {
        try {
            return PostSelections.read(details, field, legacyField);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    record Submission(PostKind kind, String subject, String title, String body, JsonNode details) {}
}
