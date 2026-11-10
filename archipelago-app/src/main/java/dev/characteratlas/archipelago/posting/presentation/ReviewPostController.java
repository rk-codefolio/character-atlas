package dev.characteratlas.archipelago.posting.presentation;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.Content;
import dev.characteratlas.archipelago.catalog.application.Options;
import dev.characteratlas.archipelago.catalog.application.SearchContentOptions;
import dev.characteratlas.archipelago.moderation.infrastructure.DiscordModerationAccess;
import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostService;
import dev.characteratlas.archipelago.posting.domain.BuildDetails;
import dev.characteratlas.archipelago.posting.domain.CommentDetails;
import dev.characteratlas.archipelago.posting.domain.GuideDetails;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.posting.domain.PostingDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/** Reviewの投稿のREST APIを表す。 */
@RestController
@RequestMapping("/api")
public class ReviewPostController {
  /** posts。 */
  private final PostService posts;

  /** reviewの選択肢。 */
  private final CatalogOptions reviewOptions;

  /** 認可情報。 */
  private final DiscordModerationAccess access;

  /** 検索のContents。 */
  private final SearchContentOptions searchContents;

  /** Reviewの投稿のREST APIを初期化する。 */
  ReviewPostController(
      PostService posts,
      CatalogOptions reviewOptions,
      DiscordModerationAccess access,
      SearchContentOptions searchContents) {
    this.posts = posts;
    this.reviewOptions = reviewOptions;
    this.access = access;
    this.searchContents = searchContents;
  }

  /** 選択肢を扱う。 */
  @GetMapping("/review-options")
  Options options() {
    return reviewOptions.get();
  }

  /** 検索のContentsを扱う。 */
  @GetMapping("/search-contents")
  List<Content> searchContents() {
    return searchContents.available();
  }

  /** 指定された条件に沿って一覧を取得する。 */
  @GetMapping("/posts")
  List<Post> list(@RequestParam PostKind kind, @RequestParam String subject) {
    return posts.list(kind, subject);
  }

  /** 検証した入力を新しい投稿として登録する。 */
  @PostMapping("/posts")
  @ResponseStatus(HttpStatus.CREATED)
  UUID create(HttpServletRequest request, @RequestBody PostRequest post) {
    access.requireOrigin(request);
    return posts.create(draft(UUID.randomUUID(), post));
  }

  /** 投稿の更新と監査記録を同じ操作として実行する。 */
  @PutMapping("/posts/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void edit(HttpServletRequest request, @PathVariable UUID id, @RequestBody PostRequest post) {
    access.requireOrigin(request);
    // 操作者。
    String actor = access.requireModerator(request);
    posts.edit(actor, draft(id, post));
  }

  /** 対象の投稿を削除する。 */
  @DeleteMapping("/posts/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void remove(HttpServletRequest request, @PathVariable UUID id) {
    access.requireOrigin(request);
    posts.remove(access.requireModerator(request), id);
  }

  /** 入力内容を扱う。 */
  private PostDraft draft(UUID id, PostRequest post) {
    try {
      validate(post);
      return new PostDraft(
          id, post.kind(), post.subject(), post.title(), post.body(), post.details().toString());
    } catch (IllegalArgumentException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
    }
  }

  /** 検証するを扱う。 */
  private void validate(PostRequest post) {
    require(
        post.kind() != null
            && post.subject() != null
            && post.subject().matches("[a-z0-9][a-z0-9-]{0,39}")
            && post.details() != null
            && post.details().isObject(),
        "投稿先か入力項目が不正です。");
    // 有効なコンテンツ識別子。
    var contentIds =
        reviewOptions.contents().stream()
            .map(Content::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    require(
        contentIds.containsAll(selections(post.details(), "contentTags", "contentTag")),
        "コンテンツタグが見つかりません。");
    // 投稿の種類別詳細。
    PostingDetails details =
        switch (post.kind()) {
          case COMMENT -> new CommentDetails();
          case BUILD -> build(post.details());
          case CONTENT ->
              new GuideDetails(
                  optionalText(post.details().path("stage"), "ステージ・難易度"),
                  selections(post.details(), "relatedCharacters", "relatedCharacter"));
        };
    switch (details) {
      case GuideDetails(_, var characters) -> {
        require(contentIds.contains(post.subject()), "コンテンツを選んでください。");
        require(characters.stream().allMatch(posts::characterExists), "関連渡り手が見つかりません。");
      }
      case BuildDetails(var shell, var matrices, var stats, _) -> {
        require(posts.characterExists(post.subject()), "キャラクターが見つかりません。");
        // 図鑑の選択肢。
        var options = reviewOptions.get();
        require(options.shells().contains(shell), "航海具を選んでください。");
        // 有効な選択肢。
        var available = options.matrices().stream().map(matrix -> matrix.name()).toList();
        require(available.containsAll(matrices), "異なる航路紋を選んでください。");
        require(options.mainStats().containsAll(stats), "装具スロット④・⑤・⑥を選んでください。");
      }
      case CommentDetails() -> require(posts.characterExists(post.subject()), "キャラクターが見つかりません。");
    }
  }

  /** selectionsを扱う。 */
  private static List<String> selections(JsonNode details, String field, String legacyField) {
    try {
      return PostSelections.read(details, field, legacyField);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  /** ビルドを扱う。 */
  private static BuildDetails build(JsonNode details) {
    // matrices。
    var matrices = details.path("matrices");
    require(matrices.isArray(), "航路紋を2種類以上選んでください。");
    // 名称一覧。
    var names =
        matrices
            .valueStream()
            .map(matrix -> {
              // 航路紋のポイント。
              var points = matrix.path("points");
              require(
                  points.isMissingNode()
                      || points.isNull()
                      || points.isIntegralNumber() && points.bigIntegerValue().signum() >= 0,
                  "ポイントは空欄か0以上の整数にしてください。");
              return matrix.path("name").asString("");
            })
            .toList();
    // 能力値の組み合わせ。
    var stats = details.path("mainStats");
    require(stats.isArray(), "装具スロット④・⑤・⑥を選んでください。");
    return new BuildDetails(
        details.path("shell").asString(""),
        names,
        stats.valueStream().map(stat -> stat.asString("")).toList(),
        optionalText(details.path("targetStats"), "目標ステータス"));
  }

  /** 任意の説明文を扱う。 */
  private static Optional<String> optionalText(JsonNode value, String label) {
    if (value.isMissingNode() || value.isNull()) return Optional.empty();
    require(value.isTextual(), label + "は文字列で入力してください。");
    return Optional.of(value.asString());
  }

  /** 必須条件を確認するを扱う。 */
  private static void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
