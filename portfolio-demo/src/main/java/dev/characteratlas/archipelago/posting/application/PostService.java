package dev.characteratlas.archipelago.posting.application;

import dev.characteratlas.archipelago.posting.domain.PostDraft;
import dev.characteratlas.archipelago.posting.domain.PostKind;
import dev.characteratlas.archipelago.moderation.application.AuditLog;
import dev.characteratlas.archipelago.shared.application.UseCaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {
    private final PostStore store;
    private final AuditLog log;

    public PostService(PostStore store, AuditLog log) {
        this.store = store;
        this.log = log;
    }

    public List<Post> list(PostKind kind, String subject) {
        return store.list(kind, subject);
    }

    public boolean characterExists(String id) {
        return store.characterExists(id);
    }

    public UUID create(PostDraft draft) {
        store.insert(draft);
        return draft.id();
    }

    @Transactional
    public void edit(String actor, PostDraft draft) {
        String before = store.lock(draft.id()).orElseThrow(UseCaseException::notFound);
        String after = store.replace(draft).orElseThrow(UseCaseException::notFound);
        log.append(actor, "EDIT_POST", draft.id().toString(), before, after);
    }

    @Transactional
    public void remove(String actor, UUID id) {
        String before = store.lock(id).orElseThrow(UseCaseException::notFound);
        store.delete(id);
        log.append(actor, "DELETE_POST", id.toString(), before, null);
    }
}
