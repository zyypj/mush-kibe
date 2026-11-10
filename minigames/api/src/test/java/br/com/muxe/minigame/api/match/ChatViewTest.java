package br.com.muxe.minigame.api.match;

import static br.com.muxe.minigame.api.match.MatchFixtures.DOUBLES;
import static br.com.muxe.minigame.api.match.MatchFixtures.ONE_V_ONE;
import static br.com.muxe.minigame.api.match.MatchFixtures.match;
import static br.com.muxe.minigame.api.match.TestArena.RED_BLUE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatViewTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");

    @Test
    void chatGoesToTheMatchAndShowsTheTeamOnlyDuringTheMatch() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        UUID red = UUID.randomUUID();
        UUID blue = UUID.randomUUID();
        match.join(red, "tadeu");
        match.join(blue, "x572");

        Map<UUID, ChatView> waiting = ChatView.of(match, Set.of(red, blue));
        match.start(List.of(List.of(red), List.of(blue)), NOW);
        Map<UUID, ChatView> playing = ChatView.of(match, Set.of(red));

        assertThat(waiting.get(red).prefix()).isEmpty();
        assertThat(waiting.get(red).audience(false)).containsExactlyInAnyOrder(red, blue);
        assertThat(playing.get(red).prefix()).isEqualTo("&c[V] ");
        assertThat(playing.get(blue).audience(false)).containsExactly(red);
        assertThat(playing.get(blue).teamOnly()).isFalse();
    }

    @Test
    void aRenamedTeamShowsTheFirstLetterOfItsNewName() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        UUID red = UUID.randomUUID();
        UUID blue = UUID.randomUUID();
        match.join(red, "tadeu");
        match.join(blue, "x572");
        match.teamNames(Map.of(TestArena.RED_BLUE.getFirst(), "Tesão"));
        match.start(List.of(List.of(red), List.of(blue)), NOW);

        String prefix = ChatView.of(match, Set.of(red, blue)).get(match.members(TestArena.RED_BLUE.getFirst()).getFirst().id()).prefix();

        assertThat(prefix).isEqualTo("&c[T] ");
    }

    @Test
    void inTeamModesChatGoesToTheTeamAndGlobalChatToEveryone() {
        Match match = match(DOUBLES, RED_BLUE, true);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();
        match.join(first, "tadeu");
        match.join(second, "x572");
        match.join(third, "lia");
        match.join(fourth, "rui");
        Set<UUID> online = Set.of(first, second, third, fourth);

        Map<UUID, ChatView> waiting = ChatView.of(match, online);
        match.start(List.of(List.of(first, second), List.of(third, fourth)), NOW);
        Map<UUID, ChatView> playing = ChatView.of(match, online);

        assertThat(waiting.get(first).audience(false)).containsExactlyInAnyOrderElementsOf(online);
        assertThat(playing.get(first).teamOnly()).isTrue();
        assertThat(playing.get(first).audience(false)).containsExactlyInAnyOrder(first, second);
        assertThat(playing.get(third).audience(false)).containsExactlyInAnyOrder(third, fourth);
        assertThat(playing.get(first).audience(true)).containsExactlyInAnyOrderElementsOf(online);
    }
}
