CREATE TABLE player_languages (
    player_id UUID PRIMARY KEY,
    language VARCHAR(8) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
