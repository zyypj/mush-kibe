package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.proxy.language.ProxyLanguages;
import br.com.muxe.core.proxy.routing.NetworkGate;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;

public final class NetworkGateListener {

    private final NetworkGate gate;
    private final ProxyLanguages languages;
    private final ProxyMessages messages;

    public NetworkGateListener(NetworkGate gate, ProxyLanguages languages, ProxyMessages messages) {
        this.gate = gate;
        this.languages = languages;
        this.messages = messages;
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public EventTask onLogin(LoginEvent event) {
        if (gate.acceptsLogins()) {
            return null;
        }
        event.setResult(ResultedEvent.ComponentResult.denied(messages.get("no-auth-server")));
        return EventTask.resumeWhenComplete(languages.load(event.getPlayer().getUniqueId())
            .thenAccept(language -> event.setResult(ResultedEvent.ComponentResult.denied(messages.get(language, "no-auth-server")))));
    }
}
