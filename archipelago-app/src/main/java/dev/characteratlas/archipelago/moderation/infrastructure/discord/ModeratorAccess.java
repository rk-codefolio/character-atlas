package dev.characteratlas.archipelago.moderation.infrastructure.discord;

/** モデレーターの権限を表す。 */
public record ModeratorAccess(
    /* configured。 */ boolean configured, /* オーナーの識別子。 */
    boolean owner, /* モデレーター。 */
    boolean moderator) {
  /** canのModerateを扱う。 */
  public boolean canModerate() {
    return owner || moderator;
  }
}
