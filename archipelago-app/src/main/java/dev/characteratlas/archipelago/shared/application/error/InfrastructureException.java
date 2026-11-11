package dev.characteratlas.archipelago.shared.application.error;

import dev.characteratlas.domain.validation.ErrorMessage;

/** 外部接続または実行環境の設定によって処理を完了できない障害。 */
public final class InfrastructureException extends RuntimeException {
  /**
   * 設定に関する障害を保持する。
   *
   * @param message 管理者が原因を調査するための情報
   */
  public InfrastructureException(ErrorMessage message) {
    super(message.message());
  }

  /**
   * 元の原因を保持して障害を通知する。
   *
   * @param message 失敗した処理の説明
   * @param cause 外部処理で発生した原因
   */
  public InfrastructureException(ErrorMessage message, Throwable cause) {
    super(message.message(), cause);
  }
}
