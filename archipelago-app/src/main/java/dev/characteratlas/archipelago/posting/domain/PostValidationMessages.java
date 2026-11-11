package dev.characteratlas.archipelago.posting.domain;

/** 投稿モデルの検証で使う文言。 */
public final class PostValidationMessages {
  private PostValidationMessages() {}

  public static final String KIND_DETAILS_MISMATCH = "投稿種別と詳細の組み合わせが不正です。";
  public static final String TEXT_TOO_LONG = "入力が長すぎます。";
  public static final String INVALID_SUBJECT = "投稿先か入力項目が不正です。";
  public static final String TITLE_REQUIRED = "タイトルを入力してください。";
  public static final String BODY_REQUIRED = "本文を入力してください。";
  public static final String MATRIX_COUNT = "航路紋を2種類以上選んでください。";
  public static final String DUPLICATE_MATRICES = "異なる航路紋を選んでください。";
  public static final String MAIN_STAT_SLOTS = "装具スロット④・⑤・⑥を選んでください。";
  public static final String TARGET_STATS_TOO_LONG = "目標ステータスは240文字以内で入力してください。";
  public static final String STAGE_TOO_LONG = "ステージ・難易度は120文字以内で入力してください。";
  public static final String NEGATIVE_MATRIX_POINTS = "ポイントは空欄か0以上の整数にしてください。";

  /** コンテンツタグが見つかりません。 */
  public static final String CONTENT_TAG_NOT_FOUND = "コンテンツタグが見つかりません。";

  /** コンテンツを選んでください。 */
  public static final String CONTENT_REQUIRED = "コンテンツを選んでください。";

  /** 関連渡り手が見つかりません。 */
  public static final String RELATED_CHARACTER_NOT_FOUND = "関連渡り手が見つかりません。";

  /** キャラクターが見つかりません。 */
  public static final String CHARACTER_NOT_FOUND = "キャラクターが見つかりません。";

  /** 航海具を選んでください。 */
  public static final String SHELL_REQUIRED = "航海具を選んでください。";

  /** 選択項目を確認してください。 */
  public static final String INVALID_SELECTION = "選択項目を確認してください。";
}
