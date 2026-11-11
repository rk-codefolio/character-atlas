package dev.characteratlas.archipelago.posting.infrastructure;

import dev.characteratlas.archipelago.posting.application.Post;
import dev.characteratlas.archipelago.posting.application.PostStore;
import dev.characteratlas.archipelago.posting.domain.BuildDetails;
import dev.characteratlas.archipelago.posting.domain.CommentDetails;
import dev.characteratlas.archipelago.posting.domain.GuideDetails;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.shared.application.UseCaseException;
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
    mapper.insert(toWrite(draft));
  }

  /** 置き換えるを扱う。 */
  @Override
  public Optional<String> replace(PostDraft draft) {
    return Optional.ofNullable(mapper.replace(toWrite(draft)));
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

  /** 型付きの詳細から既存スキーマと互換性のあるJSONを組み立てる。 */
  private PostWrite toWrite(PostDraft draft) {
    // PostgreSQLのdetails列に格納するオブジェクト。
    var details = json.createObjectNode();
    if (!draft.contentTags().isEmpty()) {
      // 複数のコンテンツタグを格納する配列。
      var tags = details.putArray("contentTags");
      draft.contentTags().forEach(tags::add);
    }
    switch (draft.details()) {
      case CommentDetails() -> {}
      case GuideDetails(var stage, var characters) -> {
        stage.ifPresent(value -> details.put("stage", value));
        // 攻略に関連するキャラクターIDの配列。
        var related = details.putArray("relatedCharacters");
        characters.forEach(related::add);
      }
      case BuildDetails(var shell, var matrices, var stats, var targetStats) -> {
        details.put("shell", shell);
        // 航路紋とポイントの配列。
        var matrixRows = details.putArray("matrices");
        matrices.forEach(matrix -> {
          // 航路紋ごとの名称とポイント。
          var row = matrixRows.addObject().put("name", matrix.name());
          matrix
              .points()
              .ifPresentOrElse(value -> row.put("points", value), () -> row.putNull("points"));
        });
        // 装具3枠のメインステータス。
        var slots = details.putArray("mainStats");
        stats.forEach(slots::add);
        targetStats.ifPresent(value -> details.put("targetStats", value));
      }
    }
    // SQLパラメーターに渡すJSON文字列。
    String encoded = details.toString();
    if (encoded.length() > 12000) throw UseCaseException.invalid("入力が長すぎます。");
    return new PostWrite(
        draft.id(), draft.kind().name(), draft.subject(), draft.title(), draft.body(), encoded);
  }
}
