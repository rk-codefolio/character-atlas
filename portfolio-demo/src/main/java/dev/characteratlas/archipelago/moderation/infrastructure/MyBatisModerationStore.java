package dev.characteratlas.archipelago.moderation.infrastructure;

import dev.characteratlas.archipelago.moderation.application.AuditLog;
import dev.characteratlas.archipelago.moderation.application.ModeratedPosts;
import dev.characteratlas.archipelago.moderation.application.ReportStore;
import dev.characteratlas.archipelago.shared.application.UseCaseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisModerationStore implements AuditLog, ModeratedPosts, ReportStore {
    private final ModerationMapper mapper;

    MyBatisModerationStore(ModerationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void append(String actor, String action, String target, String before, String after) {
        mapper.append(actor, action, target, before, after);
    }

    @Override
    public List<Entry> entries(int offset) {
        return mapper.entries(offset);
    }

    @Override
    public List<PostEntry> list(int offset) {
        return mapper.posts(offset);
    }

    @Override
    public void add(UUID id, String reason, String note) {
        if (mapper.addReport(UUID.randomUUID(), id, reason, note) == 0 && !mapper.postExists(id)) {
            throw UseCaseException.notFound();
        }
    }

    @Override
    public List<Report> list(UUID report, int offset) {
        return mapper.reports(report, offset);
    }

    @Override
    public boolean resolve(UUID id) {
        return mapper.resolve(id) > 0;
    }
}
