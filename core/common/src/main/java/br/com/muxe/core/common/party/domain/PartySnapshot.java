package br.com.muxe.core.common.party.domain;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PartySnapshot(UUID id, UUID leader, boolean open, List<Member> members) {

    public PartySnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(leader, "leader");
        members = List.copyOf(members);
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public boolean contains(UUID player) {
        return members.stream().anyMatch(member -> member.id().equals(player));
    }

    public List<UUID> memberIds() {
        return members.stream().map(Member::id).toList();
    }

    public List<UUID> onlineMembers() {
        return members.stream().filter(Member::online).map(Member::id).toList();
    }

    public List<UUID> onlineCompanions() {
        return members.stream().filter(Member::online).map(Member::id).filter(id -> !id.equals(leader)).toList();
    }

    public Optional<Member> named(String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        return members.stream().filter(member -> member.name().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }

    public record Member(UUID id, String name, boolean online) {

        public Member {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }
    }
}
