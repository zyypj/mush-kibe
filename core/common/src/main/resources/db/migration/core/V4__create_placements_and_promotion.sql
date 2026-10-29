CREATE TABLE npc_placements (
    server_group VARCHAR(32) NOT NULL,
    id VARCHAR(32) NOT NULL,
    world VARCHAR(64) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    yaw REAL NOT NULL,
    pitch REAL NOT NULL,
    name VARCHAR(128) NOT NULL,
    action VARCHAR(16) NOT NULL,
    action_target VARCHAR(32) NOT NULL,
    skin_name VARCHAR(16) NOT NULL,
    skin_value TEXT,
    skin_signature TEXT,
    PRIMARY KEY (server_group, id)
);

CREATE TABLE hologram_placements (
    server_group VARCHAR(32) NOT NULL,
    id VARCHAR(32) NOT NULL,
    world VARCHAR(64) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    yaw REAL NOT NULL,
    pitch REAL NOT NULL,
    lines TEXT[] NOT NULL,
    PRIMARY KEY (server_group, id)
);

CREATE TABLE register_placements (
    server_group VARCHAR(32) NOT NULL,
    id VARCHAR(32) NOT NULL,
    world VARCHAR(64) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    yaw REAL NOT NULL,
    pitch REAL NOT NULL,
    seller_world VARCHAR(64),
    seller_x DOUBLE PRECISION,
    seller_y DOUBLE PRECISION,
    seller_z DOUBLE PRECISION,
    seller_yaw REAL,
    seller_pitch REAL,
    PRIMARY KEY (server_group, id)
);

CREATE TABLE shop_promotion (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    product_id VARCHAR(64) NOT NULL,
    lines TEXT[] NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL
);
