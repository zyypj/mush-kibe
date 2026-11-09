package br.com.muxe.core.common.party.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartyBookTest {

    private static final PartyPlayer TADEU = new PartyPlayer(UUID.randomUUID(), "tadeu");
    private static final PartyPlayer DOBW = new PartyPlayer(UUID.randomUUID(), "tadeudobw");
    private static final PartyPlayer ANA = new PartyPlayer(UUID.randomUUID(), "ana");

    private MutableClock clock;
    private PartyBook book;

    @BeforeEach
    void setUp() {
        clock = MutableClock.startingAt("2026-11-09T12:00:00Z");
        book = new PartyBook(PartyRules.DEFAULT, player -> "&5" + player.name());
    }

    @Test
    void invitingCreatesThePartyAndTellsBothSides() {
        PartyOutcome outcome = book.invite(TADEU, DOBW, clock.instant());

        assertThat(outcome.accepted()).isTrue();
        assertThat(book.partyOf(TADEU.id())).hasValueSatisfying(party -> assertThat(party.isLeader(TADEU.id())).isTrue());
        assertThat(outcome.notices()).extracting(PartyNotice::key).containsExactly("party-invite-sent", "party-invite");
        PartyNotice sent = outcome.notices().getFirst();
        assertThat(sent.recipients()).containsExactly(TADEU.id());
        assertThat(sent.value("player")).hasValue("&5tadeudobw");
        PartyNotice invite = outcome.notices().get(1);
        assertThat(invite.recipients()).containsExactly(DOBW.id());
        assertThat(invite.value("player")).hasValue("&5tadeu");
        assertThat(invite.clickCommand()).hasValue("/party aceitar tadeu");
    }

    @Test
    void acceptingJoinsThePartyAndTellsEveryone() {
        book.invite(TADEU, DOBW, clock.instant());

        PartyOutcome outcome = book.accept(DOBW, "TADEU", clock.instant());

        assertThat(book.partyOf(DOBW.id())).hasValueSatisfying(party -> assertThat(party.memberIds()).containsExactly(TADEU.id(), DOBW.id()));
        assertThat(outcome.notices()).singleElement().satisfies(notice -> {
            assertThat(notice.key()).isEqualTo("party-joined");
            assertThat(notice.recipients()).containsExactlyInAnyOrder(TADEU.id(), DOBW.id());
            assertThat(notice.value("player")).hasValue("&5tadeudobw");
        });
    }

    @Test
    void anExpiredInviteCannotBeAccepted() {
        book.invite(TADEU, DOBW, clock.instant());
        clock.advance(Duration.ofSeconds(61));

        PartyOutcome outcome = book.accept(DOBW, "tadeu", clock.instant());

        assertThat(outcome.accepted()).isFalse();
        assertThat(outcome.notices().getFirst().key()).isEqualTo("party-no-invite");
        assertThat(outcome.notices().getFirst().value("name")).hasValue("tadeu");
    }

    @Test
    void invitesExpireAfterSixtySecondsTellingBothSidesAndDropTheLonelyParty() {
        book.invite(TADEU, DOBW, clock.instant());
        clock.advance(Duration.ofSeconds(59));
        assertThat(book.tick(clock.instant()).notices()).isEmpty();

        clock.advance(Duration.ofSeconds(1));
        PartyOutcome outcome = book.tick(clock.instant());

        assertThat(outcome.notices()).extracting(PartyNotice::key).containsExactly("party-invite-expired-sender", "party-invite-expired");
        assertThat(outcome.notices().getFirst().recipients()).containsExactly(TADEU.id());
        assertThat(outcome.notices().get(1).recipients()).containsExactly(DOBW.id());
        assertThat(book.partyOf(TADEU.id())).isEmpty();
        assertThat(outcome.removed()).hasSize(1);
    }

    @Test
    void onlyTheLeaderInvites() {
        partyOfThree();

        PartyOutcome outcome = book.invite(DOBW, new PartyPlayer(UUID.randomUUID(), "outro"), clock.instant());

        assertThat(outcome.accepted()).isFalse();
        assertThat(outcome.notices().getFirst().key()).isEqualTo("party-not-leader");
    }

    @Test
    void invitesAreRefusedForMembersPlayersInOtherPartiesSelfAndTwice() {
        book.invite(TADEU, DOBW, clock.instant());
        assertThat(book.invite(TADEU, DOBW, clock.instant()).notices().getFirst().key()).isEqualTo("party-already-invited");
        assertThat(book.invite(TADEU, TADEU, clock.instant()).notices().getFirst().key()).isEqualTo("party-invite-self");
        book.accept(DOBW, "tadeu", clock.instant());
        assertThat(book.invite(TADEU, DOBW, clock.instant()).notices().getFirst().key()).isEqualTo("party-already-member");
        book.invite(ANA, new PartyPlayer(UUID.randomUUID(), "bia"), clock.instant());
        assertThat(book.invite(TADEU, ANA, clock.instant()).notices().getFirst().key()).isEqualTo("party-target-in-party");
    }

    @Test
    void thereIsNoMemberLimit() {
        book.invite(TADEU, DOBW, clock.instant());
        book.accept(DOBW, "tadeu", clock.instant());
        for (int index = 0; index < 20; index++) {
            PartyPlayer guest = new PartyPlayer(UUID.randomUUID(), "guest" + index);
            book.invite(TADEU, guest, clock.instant());
            book.accept(guest, "tadeu", clock.instant());
        }

        assertThat(book.partyOf(TADEU.id())).hasValueSatisfying(party -> assertThat(party.members()).hasSize(22));
    }

    @Test
    void disbandingTellsEveryoneAndRemovesTheParty() {
        partyOfThree();

        PartyOutcome outcome = book.disband(TADEU);

        assertThat(outcome.notices()).singleElement().satisfies(notice -> {
            assertThat(notice.key()).isEqualTo("party-disbanded");
            assertThat(notice.recipients()).containsExactlyInAnyOrder(TADEU.id(), DOBW.id(), ANA.id());
        });
        assertThat(book.partyOf(DOBW.id())).isEmpty();
        assertThat(book.all()).isEmpty();
    }

    @Test
    void membersCannotDisbandKickOrTransfer() {
        partyOfThree();

        assertThat(book.disband(DOBW).notices().getFirst().key()).isEqualTo("party-not-leader");
        assertThat(book.kick(DOBW, "ana").notices().getFirst().key()).isEqualTo("party-not-leader");
        assertThat(book.transfer(DOBW, "ana").notices().getFirst().key()).isEqualTo("party-not-leader");
        assertThat(book.disband(new PartyPlayer(UUID.randomUUID(), "x")).notices().getFirst().key()).isEqualTo("party-not-in-party");
    }

    @Test
    void kickingTellsTheWholePartyIncludingTheKickedPlayer() {
        partyOfThree();

        PartyOutcome outcome = book.kick(TADEU, "TadeuDoBW");

        assertThat(outcome.notices()).singleElement().satisfies(notice -> {
            assertThat(notice.key()).isEqualTo("party-kicked");
            assertThat(notice.recipients()).containsExactlyInAnyOrder(TADEU.id(), DOBW.id(), ANA.id());
            assertThat(notice.value("leader")).hasValue("&5tadeu");
            assertThat(notice.value("player")).hasValue("&5tadeudobw");
        });
        assertThat(book.partyOf(DOBW.id())).isEmpty();
        assertThat(book.kick(TADEU, "ninguem").notices().getFirst().key()).isEqualTo("party-not-member");
        assertThat(book.kick(TADEU, "tadeu").notices().getFirst().key()).isEqualTo("party-kick-self");
    }

    @Test
    void transferringMakesAnOnlineMemberTheLeader() {
        partyOfThree();

        PartyOutcome outcome = book.transfer(TADEU, "ana");

        assertThat(outcome.notices().getFirst().key()).isEqualTo("party-new-leader");
        assertThat(outcome.notices().getFirst().value("player")).hasValue("&5ana");
        assertThat(book.partyOf(TADEU.id())).hasValueSatisfying(party -> assertThat(party.leader()).isEqualTo(ANA.id()));
        assertThat(book.transfer(ANA, "ana").notices().getFirst().key()).isEqualTo("party-already-leader");
    }

    @Test
    void leadershipCannotGoToAnOfflineMember() {
        partyOfThree();
        book.disconnected(ANA.id(), clock.instant());

        assertThat(book.transfer(TADEU, "ana").notices().getFirst().key()).isEqualTo("party-member-offline");
    }

    @Test
    void whenTheLeaderLeavesTheOldestMemberLeads() {
        partyOfThree();

        PartyOutcome outcome = book.leave(TADEU);

        assertThat(outcome.notices()).extracting(PartyNotice::key).containsExactly("party-you-left", "party-left", "party-new-leader");
        assertThat(outcome.notices().getFirst().recipients()).containsExactly(TADEU.id());
        assertThat(outcome.notices().get(1).recipients()).containsExactlyInAnyOrder(DOBW.id(), ANA.id());
        assertThat(book.partyOf(ANA.id())).hasValueSatisfying(party -> assertThat(party.leader()).isEqualTo(DOBW.id()));
    }

    @Test
    void aPartyLeftWithOnlyOneMemberEnds() {
        book.invite(TADEU, DOBW, clock.instant());
        book.accept(DOBW, "tadeu", clock.instant());

        PartyOutcome outcome = book.leave(DOBW);

        assertThat(outcome.removed()).hasSize(1);
        assertThat(book.partyOf(TADEU.id())).isEmpty();
    }

    @Test
    void anOpenPartyLetsAnyoneJoinByTheLeaderNick() {
        assertThat(book.toggleOpen(TADEU, false).notices().getFirst().key()).isEqualTo("party-open-no-permission");

        PartyOutcome opened = book.toggleOpen(TADEU, true);
        PartyOutcome joined = book.join(DOBW, "TADEU");

        assertThat(opened.notices().getFirst().key()).isEqualTo("party-opened");
        assertThat(joined.notices().getFirst().key()).isEqualTo("party-joined");
        assertThat(book.partyOf(DOBW.id())).hasValueSatisfying(party -> assertThat(party.open()).isTrue());
    }

    @Test
    void togglingAgainClosesTheParty() {
        book.toggleOpen(TADEU, true);
        book.join(DOBW, "tadeu");

        PartyOutcome closed = book.toggleOpen(TADEU, true);

        assertThat(closed.notices().getFirst().key()).isEqualTo("party-closed");
        assertThat(book.join(ANA, "tadeu").notices().getFirst().key()).isEqualTo("party-not-open");
    }

    @Test
    void aClosedPartyCannotBeJoinedWithoutAnInvite() {
        partyOfThree();

        assertThat(book.join(new PartyPlayer(UUID.randomUUID(), "x"), "tadeu").notices().getFirst().key()).isEqualTo("party-not-open");
    }

    @Test
    void whenTheLeaderGoesOfflineTheOldestOnlineMemberLeadsAtOnce() {
        partyOfThree();

        PartyOutcome outcome = book.disconnected(TADEU.id(), clock.instant());

        assertThat(outcome.notices()).singleElement().satisfies(notice -> {
            assertThat(notice.key()).isEqualTo("party-new-leader");
            assertThat(notice.value("player")).hasValue("&5tadeudobw");
            assertThat(notice.recipients()).containsExactlyInAnyOrder(DOBW.id(), ANA.id());
        });
        assertThat(book.partyOf(TADEU.id())).hasValueSatisfying(party -> {
            assertThat(party.leader()).isEqualTo(DOBW.id());
            assertThat(party.onlineMembers()).containsExactly(DOBW.id(), ANA.id());
        });
    }

    @Test
    void offlineMembersStayForFiveMinutesThenAreRemoved() {
        partyOfThree();
        book.disconnected(ANA.id(), clock.instant());
        clock.advance(Duration.ofMinutes(4));
        assertThat(book.tick(clock.instant()).notices()).isEmpty();

        clock.advance(Duration.ofMinutes(1));
        PartyOutcome outcome = book.tick(clock.instant());

        assertThat(outcome.notices()).singleElement().satisfies(notice -> {
            assertThat(notice.key()).isEqualTo("party-offline-kicked");
            assertThat(notice.value("player")).hasValue("&5ana");
        });
        assertThat(book.partyOf(ANA.id())).isEmpty();
        assertThat(book.partyOf(TADEU.id())).isPresent();
    }

    @Test
    void comingBackInTimeKeepsTheMember() {
        partyOfThree();
        book.disconnected(ANA.id(), clock.instant());
        clock.advance(Duration.ofMinutes(4));
        book.connected(ANA);
        clock.advance(Duration.ofMinutes(2));

        book.tick(clock.instant());

        assertThat(book.partyOf(ANA.id())).hasValueSatisfying(party -> assertThat(party.onlineMembers()).contains(ANA.id()));
    }

    @Test
    void thePartyEndsWhenEveryoneStaysOffline() {
        partyOfThree();
        book.disconnected(TADEU.id(), clock.instant());
        book.disconnected(DOBW.id(), clock.instant());
        book.disconnected(ANA.id(), clock.instant());
        clock.advance(Duration.ofMinutes(5));

        PartyOutcome outcome = book.tick(clock.instant());

        assertThat(outcome.removed()).hasSize(1);
        assertThat(book.all()).isEmpty();
    }

    @Test
    void removingOfflineMembersKeepsOnlyTheOnlineOnes() {
        partyOfThree();
        assertThat(book.removeOffline(TADEU).notices().getFirst().key()).isEqualTo("party-no-offline");
        book.disconnected(ANA.id(), clock.instant());

        PartyOutcome outcome = book.removeOffline(TADEU);

        assertThat(outcome.notices().getFirst().key()).isEqualTo("party-offline-removed");
        assertThat(outcome.notices().getFirst().value("count")).hasValue("1");
        assertThat(book.partyOf(ANA.id())).isEmpty();
        assertThat(book.partyOf(DOBW.id())).isPresent();
    }

    @Test
    void partyChatReachesOnlyOnlineMembers() {
        partyOfThree();
        book.disconnected(ANA.id(), clock.instant());

        PartyOutcome outcome = book.chat(DOBW, "§d[PARTY] §7tadeudobw: oi");

        assertThat(outcome.notices()).singleElement().satisfies(notice -> {
            assertThat(notice.isRaw()).isTrue();
            assertThat(notice.raw()).isEqualTo("§d[PARTY] §7tadeudobw: oi");
            assertThat(notice.recipients()).containsExactlyInAnyOrder(TADEU.id(), DOBW.id());
        });
        assertThat(book.chat(new PartyPlayer(UUID.randomUUID(), "x"), "oi").notices().getFirst().key()).isEqualTo("party-not-in-party");
    }

    @Test
    void summoningListsTheOnlineCompanionsOfTheLeader() {
        partyOfThree();
        book.disconnected(ANA.id(), clock.instant());

        PartyOutcome outcome = book.summon(TADEU);

        assertThat(outcome.summoned()).containsExactly(DOBW.id());
        assertThat(outcome.notices().getFirst().key()).isEqualTo("party-summoned");
        assertThat(book.summon(DOBW).summoned()).isEmpty();
    }

    @Test
    void snapshotsListTheLeaderFirst() {
        partyOfThree();
        book.transfer(TADEU, "ana");

        assertThat(book.partyOf(TADEU.id())).hasValueSatisfying(party ->
            assertThat(party.members()).extracting(PartySnapshot.Member::name).containsExactly("ana", "tadeu", "tadeudobw"));
    }

    @Test
    void changedPartiesAreReportedForPublishing() {
        PartyOutcome invited = book.invite(TADEU, DOBW, clock.instant());
        PartyOutcome accepted = book.accept(DOBW, "tadeu", clock.instant());

        assertThat(invited.changed()).hasSize(1);
        assertThat(accepted.changed()).isEqualTo(invited.changed());
    }

    private void partyOfThree() {
        book.invite(TADEU, DOBW, clock.instant());
        book.accept(DOBW, "tadeu", clock.instant());
        book.invite(TADEU, ANA, clock.instant());
        book.accept(ANA, "tadeu", clock.instant());
        List.of(TADEU, DOBW, ANA).forEach(player -> assertThat(book.partyOf(player.id())).isPresent());
    }
}
