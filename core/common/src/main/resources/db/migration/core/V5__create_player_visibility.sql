CREATE TABLE player_visibility (
    player_id UUID PRIMARY KEY,
    visibility VARCHAR(16) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

DO $$
BEGIN
    IF to_regclass('lobby.player_settings') IS NOT NULL THEN
        INSERT INTO player_visibility (player_id, visibility, updated_at)
        SELECT player_id, visibility, updated_at FROM lobby.player_settings
        ON CONFLICT (player_id) DO NOTHING;
    END IF;
END
$$;
