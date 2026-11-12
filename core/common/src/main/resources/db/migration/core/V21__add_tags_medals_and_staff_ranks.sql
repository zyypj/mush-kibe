ALTER TABLE perm_medals ALTER COLUMN symbol TYPE VARCHAR(32);

UPDATE perm_tags SET color = '&4' WHERE id = 'admin';
UPDATE perm_tags SET prefix = '&5&lMOD&5+ ' WHERE id = 'mod+';
UPDATE perm_tags SET color = '&3' WHERE id = 'partner+';
UPDATE perm_tags SET color = '&5', prefix = '&5&lULTRA&6+ ', weight = 50 WHERE id = 'ultra+';
UPDATE perm_groups SET weight = 50 WHERE id = 'ultra+';

UPDATE perm_tags SET weight = 41 WHERE id = 'enderlore';
UPDATE perm_tags SET weight = 42 WHERE id = 'ferias';
UPDATE perm_tags SET weight = 43 WHERE id = 'ferias-2';
UPDATE perm_tags SET weight = 45 WHERE id = 'carnaval';
UPDATE perm_tags SET weight = 46 WHERE id = 'natal';
UPDATE perm_tags SET weight = 55 WHERE id = '2021';
UPDATE perm_tags SET weight = 56 WHERE id = '2022';
UPDATE perm_tags SET weight = 57 WHERE id = '2023';
UPDATE perm_tags SET weight = 58 WHERE id = '2024';
UPDATE perm_tags SET weight = 59 WHERE id = '2025';
UPDATE perm_tags SET weight = 60 WHERE id = '2026';
UPDATE perm_tags SET weight = 62 WHERE id = 'beta';

