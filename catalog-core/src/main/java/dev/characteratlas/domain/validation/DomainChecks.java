package dev.characteratlas.domain.validation;

/** ドメイン型が定めた生成条件を検証する。 */
public final class DomainChecks {
  private DomainChecks() {}

  /**
   * 指定された条件が成り立つことを確認する。
   *
   * @param condition ドメイン型が定義する条件の判定結果
   * @param message 条件違反時に返す文言
   * @throws DomainValidationException 条件が成立しない場合
   */
  public static void require(boolean condition, ErrorMessage message) {
    if (!condition) {
      throw new DomainValidationException(message);
    }
  }
}
