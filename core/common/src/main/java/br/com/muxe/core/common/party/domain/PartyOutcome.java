package br.com.muxe.core.common.party.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PartyOutcome(boolean accepted, List<PartyNotice> notices, Set<UUID> changed, Set<UUID> removed, List<UUID> summoned) {

    public PartyOutcome {
        notices = List.copyOf(notices);
        changed = Set.copyOf(changed);
        removed = Set.copyOf(removed);
        summoned = List.copyOf(summoned);
    }

    public static PartyOutcome refused(UUID actor, String key, String... placeholders) {
        return new PartyOutcome(false, List.of(PartyNotice.of(List.of(actor), key, placeholders)), Set.of(), Set.of(), List.of());
    }

    public static PartyOutcome nothing() {
        return new PartyOutcome(true, List.of(), Set.of(), Set.of(), List.of());
    }

    public boolean isEmpty() {
        return notices.isEmpty() && changed.isEmpty() && removed.isEmpty() && summoned.isEmpty();
    }
}
