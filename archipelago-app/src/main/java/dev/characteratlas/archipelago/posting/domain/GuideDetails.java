package dev.characteratlas.archipelago.posting.domain;

import java.util.List;
import java.util.Optional;

/** 攻略投稿の任意の難易度と関連キャラクターを保持する。 */
public record GuideDetails(
    /* stage。 */ Optional<String> stage, /* 関連のCharacters。 */ List<String> relatedCharacters)
    implements PostingDetails {
  /** 攻略の詳細を初期化する。 */
  public GuideDetails {
    relatedCharacters = List.copyOf(relatedCharacters);
    if (stage.filter(value -> value.length() > 120).isPresent()) {
      throw new IllegalArgumentException("ステージ・難易度は120文字以内で入力してください。");
    }
  }
}
