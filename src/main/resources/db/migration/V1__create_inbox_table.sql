CREATE TABLE p_inbox
(
    message_id   UUID      NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_inbox PRIMARY KEY (message_id)
);
