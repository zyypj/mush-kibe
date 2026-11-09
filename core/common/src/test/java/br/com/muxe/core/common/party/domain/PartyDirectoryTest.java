package br.com.muxe.core.common.party.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartyDirectoryTest {

    private static final UUID LEADER = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID AWAY = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    private final PartyDirectory directory = new PartyDirectory();

    @Test
    void findsThePartyOfEveryMember() {
        PartySnapshot party = party(UUID.randomUUID());
        directory.put(party);

        assertThat(directory.of(MEMBER)).hasValue(party);
        assertThat(directory.of(STRANGER)).isEmpty();
    }

    @Test
    void onlyTheLeaderOrSomeoneOutOfAPartyMayQueue() {
        directory.put(party(UUID.randomUUID()));

        assertThat(directory.mayQueue(LEADER)).isTrue();
        assertThat(directory.mayQueue(MEMBER)).isFalse();
        assertThat(directory.mayQueue(STRANGER)).isTrue();
    }

    @Test
    void theLeaderBringsOnlyTheOnlineCompanions() {
        directory.put(party(UUID.randomUUID()));

        assertThat(directory.companions(LEADER)).containsExactly(MEMBER);
        assertThat(directory.companions(MEMBER)).isEmpty();
    }

    @Test
    void anUpdateReplacesTheOldMembersAndRemovalForgetsThem() {
        UUID id = UUID.randomUUID();
        directory.put(party(id));
        directory.put(new PartySnapshot(id, LEADER, false, List.of(new PartySnapshot.Member(LEADER, "tadeu", true))));

        assertThat(directory.of(MEMBER)).isEmpty();

        directory.remove(id);

        assertThat(directory.of(LEADER)).isEmpty();
        assertThat(directory.size()).isZero();
    }

    @Test
    void groupsOnlyThePartyMembersPresent() {
        directory.put(party(UUID.randomUUID()));

        assertThat(directory.groupsOf(List.of(STRANGER, MEMBER, LEADER))).containsExactly(List.of(LEADER, MEMBER));
        assertThat(directory.groupsOf(List.of(LEADER, STRANGER))).isEmpty();
    }

    private static PartySnapshot party(UUID id) {
        return new PartySnapshot(id, LEADER, false, List.of(
            new PartySnapshot.Member(LEADER, "tadeu", true),
            new PartySnapshot.Member(MEMBER, "tadeudobw", true),
            new PartySnapshot.Member(AWAY, "ana", false)
        ));
    }
}
