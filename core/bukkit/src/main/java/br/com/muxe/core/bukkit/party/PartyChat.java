package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.common.network.party.PartyAction;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.entity.Player;

public final class PartyChat {

    private final PartyClient parties;
    private final Function<UUID, RankDisplay> displays;

    public PartyChat(PartyClient parties, Function<UUID, RankDisplay> displays) {
        this.parties = parties;
        this.displays = displays;
    }

    public boolean inParty(UUID player) {
        return parties.of(player).isPresent();
    }

    public void send(Player player, String message) {
        String line = parties.texts().chat(displays.apply(player.getUniqueId()), player.getName(), message);
        parties.send(player, PartyAction.CHAT, line);
    }
}
