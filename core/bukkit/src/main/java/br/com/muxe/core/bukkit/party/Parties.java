package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Parties {

    Optional<PartySnapshot> of(UUID player);

    boolean mayQueue(UUID player);

    List<UUID> companions(UUID leader);

    List<List<UUID>> groupsOf(Collection<UUID> players);

    void onGather(PartyGathering gathering);
}
