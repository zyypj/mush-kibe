package br.com.muxe.minigame.bedwars.teamcolor.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.bedwars.teamcolor.TeamColorStore;
import br.com.muxe.minigame.bedwars.teamcolor.menu.FavouriteColorMenuSettings.Option;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class FavouriteColorMenu {

    private final Localized<FavouriteColorMenuSettings> settings;
    private final TeamColorStore store;
    private final MessageCatalog messages;
    private final BiConsumer<Player, Menu> opener;

    public FavouriteColorMenu(Localized<FavouriteColorMenuSettings> settings, TeamColorStore store, MessageCatalog messages) {
        this(settings, store, messages, (player, menu) -> menu.open(player));
    }

    FavouriteColorMenu(FavouriteColorMenuSettings settings, TeamColorStore store, MessageCatalog messages, BiConsumer<Player, Menu> opener) {
        this(Localized.same(settings), store, messages, opener);
    }

    FavouriteColorMenu(Localized<FavouriteColorMenuSettings> settings, TeamColorStore store, MessageCatalog messages, BiConsumer<Player, Menu> opener) {
        this.settings = settings;
        this.store = store;
        this.messages = messages;
        this.opener = opener;
    }

    public void open(Player player, Consumer<Player> back) {
        opener.accept(player, build(player.getUniqueId(), back));
    }

    Menu build(UUID id, Consumer<Player> back) {
        FavouriteColorMenuSettings settings = this.settings.in(messages.language(id));
        Optional<TeamColor> current = store.of(id);
        Menu.Builder menu = Menu.builder(settings.title()).rows(settings.rows());
        menu.button(settings.none().slot(), icon(settings, settings.none().item(), current.isEmpty()), context -> choose(context.player(), Optional.empty(), settings.none().item().name(), back));
        for (Map.Entry<TeamColor, Option> entry : settings.colors().entrySet()) {
            TeamColor color = entry.getKey();
            Option option = entry.getValue();
            ItemBuilder wool = ItemBuilder.of(Material.WOOL, color.woolData()).name(option.name());
            menu.button(option.slot(), icon(settings, wool, current.filter(color::equals).isPresent()), context -> choose(context.player(), Optional.of(color), wool.name(), back));
        }
        menu.button(settings.back().slot(), settings.back().item(), context -> back.accept(context.player()));
        return menu.build();
    }

    private static ItemBuilder icon(FavouriteColorMenuSettings settings, ItemBuilder base, boolean selected) {
        List<String> lore = selected ? settings.chosen() : settings.choose();
        return base.lore(lore).glow(selected).showFlags();
    }

    private void choose(Player player, Optional<TeamColor> color, String name, Consumer<Player> back) {
        UUID id = player.getUniqueId();
        if (store.of(id).equals(color)) {
            return;
        }
        store.change(id, color);
        player.sendMessage(messages.get(player, "cosmetic-selected", "name", name));
        open(player, back);
    }
}
