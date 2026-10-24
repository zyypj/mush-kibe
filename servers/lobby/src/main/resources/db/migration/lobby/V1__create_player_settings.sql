CREATE TABLE player_settings (
    player_id UUID PRIMARY KEY,
    visibility VARCHAR(16) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
