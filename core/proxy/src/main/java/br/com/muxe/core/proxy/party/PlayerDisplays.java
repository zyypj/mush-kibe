package br.com.muxe.core.proxy.party;

import br.com.muxe.core.common.party.domain.PartyPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class PlayerDisplays {

    public static final String DEFAULT_COLOR = "&7";

    private final Map<UUID, String> colored = new ConcurrentHashMap<>();

    public void remember(UUID player, String coloredName) {
        colored.put(player, coloredName);
    }

    public String of(PartyPlayer player) {
        return colored.getOrDefault(player.id(), DEFAULT_COLOR + player.name());
    }

    public void retain(Predicate<UUID> keep) {
        colored.keySet().removeIf(keep.negate());
    }

    public int size() {
        return colored.size();
    }
}
