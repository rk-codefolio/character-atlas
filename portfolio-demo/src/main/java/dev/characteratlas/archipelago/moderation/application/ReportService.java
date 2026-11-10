package dev.characteratlas.archipelago.moderation.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {
    private final ReportStore store;
    private final AuditLog log;

    public ReportService(ReportStore store, AuditLog log) {
        this.store = store;
        this.log = log;
    }

    public void report(UUID id, String reason, String note) {
        store.add(id, reason, note);
    }

    public List<ReportStore.Report> reports(UUID report, int offset) {
        return store.list(report, Math.max(0, offset));
    }

    @Transactional
    public void resolve(String actor, UUID id) {
        if (store.resolve(id)) {
            log.append(actor, "RESOLVE_REPORT", id.toString(), "{\"resolved\":false}", "{\"resolved\":true}");
        }
    }
}
