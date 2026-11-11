package dev.characteratlas.archipelago.posting.presentation;

import dev.characteratlas.archipelago.posting.domain.PostValidationMessages;
import dev.characteratlas.domain.DomainValidationException;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** 投稿のSelectionsを表す。 */
public final class PostSelections {
  /** 投稿のSelectionsを初期化する。 */
  private PostSelections() {}

  /** 読み取るを扱う。 */
  public static List<String> read(JsonNode details, String field, String legacyField) {
    // values。
    JsonNode values = details.path(field);
    if (values.isMissingNode() || values.isNull()) {
      // legacyの値。
      JsonNode legacyValue = details.path(legacyField);
      // legacy。
      String legacy = legacyValue.isTextual() ? legacyValue.asString("") : "";
      return legacy.isBlank() ? List.of() : List.of(legacy);
    }
    if (!values.isArray()
        || values
            .valueStream()
            .anyMatch(value -> !value.isTextual() || value.asString("").isBlank())) {
      throw new DomainValidationException(PostValidationMessages.INVALID_SELECTION);
    }
    return values.valueStream().map(JsonNode::asString).distinct().toList();
  }
}
