package dev.characteratlas.archipelago.moderation.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 通報のユースケースを表す。 */
@Service
public class ReportService {
  /** 保存ポート。 */
  private final ReportStore store;

  /** 監査記録のポート。 */
  private final AuditLog log;

  /** 通報のユースケースを初期化する。 */
  public ReportService(ReportStore store, AuditLog log) {
    this.store = store;
    this.log = log;
  }

  /** 通報を扱う。 */
  public void report(UUID id, String reason, String note) {
    store.add(id, reason, note);
  }

  /** reportsを扱う。 */
  public List<Report> reports(UUID report, int offset) {
    return store.list(report, Math.max(0, offset));
  }

  /** 通報を対応済みにし、監査記録を残す。 */
  @Transactional
  public void resolve(String actor, UUID id) {
    if (store.resolve(id)) {
      log.append(
          actor, "RESOLVE_REPORT", id.toString(), "{\"resolved\":false}", "{\"resolved\":true}");
    }
  }
}
