package dev.characteratlas.archipelago.posting.domain;

import java.util.Objects;
import java.util.UUID;

public record PostDraft(UUID id, PostKind kind, String subject, String title, String body, String details) {
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

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
