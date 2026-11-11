package dev.characteratlas.archipelago.posting.domain.details;

import dev.characteratlas.archipelago.posting.domain.PostValidationError;
import dev.characteratlas.domain.validation.DomainChecks;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/**
 * ビルドで選択した航路紋の名称と任意のポイント数を保持する。
 *
 * <p>ポイント未入力は空のOptionalで表し、入力された値は0以上とする。
 */
public record MatrixSelection(
    /* 航路紋の名称。 */ String name, /* ポイントが未入力なら空。 */ Optional<BigInteger> points) {
  /**
   * 入力を正規化し、この型の生成条件を検証する。
   *
   * @throws dev.characteratlas.domain.validation.DomainValidationException この型が定める生成条件に違反する場合
   * @throws NullPointerException 必須の参照やコレクション要素がnullの場合
   */
  public MatrixSelection {
    Objects.requireNonNull(name);
    Objects.requireNonNull(points);
    DomainChecks.require(
        !(points.filter(value -> value.signum() < 0).isPresent()),
        PostValidationError.NEGATIVE_MATRIX_POINTS);
  }
}
