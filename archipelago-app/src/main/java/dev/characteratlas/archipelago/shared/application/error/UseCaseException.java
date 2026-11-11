package dev.characteratlas.archipelago.shared.application.error;

import dev.characteratlas.domain.validation.ErrorMessage;
import dev.characteratlas.domain.validation.ExpectedException;

/** 処理のユースケースの例外を表す。 */
public final class UseCaseException extends ExpectedException {

  /** 種別。 */
  private final UseCaseFailure kind;

  /** 処理のユースケースの例外を初期化する。 */
  private UseCaseException(UseCaseFailure kind, ErrorMessage message) {
    super(message);
    this.kind = kind;
  }

  /** 種別を扱う。 */
  public UseCaseFailure kind() {
    return kind;
  }

  /** invalidを扱う。 */
  public static UseCaseException invalid(ErrorMessage message) {
    return new UseCaseException(UseCaseFailure.INVALID_INPUT, message);
  }

  /** notのFoundを扱う。 */
  public static UseCaseException notFound() {
    return new UseCaseException(UseCaseFailure.NOT_FOUND, null);
  }
}
