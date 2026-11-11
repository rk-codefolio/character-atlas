package dev.characteratlas.archipelago.posting.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.characteratlas.archipelago.posting.domain.details.BuildDetails;
import dev.characteratlas.archipelago.posting.domain.details.CommentDetails;
import dev.characteratlas.archipelago.posting.domain.details.GuideDetails;
import dev.characteratlas.archipelago.posting.domain.details.MatrixSelection;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** RESTを経由しない投稿入力でもドメインの制約が適用されることを検証する。 */
class PostDraftTest {
  /** ビルド種別にコメントの詳細を渡し、組み合わせを拒否することを確認する。 */
  @Test
  void rejectsDetailsOfAnotherKind() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PostDraft(
                UUID.randomUUID(),
                PostKind.BUILD,
                "marea",
                "題名",
                "",
                new CommentDetails(),
                List.of()));
  }

  /** 同じ航路紋に異なるポイントを指定し、重複したビルドを拒否することを確認する。 */
  @Test
  void rejectsDuplicateMatrixNames() {
    // 名前が同じでポイントだけが異なる2つの航路紋。
    var matrices =
        List.of(
            new MatrixSelection("追風", Optional.empty()),
            new MatrixSelection("追風", Optional.of(BigInteger.ONE)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BuildDetails("羅針盤", matrices, List.of("HP", "HP", "HP"), Optional.empty()));
  }

  /** 長すぎる難易度と負のポイントを値型に渡し、それぞれ拒否されることを確認する。 */
  @Test
  void rejectsInvalidValuesAtConstruction() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GuideDetails(Optional.of("あ".repeat(121)), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MatrixSelection("追風", Optional.of(BigInteger.valueOf(-1))));
  }
}
