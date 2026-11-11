package dev.characteratlas.archipelago.shared.presentation;

import dev.characteratlas.archipelago.shared.application.UseCaseException;
import dev.characteratlas.domain.DomainValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 処理のユースケースのErrorsを表す。 */
@RestControllerAdvice
public class UseCaseErrors {
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
