package br.com.muxe.core.proxy.auth;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.PacketContext;
import br.com.muxe.core.common.network.auth.AuthStatusRequest;
import br.com.muxe.core.common.network.auth.AuthStatusResponse;
import java.util.UUID;
import java.util.function.Predicate;

public final class AuthStatusResponder {

    private final AuthenticationGate gate;
    private final Predicate<UUID> connectedHere;

    public AuthStatusResponder(AuthenticationGate gate, Predicate<UUID> connectedHere) {
        this.gate = gate;
        this.connectedHere = connectedHere;
    }

    public void register(PacketBus bus) {
        bus.subscribe(AuthStatusRequest.TYPE, this::answer);
    }

    private void answer(PacketContext<AuthStatusRequest> context) {
        UUID playerId = context.packet().playerId();
        if (connectedHere.test(playerId)) {
            context.reply(new AuthStatusResponse(playerId, gate.isAuthenticated(playerId)));
        }
    }
}
