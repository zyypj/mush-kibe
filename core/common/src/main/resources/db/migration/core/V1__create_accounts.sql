CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(16) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX accounts_username_lower_key ON accounts (LOWER(username));