INSERT INTO perm_tags (id, name, color, weight, prefix, decorative) VALUES
    ('blade', 'Blade', '&e', 4, NULL, TRUE),
    ('master', 'Master', '&6', 6, NULL, TRUE),
    ('ferias-3', 'Ferias', '&9', 44, NULL, TRUE),
    ('2017', '2017', '&b', 51, NULL, TRUE),
    ('2018', '2018', '&b', 52, NULL, TRUE),
    ('2019', '2019', '&b', 53, NULL, TRUE),
    ('2020', '2020', '&b', 54, NULL, TRUE),
    ('nitro', 'Nitro', '&d', 61, NULL, TRUE),
    ('champion', 'Champion', '&6', 63, NULL, TRUE),
    ('stream', 'Stream', '&b', 64, NULL, TRUE),
    ('yt', 'YT', '&b', 65, NULL, TRUE),
    ('builder', 'Builder', '&2', 122, NULL, FALSE),
    ('helper', 'Helper', '&9', 124, NULL, FALSE),
    ('trial', 'Trial', '&5', 126, NULL, FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO perm_groups (id, display_name, weight, tag_id) VALUES
    ('builder', 'Builder', 122, 'builder'),
    ('helper', 'Helper', 124, 'helper'),
    ('trial', 'Trial', 126, 'trial')
ON CONFLICT (id) DO NOTHING;

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('builder', 'group.partner+', TRUE, '', NULL, now(), now()),
    ('helper', 'group.builder', TRUE, '', NULL, now(), now()),
    ('trial', 'group.helper', TRUE, '', NULL, now(), now()),
    ('mod', 'group.trial', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;

DELETE FROM perm_group_nodes WHERE group_id = 'mod' AND permission = 'group.partner+' AND contexts = '';

UPDATE perm_tracks SET groups = ARRAY['builder', 'helper', 'trial']::TEXT[] || array_remove(array_remove(array_remove(groups, 'builder'), 'helper'), 'trial')
WHERE id = 'staff';

UPDATE perm_medals SET symbol = '&l⚡' WHERE id = 'flash';
UPDATE perm_medals SET symbol = '&lツ' WHERE id = 'carinha';
UPDATE perm_medals SET symbol = '&l✝' WHERE id = 'halloween';
UPDATE perm_medals SET symbol = '&l۩' WHERE id = 'casa-de-gengibre';
UPDATE perm_medals SET symbol = '&l⚠' WHERE id = 'alerta';
UPDATE perm_medals SET symbol = '&l✷' WHERE id = 'carnaval';
UPDATE perm_medals SET symbol = '&l⊰⊱' WHERE id = 'feiticeiro-aprendiz';
UPDATE perm_medals SET symbol = '&l۞' WHERE id = 'biscoito-de-gengibre';
UPDATE perm_medals SET symbol = '氣' WHERE id = 'ar';
UPDATE perm_medals SET symbol = '&l☘' WHERE id = 'sortudo';

INSERT INTO perm_medals (id, name, symbol, color, weight) VALUES
    ('quartel-quell-1', 'Quartel Quell 1', '✪', '&6', 320),
    ('quartel-quell-2', 'Quartel Quell 2', '➣', '&6', 330),
    ('quartel-quell-3', 'Quartel Quell 3', '✵', '&6', 340),
    ('general-season-3', 'General Season 3', '❂', '&4', 350),
    ('master', 'Master', '❂', '&4', 360),
    ('errado', 'Errado', '&l✖', '&c', 370),
    ('cxc', 'Cxc', '&l✠', '&c', 380),
    ('nitro', 'Nitro', '&l✦', '&0', 390),
    ('bgs', 'BGS', '&l➤', '&3', 400),
    ('top', 'Top', '❁', '&b', 410),
    ('arzim', 'Arzim', '✵', '&d', 420),
    ('aluno', 'Aluno', '✏', '&5', 430),
    ('muxe', 'Muxe', '&lM', '&b', 440),
    ('wg', 'WG', '&lW', '&b', 450),
    ('general', 'General', '✫', '&4', 460),
    ('grammy', 'Grammy', '♪', '&6', 470),
    ('promotor-de-eventos', 'Promotor de Eventos', '❃', '&5', 480),
    ('staff', 'Staff', '✷', '&1', 490),
    ('helper', 'Helper', '✤', '&9', 500),
    ('mod', 'Mod', '✥', '&b', 510),
    ('ajudante-de-papai-noel', 'Ajudante de Papai Noel', '❀', '&6', 520),
    ('mega-muxe', 'Mega Muxe', '✹', '&4', 530),
    ('correto', 'Correto', '&l✓', '&a', 540),
    ('aniversario-do-muxe-2022', 'Aniversario do Muxe 2022', '❣', '&c', 550),
    ('yt', 'YT', '&l✩', '&b', 560),
    ('liga-diamante', 'Liga Diamante (Bed Wars Champions)', '✫', '&d', 570),
    ('partner', 'Partner', '&l✧', '&b', 580),
    ('nivel-100', 'Nivel 100', '&l✩', '&6', 590),
    ('aniversario-do-muxe-2023', 'Aniversario do Muxe 2023', '✺', '&e', 600),
    ('pix', 'Pix', '❖', '&3', 610),
    ('feiticeiro', 'Feiticeiro', '✲', '&c', 620),
    ('estacio', 'Estacio', '✏', '&b', 630),
    ('ruby', 'Ruby', '✦', '&c', 640),
    ('grana', 'Grana', '&l$', '&a', 650),
    ('catapato', 'CataPato', '✕', '&e', 660),
    ('floco-de-neve', 'Floco de Neve', '❅', '&f', 670),
    ('show-de-fogos-2024', 'Show de Fogos 2024', '✺', '&c', 680),
    ('mod+', 'Mod+', '❤', '&5', 690),
    ('muxe-league', 'Muxe League', '❂', '&6', 700),
    ('clan-clash', 'Clan Clash', '✽', '&6', 710),
    ('feiticeiro-amador', 'Feiticeiro Amador', '&l⊰&e✰&a&l⊱', '&a', 720),
    ('feiticeiro-mestre', 'Feiticeiro Mestre', '&l⊰&e✰&d&l⊱', '&d', 730),
    ('show-de-fogos-2025', 'Show de Fogos 2025', '✺', '&a', 740),
    ('top-cyan', 'Top Cyan', '❁', '&3', 750),
    ('top-dark-blue', 'Top Dark Blue', '❁', '&1', 760),
    ('top-red', 'Top Red', '❁', '&4', 770),
    ('gamescom', 'Gamescom', '۞', '&f', 780),
    ('desenhista', 'Desenhista', '✎', '&6', 790),
    ('ajudante-da-nasco', 'Ajudante da Nascó', '⚝', '&6', 800),
    ('clan-clash-2', 'Clan Clash 2', '✟', '&6', 810),
    ('show-de-fogos-2026', 'Show de Fogos 2026', '✺', '&6', 820),
    ('liga-mestre', 'Liga Mestre (Bed Wars Champions)', '✪', '&4', 830),
    ('liga-platina', 'Liga Platina (Bed Wars Champions)', '✹', '&3', 840),
    ('liga-ouro', 'Liga Ouro (Bed Wars Champions)', '&l✷', '&6', 850),
    ('satturnni', 'Satturnni', '➊', '&6', 860),
    ('kindome', 'Kindome', '&lK', '&5', 870)
ON CONFLICT (id) DO NOTHING;
