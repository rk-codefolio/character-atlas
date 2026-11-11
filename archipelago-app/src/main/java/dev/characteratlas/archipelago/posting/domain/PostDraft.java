package dev.characteratlas.archipelago.posting.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 投稿の入力内容を表す。 */
public record PostDraft(
    /* 識別子。 */ UUID id, /* 種別。 */
    PostKind kind, /* 投稿先の識別子。 */
    String subject, /* 投稿タイトル。 */
    String title, /* 投稿本文。 */
    String body, /* 投稿の種類別詳細。 */
    PostingDetails details,
    /* 関連コンテンツの識別子。 */ List<String> contentTags) {
  /** 投稿の入力内容を初期化する。 */
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
    if (kind != detailKind) {
      throw new IllegalArgumentException("投稿種別と詳細の組み合わせが不正です。");
    }
    title = clean(title);
    body = clean(body);
    if (title.length() > 120 || body.length() > 8000) {
      throw new IllegalArgumentException("入力が長すぎます。");
    }
    if (!subject.matches("[a-z0-9][a-z0-9-]{0,39}")) {
      throw new IllegalArgumentException("投稿先か入力項目が不正です。");
    }
    if (kind != PostKind.COMMENT && title.isEmpty()) {
      throw new IllegalArgumentException("タイトルを入力してください。");
    }
    if (kind != PostKind.BUILD && body.isEmpty()) {
      throw new IllegalArgumentException("本文を入力してください。");
    }
  }

  /** cleanを扱う。 */
  private static String clean(String value) {
    return value == null ? "" : value.strip();
  }
}
