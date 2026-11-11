package dev.characteratlas.archipelago.posting.domain.details;

/**
 * コメント、ビルド、攻略で異なる投稿詳細を表す閉じた型。
 *
 * <p>CommentDetailsは追加項目のないコメント、BuildDetailsは装具構成、GuideDetailsは攻略の対象情報を表す。許可された型の追加時は投稿入力の変換と検証の分岐も更新する。
 */
public sealed interface PostingDetails permits CommentDetails, BuildDetails, GuideDetails {}
