package dev.characteratlas.domain.validation;

/** 入力または業務上の条件によって発生する、処理方法が定義された失敗。 */
public abstract class ExpectedException extends IllegalArgumentException {
  /** 呼び出し側が判定に使うエラー識別子。 */
  private final ErrorMessage error;

  /**
   * @return エラー識別子
   */
  public ErrorMessage error() {
    return error;
  }

  /**
   * 失敗の理由を保持する。
   *
   * @param message 呼び出し側に伝える失敗理由
   */
  protected ExpectedException(ErrorMessage message) {
    super(message == null ? null : message.message());
    this.error = message;
  }
}
