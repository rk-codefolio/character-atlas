package dev.characteratlas.archipelago.posting.domain.details;

import dev.characteratlas.archipelago.posting.domain.PostValidationMessages;
import dev.characteratlas.domain.DomainChecks;
import java.util.List;
import java.util.Optional;

/**
 * ビルド投稿に必要な装具と航路紋の選択を保持する。
 *
 * <p>航路紋は2〜9種類で名称の重複を許可しない。メインステータスは3枠、目標ステータスは240文字以内とする。選択肢への登録有無はアプリケーション層が確認する。
 */
public record BuildDetails(
    /* 航海具の名称。 */ String shell, /* 選択した航路紋。 */
    List<MatrixSelection> matrices, /* 3枠のメインステータス。 */
    List<String> mainStats,
    /* 任意の目標ステータス。 */ Optional<String> targetStats)
    implements PostingDetails {
  /**
   * 入力を正規化し、この型の生成条件を検証する。
   *
   * @throws dev.characteratlas.domain.DomainValidationException この型が定める生成条件に違反する場合
   * @throws NullPointerException 必須の参照やコレクション要素がnullの場合
   */
  public BuildDetails {
    matrices = List.copyOf(matrices);
    mainStats = List.copyOf(mainStats);
    DomainChecks.require(
        !(matrices.size() < 2 || matrices.size() > 9), PostValidationMessages.MATRIX_COUNT);
    DomainChecks.require(
        !(matrices.stream().map(MatrixSelection::name).distinct().count() != matrices.size()),
        PostValidationMessages.DUPLICATE_MATRICES);
    DomainChecks.require(!(mainStats.size() != 3), PostValidationMessages.MAIN_STAT_SLOTS);
    DomainChecks.require(
        !(targetStats.filter(value -> value.length() > 240).isPresent()),
        PostValidationMessages.TARGET_STATS_TOO_LONG);
  }
}
