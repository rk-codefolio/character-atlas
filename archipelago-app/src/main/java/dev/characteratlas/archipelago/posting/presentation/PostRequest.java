package dev.characteratlas.archipelago.posting.presentation;

import dev.characteratlas.archipelago.posting.domain.PostKind;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

/** 投稿のリクエストを表す。 */
public record PostRequest(
    /* 種別。 */ PostKind kind, /* 投稿先の識別子。 */
    String subject, /* 投稿タイトル。 */
    String title, /* 投稿本文。 */
    String body, /* 投稿の種類別詳細。 */
    JsonNode details) {}
