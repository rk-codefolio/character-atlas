package dev.characteratlas.archipelago.posting.presentation;

import dev.characteratlas.archipelago.catalog.application.options.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.options.Content;
import dev.characteratlas.archipelago.catalog.application.options.Options;
import dev.characteratlas.archipelago.catalog.application.search.SearchContentOptions;
import dev.characteratlas.archipelago.moderation.infrastructure.discord.DiscordModerationAccess;
import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostService;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.posting.domain.PostValidationError;
import dev.characteratlas.archipelago.posting.domain.details.BuildDetails;
import dev.characteratlas.archipelago.posting.domain.details.CommentDetails;
import dev.characteratlas.archipelago.posting.domain.details.GuideDetails;
import dev.characteratlas.archipelago.posting.domain.details.MatrixSelection;
import dev.characteratlas.archipelago.posting.domain.details.PostingDetails;
import dev.characteratlas.archipelago.posting.presentation.request.PostRequest;
import dev.characteratlas.archipelago.posting.presentation.request.PostRequestError;
import dev.characteratlas.archipelago.posting.presentation.request.PostSelections;
import dev.characteratlas.archipelago.shared.presentation.error.ApiRequestException;
import dev.characteratlas.domain.validation.DomainValidationException;
import dev.characteratlas.domain.validation.ErrorMessage;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
      require(
          post.kind() != null
              && post.subject() != null
              && post.details() != null
              && post.details().isObject(),
          PostValidationError.INVALID_SUBJECT);
      require(post.details().toString().length() <= 12000, PostValidationError.TEXT_TOO_LONG);
      // 投稿種別ごとにREST入力をドメインの値に変換する。
      PostingDetails details =
          switch (post.kind()) {
            case COMMENT -> new CommentDetails();
            case BUILD -> build(post.details());
            case CONTENT ->
                new GuideDetails(
                    optionalText(post.details().path("stage"), PostRequestError.STAGE_NOT_TEXT),
                    selections(post.details(), "relatedCharacters", "relatedCharacter"));
          };
      return new PostDraft(
          id,
          post.kind(),
          post.subject(),
          post.title(),
          post.body(),
          details,
          selections(post.details(), "contentTags", "contentTag"));
    } catch (DomainValidationException error) {
      throw error;
    } catch (IllegalArgumentException error) {
      throw new ApiRequestException(
          HttpStatus.BAD_REQUEST, PostValidationError.INVALID_SUBJECT, error);
    }
  }

  /** selectionsを扱う。 */
  private static List<String> selections(JsonNode details, String field, String legacyField) {
    try {
      return PostSelections.read(details, field, legacyField);
    } catch (IllegalArgumentException e) {
      throw new ApiRequestException(
          HttpStatus.BAD_REQUEST, PostValidationError.INVALID_SELECTION, e);
    }
  }

  /** ビルドを扱う。 */
  private static BuildDetails build(JsonNode details) {
    // matrices。
    var matrices = details.path("matrices");
    require(matrices.isArray(), PostValidationError.MATRIX_COUNT);
    // 名称一覧。
    var selections =
        matrices
            .valueStream()
            .map(matrix -> {
              // 航路紋のポイント。
              var points = matrix.path("points");
              require(
                  points.isMissingNode()
                      || points.isNull()
                      || points.isIntegralNumber() && points.bigIntegerValue().signum() >= 0,
                  PostValidationError.NEGATIVE_MATRIX_POINTS);
              return new MatrixSelection(
                  matrix.path("name").asString(""),
                  points.isIntegralNumber()
                      ? Optional.of(points.bigIntegerValue())
                      : Optional.empty());
            })
            .toList();
    // 能力値の組み合わせ。
    var stats = details.path("mainStats");
    require(stats.isArray(), PostValidationError.MAIN_STAT_SLOTS);
    return new BuildDetails(
        details.path("shell").asString(""),
        selections,
        stats.valueStream().map(stat -> stat.asString("")).toList(),
        optionalText(details.path("targetStats"), PostRequestError.TARGET_STATS_NOT_TEXT));
  }

  /** 任意の説明文を扱う。 */
  private static Optional<String> optionalText(JsonNode value, ErrorMessage error) {
    if (value.isMissingNode() || value.isNull()) return Optional.empty();
    require(value.isTextual(), error);
    return Optional.of(value.asString());
  }

  /** 必須条件を確認するを扱う。 */
  private static void require(boolean condition, ErrorMessage message) {
    if (!condition) throw new ApiRequestException(HttpStatus.BAD_REQUEST, message);
  }
}
