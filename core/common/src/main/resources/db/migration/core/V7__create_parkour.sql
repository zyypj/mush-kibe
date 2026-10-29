CREATE TABLE parkour_placements (
    server_group VARCHAR(32) NOT NULL,
    id VARCHAR(32) NOT NULL,
    world VARCHAR(64) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    yaw REAL NOT NULL,
    pitch REAL NOT NULL,
    PRIMARY KEY (server_group, id)
);

CREATE TABLE parkour_records (
    server_group VARCHAR(32) NOT NULL,
    player_id UUID NOT NULL,
    best_millis BIGINT NOT NULL,
    achieved_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (server_group, player_id)
);
