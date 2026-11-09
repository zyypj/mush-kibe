INSERT INTO perm_tags (id, name, color, weight, prefix, decorative) VALUES
    ('ultra+', 'Ultra&6+', '&d', 45, '&d&lULTRA&6+ ', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO perm_groups (id, display_name, weight, tag_id) VALUES
    ('ultra+', 'Ultra+', 45, 'ultra+')
ON CONFLICT (id) DO NOTHING;

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('ultra+', 'group.ultra', TRUE, '', NULL, now(), now()),
    ('ultra+', 'muxe.party.open', TRUE, '', NULL, now(), now()),
    ('partner', 'group.ultra+', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;

DELETE FROM perm_group_nodes WHERE group_id = 'partner' AND permission = 'group.ultra' AND contexts = '';

UPDATE perm_tracks SET groups = array_append(groups, 'ultra+') WHERE id = 'doacao' AND NOT ('ultra+' = ANY (groups));
