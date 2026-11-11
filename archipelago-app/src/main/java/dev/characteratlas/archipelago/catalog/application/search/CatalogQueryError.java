package dev.characteratlas.archipelago.catalog.application.search;

import dev.characteratlas.domain.validation.ErrorMessage;

/** 図鑑の照会条件に関するエラー。 */
public enum CatalogQueryError implements ErrorMessage {
  /** 不明な役割があります */
  UNKNOWN_ROLE("不明な役割があります"),
  /** コンテンツが見つかりません。 */
  UNKNOWN_CONTENT("コンテンツが見つかりません。");

  /** 表示または記録に使う既定の文言。 */
  private final String message;

  CatalogQueryError(String message) {
    this.message = message;
  }

  @Override
  public String message() {
    return message;
  }
}
