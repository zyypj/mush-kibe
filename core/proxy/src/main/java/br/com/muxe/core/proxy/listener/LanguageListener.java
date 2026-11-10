package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.proxy.language.ProxyLanguages;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;

public final class LanguageListener {

    private final ProxyLanguages languages;

    public LanguageListener(ProxyLanguages languages) {
        this.languages = languages;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        languages.forget(event.getPlayer().getUniqueId());
    }
}
