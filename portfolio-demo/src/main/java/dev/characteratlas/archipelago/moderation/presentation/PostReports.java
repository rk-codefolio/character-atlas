package dev.characteratlas.archipelago.moderation.presentation;

import dev.characteratlas.archipelago.moderation.infrastructure.DiscordModerationAccess;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import dev.characteratlas.archipelago.moderation.application.ReportService;
import dev.characteratlas.archipelago.moderation.application.ReportStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PostReports {
    private final ReportService reports;
    private final DiscordModerationAccess access;

    public PostReports(ReportService reports, DiscordModerationAccess access) {
        this.reports = reports;
        this.access = access;
    }

    @PostMapping("/posts/{id}/reports")
    public ResponseEntity<Void> report(HttpServletRequest request, @PathVariable UUID id, @RequestBody Submission submission) {
        access.requireOrigin(request);
        String note = submission.note() == null ? "" : submission.note().strip();
        if (submission.reason() == null || note.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        reports.report(id, submission.reason().name(), note);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/admin/reports")
    public ResponseEntity<List<ReportStore.Report>> reports(HttpServletRequest request,
                                       @RequestParam(required = false) UUID report,
                                       @RequestParam(defaultValue = "0") int offset) {
        access.requireModerator(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reports.reports(report, offset));
    }

    @PutMapping("/admin/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(HttpServletRequest request, @PathVariable UUID id) {
        access.requireOrigin(request);
        String actor = access.requireModerator(request);
        reports.resolve(actor, id);
    }

    enum Reason { SPAM, ABUSE, OTHER }
    public record Submission(Reason reason, String note) {}
}
