package dev.characteratlas.archipelago.moderation.application.audit;

import java.util.List;

/** 監査のログを表す。 */
public interface AuditLog {
  /** 追加を扱う。 */
  void append(String actor, String action, String target, String before, String after);

  /** entriesを扱う。 */
  List<AuditEntry> entries(int offset);
}
