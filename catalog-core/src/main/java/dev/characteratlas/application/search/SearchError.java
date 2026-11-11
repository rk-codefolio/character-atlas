package dev.characteratlas.application.search;

import dev.characteratlas.domain.validation.ErrorMessage;

/** 図鑑検索の入力違反を識別する。 */
public enum SearchError implements ErrorMessage {
  /** 不明な検索条件があります */
  UNKNOWN_SEARCH_TERM("不明な検索条件があります");

  /** 表示または記録に使う既定の文言。 */
  private final String message;

  SearchError(String message) {
    this.message = message;
  }

  @Override
  public String message() {
    return message;
  }
}
