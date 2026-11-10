package br.com.muxe.minigame.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.TestArena;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchRuntimeLanguageTest {

    private static final GameMode ONE_V_ONE = new GameMode("1v1", "1v1", 2, 2, 1);
    private static final MatchRules RESPAWNING = MatchRules.defaults("mxt").respawning();

    private final MutableClock clock = MutableClock.startingAt("2026-11-07T20:00:00Z");
    private final FakeStage stage = new FakeStage();
    private final List<MatchRuntime<RecordingState>> closed = new ArrayList<>();
    private final UUID red = UUID.nameUUIDFromBytes("red".getBytes());
    private final UUID blue = UUID.nameUUIDFromBytes("blue".getBytes());

    @Test
    void aJoinIsAnnouncedToEachReaderInTheirOwnLanguage() {
        stage.languages.put(blue, Language.ENGLISH);
        MatchRuntime<RecordingState> runtime = runtime(RESPAWNING);

        join(runtime, red, "red");
        join(runtime, blue, "blue");

        assertThat(stage.messagesOf(red)).containsExactly("red entrou (1/2)", "blue entrou (2/2)", "inicia em 10 segundos");
        assertThat(stage.messagesOf(blue)).containsExactly("blue joined (2/2)", "starts in 10 seconds");
    }

    @Test
    void aBroadcastTextIsRenderedForEveryReader() {
        stage.languages.put(blue, Language.ENGLISH);
        MatchRuntime<RecordingState> runtime = runtime(RESPAWNING);
        join(runtime, red, "red");
        join(runtime, blue, "blue");
        stage.messages.clear();

        runtime.broadcast(Text.of("Olá", "Hello"));
        runtime.announce("team-eliminated", "team", TeamColor.VERMELHO.label());

        assertThat(stage.messagesOf(red)).containsExactly("Olá", "Vermelho eliminado");
        assertThat(stage.messagesOf(blue)).containsExactly("Hello", "Red eliminated");
    }

    @Test
    void theCountdownTitleFollowsTheReader() {
        stage.languages.put(blue, Language.ENGLISH);
        MatchRuntime<RecordingState> runtime = runtime(RESPAWNING);

        join(runtime, red, "red");
        join(runtime, blue, "blue");

        assertThat(stage.lastTitle(red).orElseThrow().title()).isEqualTo("§610 segundos");
        assertThat(stage.lastTitle(blue).orElseThrow().title()).isEqualTo("§610 seconds");
    }

    @Test
    void deathTitlesAndDeathMessagesAreInTheLanguageOfWhoReadsThem() {
        stage.languages.put(blue, Language.ENGLISH);
        MatchRuntime<RecordingState> runtime = started(RESPAWNING);
        stage.messages.clear();

        runtime.die(blue, false);

        assertThat(stage.lastTitle(blue).orElseThrow().title()).isEqualTo("YOU DIED");
        assertThat(stage.lastTitle(blue).orElseThrow().subtitle()).isEqualTo("respawning in 5 seconds");
        assertThat(stage.messagesOf(blue)).contains("§9blue died");
        assertThat(stage.messagesOf(red)).contains("§9blue morreu");

        runtime.die(red, false);

        assertThat(stage.lastTitle(red).orElseThrow().title()).isEqualTo("MORREU");
        assertThat(stage.lastTitle(red).orElseThrow().subtitle()).isEqualTo("renasce em 5 segundos");
    }

    @Test
    void eachBoardIsRenderedInItsViewersLanguage() {
        stage.languages.put(blue, Language.ENGLISH);
        MatchRuntime<RecordingState> runtime = runtime(RESPAWNING);
        join(runtime, red, "red");

        join(runtime, blue, "blue");

        assertThat(stage.boardTitles).containsEntry(red, "§b§lJOGO").containsEntry(blue, "§b§lGAME");
        assertThat(stage.boards.get(blue)).containsExactly("Starts in: 10s");
        assertThat(stage.boards.get(red)).containsExactly("Inicia em: 10s");

        seconds(runtime, 10);

        assertThat(stage.boards.get(blue)).contains("Points: 7", "§9A §fBlue: §a✔ §7YOU", "§cV §fRed: §a✔");
        assertThat(stage.boards.get(red)).contains("Pontos: 7", "§cV §fVermelho: §a✔ §7VOCÊ");
    }

    @Test
    void theResultIsCenteredInEveryReadersLanguage() {
        stage.languages.put(blue, Language.ENGLISH);
        MatchRuntime<RecordingState> runtime = started(RESPAWNING);
        Match match = runtime.match();
        TeamColor redTeam = match.player(red).orElseThrow().team();
        match.credit().hit(red, blue, clock.instant());
        match.team(redTeam).stopRespawning(blue);

        runtime.die(red, false);

        assertThat(stage.centered.get(red)).containsExactly("Vencedor: Azul", "1. blue 1");
        assertThat(stage.centered.get(blue)).containsExactly("Winner: Blue", "1st blue 1");
        assertThat(stage.messagesOf(blue)).contains("§cred was killed by §9blue", "Red eliminated");
    }

    @Test
    void switchingLanguageRedrawsTheBoardAndTheHotbar() {
        MatchRuntime<RecordingState> runtime = runtime(RESPAWNING);
        join(runtime, red, "red");
        assertThat(stage.boards.get(red)).containsExactly("abc123", "Jogadores: 1/2");

        stage.languages.put(red, Language.ENGLISH);
        runtime.refreshLanguage(red);

        assertThat(stage.boards.get(red)).containsExactly("abc123", "Players: 1/2");
        assertThat(stage.items.get(red)).extracting("name").containsExactly("&aMenu", "&cLeave");
    }

    private MatchRuntime<RecordingState> runtime(MatchRules rules) {
        Match match = new Match("abc123", ONE_V_ONE, TestArena.of("1v1", TestArena.RED_BLUE), new Cell(0, 0, 0, 64), rules.respawn());
        return new MatchRuntime<>(match, stage, RuntimeFixtures.localizedEnvironment(clock, rules), closed::add, opened -> new RecordingState());
    }

    private MatchRuntime<RecordingState> started(MatchRules rules) {
        MatchRuntime<RecordingState> runtime = runtime(rules);
        join(runtime, red, "red");
        join(runtime, blue, "blue");
        seconds(runtime, 10);
        return runtime;
    }

    private void join(MatchRuntime<RecordingState> runtime, UUID id, String name) {
        stage.present.add(id);
        runtime.join(id, name);
    }

    private void seconds(MatchRuntime<RecordingState> runtime, int count) {
        for (int second = 0; second < count; second++) {
            runtime.second();
        }
    }
}
