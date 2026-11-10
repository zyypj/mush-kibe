package br.com.muxe.core.proxy.tablist;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public final class TabListListener {

    private final Localized<Component> headers;
    private final Localized<Component> footers;
    private final LanguageLookup languages;

    public TabListListener(TabListSettings settings) {
        this(Localized.same(settings), LanguageLookup.DEFAULT);
    }

    public TabListListener(Localized<TabListSettings> settings, LanguageLookup languages) {
        this.headers = settings.map(TabListSettings::headerComponent);
        this.footers = settings.map(TabListSettings::footerComponent);
        this.languages = languages;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        show(event.getPlayer());
    }

    public void show(Player player) {
        Language language = languages.of(player.getUniqueId());
        player.sendPlayerListHeaderAndFooter(headers.in(language), footers.in(language));
    }
}
