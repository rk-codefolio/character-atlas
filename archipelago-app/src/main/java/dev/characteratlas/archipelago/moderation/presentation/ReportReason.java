package dev.characteratlas.archipelago.moderation.presentation;

import org.springframework.web.bind.annotation.*;

/** 通報の理由を表す。 */
public enum ReportReason {
  /* Spam。 */
  SPAM, /* Abuse。 */
  ABUSE, /* Other。 */
  OTHER
}
