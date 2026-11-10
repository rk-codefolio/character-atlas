package dev.characteratlas.archipelago.posting.infrastructure;


import tools.jackson.databind.JsonNode;

import java.util.List;

final public class PostSelections {
    public static final String CONTENT_TAGS = arrayOrLegacy("contentTags", "contentTag");
    public static final String RELATED_CHARACTERS = arrayOrLegacy("relatedCharacters", "relatedCharacter");

    private PostSelections() {}

    private static String arrayOrLegacy(String field, String legacyField) {
        return """
                CASE WHEN jsonb_typeof(details->'%1$s') = 'array' THEN details->'%1$s'
                     ELSE jsonb_build_array(details->>'%2$s') END
                """.formatted(field, legacyField);
    }

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
