package br.com.muxe.core.common.party.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyDirectory {

    private final Map<UUID, PartySnapshot> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> byMember = new ConcurrentHashMap<>();

    public synchronized void put(PartySnapshot party) {
        remove(party.id());
        parties.put(party.id(), party);
        party.memberIds().forEach(member -> byMember.put(member, party.id()));
    }

    public synchronized void remove(UUID partyId) {
        PartySnapshot old = parties.remove(partyId);
        if (old != null) {
            old.memberIds().forEach(member -> byMember.remove(member, partyId));
        }
    }

    public synchronized void replaceAll(Collection<PartySnapshot> all) {
        parties.clear();
        byMember.clear();
        all.forEach(this::put);
    }

    public Optional<PartySnapshot> of(UUID player) {
        UUID partyId = byMember.get(player);
        return partyId == null ? Optional.empty() : Optional.ofNullable(parties.get(partyId));
    }

    public boolean mayQueue(UUID player) {
        return of(player).map(party -> party.isLeader(player)).orElse(true);
    }

    public List<UUID> companions(UUID leader) {
        return of(leader).filter(party -> party.isLeader(leader)).map(PartySnapshot::onlineCompanions).orElse(List.of());
    }

    public List<List<UUID>> groupsOf(Collection<UUID> players) {
        Set<UUID> present = new HashSet<>(players);
        Set<UUID> seen = new HashSet<>();
        List<List<UUID>> groups = new ArrayList<>();
        for (UUID player : players) {
            Optional<PartySnapshot> party = of(player);
            if (party.isEmpty() || !seen.add(party.get().id())) {
                continue;
            }
            List<UUID> together = party.get().memberIds().stream().filter(present::contains).toList();
            if (together.size() > 1) {
                groups.add(together);
            }
        }
        return groups;
    }

    public List<PartySnapshot> all() {
        return List.copyOf(parties.values());
    }

    public int size() {
        return parties.size();
    }
}
