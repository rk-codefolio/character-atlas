CREATE TABLE review_post (
    post_id uuid PRIMARY KEY,
    kind text NOT NULL CHECK (kind IN ('COMMENT', 'BUILD', 'CONTENT')),
    subject_id text NOT NULL,
    title text NOT NULL,
    body text NOT NULL,
    details jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- A snapshot refresh replaces the imported rows. Posts retain their stable subject IDs.
CREATE INDEX review_post_subject ON review_post (kind, subject_id, created_at DESC);
