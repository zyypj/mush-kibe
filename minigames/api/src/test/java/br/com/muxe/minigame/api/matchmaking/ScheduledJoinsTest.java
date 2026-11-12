package br.com.muxe.minigame.api.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.scheduledgame.ScheduledGame;
import br.com.muxe.core.bukkit.scheduledgame.ScheduledGames;
import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.network.RoomInfo;
import br.com.muxe.minigame.api.network.RoomsPacket;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ScheduledJoinsTest {

    private final MutableClock clock = MutableClock.startingAt("2026-11-12T12:00:00Z");

    @Test
    void onlyARoomOfTheScheduledModeWithSeatsLeftKeepsItJoinable() throws InvalidConfigurationException {
        ScheduledGames games = games();

        assertThat(ScheduledJoins.waiting(games, "bedwars", List.of(room("mega", true, 10, 80)))).containsExactly("mega");
        assertThat(ScheduledJoins.waiting(games, "bedwars", List.of(room("mega", true, 80, 80)))).isEmpty();
        assertThat(ScheduledJoins.waiting(games, "bedwars", List.of(room("mega", false, 10, 80)))).isEmpty();
        assertThat(ScheduledJoins.waiting(games, "bedwars", List.of(room("solo", true, 1, 8)))).isEmpty();
        assertThat(ScheduledJoins.waiting(games, "skywars", List.of(room("mega", true, 10, 80)))).isEmpty();
    }

    @Test
    void aServerWithAWaitingRoomOfTheModeIsFound() {
        List<RoomsPacket> servers = List.of(
            new RoomsPacket("bedwars-1", List.of(room("solo", true, 1, 8)), List.of()),
            new RoomsPacket("bedwars-2", List.of(room("mega", true, 30, 80)), List.of()));

        assertThat(ScheduledJoins.hasRoom(servers, "mega")).isTrue();
        assertThat(ScheduledJoins.hasRoom(servers.subList(0, 1), "mega")).isFalse();
    }

    private ScheduledGames games() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
            mega:
              game: bedwars
              mode: mega
              weekdays:
                - "19:00"
            """);
        return new ScheduledGames(ScheduledGame.from(config), clock, new InMemoryExpiringStore(clock));
    }

    private static RoomInfo room(String mode, boolean open, int players, int max) {
        return new RoomInfo("abc", mode, "mapa", open, players, max);
    }
}
