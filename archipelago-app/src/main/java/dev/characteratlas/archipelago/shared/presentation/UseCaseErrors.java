package dev.characteratlas.archipelago.shared.presentation;

import dev.characteratlas.archipelago.shared.application.UseCaseException;
import dev.characteratlas.domain.DomainValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 業務上の失敗と予期しない障害を HTTP 応答に変換する。 */
@RestControllerAdvice
public class UseCaseErrors {
  /** 原因を含む障害記録の出力先。 */
  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(UseCaseErrors.class);

  /**
   * 予期しない障害の詳細を記録し、共通の応答を返す。
   *
   * @param error 処理を完了できなかった原因
   * @return HTTP 500 のエラー情報
   * @throws org.springframework.web.server.ResponseStatusException HTTP の状態が明示された場合
   */
  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception error) {
    if (error instanceof org.springframework.web.server.ResponseStatusException status) {
      throw status;
    }
    if (error instanceof org.springframework.web.ErrorResponse response) {
      return response.getBody();
    }
    LOGGER.error("API処理で予期しない障害が発生しました。", error);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorMessages.UNEXPECTED_FAILURE);
  }

  /**
   * ドメインの入力違反を HTTP 400 に変換する。
   *
   * @param error 違反した入力条件
   * @return 入力の修正に必要な情報
   */
  @ExceptionHandler(DomainValidationException.class)
  ProblemDetail invalidDomainInput(DomainValidationException error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
  }

  /** ユースケースが定義する失敗を HTTP ステータスへ変換する。 */
  @ExceptionHandler(UseCaseException.class)
  ResponseEntity<Void> translate(UseCaseException error) {
    return ResponseEntity.status(
            switch (error.kind()) {
              case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
              case NOT_FOUND -> HttpStatus.NOT_FOUND;
            })
        .build();
  }
}
