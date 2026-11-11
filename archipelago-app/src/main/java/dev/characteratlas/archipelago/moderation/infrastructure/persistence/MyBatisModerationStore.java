package dev.characteratlas.archipelago.moderation.infrastructure.persistence;

import dev.characteratlas.archipelago.moderation.application.audit.AuditEntry;
import dev.characteratlas.archipelago.moderation.application.audit.AuditLog;
import dev.characteratlas.archipelago.moderation.application.post.ModeratedPosts;
import dev.characteratlas.archipelago.moderation.application.post.PostEntry;
import dev.characteratlas.archipelago.moderation.application.report.Report;
import dev.characteratlas.archipelago.moderation.application.report.ReportStore;
import dev.characteratlas.archipelago.shared.application.error.UseCaseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyのBatisのモデレーションの保存ポートを表す。 */
@Repository
public class MyBatisModerationStore implements AuditLog, ModeratedPosts, ReportStore {
  /** PostgreSQLのクエリーを実行するMapper。 */
  private final ModerationMapper mapper;

  /** MyのBatisのモデレーションの保存ポートを初期化する。 */
  MyBatisModerationStore(ModerationMapper mapper) {
    this.mapper = mapper;
  }

  /** 追加を扱う。 */
  @Override
  public void append(String actor, String action, String target, String before, String after) {
    mapper.append(actor, action, target, before, after);
  }

  /** entriesを扱う。 */
  @Override
  public List<AuditEntry> entries(int offset) {
    return mapper.entries(offset);
  }

  /** 指定された条件に沿って一覧を取得する。 */
  @Override
  public List<PostEntry> list(int offset) {
    return mapper.posts(offset);
  }

  /** 追加するを扱う。 */
  @Override
  public void add(UUID id, String reason, String note) {
    if (mapper.addReport(UUID.randomUUID(), id, reason, note) == 0 && !mapper.postExists(id)) {
      throw UseCaseException.notFound();
    }
  }

  /** 指定された条件に沿って一覧を取得する。 */
  @Override
  public List<Report> list(UUID report, int offset) {
    return mapper.reports(report, offset);
  }

  /** 通報を対応済みにし、監査記録を残す。 */
  @Override
  public boolean resolve(UUID id) {
    return mapper.resolve(id) > 0;
  }
}
