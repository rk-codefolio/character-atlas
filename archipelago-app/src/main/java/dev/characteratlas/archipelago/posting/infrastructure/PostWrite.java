package dev.characteratlas.archipelago.posting.infrastructure;

import java.util.UUID;

/** 投稿保存用のSQLパラメーター。詳細は既存DB形式のJSONを使用する。 */
record PostWrite(
    /* 投稿ID。 */ UUID id,
    /* 投稿の種別。 */ String kind,
    /* 投稿先のID。 */ String subject,
    /* 投稿タイトル。 */ String title,
    /* 投稿本文。 */ String body,
    /* 詳細のJSON。 */ String details) {}
