package dev.characteratlas.archipelago.moderation.infrastructure.discord;

import dev.characteratlas.domain.validation.ErrorMessage;

/** Discord 通知の設定エラー。 */
public enum DiscordConfigurationError implements ErrorMessage {
  /** Invalid Discord report webhook URL */
  INVALID_WEBHOOK("Invalid Discord report webhook URL"),
  /** Discord report notifications require a valid owner ID and at most 100 recipients */
  INVALID_RECIPIENTS(
      "Discord report notifications require a valid owner ID and at most 100 recipients"),
  /** Discord report notification exceeds 2000 characters. Reduce the recipient list. */
  MESSAGE_TOO_LONG(
      "Discord report notification exceeds 2000 characters. Reduce the recipient list.");

  /** 表示または記録に使う既定の文言。 */
  private final String message;

  DiscordConfigurationError(String message) {
    this.message = message;
  }

  @Override
  public String message() {
    return message;
  }
}
