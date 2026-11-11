package dev.characteratlas.domain;

/** ドメインの生成条件を満たさない入力を表す。 */
public final class DomainValidationException extends ExpectedException {
  /**
   * 検証に失敗した理由を保持する。
   *
   * @param message 違反した条件を伝える文言
   */
  public DomainValidationException(String message) {
    super(message);
  }
}
