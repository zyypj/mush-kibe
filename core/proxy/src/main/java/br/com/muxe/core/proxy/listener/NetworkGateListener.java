package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.proxy.routing.NetworkGate;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;

public final class NetworkGateListener {

    private final NetworkGate gate;
    private final ProxyMessages messages;

    public NetworkGateListener(NetworkGate gate, ProxyMessages messages) {
        this.gate = gate;
        this.messages = messages;
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onLogin(LoginEvent event) {
        if (!gate.acceptsLogins()) {
            event.setResult(ResultedEvent.ComponentResult.denied(messages.get("no-auth-server")));
        }
    }
}
