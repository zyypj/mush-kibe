package br.com.muxe.core.common.party.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PartyBook {

    private final PartyRules rules;
    private final Function<PartyPlayer, String> displays;
    private final Supplier<UUID> ids;
    private final Map<UUID, Party> parties = new LinkedHashMap<>();
    private final Map<UUID, UUID> byMember = new HashMap<>();

    public PartyBook(PartyRules rules, Function<PartyPlayer, String> displays, Supplier<UUID> ids) {
        this.rules = rules;
        this.displays = displays;
        this.ids = ids;
    }

    public PartyBook(PartyRules rules, Function<PartyPlayer, String> displays) {
        this(rules, displays, UUID::randomUUID);
    }

    public Optional<PartySnapshot> partyOf(UUID player) {
        return find(player).map(Party::snapshot);
    }

    public Optional<PartySnapshot> snapshot(UUID partyId) {
        return Optional.ofNullable(parties.get(partyId)).map(Party::snapshot);
    }

    public List<PartySnapshot> all() {
        return parties.values().stream().map(Party::snapshot).toList();
    }

    public PartyOutcome invite(PartyPlayer actor, PartyPlayer target, Instant now) {
        if (actor.id().equals(target.id())) {
            return PartyOutcome.refused(actor.id(), "party-invite-self");
        }
        Party party = find(actor.id()).orElse(null);
        if (party != null && !party.isLeader(actor.id())) {
            return PartyOutcome.refused(actor.id(), "party-not-leader");
        }
        if (party != null && party.contains(target.id())) {
            return PartyOutcome.refused(actor.id(), "party-already-member", "player", display(target));
        }
        if (find(target.id()).isPresent()) {
            return PartyOutcome.refused(actor.id(), "party-target-in-party", "player", display(target));
        }
        if (party != null && party.invited(target.id(), now)) {
            return PartyOutcome.refused(actor.id(), "party-already-invited", "player", display(target));
        }
        Effects effects = new Effects();
        if (party == null) {
            party = create(actor);
        }
        party.invite(target, now.plus(rules.inviteLifetime()));
        effects.changed(party);
        effects.notice(PartyNotice.of(party.online(), "party-invite-sent", "player", display(target)));
        effects.notice(PartyNotice.of(List.of(target.id()), "party-invite", "player", display(actor)).clicking("/party aceitar " + actor.name()));
        return effects.outcome();
    }

    public PartyOutcome accept(PartyPlayer actor, String inviter, Instant now) {
        if (find(actor.id()).isPresent()) {
            return PartyOutcome.refused(actor.id(), "party-already-in-party");
        }
        Optional<Party> found = parties.values().stream()
            .filter(party -> party.named(inviter).isPresent() && party.invited(actor.id(), now))
            .findFirst();
        if (found.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-no-invite", "name", inviter);
        }
        return admit(found.get(), actor);
    }

    public PartyOutcome join(PartyPlayer actor, String leader) {
        if (find(actor.id()).isPresent()) {
            return PartyOutcome.refused(actor.id(), "party-already-in-party");
        }
        Optional<Party> found = parties.values().stream()
            .filter(Party::isOpen)
            .filter(party -> party.leaderPlayer().name().equalsIgnoreCase(leader))
            .findFirst();
        if (found.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-not-open");
        }
        return admit(found.get(), actor);
    }

    public PartyOutcome leave(PartyPlayer actor) {
        Optional<Party> found = find(actor.id());
        if (found.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-not-in-party");
        }
        Party party = found.get();
        boolean wasLeader = party.isLeader(actor.id());
        Effects effects = new Effects();
        party.remove(actor.id());
        byMember.remove(actor.id());
        effects.notice(PartyNotice.of(List.of(actor.id()), "party-you-left"));
        effects.notice(PartyNotice.of(party.online(), "party-left", "player", display(actor)));
        if (wasLeader) {
            promote(party, effects);
        }
        effects.changed(party);
        prune(party, effects);
        return effects.outcome();
    }

    public PartyOutcome disband(PartyPlayer actor) {
        Optional<Party> led = led(actor);
        if (led.isEmpty()) {
            return notLeading(actor);
        }
        Effects effects = new Effects();
        effects.notice(PartyNotice.of(led.get().online(), "party-disbanded", "player", display(actor)));
        remove(led.get(), effects);
        return effects.outcome();
    }

    public PartyOutcome kick(PartyPlayer actor, String name) {
        Optional<Party> led = led(actor);
        if (led.isEmpty()) {
            return notLeading(actor);
        }
        Party party = led.get();
        Optional<PartyPlayer> target = party.named(name);
        if (target.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-not-member", "name", name);
        }
        if (target.get().id().equals(actor.id())) {
            return PartyOutcome.refused(actor.id(), "party-kick-self");
        }
        Effects effects = new Effects();
        effects.notice(PartyNotice.of(party.online(), "party-kicked", "leader", display(actor), "player", display(target.get())));
        party.remove(target.get().id());
        byMember.remove(target.get().id());
        effects.changed(party);
        prune(party, effects);
        return effects.outcome();
    }

    public PartyOutcome transfer(PartyPlayer actor, String name) {
        Optional<Party> led = led(actor);
        if (led.isEmpty()) {
            return notLeading(actor);
        }
        Party party = led.get();
        Optional<PartyPlayer> target = party.named(name);
        if (target.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-not-member", "name", name);
        }
        if (target.get().id().equals(actor.id())) {
            return PartyOutcome.refused(actor.id(), "party-already-leader");
        }
        if (!party.isOnline(target.get().id())) {
            return PartyOutcome.refused(actor.id(), "party-member-offline", "name", target.get().name());
        }
        Effects effects = new Effects();
        party.leader(target.get().id());
        effects.changed(party);
        effects.notice(PartyNotice.of(party.online(), "party-new-leader", "player", display(target.get())));
        return effects.outcome();
    }

    public PartyOutcome toggleOpen(PartyPlayer actor, boolean allowed) {
        if (!allowed) {
            return PartyOutcome.refused(actor.id(), "party-open-no-permission");
        }
        Party party = find(actor.id()).orElse(null);
        if (party != null && !party.isLeader(actor.id())) {
            return PartyOutcome.refused(actor.id(), "party-not-leader");
        }
        Effects effects = new Effects();
        if (party == null) {
            party = create(actor);
        }
        party.open(!party.isOpen());
        effects.changed(party);
        effects.notice(PartyNotice.of(party.online(), party.isOpen() ? "party-opened" : "party-closed", "player", display(actor)));
        prune(party, effects);
        return effects.outcome();
    }

    public PartyOutcome removeOffline(PartyPlayer actor) {
        Optional<Party> led = led(actor);
        if (led.isEmpty()) {
            return notLeading(actor);
        }
        Party party = led.get();
        List<PartyPlayer> offline = party.offline();
        if (offline.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-no-offline");
        }
        Effects effects = new Effects();
        offline.forEach(player -> {
            party.remove(player.id());
            byMember.remove(player.id());
        });
        effects.changed(party);
        effects.notice(PartyNotice.of(party.online(), "party-offline-removed", "player", display(actor), "count", String.valueOf(offline.size())));
        prune(party, effects);
        return effects.outcome();
    }

    public PartyOutcome chat(PartyPlayer actor, String line) {
        Optional<Party> found = find(actor.id());
        if (found.isEmpty()) {
            return PartyOutcome.refused(actor.id(), "party-not-in-party");
        }
        Effects effects = new Effects();
        effects.notice(PartyNotice.raw(found.get().online(), line));
        return effects.outcome();
    }

    public PartyOutcome summon(PartyPlayer actor) {
        Optional<Party> led = led(actor);
        if (led.isEmpty()) {
            return notLeading(actor);
        }
        Party party = led.get();
        Effects effects = new Effects();
        effects.notice(PartyNotice.of(party.online(), "party-summoned", "player", display(actor)));
        party.online().stream().filter(id -> !party.isLeader(id)).forEach(effects.summoned::add);
        return effects.outcome();
    }

    public PartyOutcome connected(PartyPlayer player) {
        Optional<Party> found = find(player.id());
        if (found.isEmpty()) {
            return PartyOutcome.nothing();
        }
        Party party = found.get();
        boolean wasOffline = !party.isOnline(player.id());
        Effects effects = new Effects();
        party.online(player);
        effects.changed(party);
        if (wasOffline) {
            List<UUID> others = party.online().stream().filter(id -> !id.equals(player.id())).toList();
            effects.notice(PartyNotice.of(others, "party-member-reconnected", "player", display(player)));
        }
        return effects.outcome();
    }

    public PartyOutcome disconnected(UUID player, Instant now) {
        parties.values().forEach(party -> party.forgetInvite(player));
        Optional<Party> found = find(player);
        if (found.isEmpty()) {
            return PartyOutcome.nothing();
        }
        Party party = found.get();
        Effects effects = new Effects();
        party.offline(player, now);
        effects.changed(party);
        party.player(player).ifPresent(gone -> effects.notice(PartyNotice.of(party.online(), "party-member-disconnected",
            "player", display(gone), "minutes", String.valueOf(rules.offlineGrace().toMinutes()))));
        if (party.isLeader(player)) {
            party.oldestOnlineExcept(player).ifPresent(next -> {
                party.leader(next.id());
                effects.notice(PartyNotice.of(party.online(), "party-new-leader", "player", display(next)));
            });
        }
        return effects.outcome();
    }

    public PartyOutcome tick(Instant now) {
        Effects effects = new Effects();
        Instant limit = now.minus(rules.offlineGrace());
        for (Party party : List.copyOf(parties.values())) {
            for (PartyPlayer target : party.expireInvites(now)) {
                effects.notice(PartyNotice.of(List.of(party.leader()), "party-invite-expired-sender", "player", display(target)));
                effects.notice(PartyNotice.of(List.of(target.id()), "party-invite-expired", "player", display(party.leaderPlayer())));
                effects.changed(party);
            }
            for (PartyPlayer stale : party.offlineSince(limit)) {
                boolean wasLeader = party.isLeader(stale.id());
                party.remove(stale.id());
                byMember.remove(stale.id());
                effects.notice(PartyNotice.of(party.online(), "party-offline-kicked", "player", display(stale)));
                if (wasLeader) {
                    promote(party, effects);
                }
                effects.changed(party);
            }
            prune(party, effects);
        }
        return effects.outcome();
    }

    private PartyOutcome admit(Party party, PartyPlayer actor) {
        Effects effects = new Effects();
        party.add(actor);
        byMember.put(actor.id(), party.id());
        effects.changed(party);
        effects.notice(PartyNotice.of(party.online(), "party-joined", "player", display(actor)));
        return effects.outcome();
    }

    private void promote(Party party, Effects effects) {
        Optional<PartyPlayer> next = party.oldestOnlineExcept(party.leader()).or(() -> party.oldestExcept(party.leader()));
        if (next.isEmpty() || party.contains(party.leader())) {
            return;
        }
        party.leader(next.get().id());
        effects.notice(PartyNotice.of(party.online(), "party-new-leader", "player", display(next.get())));
    }

    private void prune(Party party, Effects effects) {
        if (!parties.containsKey(party.id())) {
            return;
        }
        boolean empty = party.size() == 0;
        boolean alone = party.size() == 1 && !party.isOpen() && !party.hasInvites();
        if (empty || alone) {
            remove(party, effects);
        }
    }

    private void remove(Party party, Effects effects) {
        parties.remove(party.id());
        party.ids().forEach(byMember::remove);
        effects.removed(party);
    }

    private Party create(PartyPlayer leader) {
        Party party = new Party(ids.get(), leader);
        parties.put(party.id(), party);
        byMember.put(leader.id(), party.id());
        return party;
    }

    private Optional<Party> find(UUID player) {
        UUID partyId = byMember.get(player);
        return partyId == null ? Optional.empty() : Optional.ofNullable(parties.get(partyId));
    }

    private Optional<Party> led(PartyPlayer actor) {
        return find(actor.id()).filter(party -> party.isLeader(actor.id()));
    }

    private PartyOutcome notLeading(PartyPlayer actor) {
        return PartyOutcome.refused(actor.id(), find(actor.id()).isPresent() ? "party-not-leader" : "party-not-in-party");
    }

    private String display(PartyPlayer player) {
        return displays.apply(player);
    }

    private static final class Effects {

        private final List<PartyNotice> notices = new ArrayList<>();
        private final Set<UUID> changed = new LinkedHashSet<>();
        private final Set<UUID> removed = new LinkedHashSet<>();
        private final List<UUID> summoned = new ArrayList<>();

        void notice(PartyNotice notice) {
            if (!notice.recipients().isEmpty()) {
                notices.add(notice);
            }
        }

        void changed(Party party) {
            changed.add(party.id());
        }

        void removed(Party party) {
            removed.add(party.id());
            changed.remove(party.id());
        }

        PartyOutcome outcome() {
            Set<UUID> stillThere = new LinkedHashSet<>(changed);
            stillThere.removeAll(removed);
            return new PartyOutcome(true, notices, stillThere, removed, summoned);
        }
    }
}
