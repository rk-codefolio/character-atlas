package dev.characteratlas.archipelago.posting.domain;

import dev.characteratlas.domain.validation.ErrorMessage;

/** 投稿の入力違反を識別する。 */
public enum PostValidationError implements ErrorMessage {
  /** 投稿種別と詳細の組み合わせが不正です。 */
  KIND_DETAILS_MISMATCH("投稿種別と詳細の組み合わせが不正です。"),
  /** 入力が長すぎます。 */
  TEXT_TOO_LONG("入力が長すぎます。"),
  /** 投稿先か入力項目が不正です。 */
  INVALID_SUBJECT("投稿先か入力項目が不正です。"),
  /** タイトルを入力してください。 */
  TITLE_REQUIRED("タイトルを入力してください。"),
  /** 本文を入力してください。 */
  BODY_REQUIRED("本文を入力してください。"),
  /** 航路紋を2種類以上選んでください。 */
  MATRIX_COUNT("航路紋を2種類以上選んでください。"),
  /** 異なる航路紋を選んでください。 */
  DUPLICATE_MATRICES("異なる航路紋を選んでください。"),
  /** 装具スロット④・⑤・⑥を選んでください。 */
  MAIN_STAT_SLOTS("装具スロット④・⑤・⑥を選んでください。"),
  /** 目標ステータスは240文字以内で入力してください。 */
  TARGET_STATS_TOO_LONG("目標ステータスは240文字以内で入力してください。"),
  /** ステージ・難易度は120文字以内で入力してください。 */
  STAGE_TOO_LONG("ステージ・難易度は120文字以内で入力してください。"),
  /** ポイントは空欄か0以上の整数にしてください。 */
  NEGATIVE_MATRIX_POINTS("ポイントは空欄か0以上の整数にしてください。"),
  /** コンテンツタグが見つかりません。 */
  CONTENT_TAG_NOT_FOUND("コンテンツタグが見つかりません。"),
  /** コンテンツを選んでください。 */
  CONTENT_REQUIRED("コンテンツを選んでください。"),
  /** 関連渡り手が見つかりません。 */
  RELATED_CHARACTER_NOT_FOUND("関連渡り手が見つかりません。"),
  /** キャラクターが見つかりません。 */
  CHARACTER_NOT_FOUND("キャラクターが見つかりません。"),
  /** 航海具を選んでください。 */
  SHELL_REQUIRED("航海具を選んでください。"),
  /** 選択項目を確認してください。 */
  INVALID_SELECTION("選択項目を確認してください。");

  /** 表示または記録に使う既定の文言。 */
  private final String message;

  PostValidationError(String message) {
    this.message = message;
  }

  @Override
  public String message() {
    return message;
  }
}
