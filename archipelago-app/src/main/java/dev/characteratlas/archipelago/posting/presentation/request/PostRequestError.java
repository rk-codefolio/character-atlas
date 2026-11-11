package dev.characteratlas.archipelago.posting.presentation.request;

import dev.characteratlas.domain.validation.ErrorMessage;

/** 投稿リクエストの形式違反。 */
public enum PostRequestError implements ErrorMessage {
  /** ステージ・難易度は文字列で入力してください。 */
  STAGE_NOT_TEXT("ステージ・難易度は文字列で入力してください。"),
  /** 目標ステータスは文字列で入力してください。 */
  TARGET_STATS_NOT_TEXT("目標ステータスは文字列で入力してください。");

  /** 表示または記録に使う既定の文言。 */
  private final String message;

  PostRequestError(String message) {
    this.message = message;
  }

  @Override
  public String message() {
    return message;
  }
}
