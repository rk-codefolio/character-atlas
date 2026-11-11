package dev.characteratlas.archipelago.catalog.application.options;

import java.util.List;

/** コンテンツを表す。 */
public record Content(
    /* 識別子。 */ String id, /* 表示名。 */
    String name, /* 種別。 */
    String kind, /* 同名条件のグループ。 */
    String group, /* aliases。 */
    List<String> aliases) {}
