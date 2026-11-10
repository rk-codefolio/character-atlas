package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.Content;
import dev.characteratlas.archipelago.catalog.application.Matrix;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 同梱設定を表す。 */
public record Book(
    /* contents。 */ List<Content> contents, /* searchable。 */
    Set<String> searchable, /* shells。 */
    List<String> shells, /* matrices。 */
    List<Matrix> matrices,
    /* 起動処理のStats。 */ List<String> mainStats, /* template。 */
    List<String> template, /* attributes。 */
    Map<String, String> attributes) {}
