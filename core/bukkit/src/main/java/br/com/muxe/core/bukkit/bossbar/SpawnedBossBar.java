package br.com.muxe.core.bukkit.bossbar;

import org.bukkit.Location;

public interface SpawnedBossBar {

    SpawnedBossBar NONE = new SpawnedBossBar() {
        @Override
        public void move(Location location) {
        }

        @Override
        public void rename(String text) {
        }

        @Override
        public void remove() {
        }
    };

    void move(Location location);

    void rename(String text);

    void remove();
}
