package dev.characteratlas.archipelago.posting.application;

import dev.characteratlas.archipelago.catalog.application.CatalogOptions;
import dev.characteratlas.archipelago.catalog.application.Content;
import dev.characteratlas.archipelago.moderation.application.AuditLog;
import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.posting.domain.PostValidationMessages;
import dev.characteratlas.archipelago.posting.domain.details.BuildDetails;
import dev.characteratlas.archipelago.posting.domain.details.CommentDetails;
import dev.characteratlas.archipelago.posting.domain.details.GuideDetails;
import dev.characteratlas.archipelago.posting.domain.details.MatrixSelection;
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

  /** 投稿先と装具の選択肢を照会するポート。 */
  private final CatalogOptions options;

  /** 投稿のユースケースを初期化する。 */
  public PostService(PostStore store, AuditLog log, CatalogOptions options) {
    this.store = store;
    this.log = log;
    this.options = options;
  }

  /** 指定された条件に沿って一覧を取得する。 */
  public List<Post> list(PostKind kind, String subject) {
    return store.list(kind, subject);
  }

  /** 検証した入力を新しい投稿として登録する。 */
  public UUID create(PostDraft draft) {
    validate(draft);
    store.insert(draft);
    return draft.id();
  }

  /** 投稿の更新と監査記録を同じ操作として実行する。 */
  @Transactional
  public void edit(String actor, PostDraft draft) {
    validate(draft);
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

  /** 投稿先、タグ、装具が図鑑に登録されていることを確認する。 */
  private void validate(PostDraft draft) {
    // 図鑑の登録済みコンテンツ識別子。
    var contentIds = options.contents().stream().map(Content::id).toList();
    require(
        contentIds.containsAll(draft.contentTags()), PostValidationMessages.CONTENT_TAG_NOT_FOUND);
    switch (draft.details()) {
      case GuideDetails(_, var characters) -> {
        require(contentIds.contains(draft.subject()), PostValidationMessages.CONTENT_REQUIRED);
        require(
            characters.stream().allMatch(store::characterExists),
            PostValidationMessages.RELATED_CHARACTER_NOT_FOUND);
      }
      case BuildDetails(var shell, var matrices, var stats, _) -> {
        require(store.characterExists(draft.subject()), PostValidationMessages.CHARACTER_NOT_FOUND);
        // ビルドで選択できる装具の一覧。
        var equipment = options.get();
        require(equipment.shells().contains(shell), PostValidationMessages.SHELL_REQUIRED);
        // 登録済みの航路紋の名称。
        var available = equipment.matrices().stream().map(matrix -> matrix.name()).toList();
        require(
            available.containsAll(matrices.stream().map(MatrixSelection::name).toList()),
            PostValidationMessages.DUPLICATE_MATRICES);
        require(equipment.mainStats().containsAll(stats), PostValidationMessages.MAIN_STAT_SLOTS);
      }
      case CommentDetails() ->
          require(
              store.characterExists(draft.subject()), PostValidationMessages.CHARACTER_NOT_FOUND);
    }
  }

  /** 利用者が指定した項目が図鑑にない場合はユースケースを中断する。 */
  private static void require(boolean condition, String message) {
    if (!condition) throw UseCaseException.invalid(message);
  }
}
