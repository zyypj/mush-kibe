package br.com.muxe.core.bukkit.selector.lobby;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.menu.PaginatedMenu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.List;
import org.bukkit.entity.Player;

public final class LobbySelectorMenu {

    private final MuxeCore core;
    private final Localized<LobbySelectorSettings> settings;
    private final MessageCatalog messages;

    public LobbySelectorMenu(MuxeCore core, LobbySelectorSettings settings, MessageCatalog messages) {
        this(core, Localized.same(settings), messages);
    }

    public LobbySelectorMenu(MuxeCore core, Localized<LobbySelectorSettings> settings, MessageCatalog messages) {
        this.core = core;
        this.settings = settings;
        this.messages = messages;
    }

    public void open(Player player) {
        menu(messages.language(player)).open(player, 1);
    }

    public PaginatedMenu menu() {
        return menu(Language.DEFAULT);
    }

    public PaginatedMenu menu(Language language) {
        LobbySelectorSettings settings = this.settings.in(language);
        List<LobbyEntry> entries = LobbyEntries.from(core.servers().aliveInGroup(settings.group()), core.identity().id());
        List<MenuButton> buttons = entries.stream().map(entry -> button(settings, entry)).toList();
        return PaginatedMenu.builder(settings.titleTemplate())
            .items(buttons)
            .previous(settings.previousPage())
            .next(settings.nextPage())
            .build();
    }

    private MenuButton button(LobbySelectorSettings settings, LobbyEntry entry) {
        ItemBuilder template = entry.current() ? settings.current() : settings.other();
        ItemBuilder icon = template.replacing(
            "number", entry.label(),
            "online", Integer.toString(entry.online()),
            "max", Integer.toString(entry.max())
        );
        return MenuButton.of(icon, context -> choose(context, entry));
    }

    private void choose(ClickContext context, LobbyEntry entry) {
        Player player = context.player();
        if (entry.current()) {
            player.sendMessage(messages.get(player, "lobby-current"));
            return;
        }
        if (!entry.hasRoom()) {
            player.sendMessage(messages.get(player, "lobby-full"));
            return;
        }
        context.close();
        core.network().connectToServer(player.getUniqueId(), entry.serverId()).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get(player, "lobby-failed")));
            return null;
        });
    }
}
