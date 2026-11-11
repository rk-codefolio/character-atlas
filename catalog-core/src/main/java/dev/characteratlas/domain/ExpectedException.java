package dev.characteratlas.domain;

/** 入力または業務上の条件によって発生する、処理方法が定義された失敗。 */
public abstract class ExpectedException extends IllegalArgumentException {
  /**
   * 失敗の理由を保持する。
   *
   * @param message 呼び出し側に伝える失敗理由
   */
  protected ExpectedException(String message) {
    super(message);
  }
}
