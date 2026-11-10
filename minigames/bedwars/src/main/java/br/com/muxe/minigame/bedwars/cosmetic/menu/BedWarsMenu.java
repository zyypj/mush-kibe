package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.cosmetic.menu.CosmeticMenus;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class BedWarsMenu {

    private final Localized<BedWarsMenuSettings> settings;
    private final CosmeticMenus cosmetics;
    private final MessageCatalog messages;
    private BiConsumer<Player, Consumer<Player>> quickBuy = (player, back) -> {
    };
    private BiConsumer<Player, Consumer<Player>> preferences = (player, back) -> {
    };
    private BiConsumer<Player, Consumer<Player>> hotbar = (player, back) -> {
    };
    private BiConsumer<Player, Optional<Consumer<Player>>> missions = (player, back) -> {
    };

    public BedWarsMenu(Localized<BedWarsMenuSettings> settings, CosmeticMenus cosmetics, MessageCatalog messages) {
        this.settings = settings;
        this.cosmetics = cosmetics;
        this.messages = messages;
    }

    public void onMissions(BiConsumer<Player, Optional<Consumer<Player>>> opener) {
        this.missions = opener;
    }

    public void onQuickBuy(BiConsumer<Player, Consumer<Player>> opener) {
        this.quickBuy = opener;
    }

    public void onPreferences(BiConsumer<Player, Consumer<Player>> opener) {
        this.preferences = opener;
    }

    public void onHotbar(BiConsumer<Player, Consumer<Player>> opener) {
        this.hotbar = opener;
    }

    public CosmeticMenus cosmetics() {
        return cosmetics;
    }

    public void open(Player player) {
        Language language = messages.language(player);
        BedWarsMenuSettings settings = this.settings.in(language);
        Menu.Builder menu = Menu.builder(settings.title()).rows(settings.rows());
        settings.items().forEach(menu::button);
        settings.missions().ifPresent(button -> menu.button(button.slot(), button.item(),
            context -> missions.accept(context.player(), Optional.of(this::open))));
        settings.quickBuy().ifPresent(button -> menu.button(button.slot(), button.item(),
            context -> quickBuy.accept(context.player(), this::open)));
        settings.preferences().ifPresent(button -> menu.button(button.slot(), button.item(),
            context -> preferences.accept(context.player(), this::open)));
        settings.hotbar().ifPresent(button -> menu.button(button.slot(), button.item(),
            context -> hotbar.accept(context.player(), this::open)));
        settings.soon().forEach((slot, item) -> menu.button(slot, item, context -> context.player().sendMessage(messages.get(language, "coming-soon"))));
        menu.button(settings.cosmetics().slot(), settings.cosmetics().item(), context -> cosmetics.openCategories(context.player(), Optional.of(this::open)))
            .build()
            .open(player);
    }
}
