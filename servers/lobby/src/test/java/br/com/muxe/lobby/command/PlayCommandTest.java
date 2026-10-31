package br.com.muxe.lobby.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import br.com.muxe.core.common.testing.TestNetwork;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayCommandTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private static final UUID PLAYER_ID = UUID.fromString("595a25f1-eb36-4999-baa8-9c73efc8fdb7");

    private final TestNetwork network = new TestNetwork();
    private final ServerDirectory servers = new ServerDirectory(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30));
    private final List<ConnectPlayerPacket> proxyReceived = new CopyOnWriteArrayList<>();
    private final List<String> chat = new CopyOnWriteArrayList<>();
    private PlayCommand command;

    @BeforeEach
    void setUp() {
        TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> proxyReceived.add(context.packet()));
        proxy.start();
        NetworkGateway gateway = new NetworkGateway(network.node("lobby-1", "lobby", ServerKind.LOBBY).start().bus());
        MuxeCore core = (MuxeCore) Proxy.newProxyInstance(MuxeCore.class.getClassLoader(), new Class<?>[] {MuxeCore.class}, (proxyCore, method, args) -> switch (method.getName()) {
            case "servers" -> servers;
            case "network" -> gateway;
            case "mainThread" -> (Executor) Runnable::run;
            default -> null;
        });
        command = new PlayCommand(core, MessageCatalog.of(Map.of("play-failed", "failed")), List.of("skywars", "bedwars"));
    }

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void playingAGameWithALobbySendsThePlayerToTheLobbyWithoutAMessage() {
        online("bedwars-1", "bedwars", ServerKind.MINIGAME);
        online("bedwars-lobby-1", "bedwars-lobby", ServerKind.LOBBY);

        command.play(player(), "BedWars");

        assertThat(proxyReceived).containsExactly(ConnectPlayerPacket.toGroup(PLAYER_ID, "bedwars-lobby"));
        assertThat(chat).isEmpty();
    }

    @Test
    void playingAGameWithoutALobbySendsThePlayerStraightToIt() {
        online("skywars-1", "skywars", ServerKind.MINIGAME);

        command.play(player(), "skywars");

        assertThat(proxyReceived).containsExactly(ConnectPlayerPacket.toGroup(PLAYER_ID, "skywars"));
    }

    private void online(String id, String group, ServerKind kind) {
        servers.update(new ServerStatus(new ServerIdentity(id, group, kind), 1, 100, true, NOW, NOW));
    }

    private Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> PLAYER_ID;
            case "sendMessage" -> {
                chat.add((String) args[0]);
                yield null;
            }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> method.getReturnType() == boolean.class ? false : null;
        });
    }
}
