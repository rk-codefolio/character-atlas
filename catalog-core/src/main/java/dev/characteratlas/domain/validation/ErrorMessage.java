package dev.characteratlas.domain.validation;

/** アプリケーションが定義するエラーの識別子と既定文言を結び付ける。 */
public interface ErrorMessage {
  /**
   * @return 表示または記録に使う既定の文言
   */
  String message();
}
