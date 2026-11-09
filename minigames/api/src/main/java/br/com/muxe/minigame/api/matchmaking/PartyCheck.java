package br.com.muxe.minigame.api.matchmaking;

import br.com.muxe.core.bukkit.party.Parties;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record PartyCheck(Optional<String> refusal, List<UUID> companions) {

    public static final String LEADER_ONLY = "party-leader-only";
    public static final String TOO_BIG = "party-too-big";

    public PartyCheck {
        companions = List.copyOf(companions);
    }

    public static PartyCheck of(Parties parties, UUID player, Optional<GameMode> mode) {
        if (!parties.mayQueue(player)) {
            return new PartyCheck(Optional.of(LEADER_ONLY), List.of());
        }
        List<UUID> companions = parties.companions(player);
        if (mode.isPresent() && mode.get().maxPlayers() < 1 + companions.size()) {
            return new PartyCheck(Optional.of(TOO_BIG), List.of());
        }
        return new PartyCheck(Optional.empty(), companions);
    }
}
