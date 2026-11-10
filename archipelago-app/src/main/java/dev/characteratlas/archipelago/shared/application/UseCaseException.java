package dev.characteratlas.archipelago.shared.application;

/** 処理のユースケースの例外を表す。 */
public final class UseCaseException extends RuntimeException {

  /** 種別。 */
  private final UseCaseFailure kind;

  /** 処理のユースケースの例外を初期化する。 */
  private UseCaseException(UseCaseFailure kind, String message) {
    super(message);
    this.kind = kind;
  }

  /** 種別を扱う。 */
  public UseCaseFailure kind() {
    return kind;
  }

  /** invalidを扱う。 */
  public static UseCaseException invalid(String message) {
    return new UseCaseException(UseCaseFailure.INVALID_INPUT, message);
  }

  /** notのFoundを扱う。 */
  public static UseCaseException notFound() {
    return new UseCaseException(UseCaseFailure.NOT_FOUND, null);
  }
}
