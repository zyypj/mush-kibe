ALTER TABLE npc_placements ADD COLUMN lines TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE npc_placements ADD COLUMN hand_material VARCHAR(64);
ALTER TABLE npc_placements ADD COLUMN hand_data SMALLINT NOT NULL DEFAULT 0;
