package dev.characteratlas.archipelago.posting.infrastructure;

import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostStore;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

/** MyのBatisの投稿の保存ポートを表す。 */
@Repository
public class MyBatisPostStore implements PostStore {
  /** PostgreSQLのクエリーを実行するMapper。 */
  private final PostMapper mapper;

  /** json。 */
  private final ObjectMapper json;

  /** MyのBatisの投稿の保存ポートを初期化する。 */
  MyBatisPostStore(PostMapper mapper, ObjectMapper json) {
    this.mapper = mapper;
    this.json = json;
  }

  /** 指定された条件に沿って一覧を取得する。 */
  @Override
  public List<Post> list(PostKind kind, String subject) {
    return mapper.list(kind.name(), subject).stream()
        .map(row ->
            new Post(
                row.id(),
                row.title(),
                row.body(),
                row.details(),
                row.createdAt(),
                row.updatedAt(),
                List.of(json.readValue(row.relatedNames(), String[].class))))
        .toList();
  }

  /** キャラクターの存在するか確認するを扱う。 */
  @Override
  public boolean characterExists(String id) {
    return mapper.characterExists(id);
  }

  /** 登録するを扱う。 */
  @Override
  public void insert(PostDraft draft) {
    mapper.insert(draft);
  }

  /** 置き換えるを扱う。 */
  @Override
  public Optional<String> replace(PostDraft draft) {
    return Optional.ofNullable(mapper.replace(draft));
  }

  /** ロックするを扱う。 */
  @Override
  public Optional<String> lock(UUID id) {
    return Optional.ofNullable(mapper.lock(id));
  }

  /** 削除するを扱う。 */
  @Override
  public void delete(UUID id) {
    mapper.delete(id);
  }
}
