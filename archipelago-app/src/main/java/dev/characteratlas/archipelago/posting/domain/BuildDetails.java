package dev.characteratlas.archipelago.posting.domain;

import dev.characteratlas.domain.DomainChecks;
import java.util.List;
import java.util.Optional;

/** ビルドに必要な航海具、航路紋、装具スロットの組み合わせを表す。 */
public record BuildDetails(
    /* 航海具の名称。 */ String shell, /* 選択した航路紋。 */
    List<MatrixSelection> matrices, /* 3枠のメインステータス。 */
    List<String> mainStats,
    /* 任意の目標ステータス。 */ Optional<String> targetStats)
    implements PostingDetails {
  /** ビルドの詳細を初期化する。 */
  public BuildDetails {
    matrices = List.copyOf(matrices);
    mainStats = List.copyOf(mainStats);
    DomainChecks.require(!(matrices.size() < 2 || matrices.size() > 9), PostValidationMessages.MATRIX_COUNT);
    DomainChecks.require(!(matrices.stream().map(MatrixSelection::name).distinct().count() != matrices.size()), PostValidationMessages.DUPLICATE_MATRICES);
    DomainChecks.require(!(mainStats.size() != 3), PostValidationMessages.MAIN_STAT_SLOTS);
    DomainChecks.require(!(targetStats.filter(value -> value.length() > 240).isPresent()), PostValidationMessages.TARGET_STATS_TOO_LONG);
  }
}
