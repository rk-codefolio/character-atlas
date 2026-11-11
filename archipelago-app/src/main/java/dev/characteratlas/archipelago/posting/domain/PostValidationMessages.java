package dev.characteratlas.archipelago.posting.domain;

/** 投稿モデルの検証で使う文言。 */
final class PostValidationMessages {
  private PostValidationMessages() {}

  static final String KIND_DETAILS_MISMATCH = "投稿種別と詳細の組み合わせが不正です。";
  static final String TEXT_TOO_LONG = "入力が長すぎます。";
  static final String INVALID_SUBJECT = "投稿先か入力項目が不正です。";
  static final String TITLE_REQUIRED = "タイトルを入力してください。";
  static final String BODY_REQUIRED = "本文を入力してください。";
  static final String MATRIX_COUNT = "航路紋を2種類以上選んでください。";
  static final String DUPLICATE_MATRICES = "異なる航路紋を選んでください。";
  static final String MAIN_STAT_SLOTS = "装具スロット④・⑤・⑥を選んでください。";
  static final String TARGET_STATS_TOO_LONG = "目標ステータスは240文字以内で入力してください。";
  static final String STAGE_TOO_LONG = "ステージ・難易度は120文字以内で入力してください。";
  static final String NEGATIVE_MATRIX_POINTS = "ポイントは空欄か0以上の整数にしてください。";
}
