package dev.characteratlas.archipelago.posting.domain;

import java.util.Objects;
import java.util.UUID;

/** 投稿の入力内容を表す。 */
public record PostDraft(
    /* 識別子。 */ UUID id, /* 種別。 */
    PostKind kind, /* 投稿先の識別子。 */
    String subject, /* 投稿タイトル。 */
    String title, /* 投稿本文。 */
    String body, /* 投稿の種類別詳細。 */
    String details) {
  /** 投稿の入力内容を初期化する。 */
  public PostDraft {
    Objects.requireNonNull(id);
    Objects.requireNonNull(kind);
    Objects.requireNonNull(subject);
    Objects.requireNonNull(details);
    title = clean(title);
    body = clean(body);
    if (title.length() > 120 || body.length() > 8000 || details.length() > 12000) {
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
