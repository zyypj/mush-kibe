CREATE TABLE social_head_placements (
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
