package dev.characteratlas.archipelago.shared.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.characteratlas.archipelago.shared.application.InfrastructureException;
import dev.characteratlas.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 共通ハンドラーによる想定内と想定外の応答を検証する。 */
class UseCaseErrorsTest {
  /** 入力違反を渡すと、修正に必要な文言を伴う400応答になる。 */
  @Test
  void returnsValidationMessage() {
    var result = new UseCaseErrors().invalidDomainInput(new DomainValidationException("入力を確認"));
    assertEquals(400, result.getStatus());
    assertEquals("入力を確認", result.getDetail());
  }

  /** 外部障害を渡すと、原因を応答に含めず共通の500応答になる。 */
  @Test
  void concealsUnexpectedCause() {
    var cause = new IllegalStateException("内部接続情報");
    var error = new InfrastructureException("接続失敗", cause);
    assertSame(cause, error.getCause());
    var result = new UseCaseErrors().unexpected(error);
    assertEquals(500, result.getStatus());
    assertEquals(ApiErrorMessages.UNEXPECTED_FAILURE, result.getDetail());
  }

  /** 明示されたHTTP失敗を渡すと、既存のステータスを保って再送出する。 */
  @Test
  void preservesExplicitHttpFailure() {
    var error = new ApiRequestException(HttpStatus.FORBIDDEN);
    assertSame(
        error,
        assertThrows(ApiRequestException.class, () -> new UseCaseErrors().unexpected(error)));
  }
}
