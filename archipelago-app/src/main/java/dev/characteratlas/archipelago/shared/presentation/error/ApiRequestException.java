package dev.characteratlas.archipelago.shared.presentation.error;

import dev.characteratlas.domain.validation.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** HTTP入力またはアクセス条件を満たさないリクエスト。 */
public final class ApiRequestException extends ResponseStatusException {
  /**
   * HTTP境界で拒否する理由を保持する。
   *
   * @param status 返却するHTTPステータス
   */
  public ApiRequestException(HttpStatus status) {
    super(status);
  }

  /**
   * 利用者に提示する説明を保持する。
   *
   * @param status 返却するHTTPステータス
   * @param message 拒否した条件の説明
   */
  public ApiRequestException(HttpStatus status, ErrorMessage message) {
    super(status, message.message());
  }

  /**
   * 入力の変換に失敗した原因を保持する。
   *
   * @param status 返却するHTTPステータス
   * @param message 拒否した条件の説明
   * @param cause 入力変換時の原因
   */
  public ApiRequestException(HttpStatus status, ErrorMessage message, Throwable cause) {
    super(status, message.message(), cause);
  }
}
