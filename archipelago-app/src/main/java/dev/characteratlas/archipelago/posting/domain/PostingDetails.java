package dev.characteratlas.archipelago.posting.domain;

/** 投稿種別によって異なる詳細の制約を表す。 */
public sealed interface PostingDetails permits CommentDetails, BuildDetails, GuideDetails {}
