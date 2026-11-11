package dev.characteratlas.archipelago.catalog.application.options;

import java.util.List;
import java.util.Map;

/** 選択肢を表す。 */
public record Options(
    /* contents。 */ List<Content> contents, /* shells。 */
    List<String> shells, /* matrices。 */
    List<Matrix> matrices, /* 起動処理のStats。 */
    List<String> mainStats,
    /* template。 */ List<String> template, /* roles。 */
    Map<String, List<String>> roles, /* 効果のKinds。 */
    Map<String, String> effectKinds,
    /* attributes。 */ Map<String, String> attributes, /* advancedのIds。 */
    List<String> advancedIds) {}
