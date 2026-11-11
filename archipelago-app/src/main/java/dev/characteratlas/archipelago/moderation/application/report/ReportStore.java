package dev.characteratlas.archipelago.moderation.application.report;

import java.util.List;
import java.util.UUID;

/** 通報の保存ポートを表す。 */
public interface ReportStore {
  /** 追加するを扱う。 */
  void add(UUID id, String reason, String note);

  /** 指定された条件に沿って一覧を取得する。 */
  List<Report> list(UUID report, int offset);

  /** 通報を対応済みにし、監査記録を残す。 */
  boolean resolve(UUID id);
}
