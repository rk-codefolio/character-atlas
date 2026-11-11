package dev.characteratlas.archipelago.moderation.presentation.report;

import dev.characteratlas.archipelago.moderation.application.report.Report;
import dev.characteratlas.archipelago.moderation.application.report.ReportService;
import dev.characteratlas.archipelago.moderation.infrastructure.discord.DiscordModerationAccess;
import dev.characteratlas.archipelago.shared.presentation.error.ApiRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 投稿のReportsを表す。 */
@RestController
@RequestMapping("/api")
public class PostReports {
  /** reports。 */
  private final ReportService reports;

  /** 認可情報。 */
  private final DiscordModerationAccess access;

  /** 投稿のReportsを初期化する。 */
  public PostReports(ReportService reports, DiscordModerationAccess access) {
    this.reports = reports;
    this.access = access;
  }

  /** 通報を扱う。 */
  @PostMapping("/posts/{id}/reports")
  public ResponseEntity<Void> report(
      HttpServletRequest request, @PathVariable UUID id, @RequestBody ReportSubmission submission) {
    access.requireOrigin(request);
    // 通報の補足。
    String note = submission.note() == null ? "" : submission.note().strip();
    if (submission.reason() == null || note.length() > 500)
      throw new ApiRequestException(HttpStatus.BAD_REQUEST);
    reports.report(id, submission.reason().name(), note);
    return ResponseEntity.accepted().build();
  }

  /** reportsを扱う。 */
  @GetMapping("/admin/reports")
  public ResponseEntity<List<Report>> reports(
      HttpServletRequest request,
      @RequestParam(required = false) UUID report,
      @RequestParam(defaultValue = "0") int offset) {
    access.requireModerator(request);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(reports.reports(report, offset));
  }

  /** 通報を対応済みにし、監査記録を残す。 */
  @PutMapping("/admin/reports/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resolve(HttpServletRequest request, @PathVariable UUID id) {
    access.requireOrigin(request);
    // 操作者。
    String actor = access.requireModerator(request);
    reports.resolve(actor, id);
  }
}
