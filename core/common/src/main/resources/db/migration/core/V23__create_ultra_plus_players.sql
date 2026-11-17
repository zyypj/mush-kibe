CREATE TABLE ultra_plus_players (
    player_id UUID PRIMARY KEY,
    granted_days INTEGER NOT NULL DEFAULT 0,
    emblem VARCHAR(32),
    plus_color VARCHAR(32),
    tag_level SMALLINT NOT NULL DEFAULT 1,
    reward_month VARCHAR(7),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO ultra_plus_players (player_id, granted_days)
SELECT player_id, GREATEST(0, ROUND(EXTRACT(EPOCH FROM (expires_at - added_at)) / 86400))::INTEGER
FROM perm_user_nodes
WHERE permission = 'group.ultra+' AND value AND expires_at IS NOT NULL AND contexts = ''
ON CONFLICT (player_id) DO NOTHING;

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('ultra+', 'muxe.pregame.fly', TRUE, '', NULL, now(), now()),
    ('ultra+', 'muxe.chat.fast', TRUE, '', NULL, now(), now()),
    ('builder', 'muxe.chat.no-cooldown', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;
