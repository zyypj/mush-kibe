CREATE TABLE perm_tags (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(24) NOT NULL,
    color VARCHAR(2) NOT NULL,
    weight INTEGER NOT NULL,
    prefix VARCHAR(64),
    decorative BOOLEAN NOT NULL
);

CREATE TABLE perm_groups (
    id VARCHAR(32) PRIMARY KEY,
    display_name VARCHAR(32) NOT NULL,
    weight INTEGER NOT NULL,
    tag_id VARCHAR(32) REFERENCES perm_tags (id) ON DELETE SET NULL
);

CREATE TABLE perm_group_nodes (
    group_id VARCHAR(32) NOT NULL REFERENCES perm_groups (id) ON DELETE CASCADE,
    permission VARCHAR(128) NOT NULL,
    value BOOLEAN NOT NULL,
    contexts VARCHAR(256) NOT NULL DEFAULT '',
    expires_at TIMESTAMPTZ,
    added_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (group_id, permission, contexts)
);

CREATE TABLE perm_user_nodes (
    player_id UUID NOT NULL,
    permission VARCHAR(128) NOT NULL,
    value BOOLEAN NOT NULL,
    contexts VARCHAR(256) NOT NULL DEFAULT '',
    expires_at TIMESTAMPTZ,
    added_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (player_id, permission, contexts)
);

CREATE INDEX perm_user_nodes_permission ON perm_user_nodes (permission);

CREATE TABLE perm_tracks (
    id VARCHAR(32) PRIMARY KEY,
    groups TEXT[] NOT NULL
);

CREATE TABLE player_tags (
    player_id UUID PRIMARY KEY,
    tag_id VARCHAR(32) NOT NULL REFERENCES perm_tags (id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE perm_actions (
    id BIGSERIAL PRIMARY KEY,
    at TIMESTAMPTZ NOT NULL,
    actor VARCHAR(64) NOT NULL,
    description TEXT NOT NULL
);

INSERT INTO perm_tags (id, name, color, weight, prefix, decorative) VALUES
    ('membro', 'Membro', '&7', 0, '', FALSE),
    ('vip', 'VIP', '&a', 10, NULL, FALSE),
    ('mvp', 'MVP', '&9', 20, NULL, FALSE),
    ('pro', 'Pro', '&6', 30, NULL, FALSE),
    ('ultra', 'Ultra', '&d', 40, NULL, FALSE),
    ('2021', '2021', '&b', 50, NULL, TRUE),
    ('2022', '2022', '&b', 51, NULL, TRUE),
    ('2023', '2023', '&b', 52, NULL, TRUE),
    ('2024', '2024', '&b', 53, NULL, TRUE),
    ('2025', '2025', '&b', 54, NULL, TRUE),
    ('2026', '2026', '&b', 55, NULL, TRUE),
    ('beta', 'Beta', '&1', 60, NULL, TRUE),
    ('enderlore', 'Enderlore', '&5', 70, NULL, TRUE),
    ('carnaval', 'Carnaval', '&6', 80, NULL, TRUE),
    ('natal', 'Natal', '&c', 90, NULL, TRUE),
    ('ferias', 'Ferias', '&a', 100, NULL, TRUE),
    ('ferias-2', 'Ferias', '&2', 101, NULL, TRUE),
    ('partner', 'Partner', '&b', 110, NULL, FALSE),
    ('partner+', 'Partner+', '&1', 120, NULL, FALSE),
    ('mod', 'Mod', '&5', 130, NULL, FALSE),
    ('mod+', 'Mod+', '&5', 140, NULL, FALSE),
    ('admin', 'Admin', '&c', 150, NULL, FALSE);

INSERT INTO perm_groups (id, display_name, weight, tag_id) VALUES
    ('membro', 'Membro', 0, 'membro'),
    ('vip', 'VIP', 10, 'vip'),
    ('mvp', 'MVP', 20, 'mvp'),
    ('pro', 'Pro', 30, 'pro'),
    ('ultra', 'Ultra', 40, 'ultra'),
    ('partner', 'Partner', 110, 'partner'),
    ('partner+', 'Partner+', 120, 'partner+'),
    ('mod', 'Mod', 130, 'mod'),
    ('mod+', 'Mod+', 140, 'mod+'),
    ('admin', 'Admin', 150, 'admin');

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('vip', 'group.membro', TRUE, '', NULL, now(), now()),
    ('mvp', 'group.vip', TRUE, '', NULL, now(), now()),
    ('pro', 'group.mvp', TRUE, '', NULL, now(), now()),
    ('ultra', 'group.pro', TRUE, '', NULL, now(), now()),
    ('partner', 'group.ultra', TRUE, '', NULL, now(), now()),
    ('partner+', 'group.partner', TRUE, '', NULL, now(), now()),
    ('mod', 'group.partner+', TRUE, '', NULL, now(), now()),
    ('mod+', 'group.mod', TRUE, '', NULL, now(), now()),
    ('admin', 'group.mod+', TRUE, '', NULL, now(), now()),
    ('admin', '*', TRUE, '', NULL, now(), now());

INSERT INTO perm_tracks (id, groups) VALUES
    ('doacao', ARRAY['membro', 'vip', 'mvp', 'pro', 'ultra']),
    ('staff', ARRAY['mod', 'mod+', 'admin']);
