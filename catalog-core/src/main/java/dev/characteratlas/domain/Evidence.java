package dev.characteratlas.domain;

/** 根拠を表す。 */
public enum Evidence {
  /* 明示一致の効果。 */ DECLARED_EFFECT,
  /* 明示一致のTag。 */ DECLARED_TAG,
  /* 関連の関連候補。 */ RELATED_CANDIDATE;

  /** 明示一致に含めない関連候補かを判定する。 */
  public boolean isCandidate() {
    return this == RELATED_CANDIDATE;
  }
}
