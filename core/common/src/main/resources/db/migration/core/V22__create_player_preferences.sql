CREATE TABLE player_preferences (
    player_id UUID NOT NULL,
    preference VARCHAR(32) NOT NULL,
    value VARCHAR(16) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (player_id, preference)
);
