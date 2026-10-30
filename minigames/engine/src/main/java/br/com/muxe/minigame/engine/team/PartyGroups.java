package br.com.muxe.minigame.engine.team;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface PartyGroups {

    PartyGroups NONE = players -> List.of();

    List<List<UUID>> groupsOf(Collection<UUID> players);
}
