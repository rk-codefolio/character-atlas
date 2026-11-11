package dev.characteratlas.archipelago.shared.presentation;

/** API の共通エラー応答に使用する文言。 */
final class ApiErrorMessages {
  private ApiErrorMessages() {}

  /** 内部原因の開示を伴わない障害の通知。 */
  static final String UNEXPECTED_FAILURE = "処理を完了できませんでした。時間をおいて再度お試しください。";
}
