CREATE TABLE moderation_log (
    log_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    actor_id VARCHAR(20) NOT NULL,
    action TEXT NOT NULL CHECK (action IN ('EDIT_POST', 'DELETE_POST', 'SEARCH_CONTENT')),
    target_id TEXT NOT NULL,
    before_value JSONB NOT NULL,
    after_value JSONB
);

CREATE TABLE search_content_override (
    content_id TEXT PRIMARY KEY,
    visible BOOLEAN NOT NULL
);
