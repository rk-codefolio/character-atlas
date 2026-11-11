package dev.characteratlas.archipelago.posting.domain;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/** 選択した航路紋と任意のポイント数。 */
public record MatrixSelection(
    /* 航路紋の名称。 */ String name, /* ポイントが未入力なら空。 */ Optional<BigInteger> points) {
  /** ポイントが入力された場合は非負であることを確認する。 */
  public MatrixSelection {
    Objects.requireNonNull(name);
    Objects.requireNonNull(points);
    if (points.filter(value -> value.signum() < 0).isPresent()) {
      throw new IllegalArgumentException("ポイントは空欄か0以上の整数にしてください。");
    }
  }
}
