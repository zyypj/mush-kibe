INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('mod', 'muxe.account.others', TRUE, '', NULL, now(), now()),
    ('mod', 'muxe.account.staff', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;
