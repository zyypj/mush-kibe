package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.api.runtime.MatchRuntime;
import java.util.List;
import org.bukkit.entity.Player;

public final class ShopGifts {

    private volatile Source source = new Source() {
        @Override
        public List<String> items() {
            return List.of();
        }

        @Override
        public boolean give(Player player, MatchRuntime<BedWarsMatch> runtime, String itemId) {
            return false;
        }
    };

    public void use(Source source) {
        this.source = source;
    }

    public List<String> items() {
        return source.items();
    }

    public boolean give(Player player, MatchRuntime<BedWarsMatch> runtime, String itemId) {
        return source.give(player, runtime, itemId);
    }

    public interface Source {

        List<String> items();

        boolean give(Player player, MatchRuntime<BedWarsMatch> runtime, String itemId);
    }
}
