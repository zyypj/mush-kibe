package br.com.muxe.core.common.party.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class Party {

    private final UUID id;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final Map<UUID, Invite> invites = new LinkedHashMap<>();
    private UUID leader;
    private boolean open;

    Party(UUID id, PartyPlayer leader) {
        this.id = id;
        this.leader = leader.id();
        members.put(leader.id(), new Member(leader, Optional.empty()));
    }

    UUID id() {
        return id;
    }

    UUID leader() {
        return leader;
    }

    PartyPlayer leaderPlayer() {
        return members.get(leader).player();
    }

    boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    void leader(UUID player) {
        leader = player;
    }

    boolean isOpen() {
        return open;
    }

    void open(boolean value) {
        open = value;
    }

    boolean contains(UUID player) {
        return members.containsKey(player);
    }

    void add(PartyPlayer player) {
        members.put(player.id(), new Member(player, Optional.empty()));
        invites.remove(player.id());
    }

    void remove(UUID player) {
        members.remove(player);
    }

    Optional<PartyPlayer> named(String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        return members.values().stream().map(Member::player).filter(player -> player.name().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }

    Optional<PartyPlayer> player(UUID id) {
        return Optional.ofNullable(members.get(id)).map(Member::player);
    }

    List<PartyPlayer> players() {
        return members.values().stream().map(Member::player).toList();
    }

    List<UUID> ids() {
        return List.copyOf(members.keySet());
    }

    List<UUID> online() {
        return members.values().stream().filter(Member::online).map(member -> member.player().id()).toList();
    }

    List<PartyPlayer> offline() {
        return members.values().stream().filter(member -> !member.online()).map(Member::player).toList();
    }

    boolean isOnline(UUID player) {
        Member member = members.get(player);
        return member != null && member.online();
    }

    int size() {
        return members.size();
    }

    void offline(UUID player, Instant since) {
        members.computeIfPresent(player, (id, member) -> new Member(member.player(), Optional.of(since)));
    }

    void online(PartyPlayer player) {
        members.computeIfPresent(player.id(), (id, member) -> new Member(player, Optional.empty()));
    }

    List<PartyPlayer> offlineSince(Instant limit) {
        return members.values().stream()
            .filter(member -> member.offlineSince().filter(since -> !since.isAfter(limit)).isPresent())
            .map(Member::player)
            .toList();
    }

    Optional<PartyPlayer> oldestOnlineExcept(UUID player) {
        return members.values().stream().filter(Member::online).map(Member::player).filter(member -> !member.id().equals(player)).findFirst();
    }

    Optional<PartyPlayer> oldestExcept(UUID player) {
        return members.values().stream().map(Member::player).filter(member -> !member.id().equals(player)).findFirst();
    }

    void invite(PartyPlayer target, Instant expiresAt) {
        invites.put(target.id(), new Invite(target, expiresAt));
    }

    boolean invited(UUID target, Instant now) {
        Invite invite = invites.get(target);
        return invite != null && invite.expiresAt().isAfter(now);
    }

    boolean hasInvites() {
        return !invites.isEmpty();
    }

    void forgetInvite(UUID target) {
        invites.remove(target);
    }

    List<PartyPlayer> expireInvites(Instant now) {
        List<PartyPlayer> expired = new ArrayList<>();
        invites.values().removeIf(invite -> {
            if (invite.expiresAt().isAfter(now)) {
                return false;
            }
            expired.add(invite.target());
            return true;
        });
        return expired;
    }

    PartySnapshot snapshot() {
        List<PartySnapshot.Member> listed = new ArrayList<>();
        Member first = members.get(leader);
        if (first != null) {
            listed.add(new PartySnapshot.Member(leader, first.player().name(), first.online()));
        }
        members.values().stream()
            .filter(member -> !member.player().id().equals(leader))
            .forEach(member -> listed.add(new PartySnapshot.Member(member.player().id(), member.player().name(), member.online())));
        return new PartySnapshot(id, leader, open, listed);
    }

    private record Member(PartyPlayer player, Optional<Instant> offlineSince) {

        boolean online() {
            return offlineSince.isEmpty();
        }
    }

    private record Invite(PartyPlayer target, Instant expiresAt) {
    }
}
