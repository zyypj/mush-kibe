package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.Page;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cosmetic.application.command.BuyCosmeticCommand;
import br.com.muxe.core.common.cosmetic.application.command.BuyCosmeticHandler;
import br.com.muxe.core.common.cosmetic.application.command.SelectCosmeticCommand;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.BedCosmetic;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.CosmeticCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticProfiles;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticState;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticWallet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class CosmeticMenus {

    private static final int SPECIAL_ENTRIES = 2;

    private final CosmeticMenuSettings settings;
    private final CosmeticCatalog catalog;
    private final CosmeticIcons icons;
    private final CosmeticProfiles profiles;
    private final CommandBus commands;
    private final Executor mainThread;
    private final MessageCatalog messages;
    private final PreviewGate previews;
    private final Logger logger;

    public CosmeticMenus(CosmeticMenuSettings settings, CosmeticCatalog catalog, CosmeticProfiles profiles, CommandBus commands, Executor mainThread,
                         MessageCatalog messages, PreviewGate previews, Logger logger) {
        this.settings = settings;
        this.catalog = catalog;
        this.icons = new CosmeticIcons(settings, catalog);
        this.profiles = profiles;
        this.commands = commands;
        this.mainThread = mainThread;
        this.messages = messages;
        this.previews = previews;
        this.logger = logger;
    }

    public void openBedWars(Player player) {
        Menu.builder(settings.bedwarsTitle())
            .rows(settings.bedwarsRows())
            .button(settings.cosmetics().slot(), settings.cosmetics().item(), context -> openCategories(context.player()))
            .build()
            .open(player);
    }

    public void openCategories(Player player) {
        withWallet(player, wallet -> {
            Menu.Builder menu = Menu.builder(settings.categoriesTitle()).rows(settings.categoriesRows());
            menu.button(settings.bedDestruction().slot(), icons.category(wallet), context -> openList(context.player(), 1));
            settings.soon().forEach((slot, item) -> menu.button(slot, item, context -> context.player().sendMessage(messages.get("coming-soon"))));
            menu.button(settings.categoriesBack().slot(), settings.categoriesBack().item(), context -> openBedWars(context.player()));
            menu.build().open(player);
        });
    }

    public void openList(Player player, int page) {
        withWallet(player, wallet -> list(wallet, page).open(player));
    }

    Menu list(CosmeticWallet wallet, int requested) {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(icons.none(wallet), context -> choose(context, CosmeticWallet.NONE, icons.selectedName(wallet.selecting(CosmeticWallet.NONE)))));
        entries.add(new Entry(icons.random(wallet), context -> choose(context, CosmeticWallet.RANDOM, icons.selectedName(wallet.selecting(CosmeticWallet.RANDOM)))));
        for (BedCosmetic cosmetic : wallet.sorted(catalog)) {
            entries.add(new Entry(icons.cosmetic(cosmetic, wallet), context -> clicked(context, cosmetic)));
        }
        Page<Entry> page = Page.of(entries, settings.listSlots().size(), requested);
        String title = settings.listTitle().replace("{page}", Integer.toString(page.number())).replace("{pages}", Integer.toString(page.total()));
        Menu.Builder menu = Menu.builder(title).rows(settings.listRows());
        for (int index = 0; index < page.items().size(); index++) {
            Entry entry = page.items().get(index);
            menu.button(settings.listSlots().get(index), entry.icon(), entry.click()::accept);
        }
        int number = page.number();
        if (page.hasPrevious()) {
            menu.button(settings.previous().slot(), settings.previous().item().replacing("page", Integer.toString(number - 1)), context -> openList(context.player(), number - 1));
        } else {
            menu.button(settings.listBack().slot(), settings.listBack().item(), context -> openCategories(context.player()));
        }
        if (page.hasNext()) {
            menu.button(settings.next().slot(), settings.next().item().replacing("page", Integer.toString(number + 1)), context -> openList(context.player(), number + 1));
        }
        menu.button(settings.selected().slot(), icons.selected(wallet));
        menu.button(settings.shop().slot(), settings.shop().item(), context -> context.player().sendMessage(messages.get("coming-soon")));
        return menu.build();
    }

    static int pageOf(int entryIndex, int perPage) {
        return entryIndex / perPage + 1;
    }

    private void clicked(ClickContext context, BedCosmetic cosmetic) {
        Player player = context.player();
        CosmeticWallet wallet = profiles.of(player.getUniqueId()).orElse(CosmeticWallet.EMPTY);
        if (context.isRightClick()) {
            if (!cosmetic.animated()) {
                player.sendMessage(messages.get("cosmetic-no-preview"));
                return;
            }
            player.closeInventory();
            previews.preview(player, cosmetic);
            return;
        }
        CosmeticState state = wallet.state(cosmetic);
        switch (state) {
            case OWNED -> choose(context, cosmetic.id(), cosmetic.name());
            case BUY -> buy(context, cosmetic);
            case UNAVAILABLE -> player.sendMessage(messages.get("cosmetic-unavailable"));
            case SELECTED -> {
            }
        }
    }

    private void choose(ClickContext context, String id, String name) {
        Player player = context.player();
        int page = currentPage(player, id);
        commands.dispatch(new SelectCosmeticCommand(player.getUniqueId(), catalog.category(), id)).whenCompleteAsync((result, error) -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null || result.isFailure()) {
                failed(player, error);
                return;
            }
            profiles.of(player.getUniqueId()).ifPresent(wallet -> profiles.update(player.getUniqueId(), wallet.selecting(id)));
            player.sendMessage(messages.get("cosmetic-selected", "name", name));
            openList(player, page);
        }, mainThread);
    }

    private void buy(ClickContext context, BedCosmetic cosmetic) {
        Player player = context.player();
        long price = catalog.price(cosmetic);
        int page = currentPage(player, cosmetic.id());
        commands.dispatch(new BuyCosmeticCommand(player.getUniqueId(), catalog.category(), cosmetic.id(), price)).whenCompleteAsync((result, error) -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                failed(player, error);
                return;
            }
            if (result instanceof Result.Failure<Long> failure) {
                String key = BuyCosmeticHandler.ALREADY_OWNED.equals(failure.problem().code()) ? "cosmetic-already-owned" : "cosmetic-not-enough";
                player.sendMessage(messages.get(key));
                return;
            }
            long balance = result.orElseThrow();
            profiles.of(player.getUniqueId()).ifPresent(wallet -> profiles.update(player.getUniqueId(), wallet.buying(cosmetic.id(), balance)));
            player.sendMessage(messages.get("cosmetic-bought", "name", cosmetic.name(), "price", CreditFormat.format(price)));
            openList(player, page);
        }, mainThread);
    }

    private int currentPage(Player player, String id) {
        CosmeticWallet wallet = profiles.of(player.getUniqueId()).orElse(CosmeticWallet.EMPTY);
        List<BedCosmetic> sorted = wallet.sorted(catalog);
        for (int index = 0; index < sorted.size(); index++) {
            if (sorted.get(index).id().equals(id)) {
                return pageOf(index + SPECIAL_ENTRIES, settings.listSlots().size());
            }
        }
        return 1;
    }

    private void withWallet(Player player, Consumer<CosmeticWallet> action) {
        profiles.load(player.getUniqueId(), player.hasPermission(CosmeticProfiles.ULTRA_PERMISSION)).whenCompleteAsync((wallet, error) -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                failed(player, error);
                return;
            }
            action.accept(wallet);
        }, mainThread);
    }

    private void failed(Player player, Throwable error) {
        if (error != null) {
            logger.log(Level.WARNING, "A cosmetic action of " + player.getName() + " failed", error);
        }
        player.sendMessage(messages.get("cosmetic-failed"));
    }

    @FunctionalInterface
    public interface PreviewGate {

        void preview(Player player, BedCosmetic cosmetic);
    }

    private record Entry(ItemBuilder icon, Consumer<ClickContext> click) {
    }
}
