package br.com.muxe.core.common.network;

import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.network.auth.AuthStatusRequest;
import br.com.muxe.core.common.network.auth.AuthStatusResponse;
import br.com.muxe.core.common.network.auth.PlayerAuthenticatedPacket;
import br.com.muxe.core.common.network.permission.PermissionsChangedPacket;
import br.com.muxe.core.common.network.placement.PlacementsChangedPacket;
import br.com.muxe.core.common.network.player.ConnectPlayerPacket;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.common.network.server.ServerShutdownPacket;
import br.com.muxe.core.common.network.server.ServerStatusPacket;
import br.com.muxe.core.common.network.shop.PromotionChangedPacket;

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
            .register(PlayerAuthenticatedPacket.TYPE)
            .register(AuthStatusRequest.TYPE)
            .register(AuthStatusResponse.TYPE)
            .register(PlacementsChangedPacket.TYPE)
            .register(PromotionChangedPacket.TYPE)
            .register(PermissionsChangedPacket.TYPE);
    }
}
