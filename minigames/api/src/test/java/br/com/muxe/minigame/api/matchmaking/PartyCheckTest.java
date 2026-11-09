package br.com.muxe.minigame.api.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.party.Parties;
import br.com.muxe.core.bukkit.party.PartyGathering;
import br.com.muxe.core.common.party.domain.PartyDirectory;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartyCheckTest {

    private static final GameMode SOLO = new GameMode("solo", "Solo", 2, 8, 1);
    private static final GameMode DUEL = new GameMode("1v1", "1v1", 2, 2, 1);
    private static final UUID LEADER = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    private static final UUID ALONE = UUID.randomUUID();

    private final DirectoryParties parties = new DirectoryParties();

    @Test
    void onlyTheLeaderMaySearchForARoom() {
        parties.directory.put(party(true, true));

        assertThat(PartyCheck.of(parties, MEMBER, Optional.of(SOLO)).refusal()).contains(PartyCheck.LEADER_ONLY);
    }

    @Test
    void theLeaderBringsEveryOnlineCompanion() {
        parties.directory.put(party(true, false));

        PartyCheck check = PartyCheck.of(parties, LEADER, Optional.of(SOLO));

        assertThat(check.refusal()).isEmpty();
        assertThat(check.companions()).containsExactly(MEMBER);
    }

    @Test
    void aModeTooSmallForThePartyIsRefused() {
        parties.directory.put(party(true, true));

        assertThat(PartyCheck.of(parties, LEADER, Optional.of(DUEL)).refusal()).contains(PartyCheck.TOO_BIG);
        assertThat(PartyCheck.of(parties, LEADER, Optional.of(SOLO)).refusal()).isEmpty();
    }

    @Test
    void someoneOutOfAPartyGoesAlone() {
        PartyCheck check = PartyCheck.of(parties, ALONE, Optional.of(DUEL));

        assertThat(check.refusal()).isEmpty();
        assertThat(check.companions()).isEmpty();
    }

    private static PartySnapshot party(boolean memberOnline, boolean otherOnline) {
        return new PartySnapshot(UUID.randomUUID(), LEADER, false, List.of(
            new PartySnapshot.Member(LEADER, "tadeu", true),
            new PartySnapshot.Member(MEMBER, "tadeudobw", memberOnline),
            new PartySnapshot.Member(OTHER, "ana", otherOnline)));
    }

    private static final class DirectoryParties implements Parties {

        private final PartyDirectory directory = new PartyDirectory();

        @Override
        public Optional<PartySnapshot> of(UUID player) {
            return directory.of(player);
        }

        @Override
        public boolean mayQueue(UUID player) {
            return directory.mayQueue(player);
        }

        @Override
        public List<UUID> companions(UUID leader) {
            return directory.companions(leader);
        }

        @Override
        public List<List<UUID>> groupsOf(Collection<UUID> players) {
            return directory.groupsOf(players);
        }

        @Override
        public void onGather(PartyGathering gathering) {
        }
    }
}
