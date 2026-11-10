CREATE TABLE post_report (
    report_id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    reason TEXT NOT NULL,
    note TEXT NOT NULL,
    post_snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    notified_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX post_report_open_post ON post_report(post_id) WHERE resolved_at IS NULL;
CREATE INDEX post_report_pending_notification ON post_report(next_attempt) WHERE notified_at IS NULL AND resolved_at IS NULL;
ALTER TABLE moderation_log DROP CONSTRAINT moderation_log_action_check;
ALTER TABLE moderation_log ADD CHECK (action IN ('EDIT_POST', 'DELETE_POST', 'SEARCH_CONTENT', 'RESOLVE_REPORT'));
