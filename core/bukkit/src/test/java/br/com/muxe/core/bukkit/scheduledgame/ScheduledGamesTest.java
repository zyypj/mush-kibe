package br.com.muxe.core.bukkit.scheduledgame;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.menu.button.MenuFixtures;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduledGamesTest {

    private static final String THURSDAY_BEFORE = "2026-11-12T21:59:00Z";

    private final MutableClock clock = MutableClock.startingAt(THURSDAY_BEFORE);
    private final InMemoryExpiringStore store = new InMemoryExpiringStore(clock);
    private final Map<String, ScheduledGame> configured = ScheduledGame.from(DefaultedSections.of(ShopFixtures.CONFIG, "scheduled-games"));
    private final ScheduledGames games = new ScheduledGames(configured, clock, store);
    private final TestNetwork network = new TestNetwork();
    private final List<ConnectPlayerPacket> proxyReceived = new CopyOnWriteArrayList<>();
    private ScheduledPlay play;

    @BeforeEach
    void setUp() {
        TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> proxyReceived.add(context.packet()));
        proxy.start();
        play = new ScheduledPlay(games, new NetworkGateway(network.node("lobby-1", "lobby", ServerKind.LOBBY).start().bus()), Runnable::run, Runnable::run,
            MenuFixtures.MESSAGES);
    }

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void megaIsConfiguredForTheBedWarsLobbyAtTheAnnouncedTimes() {
        ScheduledGame mega = games.game("mega").orElseThrow();

        assertThat(mega.game()).isEqualTo("bedwars");
        assertThat(mega.mode()).isEqualTo("mega");
        assertThat(mega.command()).isEqualTo("megabw");
        assertThat(mega.lobbyGroup()).isEqualTo("bedwars-lobby");
        assertThat(mega.schedule().weekdays()).containsExactly(LocalTime.of(19, 0));
        assertThat(mega.schedule().weekends()).containsExactly(LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(19, 0));
        assertThat(mega.schedule().window()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void onlyAScheduledModeWaitsForItsTimeToOpenNewRooms() {
        assertThat(games.allowsNewRoom("bedwars", "solo")).isTrue();
        assertThat(games.allowsNewRoom("bedwars", "mega")).isFalse();

        clock.advance(Duration.ofMinutes(1));

        assertThat(games.allowsNewRoom("bedwars", "mega")).isTrue();
        assertThat(games.isOpen("mega")).isTrue();
    }

    @Test
    void theOpeningIsAnnouncedOnceAMinuteWhileItLasts() {
        assertThat(games.dueAnnouncements()).isEmpty();

        clock.advance(Duration.ofMinutes(1));
        assertThat(games.dueAnnouncements()).extracting(ScheduledGame::id).containsExactly("mega");
        clock.advance(Duration.ofSeconds(59));
        assertThat(games.dueAnnouncements()).isEmpty();
        clock.advance(Duration.ofSeconds(1));
        assertThat(games.dueAnnouncements()).extracting(ScheduledGame::id).containsExactly("mega");

        clock.advance(Duration.ofMinutes(15));
        assertThat(games.dueAnnouncements()).isEmpty();
    }

    @Test
    void outsideItsTimeAndWithoutRoomsNoRoomIsFound() {
        FakeSender ana = FakeSender.named("Ana");

        play.play(ana.player(), "mega");

        assertThat(ana.messages()).containsExactly("§cNenhuma sala foi encontrada.");
        assertThat(proxyReceived).isEmpty();
    }

    @Test
    void whileOpenThePlayerIsSentToTheGameLobbyRememberingToJoin() {
        clock.advance(Duration.ofMinutes(2));
        FakeSender ana = FakeSender.named("Ana");

        play.play(ana.player(), "mega");

        assertThat(proxyReceived).containsExactly(ConnectPlayerPacket.toGroup(ana.player().getUniqueId(), "bedwars-lobby"));
        assertThat(games.takeJoin(ana.player().getUniqueId())).contains("mega");
        assertThat(games.takeJoin(ana.player().getUniqueId())).isEmpty();
    }

    @Test
    void aRoomStillWaitingAfterTheTimeCanBeJoinedUntilItsMarkExpires() {
        FakeSender ana = FakeSender.named("Ana");
        games.roomsOpen("mega");

        play.play(ana.player(), "mega");
        assertThat(proxyReceived).hasSize(1);

        clock.advance(ScheduledGames.ROOMS_TIME_TO_LIVE);
        play.play(ana.player(), "mega");
        assertThat(ana.messages()).containsExactly("§cNenhuma sala foi encontrada.");
    }

    @Test
    void aServerOfTheGameJoinsOnTheSpot() {
        List<Player> joined = new ArrayList<>();
        games.playHere("mega", joined::add);
        Player ana = FakeSender.named("Ana").player();

        play.play(ana, "mega");

        assertThat(joined).extracting(Player::getName).containsExactly("Ana");
        assertThat(proxyReceived).isEmpty();
    }

    @Test
    void theAnnouncementRunsTheCommandWhenClicked() {
        ScheduledAnnouncer announcer = new ScheduledAnnouncer(games, List::of, MenuFixtures.MESSAGES);

        BaseComponent[] line = announcer.line(FakeSender.named("Ana").player(), games.game("mega").orElseThrow());

        assertThat(BaseComponent.toLegacyText(line)).contains("MEGA BED WARS").contains("/megabw");
        assertThat(line).allSatisfy(part -> {
            assertThat(part.getClickEvent().getAction()).isEqualTo(ClickEvent.Action.RUN_COMMAND);
            assertThat(part.getClickEvent().getValue()).isEqualTo("/megabw");
        });
    }
}
