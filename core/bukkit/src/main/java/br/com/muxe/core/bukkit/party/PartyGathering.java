package br.com.muxe.core.bukkit.party;

import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface PartyGathering {

    PartyGathering EVERYONE = (leader, members) -> members;

    List<UUID> gather(UUID leader, List<UUID> members);
}
