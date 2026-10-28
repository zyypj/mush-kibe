ALTER TABLE accounts ADD COLUMN number BIGSERIAL;

CREATE UNIQUE INDEX accounts_number_key ON accounts (number);

CREATE TABLE account_locations (
    player_id UUID PRIMARY KEY REFERENCES accounts (id) ON DELETE CASCADE,
    country VARCHAR(64),
    region VARCHAR(96),
    city VARCHAR(96),
    asn VARCHAR(160),
    updated_at TIMESTAMPTZ NOT NULL
);
