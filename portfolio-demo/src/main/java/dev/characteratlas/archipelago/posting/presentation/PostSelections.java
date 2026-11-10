package dev.characteratlas.archipelago.posting.presentation;


import tools.jackson.databind.JsonNode;

import java.util.List;

final public class PostSelections {
    private PostSelections() {}

    public static List<String> read(JsonNode details, String field, String legacyField) {
        JsonNode values = details.path(field);
        if (values.isMissingNode() || values.isNull()) {
            JsonNode legacyValue = details.path(legacyField);
            String legacy = legacyValue.isTextual() ? legacyValue.asString("") : "";
            return legacy.isBlank() ? List.of() : List.of(legacy);
        }
        if (!values.isArray() || values.valueStream().anyMatch(value -> !value.isTextual() || value.asString("").isBlank())) {
            throw new IllegalArgumentException("選択項目を確認してください。");
        }
        return values.valueStream().map(JsonNode::asString).distinct().toList();
    }
}
