package dev.characteratlas.archipelago.posting.domain;

import dev.characteratlas.archipelago.posting.domain.details.BuildDetails;
import dev.characteratlas.archipelago.posting.domain.details.CommentDetails;
import dev.characteratlas.archipelago.posting.domain.details.GuideDetails;
import dev.characteratlas.archipelago.posting.domain.details.PostingDetails;
import dev.characteratlas.domain.validation.DomainChecks;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存前の投稿内容と種別ごとの詳細を保持する。
 *
 * <p>生成時に種別と詳細の組み合わせ、投稿先の形式、タイトルと本文の長さと必須条件を検証する。タイトルと本文は前後の空白を除去する。投稿先やタグの実在確認はアプリケーション層が行う。
 */
public record PostDraft(
    /* 識別子。 */ UUID id, /* 種別。 */
    PostKind kind, /* 投稿先の識別子。 */
    String subject, /* 投稿タイトル。 */
    String title, /* 投稿本文。 */
    String body, /* 投稿の種類別詳細。 */
    PostingDetails details,
    /* 関連コンテンツの識別子。 */ List<String> contentTags) {
  /**
   * 入力を正規化し、この型の生成条件を検証する。
   *
   * @throws dev.characteratlas.domain.validation.DomainValidationException この型が定める生成条件に違反する場合
   * @throws NullPointerException 必須の参照やコレクション要素がnullの場合
   */
  public PostDraft {
    Objects.requireNonNull(id);
    Objects.requireNonNull(kind);
    Objects.requireNonNull(subject);
    Objects.requireNonNull(details);
    contentTags = List.copyOf(contentTags);
    // 詳細の型から判定した投稿種別。
    PostKind detailKind =
        switch (details) {
          case CommentDetails() -> PostKind.COMMENT;
          case BuildDetails(_, _, _, _) -> PostKind.BUILD;
          case GuideDetails(_, _) -> PostKind.CONTENT;
        };
    DomainChecks.require(kind == detailKind, PostValidationError.KIND_DETAILS_MISMATCH);
    title = clean(title);
    body = clean(body);
    DomainChecks.require(
        title.length() <= 120 && body.length() <= 8000, PostValidationError.TEXT_TOO_LONG);
    DomainChecks.require(
        subject.matches("[a-z0-9][a-z0-9-]{0,39}"), PostValidationError.INVALID_SUBJECT);
    DomainChecks.require(
        kind == PostKind.COMMENT || !title.isEmpty(), PostValidationError.TITLE_REQUIRED);
    DomainChecks.require(
        kind == PostKind.BUILD || !body.isEmpty(), PostValidationError.BODY_REQUIRED);
  }

  /** cleanを扱う。 */
  private static String clean(String value) {
    return value == null ? "" : value.strip();
  }
}
