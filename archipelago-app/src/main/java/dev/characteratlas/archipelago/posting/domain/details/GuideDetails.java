package dev.characteratlas.archipelago.posting.domain.details;

import dev.characteratlas.archipelago.posting.domain.PostValidationError;
import dev.characteratlas.domain.validation.DomainChecks;
import java.util.List;
import java.util.Optional;

/**
 * 攻略投稿のステージ表記と関連渡り手の識別子を保持する。
 *
 * <p>ステージ表記は任意で120文字以内とし、関連渡り手の一覧を防御的にコピーする。識別子の実在確認はアプリケーション層が行う。
 */
public record GuideDetails(
    /* stage。 */ Optional<String> stage, /* 関連のCharacters。 */ List<String> relatedCharacters)
    implements PostingDetails {
  /**
   * 入力を正規化し、この型の生成条件を検証する。
   *
   * @throws dev.characteratlas.domain.validation.DomainValidationException この型が定める生成条件に違反する場合
   * @throws NullPointerException 必須の参照やコレクション要素がnullの場合
   */
  public GuideDetails {
    relatedCharacters = List.copyOf(relatedCharacters);
    DomainChecks.require(
        !(stage.filter(value -> value.length() > 120).isPresent()),
        PostValidationError.STAGE_TOO_LONG);
  }
}
