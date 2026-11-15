package br.com.muxe.minigame.bedwars.room;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.bedwars.game.XpRewards;
import br.com.muxe.minigame.bedwars.mission.MissionRules;
import br.com.muxe.minigame.bedwars.stats.domain.MatchOutcome;
import br.com.muxe.minigame.bedwars.stats.domain.MatchResult;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomStatsTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private static final List<TeamColor> RED_BLUE = List.of(TeamColor.VERMELHO, TeamColor.AZUL);

    @Test
    void dyingWithABedIsADeathAndWithoutItAFinalDeath() {
        Room room = started();
        Match match = room.match();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);

        match.die(red, 5);
        room.breakBed(TeamColor.VERMELHO, blue);
        match.respawned(red, NOW);
        match.die(red, 5);

        assertThat(red.deaths()).isEqualTo(1);
        assertThat(red.finalDeaths()).isEqualTo(1);
        assertThat(room.player(red.id()).bedsLost()).isEqualTo(1);
        assertThat(room.player(blue.id()).bedsLost()).isZero();
        assertThat(room.player(blue.id()).beds()).isEqualTo(1);
    }

    @Test
    void killsAndBedsAddTheirExperience() {
        Room room = started();
        Participant blue = player(room, TeamColor.AZUL);

        blue.addXp(12);
        blue.addXp(100);

        assertThat(blue.xp()).isEqualTo(112);
    }

    @Test
    void theEndOfTheMatchTurnsEveryPlayerIntoAnOutcome() {
        Room room = started();
        Match match = room.match();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);
        room.breakBed(TeamColor.VERMELHO, blue);
        blue.addXp(100);
        match.die(red, 5);
        match.credit(blue, true);
        blue.addXp(60);
        for (int second = 0; second < 125; second++) {
            match.tick();
        }

        List<MatchOutcome> outcomes = room.outcomes(Optional.of(TeamColor.AZUL), new XpRewards(10, 5, 250, 100, 12, 60));

        assertThat(outcomes).containsExactlyInAnyOrder(
            new MatchOutcome(blue.id(), "1v1", 1, 0, 0, 0, 0, 1, 0, 0, MatchResult.WIN, 160 + 20 + 250),
            new MatchOutcome(red.id(), "1v1", 0, 1, 0, 0, 0, 0, 1, 0, MatchResult.LOSS, 20));
        assertThat(room.outcomes(Optional.empty(), new XpRewards(10, 5, 250, 100, 12, 60))).extracting(MatchOutcome::result).containsOnly(MatchResult.NONE);
    }

    @Test
    void missionTotalsFollowTheMatch() {
        Room room = started();
        Match match = room.match();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);
        match.credit(red, false);
        match.credit(red, false);
        room.died(red);
        match.die(red, 5);
        match.credit(red, true);
        room.player(red.id()).addEmeralds(3);
        room.player(red.id()).addFireballDamage(2.5);
        room.player(red.id()).addClay(16);
        for (int second = 0; second < 200; second++) {
            match.tick();
        }

        Map<String, Long> winning = room.missionTotals(red, Optional.of(TeamColor.VERMELHO), MissionRules.DEFAULT);
        room.player(blue.id()).useExplosive();
        Map<String, Long> losing = room.missionTotals(blue, Optional.of(TeamColor.VERMELHO), MissionRules.DEFAULT);

        assertThat(winning).containsEntry("KILL_STREAK", 2L).containsEntry("FINAL_KILLS", 1L).containsEntry("EMERALDS_IN_A_LIFE", 3L)
            .containsEntry("FIREBALL_DAMAGE", 2L).containsEntry("CLAY_BOUGHT", 16L).containsEntry("FAST_WIN", 1L).containsEntry("CLEAN_WIN", 1L);
        assertThat(losing).containsEntry("FAST_WIN", 0L).containsEntry("CLEAN_WIN", 0L);
        assertThat(room.missionTotals(red, Optional.empty(), MissionRules.DEFAULT)).containsEntry("FAST_WIN", 0L);
    }

    @Test
    void theNewMissionTotalsCountEggsDiamondsBowDamageKillsAndMilk() {
        Room room = started();
        Match match = room.match();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);
        match.credit(red, false);
        match.credit(red, false);
        match.credit(red, true);
        RoomPlayer stats = room.player(red.id());
        stats.addBridgeEgg();
        stats.addBridgeEgg();
        stats.addDiamonds(4);
        stats.addDiamonds(2);
        stats.addBowDamage(3.5);
        stats.addBowDamage(4);

        Map<String, Long> before = room.missionTotals(red, Optional.empty(), MissionRules.DEFAULT);
        stats.cleanseWithMilk();
        Map<String, Long> after = room.missionTotals(red, Optional.empty(), MissionRules.DEFAULT);

        assertThat(before).containsEntry("BRIDGE_EGGS", 2L).containsEntry("DIAMONDS", 6L).containsEntry("BOW_DAMAGE", 7L).containsEntry("KILLS", 3L)
            .containsEntry("MILK_CLEANSE", 0L);
        assertThat(after).containsEntry("MILK_CLEANSE", 1L);
        assertThat(room.missionTotals(blue, Optional.empty(), MissionRules.DEFAULT)).containsEntry("KILLS", 0L).containsEntry("DIAMONDS", 0L);
    }

    @Test
    void winningWithAtMostSixteenBlocksBoughtCountsOnlyForTheWinner() {
        Room room = started();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);
        room.player(red.id()).addBlocks(16);
        room.player(blue.id()).addBlocks(4);

        assertThat(room.missionTotals(red, Optional.of(TeamColor.VERMELHO), MissionRules.DEFAULT)).containsEntry("FEW_BLOCKS_WIN", 1L);
        assertThat(room.missionTotals(red, Optional.empty(), MissionRules.DEFAULT)).containsEntry("FEW_BLOCKS_WIN", 0L);
        assertThat(room.missionTotals(blue, Optional.of(TeamColor.VERMELHO), MissionRules.DEFAULT)).containsEntry("FEW_BLOCKS_WIN", 0L);
        room.player(red.id()).addBlocks(1);
        assertThat(room.missionTotals(red, Optional.of(TeamColor.VERMELHO), MissionRules.DEFAULT)).containsEntry("FEW_BLOCKS_WIN", 0L);
    }

    @Test
    void sharpnessTwoCountsForTheTeamThatReachedIt() {
        Room room = started();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);
        room.team(TeamColor.VERMELHO).upgrade(RoomTeam.Upgrade.SHARPNESS, 1);
        room.team(TeamColor.AZUL).upgrade(RoomTeam.Upgrade.SHARPNESS, 1);

        assertThat(room.missionTotals(red, Optional.empty(), MissionRules.DEFAULT)).containsEntry("SHARPNESS_TWO", 0L);
        room.team(TeamColor.VERMELHO).upgrade(RoomTeam.Upgrade.SHARPNESS, 2);
        assertThat(room.missionTotals(red, Optional.empty(), MissionRules.DEFAULT)).containsEntry("SHARPNESS_TWO", 1L);
        assertThat(room.missionTotals(blue, Optional.empty(), MissionRules.DEFAULT)).containsEntry("SHARPNESS_TWO", 0L);
    }

    @Test
    void emeraldsInALifeStartOverAfterADeath() {
        Room room = started();
        Participant red = player(room, TeamColor.VERMELHO);
        room.player(red.id()).addEmeralds(4);

        room.died(red);
        room.player(red.id()).addEmeralds(2);

        assertThat(room.player(red.id()).bestEmeralds()).isEqualTo(4);
        room.player(red.id()).addEmeralds(3);
        assertThat(room.player(red.id()).bestEmeralds()).isEqualTo(5);
    }

    @Test
    void playersOfAPartySplitAcrossTeamsAreNotRecorded() {
        Room room = started();
        Participant red = player(room, TeamColor.VERMELHO);
        Participant blue = player(room, TeamColor.AZUL);

        room.match().unrank(List.of(red.id()));

        assertThat(room.outcomes(Optional.of(TeamColor.VERMELHO), new XpRewards(10, 5, 250, 100, 12, 60))).extracting(MatchOutcome::playerId).containsExactly(blue.id());
    }

    private static Room started() {
        Room room = RoomFixtures.room("2v7bss", RoomFixtures.ONE_V_ONE, RoomFixtures.arena("1v1", "Slip", RED_BLUE));
        room.match().join(UUID.randomUUID(), "tadeu");
        room.match().join(UUID.randomUUID(), "x572");
        room.match().start(List.of(), NOW);
        return room;
    }

    private static Participant player(Room room, TeamColor team) {
        return room.match().players().stream().filter(player -> player.team() == team).findFirst().orElseThrow();
    }
}
