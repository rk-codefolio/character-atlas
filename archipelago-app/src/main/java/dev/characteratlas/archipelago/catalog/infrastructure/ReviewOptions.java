package dev.characteratlas.archipelago.catalog.infrastructure;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.Content;
import dev.characteratlas.archipelago.catalog.application.EffectTip;
import dev.characteratlas.archipelago.catalog.application.Options;
import dev.characteratlas.archipelago.catalog.application.Progress;
import dev.characteratlas.archipelago.catalog.application.SkillInfo;
import dev.characteratlas.archipelago.catalog.application.SkillLevel;
import dev.characteratlas.domain.CharacterProfile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Reviewの選択肢を表す。 */
@Component
public class ReviewOptions implements CatalogOptions {
  /** PostgreSQLのクエリーを実行するMapper。 */
  private final OptionsMapper mapper;

  /** 同梱の図鑑設定。 */
  private final Book book;

  /** Reviewの選択肢を初期化する。 */
  ReviewOptions(OptionsMapper mapper, ObjectMapper json) throws IOException {
    this.mapper = mapper;
    try (var input = new ClassPathResource("archipelago-options.json").getInputStream()) {
      book = json.readValue(input, Book.class);
    }
  }

  /** 取得するを扱う。 */
  @Override
  public Options get() {
    // roles。
    var roles =
        mapper.roles().stream()
            .collect(
                Collectors.groupingBy(
                    RoleRow::role, Collectors.mapping(RoleRow::characterId, Collectors.toList())));
    // kinds。
    var kinds =
        mapper.kinds().stream()
            .collect(Collectors.toMap(KindRow::label, KindRow::kind, (first, _) -> first));
    return new Options(
        book.contents(),
        book.shells(),
        book.matrices(),
        book.mainStats(),
        book.template(),
        roles,
        kinds,
        book.attributes(),
        mapper.advanced());
  }

  /** isのSearchableを扱う。 */
  @Override
  public boolean isSearchable(String id) {
    return book.searchable().contains(id);
  }

  /** contentsを扱う。 */
  @Override
  public List<Content> contents() {
    return book.contents();
  }

  /** effectsを扱う。 */
  @Override
  public List<EffectTip> effects(String termId) {
    return mapper.effects(termId);
  }

  /** 育成情報を扱う。 */
  @Override
  public Progress progress(CharacterProfile character) {
    // levels。
    var levels =
        mapper.levels(character.id()).stream()
            .collect(
                Collectors.groupingBy(
                    LevelRow::skillId,
                    Collectors.mapping(
                        row -> new SkillLevel(row.level(), row.description()),
                        Collectors.toList())));
    // 補足情報。
    var metadata =
        mapper.metadata(character.id()).stream()
            .collect(
                Collectors.toMap(
                    MetaRow::skillId, row -> new SkillInfo(row.passive(), row.cooldown())));
    return new Progress(
        mapper.awakened(character.id()),
        book.attributes().get(character.attribute()),
        levels,
        metadata);
  }
}
