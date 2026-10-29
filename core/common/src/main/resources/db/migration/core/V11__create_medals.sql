CREATE TABLE perm_medals (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(48) NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    color VARCHAR(2) NOT NULL,
    weight INTEGER NOT NULL
);

CREATE TABLE player_medals (
    player_id UUID PRIMARY KEY,
    medal_id VARCHAR(32) NOT NULL REFERENCES perm_medals (id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO perm_medals (id, name, symbol, color, weight) VALUES
    ('toxico', 'Tóxico', '☣', '&a', 10),
    ('yin-yang', 'Yin Yang', '☯', '&f', 20),
    ('musico', 'Músico', '♫', '&5', 30),
    ('flash', 'Flash', '⚡', '&6', 40),
    ('paz-e-amor', 'Paz e Amor', '✌', '&c', 50),
    ('amor', 'Amor', '❤', '&4', 60),
    ('cafe', 'Café', '☕', '&e', 70),
    ('carinha', 'Carinha', 'ツ', '&6', 80),
    ('halloween', 'Halloween', '✝', '&4', 90),
    ('natal', 'Natal', '✽', '&b', 100),
    ('cacador-de-bruxas', 'Caçador de Bruxas', '∅', '&2', 110),
    ('casa-de-gengibre', 'Casa de Gengibre', '۩', '&6', 120),
    ('alerta', 'Alerta', '⚠', '&e', 130),
    ('atomo', 'Átomo', '⚛', '&b', 140),
    ('anel', 'Anel', '℧', '&6', 150),
    ('boneco-de-neve', 'Boneco de Neve', '☃', '&f', 160),
    ('carnaval', 'Carnaval', '✴', '&6', 170),
    ('feiticeiro-aprendiz', 'Feiticeiro Aprendiz', '⊰⊱', '&a', 180),
    ('biscoito-de-gengibre', 'Biscoito de Gengibre', '۞', '&6', 190),
    ('liga-prata', 'Liga Prata (Bed Wars Champions)', '✶', '&f', 200),
    ('fogo', 'Fogo', '火', '&c', 210),
    ('agua', 'Água', '水', '&b', 220),
    ('ar', 'Ar', '気', '&f', 230),
    ('estrela', 'Estrela', '✯', '&e', 240),
    ('sortudo', 'Sortudo', '☘', '&2', 250),
    ('carta', 'Carta', '回', '&f', 260),
    ('modo-aviao', 'Modo Avião', '✈', '&b', 270),
    ('celular', 'Celular', '✆', '&a', 280),
    ('flor', 'Flor', '✿', '&d', 290),
    ('briga-de-espadas', 'Briga de Espadas', '⚔', '&7', 300),
    ('imperial', 'Imperial', '⚜', '&6', 310);

INSERT INTO perm_group_nodes (group_id, permission, value, contexts, expires_at, added_at, updated_at) VALUES
    ('vip', 'muxe.medal.carinha', TRUE, '', NULL, now(), now()),
    ('vip', 'muxe.medal.toxico', TRUE, '', NULL, now(), now()),
    ('vip', 'muxe.medal.yin-yang', TRUE, '', NULL, now(), now()),
    ('ultra', 'muxe.medal.musico', TRUE, '', NULL, now(), now()),
    ('ultra', 'muxe.medal.flash', TRUE, '', NULL, now(), now()),
    ('ultra', 'muxe.medal.paz-e-amor', TRUE, '', NULL, now(), now()),
    ('ultra', 'muxe.medal.amor', TRUE, '', NULL, now(), now()),
    ('ultra', 'muxe.medal.cafe', TRUE, '', NULL, now(), now())
ON CONFLICT (group_id, permission, contexts) DO NOTHING;
