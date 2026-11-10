package dev.characteratlas.archipelago.moderation.infrastructure;

import java.time.Instant;

/** OのAuthの認証の進行状態を表す。 */
public record OAuthFlow(/* OAuth認証の照合値。 */ String state, /* expires。 */ Instant expires) {}
