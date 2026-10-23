package br.com.muxe.core.common.network;

import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.network.auth.PlayerAuthenticatedPacket;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.common.network.server.ServerShutdownPacket;
import br.com.muxe.core.common.network.server.ServerStatusPacket;

public final class NetworkPackets {

    private NetworkPackets() {
    }

    public static PacketRegistry registerAll(PacketRegistry registry) {
        return registry
            .register(ServerStatusPacket.TYPE)
            .register(ServerShutdownPacket.TYPE)
            .register(ConnectPlayerPacket.TYPE)
            .register(PlayerLocateRequest.TYPE)
            .register(PlayerLocateResponse.TYPE)
            .register(PlayerAuthenticatedPacket.TYPE);
    }
}
