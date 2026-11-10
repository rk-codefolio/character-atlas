package dev.characteratlas.archipelago.posting.application;

import dev.characteratlas.archipelago.moderation.application.AuditLog;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.shared.application.UseCaseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 投稿のユースケースを表す。 */
@Service
public class PostService {
  /** 保存ポート。 */
  private final PostStore store;

  /** 監査記録のポート。 */
  private final AuditLog log;

  /** 投稿のユースケースを初期化する。 */
  public PostService(PostStore store, AuditLog log) {
    this.store = store;
    this.log = log;
  }

  /** 指定された条件に沿って一覧を取得する。 */
  public List<Post> list(PostKind kind, String subject) {
    return store.list(kind, subject);
  }

  /** キャラクターの存在するか確認するを扱う。 */
  public boolean characterExists(String id) {
    return store.characterExists(id);
  }

  /** 検証した入力を新しい投稿として登録する。 */
  public UUID create(PostDraft draft) {
    store.insert(draft);
    return draft.id();
  }

  /** 投稿の更新と監査記録を同じ操作として実行する。 */
  @Transactional
  public void edit(String actor, PostDraft draft) {
    // 変更前。
    String before = store.lock(draft.id()).orElseThrow(UseCaseException::notFound);
    // 変更後。
    String after = store.replace(draft).orElseThrow(UseCaseException::notFound);
    log.append(actor, "EDIT_POST", draft.id().toString(), before, after);
  }

  /** 対象の投稿を削除する。 */
  @Transactional
  public void remove(String actor, UUID id) {
    // 変更前。
    String before = store.lock(id).orElseThrow(UseCaseException::notFound);
    store.delete(id);
    log.append(actor, "DELETE_POST", id.toString(), before, null);
  }
}
