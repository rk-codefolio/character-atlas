package dev.characteratlas.archipelago.posting.application;

import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 投稿の保存ポートを表す。 */
public interface PostStore {
  /** 指定された条件に沿って一覧を取得する。 */
  List<Post> list(PostKind kind, String subject);

  /** キャラクターの存在するか確認するを扱う。 */
  boolean characterExists(String id);

  /** 登録するを扱う。 */
  void insert(PostDraft draft);

  /** 置き換えるを扱う。 */
  Optional<String> replace(PostDraft draft);

  /** ロックするを扱う。 */
  Optional<String> lock(UUID id);

  /** 削除するを扱う。 */
  void delete(UUID id);
}
