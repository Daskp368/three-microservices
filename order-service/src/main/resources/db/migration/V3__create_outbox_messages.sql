CREATE TABLE outbox_messages
(
    id           UUID        PRIMARY KEY,
    aggregate_id UUID        NOT NULL,
    payload      JSONB       NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts     INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    last_error   TEXT,

    CONSTRAINT chk_outbox_attempts_non_negative
        CHECK (attempts >= 0),

    CONSTRAINT chk_outbox_payload_is_object
        CHECK (jsonb_typeof(payload) = 'object'),

    CONSTRAINT chk_outbox_published_after_creation
        CHECK (published_at IS NULL OR published_at >= created_at),

    CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED')),

    CONSTRAINT chk_outbox_publication_state
         CHECK (
            (status = 'PENDING' AND published_at IS NULL)
            OR
            (status = 'PUBLISHED' AND published_at IS NOT NULL)
         )
);

CREATE INDEX idx_outbox_messages_pending
    ON outbox_messages (created_at)
    WHERE published_at IS NULL;

CREATE INDEX idx_outbox_messages_aggregate_id
    ON outbox_messages (aggregate_id);
