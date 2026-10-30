CREATE TABLE credits (
    player_id UUID PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE credit_changes (
    id BIGSERIAL PRIMARY KEY,
    player_id UUID NOT NULL,
    amount BIGINT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX credit_changes_player ON credit_changes (player_id, changed_at);

CREATE TABLE owned_cosmetics (
    player_id UUID NOT NULL,
    category VARCHAR(32) NOT NULL,
    cosmetic_id VARCHAR(48) NOT NULL,
    source VARCHAR(32) NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (player_id, category, cosmetic_id)
);

CREATE TABLE selected_cosmetics (
    player_id UUID NOT NULL,
    category VARCHAR(32) NOT NULL,
    cosmetic_id VARCHAR(48) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (player_id, category)
);

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('ultra', 'muxe.cosmetic.ultra', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;
