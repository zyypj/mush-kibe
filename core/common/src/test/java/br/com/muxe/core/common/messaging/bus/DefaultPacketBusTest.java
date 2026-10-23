package br.com.muxe.core.common.messaging.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.IdempotentPacket;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.messaging.request.RequestTimeoutException;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.RecordingTransport;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DefaultPacketBusTest {

    private final TestNetwork network = new TestNetwork();
    private final TestNetwork.Node lobby = network.node("lobby-1", "lobby", ServerKind.LOBBY);
    private final TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void deliversPacketsToTheTargetedServer() {
        List<ConnectPlayerPacket> received = new CopyOnWriteArrayList<>();
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> received.add(context.packet()));
        start();

        ConnectPlayerPacket packet = ConnectPlayerPacket.toGroup(UUID.randomUUID(), "skywars");
        lobby.bus().publish(PacketTarget.proxy(), packet).join();

        assertThat(received).containsExactly(packet);
        assertThat(lobby.counter("muxe_packets_published_total")).isEqualTo(1);
        assertThat(proxy.counter("muxe_packets_received_total")).isEqualTo(1);
    }

    @Test
    void ignoresItsOwnBroadcasts() {
        List<ConnectPlayerPacket> received = new CopyOnWriteArrayList<>();
        lobby.bus().subscribe(ConnectPlayerPacket.TYPE, context -> received.add(context.packet()));
        start();

        lobby.bus().publish(PacketTarget.broadcast(), ConnectPlayerPacket.toGroup(UUID.randomUUID(), "lobby")).join();

        assertThat(received).isEmpty();
    }

    @Test
    void dropsReplayedPackets() {
        List<ConnectPlayerPacket> received = new CopyOnWriteArrayList<>();
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> received.add(context.packet()));
        start();

        lobby.bus().publish(PacketTarget.proxy(), ConnectPlayerPacket.toGroup(UUID.randomUUID(), "lobby")).join();
        RecordingTransport.Published original = lobby.transport().last();
        network.broker().publish(original.channel(), original.message());

        assertThat(received).hasSize(1);
        assertThat(proxy.counter("muxe_packets_duplicates_total")).isEqualTo(1);
    }

    @Test
    void rejectsExpiredPackets() {
        List<ConnectPlayerPacket> received = new CopyOnWriteArrayList<>();
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> received.add(context.packet()));
        start();

        lobby.bus().publish(PacketTarget.server("nobody"), ConnectPlayerPacket.toGroup(UUID.randomUUID(), "lobby")).join();
        RecordingTransport.Published stale = lobby.transport().last();
        network.clock().advance(Duration.ofMinutes(5));
        network.broker().publish("muxe:packets:proxy", stale.message());

        assertThat(received).isEmpty();
        assertThat(proxy.counter("muxe_packets_rejected_total")).isEqualTo(1);
    }

    @Test
    void rejectsPacketsFromServersWithAnotherSecret() {
        TestNetwork.Node intruder = network.node(new ServerIdentity("evil-1", "evil", ServerKind.LOBBY), "a-completely-different-secret-value!!");
        List<ConnectPlayerPacket> received = new CopyOnWriteArrayList<>();
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> received.add(context.packet()));
        start();
        intruder.start();

        intruder.bus().publish(PacketTarget.proxy(), ConnectPlayerPacket.toGroup(UUID.randomUUID(), "lobby")).join();

        assertThat(received).isEmpty();
        assertThat(proxy.counter("muxe_packets_rejected_total")).isEqualTo(1);
    }

    @Test
    void answersRequests() throws Exception {
        proxy.bus().subscribe(PlayerLocateRequest.TYPE, context ->
            context.reply(new PlayerLocateResponse(true, context.packet().username(), "skywars-2")));
        start();

        PlayerLocateResponse response = lobby.bus()
            .request(PacketTarget.proxy(), new PlayerLocateRequest("Steve"), PlayerLocateResponse.class, Duration.ofSeconds(2))
            .get(2, TimeUnit.SECONDS);

        assertThat(response).isEqualTo(new PlayerLocateResponse(true, "Steve", "skywars-2"));
        assertThat(lobby.metrics().snapshot().counterTotal("muxe_requests_total")).isEqualTo(1);
    }

    @Test
    void failsRequestsThatNobodyAnswers() {
        start();

        CompletableFuture<PlayerLocateResponse> response = lobby.bus()
            .request(PacketTarget.proxy(), new PlayerLocateRequest("Steve"), PlayerLocateResponse.class, Duration.ofMillis(50));

        assertThatThrownBy(() -> response.get(2, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RequestTimeoutException.class);
    }

    @Test
    void keepsDispatchingWhenAHandlerFails() {
        List<ConnectPlayerPacket> received = new CopyOnWriteArrayList<>();
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> {
            throw new IllegalStateException("boom");
        });
        proxy.bus().subscribe(ConnectPlayerPacket.TYPE, context -> received.add(context.packet()));
        start();

        lobby.bus().publish(PacketTarget.proxy(), ConnectPlayerPacket.toGroup(UUID.randomUUID(), "lobby")).join();

        assertThat(received).hasSize(1);
        assertThat(proxy.counter("muxe_packets_handler_failures_total")).isEqualTo(1);
    }

    @Test
    void deliversGroupPacketsOnlyToThatGroup() {
        TestNetwork.Node skywars = network.node("skywars-1", "skywars", ServerKind.MINIGAME);
        List<String> receivers = new CopyOnWriteArrayList<>();
        skywars.bus().subscribe(ConnectPlayerPacket.TYPE, context -> receivers.add("skywars-1"));
        lobby.bus().subscribe(ConnectPlayerPacket.TYPE, context -> receivers.add("lobby-1"));
        start();
        skywars.start();

        proxy.bus().publish(PacketTarget.group("skywars"), ConnectPlayerPacket.toGroup(UUID.randomUUID(), "x")).join();

        assertThat(receivers).containsExactly("skywars-1");
    }

    @Test
    void exclusivePacketsAreHandledByASingleReceiver() {
        TestNetwork.Node otherLobby = network.node("lobby-2", "lobby", ServerKind.LOBBY);
        List<String> handlers = new CopyOnWriteArrayList<>();
        lobby.bus().subscribe(RewardPacket.TYPE, context -> handlers.add("lobby-1"));
        otherLobby.bus().subscribe(RewardPacket.TYPE, context -> handlers.add("lobby-2"));
        proxy.registry().register(RewardPacket.TYPE);
        start();
        otherLobby.start();

        proxy.bus().publish(PacketTarget.group("lobby"), new RewardPacket("purchase-42")).join();
        proxy.bus().publish(PacketTarget.group("lobby"), new RewardPacket("purchase-42")).join();

        assertThat(handlers).hasSize(1);
    }

    @Test
    void failsToPublishUnregisteredPackets() {
        start();

        assertThatThrownBy(() -> lobby.bus().publish(PacketTarget.proxy(), new Unregistered()).join())
            .hasMessageContaining("not registered");
    }

    private void start() {
        lobby.start();
        proxy.start();
    }

    record RewardPacket(String purchaseId) implements IdempotentPacket {

        static final PacketType<RewardPacket> TYPE = PacketType.of(
            "test.reward",
            RewardPacket.class,
            PacketCodec.of((packet, out) -> out.writeString(packet.purchaseId()), in -> new RewardPacket(in.readString())),
            DeliveryPolicy.exclusive(Duration.ofMinutes(10))
        );

        @Override
        public String idempotencyKey() {
            return purchaseId;
        }
    }

    record Unregistered() implements Packet {
    }
}
