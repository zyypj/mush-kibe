package br.com.muxe.core.bukkit.display;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface StandBody {

    StandBody NONE = new StandBody() {
        @Override
        public int entityId() {
            return -1;
        }

        @Override
        public void show(Player viewer) {
        }

        @Override
        public void move(Location location, Collection<? extends Player> viewers) {
        }

        @Override
        public void hide(Player viewer) {
        }
    };

    int entityId();

    void show(Player viewer);

    void move(Location location, Collection<? extends Player> viewers);

    void hide(Player viewer);
}
