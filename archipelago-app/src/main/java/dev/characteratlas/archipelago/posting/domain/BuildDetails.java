package dev.characteratlas.archipelago.posting.domain;

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
    if (matrices.size() < 2 || matrices.size() > 9) {
      throw new IllegalArgumentException("航路紋を2種類以上選んでください。");
    }
    if (matrices.stream().map(MatrixSelection::name).distinct().count() != matrices.size()) {
      throw new IllegalArgumentException("異なる航路紋を選んでください。");
    }
    if (mainStats.size() != 3) {
      throw new IllegalArgumentException("装具スロット④・⑤・⑥を選んでください。");
    }
    if (targetStats.filter(value -> value.length() > 240).isPresent()) {
      throw new IllegalArgumentException("目標ステータスは240文字以内で入力してください。");
    }
  }
}
