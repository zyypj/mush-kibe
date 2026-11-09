CREATE TABLE custom_room_presets (
    id UUID PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    owner UUID NOT NULL,
    game VARCHAR(32) NOT NULL,
    name VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    maps TEXT NOT NULL,
    options TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX custom_room_presets_owner ON custom_room_presets (owner, game, created_at);

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('ultra+', 'muxe.customroom.create', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;
