INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('vip', 'muxe.lobby.fly', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;
