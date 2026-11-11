package dev.characteratlas.archipelago.shared.presentation.error;

import dev.characteratlas.domain.validation.ErrorMessage;

/** 共通の API エラー文言。 */
enum ApiError implements ErrorMessage {
  /** 処理を完了できませんでした。時間をおいて再度お試しください。 */
  UNEXPECTED_FAILURE("処理を完了できませんでした。時間をおいて再度お試しください。");

  /** 表示または記録に使う既定の文言。 */
  private final String message;

  ApiError(String message) {
    this.message = message;
  }

  @Override
  public String message() {
    return message;
  }
}
