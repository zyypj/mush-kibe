package br.com.muxe.core.bukkit.npc;

import org.bukkit.entity.Player;

public interface NpcBody {

    NpcBody NONE = new NpcBody() {
        @Override
        public int entityId() {
            return -1;
        }

        @Override
        public void show(Player viewer) {
        }

        @Override
        public void leaveTabList(Player viewer) {
        }

        @Override
        public void hide(Player viewer) {
        }
    };

    int entityId();

    void show(Player viewer);

    void leaveTabList(Player viewer);

    void hide(Player viewer);
}
