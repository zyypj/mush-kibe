package br.com.muxe.lobby.selector;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.menu.PaginatedMenu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.List;
import org.bukkit.entity.Player;

public final class LobbySelectorMenu {

    private final MuxeCore core;
    private final SelectorSettings settings;
    private final MessageCatalog messages;

    public LobbySelectorMenu(MuxeCore core, SelectorSettings settings, MessageCatalog messages) {
        this.core = core;
        this.settings = settings;
        this.messages = messages;
    }

    public void open(Player player) {
        menu().open(player, 1);
    }

    public PaginatedMenu menu() {
        List<LobbyEntry> entries = LobbyEntries.from(core.servers().aliveInGroup(settings.group()), core.identity().id());
        List<MenuButton> buttons = entries.stream().map(this::button).toList();
        return PaginatedMenu.builder(settings.titleTemplate())
            .items(buttons)
            .previous(settings.previousPage())
            .next(settings.nextPage())
            .build();
    }

    private MenuButton button(LobbyEntry entry) {
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
            player.sendMessage(messages.get("lobby-current"));
            return;
        }
        if (!entry.hasRoom()) {
            player.sendMessage(messages.get("lobby-full"));
            return;
        }
        context.close();
        player.sendMessage(messages.get("lobby-connecting", "number", entry.label()));
        core.network().connectToServer(player.getUniqueId(), entry.serverId()).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get("lobby-failed")));
            return null;
        });
    }
}
