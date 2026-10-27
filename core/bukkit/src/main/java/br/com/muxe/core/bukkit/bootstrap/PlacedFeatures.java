package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.AsyncReply;
import br.com.muxe.core.bukkit.command.HologramCommand;
import br.com.muxe.core.bukkit.command.NpcCommand;
import br.com.muxe.core.bukkit.command.ShopCommand;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.core.bukkit.hologram.placed.PlacedHolograms;
import br.com.muxe.core.bukkit.npc.placed.NpcLines;
import br.com.muxe.core.bukkit.npc.placed.PlacedNpcs;
import br.com.muxe.core.bukkit.network.GameConnector;
import br.com.muxe.core.bukkit.npc.skin.MojangSkins;
import br.com.muxe.core.bukkit.placement.PlacementImport;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.prompt.ChatPrompts;
import br.com.muxe.core.bukkit.shop.Shop;
import br.com.muxe.core.bukkit.shop.ShopAccess;
import br.com.muxe.core.bukkit.shop.ShopService;
import br.com.muxe.core.bukkit.shop.ShopSettings;
import br.com.muxe.core.bukkit.shop.promotion.ShopPromotion;
import br.com.muxe.core.bukkit.shop.register.ShopRegisters;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.config.source.DurationParser;
import br.com.muxe.core.common.cooldown.SpamGuard;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class PlacedFeatures {

    private static final String SHOP_SECTION = "shop";

    private final PlacementSync placements;
    private final PlacedNpcs npcs;
    private final PlacedHolograms holograms;
    private final Optional<Shop> shop;
    private final SpamGuard spam;
    private final GameConnector games;
    private final ShopAccess shopAccess;

    private PlacedFeatures(PlacementSync placements, PlacedNpcs npcs, PlacedHolograms holograms, Optional<Shop> shop, SpamGuard spam, GameConnector games, ShopAccess shopAccess) {
        this.placements = placements;
        this.npcs = npcs;
        this.holograms = holograms;
        this.shop = shop;
        this.spam = spam;
        this.games = games;
        this.shopAccess = shopAccess;
    }

    static PlacedFeatures start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, BiConsumer<String, CommandExecutor> bind) {
        MessageCatalog messages = core.messages();
        SpamGuard spam = new SpamGuard(
            core.clock(),
            config.getInt("spam.max-clicks", 3),
            DurationParser.parse(config.getString("spam.window", "1s")),
            DurationParser.parse(config.getString("spam.cooldown", "3s"))
        );
        PlacementSync placements = new PlacementSync(core.identity().group(), core.commands(), core.queries(), core.packets(), core.mainThread(), plugin.getLogger());

        Optional<ShopSettings> shopSettings = Optional.empty();
        if (config.isSet(SHOP_SECTION)) {
            shopSettings = Optional.of(ShopSettings.from(Objects.requireNonNull(config.getConfigurationSection(SHOP_SECTION))));
        } else {
            plugin.getLogger().warning("The shop is off: config.yml has no '" + SHOP_SECTION + "' section");
        }
        Optional<Shop> shop = shopSettings.filter(settings -> settings.appliesTo(core.identity().kind())).map(settings -> shop(plugin, core, settings, spam));
        GameConnector games = new GameConnector(core.servers(), core.network(), core.mainThread(), messages);
        ShopAccess shopAccess = new ShopAccess(shop.map(Shop::service), messages);
        PlacedNpcs npcs = new PlacedNpcs(plugin, games, NpcLines.from(config.getConfigurationSection("npcs.lines")), spam, messages, shopAccess::open);
        PlacedHolograms holograms = new PlacedHolograms(plugin,
            config.getDouble("holograms.top", 2.0),
            config.getDouble("holograms.line-spacing", TextHolograms.DEFAULT_LINE_SPACING));

        placements.onChange(current -> {
            npcs.show(current.npcs());
            holograms.show(current.holograms());
        });
        shop.ifPresent(found -> found.start(placements));
        placements.start().thenRun(() -> PlacementImport.run(plugin.getDataFolder(), placements, plugin.getLogger()));

        AsyncReply reply = new AsyncReply(core.mainThread(), messages, plugin.getLogger());
        bind.accept(NpcCommand.NAME, new NpcCommand(placements, MojangSkins.standard(), core.mainThread(), messages, reply));
        bind.accept(HologramCommand.NAME, new HologramCommand(placements, messages, reply));
        bind.accept(ShopCommand.NAME, new ShopCommand(shop, placements, messages, reply));
        return new PlacedFeatures(placements, npcs, holograms, shop, spam, games, shopAccess);
    }

    SpamGuard spam() {
        return spam;
    }

    GameConnector games() {
        return games;
    }

    ShopAccess shopAccess() {
        return shopAccess;
    }

    void stop() {
        placements.stop();
        npcs.stop();
        holograms.stop();
        shop.ifPresent(Shop::stop);
    }

    private static Shop shop(JavaPlugin plugin, MuxeCore core, ShopSettings settings, SpamGuard spam) {
        ShopPromotion promotion = new ShopPromotion(settings.defaultPromotion(), core.commands(), core.queries(), core.packets(), core.mainThread(), plugin.getLogger());
        ShopService service = new ShopService(plugin, settings, core.messages(), ChatPrompts.start(plugin, core.clock()), spam, () -> promotion.current().productId());
        return new Shop(service, promotion, new ShopRegisters(plugin, settings.register(), service::openPromotion));
    }
}
