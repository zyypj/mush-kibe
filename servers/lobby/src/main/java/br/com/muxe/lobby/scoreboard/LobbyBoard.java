package br.com.muxe.lobby.scoreboard;

import br.com.muxe.core.bukkit.scoreboard.SidebarContent;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.lobby.profile.PlayerInfoSource;
import java.util.UUID;
import java.util.function.Supplier;

public final class LobbyBoard {

    private final Supplier<Localized<ScoreboardTemplate>> templates;
    private final LanguageLookup languages;
    private final PlayerInfoSource info;

    public LobbyBoard(Supplier<Localized<ScoreboardTemplate>> templates, LanguageLookup languages, PlayerInfoSource info) {
        this.templates = templates;
        this.languages = languages;
        this.info = info;
    }

    public SidebarContent content(UUID player, String lobby, int players) {
        ScoreboardTemplate template = templates.get().in(languages.of(player));
        return new SidebarContent(template.title(), template.render(info.of(player), lobby, players));
    }
}
