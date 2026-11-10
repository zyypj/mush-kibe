package br.com.muxe.minigame.bedwars.shop;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.bedwars.cosmetic.wood.WoodChoice;
import br.com.muxe.minigame.bedwars.cosmetic.wood.WoodTypes;
import br.com.muxe.minigame.bedwars.game.Slingshot;
import br.com.muxe.minigame.bedwars.hotbar.HotbarDelivery;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarEditor;
import br.com.muxe.minigame.bedwars.invisibility.Invisibility;
import br.com.muxe.minigame.bedwars.item.ItemNames;
import br.com.muxe.minigame.bedwars.listener.Bedbugs;
import br.com.muxe.minigame.bedwars.quickbuy.domain.QuickBuy;
import br.com.muxe.minigame.bedwars.custom.CustomShop;
import br.com.muxe.minigame.bedwars.room.BedWarsMatch;
import br.com.muxe.minigame.bedwars.room.Kits;
import br.com.muxe.minigame.bedwars.room.Room;
import br.com.muxe.minigame.bedwars.room.RoomPlayer;
import br.com.muxe.minigame.bedwars.room.RoomTeam;
import br.com.muxe.minigame.bedwars.settings.MatchSounds;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Category;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.QuickBuyTexts;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tab;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tier;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;

public final class ShopService {

    public static final Set<String> BUYABLE_SPECIALS = Set.of(Bedbugs.ITEM_ID, Slingshot.ITEM_ID);

    public static final List<Integer> ITEM_SLOTS = List.of(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43);

    private static final int PANE_FIRST = 9;
    private static final int PANE_GREEN = 13;
    private static final int PANE_GRAY = 7;
    private static final int PANE_RED = 14;
    private static final int ADDING_SLOT = 4;
    private static final int SHOP_ROWS = 6;

    private final Localized<ShopCatalog> shops;
    private final Localized<UpgradeCatalog> upgradeCatalogs;
    private final MessageCatalog messages;
    private final MatchSounds sounds;
    private final QuickBuys quickBuys;
    private final Localized<ItemNames> itemNames;
    private final HotbarDelivery hotbarDelivery;
    private final HotbarEditor hotbarEditor;
    private final WoodTypes woods;

    public ShopService(Localized<ShopCatalog> shops, Localized<UpgradeCatalog> upgradeCatalogs, MessageCatalog messages, MatchSounds sounds, QuickBuys quickBuys,
                       Localized<ItemNames> itemNames, HotbarDelivery hotbarDelivery, HotbarEditor hotbarEditor, WoodTypes woods) {
        this.itemNames = itemNames;
        this.shops = shops;
        this.upgradeCatalogs = upgradeCatalogs;
        this.messages = messages;
        this.sounds = sounds;
        this.quickBuys = quickBuys;
        this.hotbarDelivery = hotbarDelivery;
        this.hotbarEditor = hotbarEditor;
        this.woods = woods;
    }

    public void open(Player player, MatchRuntime<BedWarsMatch> runtime, boolean upgradeMenu) {
        if (upgradeMenu) {
            openUpgrades(player, runtime);
        } else {
            openShop(player, runtime, ShopCatalog.QUICK_BUY);
        }
    }

    public void openShop(Player player, MatchRuntime<BedWarsMatch> runtime, String categoryId) {
        Optional<Participant> found = runtime.match().player(player.getUniqueId());
        if (found.isEmpty() || !found.get().isAlive()) {
            return;
        }
        Participant roomPlayer = found.get();
        ShopCatalog shop = shop(player);
        boolean quickBuy = categoryId.equals(ShopCatalog.QUICK_BUY);
        Category category = shop.categories().get(categoryId);
        String title = quickBuy ? shop.quickBuyTitle() : shop.categoryTitle().replace("{category}", category.name());
        Menu.Builder menu = Menu.builder(title).rows(SHOP_ROWS);
        int openColumn = 0;
        for (Tab tab : shop.tabs()) {
            boolean current = tab.category().equals(categoryId);
            if (current) {
                openColumn = tab.slot();
            }
            ItemBuilder icon = ItemBuilder.of(tab.material()).name(tab.name()).lore(current ? List.of() : List.of(shop.texts().openCategory()));
            menu.button(tab.slot(), tab.category().equals(ShopCatalog.QUICK_BUY) ? icon.glow() : icon, context -> openShop(context.player(), runtime, tab.category()));
        }
        for (int column = 0; column < 9; column++) {
            menu.button(PANE_FIRST + column, ItemBuilder.of(Material.STAINED_GLASS_PANE, column == openColumn ? PANE_GREEN : PANE_GRAY).name(" "));
        }
        QuickBuy layout = quickBuys.of(player.getUniqueId());
        List<String> listed = runtime.state().rules().custom() ? category.withCustom() : category.items();
        List<Optional<String>> ids = quickBuy ? positions(layout) : listed.stream().map(Optional::of).toList();
        for (int index = 0; index < ids.size() && index < ITEM_SLOTS.size(); index++) {
            Optional<ShopItem> entry = ids.get(index).map(shop.items()::get);
            if (entry.isEmpty()) {
                QuickBuyTexts texts = shop.quickBuyTexts();
                menu.button(ITEM_SLOTS.get(index), ItemBuilder.of(Material.STAINED_GLASS_PANE, PANE_RED).name(texts.emptyName()).lore(texts.empty()));
                continue;
            }
            ShopItem item = entry.get();
            Optional<ItemBuilder> shown = icon(player, runtime, roomPlayer, item, layout, shop);
            if (shown.isEmpty()) {
                if (quickBuy) {
                    QuickBuyTexts texts = shop.quickBuyTexts();
                    menu.button(ITEM_SLOTS.get(index), ItemBuilder.of(Material.STAINED_GLASS_PANE, PANE_RED).name(texts.emptyName()).lore(texts.empty()));
                }
                continue;
            }
            menu.button(ITEM_SLOTS.get(index), shown.get(), context -> {
                if (context.isShiftClick()) {
                    editQuickBuy(context.player(), runtime, item, categoryId);
                    return;
                }
                buy(context, runtime, item);
                openShop(context.player(), runtime, categoryId);
            });
        }
        if (quickBuy) {
            menu.button(shop.editor().slot(), ItemBuilder.of(shop.editor().material()).name(shop.editor().name()).lore(shop.editor().lore()),
                context -> hotbarEditor.openFromShop(context.player(), viewer -> openShop(viewer, runtime, ShopCatalog.QUICK_BUY)));
        }
        menu.build().open(player);
    }

    private void editQuickBuy(Player player, MatchRuntime<BedWarsMatch> runtime, ShopItem item, String categoryId) {
        QuickBuy layout = quickBuys.of(player.getUniqueId());
        if (layout.contains(item.id())) {
            quickBuys.change(player.getUniqueId(), layout.remove(item.id()));
            openShop(player, runtime, categoryId);
            return;
        }
        openAdding(player, runtime, item);
    }

    public void openAdding(Player player, MatchRuntime<BedWarsMatch> runtime, ShopItem adding) {
        Optional<Participant> found = runtime.match().player(player.getUniqueId());
        if (found.isEmpty() || !found.get().isAlive()) {
            return;
        }
        Participant roomPlayer = found.get();
        ShopCatalog shop = shop(player);
        QuickBuyTexts texts = shop.quickBuyTexts();
        QuickBuy layout = quickBuys.of(player.getUniqueId());
        Menu.Builder menu = Menu.builder(texts.addingTitle()).rows(SHOP_ROWS);
        ShopItem shownAdding = shop.items().getOrDefault(adding.id(), adding);
        icon(player, runtime, roomPlayer, shownAdding, layout, shop).ifPresent(shown -> menu.button(ADDING_SLOT, shown.lore(addingLore(shownAdding, texts))));
        List<Optional<String>> positions = positions(layout);
        for (int position = 0; position < positions.size(); position++) {
            int chosen = position;
            Optional<ShopItem> current = positions.get(position).map(shop.items()::get);
            ItemBuilder icon = current
                .flatMap(item -> icon(player, runtime, roomPlayer, item, layout, shop))
                .map(shown -> shown.lore(texts.replace()))
                .orElseGet(() -> ItemBuilder.of(Material.STAINED_GLASS_PANE, PANE_RED).name(texts.emptyName()).lore(texts.emptyAdd()));
            menu.button(ITEM_SLOTS.get(position), icon, context -> {
                quickBuys.change(context.player().getUniqueId(), quickBuys.of(context.player().getUniqueId()).place(chosen, adding.id()));
                openShop(context.player(), runtime, ShopCatalog.QUICK_BUY);
            });
        }
        menu.build().open(player);
    }

    static List<String> addingLore(ShopItem item, QuickBuyTexts texts) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        if (!item.description().isEmpty()) {
            lore.addAll(item.description());
            lore.add(" ");
        }
        lore.add(texts.adding());
        return lore;
    }

    private static List<Optional<String>> positions(QuickBuy layout) {
        List<Optional<String>> positions = new ArrayList<>(QuickBuy.SIZE);
        for (int position = 0; position < QuickBuy.SIZE; position++) {
            positions.add(layout.at(position));
        }
        return positions;
    }

    public void openUpgrades(Player player, MatchRuntime<BedWarsMatch> runtime) {
        Optional<Participant> found = runtime.match().player(player.getUniqueId());
        if (found.isEmpty() || !found.get().isAlive()) {
            return;
        }
        RoomTeam team = runtime.state().room().team(found.get().team());
        CustomShop custom = runtime.state().shop();
        Language language = messages.language(player);
        UpgradeCatalog upgrades = upgradeCatalogs.in(language);
        Map<String, ShopCatalog.Currency> currencies = shops.in(language).currencies();
        List<Entry> shown = new ArrayList<>();
        for (Entry base : runtime.state().rules().custom() ? upgrades.withCustom() : upgrades.entries()) {
            custom.upgrade(base, currencies, upgrades.texts()).ifPresent(shown::add);
        }
        Menu.Builder menu = Menu.builder(upgrades.title()).rows(UpgradeCatalog.rows(shown));
        for (Entry entry : shown) {
            int level = team.level(entry.upgrade());
            Material currency = entry.currency(level);
            int diamonds = runtime.state().resources(player, currency == Material.DIAMOND).count(currency);
            ItemBuilder icon = ItemBuilder.of(entry.material())
                .name(entry.name(upgrades.texts(), level, diamonds))
                .lore(entry.lore(upgrades.texts(), level, diamonds));
            menu.button(entry.slot(), icon, context -> {
                upgrade(context, runtime, team, entry, upgrades.texts());
                openUpgrades(context.player(), runtime);
            });
        }
        menu.build().open(player);
    }

    private void upgrade(ClickContext context, MatchRuntime<BedWarsMatch> runtime, RoomTeam team, Entry entry, UpgradeCatalog.Texts texts) {
        Player player = context.player();
        int level = team.level(entry.upgrade());
        if (entry.maxed(level)) {
            return;
        }
        int price = entry.nextPrice(level);
        Material currency = entry.currency(level);
        if (price > 0 && !runtime.state().resources(player, currency == Material.DIAMOND).take(currency, price)) {
            player.sendMessage(Colors.translate(texts.cannotAfford()));
            MatchSounds.play(sounds.cannotAfford(), player);
            return;
        }
        team.upgrade(entry.upgrade(), level + 1);
        runtime.state().upgraded(team, entry.upgrade(), level + 1);
        MatchSounds.play(sounds.upgrade(), player);
        String buyer = runtime.coloredName(runtime.match().player(player.getUniqueId()).orElseThrow());
        runtime.match().members(team.color()).forEach(member -> {
            Player online = player.getServer().getPlayer(member.id());
            if (online != null) {
                online.sendMessage(messages.get(online, "upgrade-bought", "player", buyer, "upgrade", upgradeName(messages.language(online), entry, level + 1)));
            }
        });
    }

    String upgradeName(Language language, Entry bought, int level) {
        return upgradeCatalogs.in(language).entry(bought.id()).orElse(bought).boughtName(level);
    }

    private Optional<Offer> offer(MatchRuntime<BedWarsMatch> runtime, ShopItem item, RoomPlayer roomPlayer, ShopCatalog shop) {
        Offer offer = Offer.of(item, roomPlayer);
        return runtime.state().shop().tier(item, offer.tier(), shop.currencies()).map(tier -> new Offer(offer.item(), offer.state(), tier, offer.level()));
    }

    private Optional<ItemBuilder> icon(Player player, MatchRuntime<BedWarsMatch> runtime, Participant roomPlayer, ShopItem item, QuickBuy layout, ShopCatalog shop) {
        Room room = runtime.state().room();
        Optional<Offer> found = offer(runtime, item, room.player(roomPlayer.id()), shop);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Offer offer = found.get();
        Tier tier = offer.tier();
        boolean affordable = runtime.state().resources(player, false).count(tier.currency().material()) >= tier.price();
        boolean locked = item.id().equals("rastreador") && room.islands().stream()
            .anyMatch(color -> room.bedAlive(color) && color != roomPlayer.team());
        boolean inQuickBuy = layout.contains(item.id());
        ItemBuilder icon = ItemBuilder.of(tier.material(), item.type() == ShopCatalog.Type.POTION || item.type() == ShopCatalog.Type.SPECIAL ? item.data() : 0)
            .amount(item.amount())
            .name(offer.name(shop.texts(), affordable && !locked))
            .lore(offer.lore(shop.texts(), affordable, inQuickBuy, locked));
        boolean glow = !item.enchantments().isEmpty() || item.isTiered() || item.type() == ShopCatalog.Type.POTION;
        return Optional.of(glow ? icon.glow() : icon);
    }

    private void buy(ClickContext context, MatchRuntime<BedWarsMatch> runtime, ShopItem item) {
        Player player = context.player();
        Participant participant = runtime.match().player(player.getUniqueId()).orElseThrow();
        RoomPlayer roomPlayer = runtime.state().room().player(participant.id());
        RoomTeam team = runtime.state().room().team(participant.team());
        ShopCatalog shop = shop(player);
        Optional<Offer> found = offer(runtime, item, roomPlayer, shop);
        if (found.isEmpty()) {
            return;
        }
        Offer offer = found.get();
        if (offer.alreadyHad()) {
            player.sendMessage(messages.get(player, "already-have"));
            return;
        }
        if (item.type() == ShopCatalog.Type.SPECIAL && !BUYABLE_SPECIALS.contains(item.id())) {
            player.sendMessage(messages.get(player, "coming-soon"));
            return;
        }
        Tier tier = offer.tier();
        PlayerInventory inventory = player.getInventory();
        CustomShop custom = runtime.state().shop();
        if (!custom.allows(item, tier, participant.team().id(), participant.id().toString(), count(inventory, tier.material()))) {
            player.sendMessage(messages.get(player, "purchase-limit"));
            MatchSounds.play(sounds.cannotAfford(), player);
            return;
        }
        ResourcePool pool = runtime.state().resources(player, false);
        if (pool.count(tier.currency().material()) < tier.price()) {
            player.sendMessage(Colors.translate(shop.texts().cannotAfford().replace("{currency}", tier.currency().many())));
            MatchSounds.play(sounds.cannotAfford(), player);
            return;
        }
        boolean needsSlot = item.type() != ShopCatalog.Type.ARMOR && item.type() != ShopCatalog.Type.PICKAXE && item.type() != ShopCatalog.Type.AXE;
        if (needsSlot && inventory.firstEmpty() < 0) {
            player.sendMessage(messages.get(player, "inventory-full"));
            return;
        }
        pool.take(tier.currency().material(), tier.price());
        custom.bought(item, tier, participant.team().id(), participant.id().toString());
        String bought = offer.boughtName(shop.texts());
        give(player, roomPlayer, team, item, offer, bought);
        if (tier.material() == Material.HARD_CLAY || tier.material() == Material.STAINED_CLAY) {
            roomPlayer.addClay(item.amount());
        }
        player.sendMessage(messages.get(player, "bought", "item", bought));
        MatchSounds.play(sounds.bought(), player);
    }

    private void give(Player player, RoomPlayer roomPlayer, RoomTeam team, ShopItem item, Offer offer, String bought) {
        PlayerInventory inventory = player.getInventory();
        Tier tier = offer.tier();
        ItemNames names = itemNames.in(messages.language(player));
        switch (item.type()) {
            case WOOL -> deliver(player, item, Kits.wool(team.color(), item.amount(), names));
            case BLOCK -> deliver(player, item, names.named(block(woods, player.getUniqueId(), item, tier), bought));
            case SWORD -> {
                inventory.remove(Material.WOOD_SWORD);
                deliver(player, item, Kits.sword(tier.material(), team, names));
            }
            case ARMOR -> {
                roomPlayer.armorTier(item.armorTier());
                Kits.armor(player, roomPlayer, team, names);
            }
            case SHEARS -> {
                roomPlayer.giveShears();
                deliver(player, item, names.named(Kits.unbreakable(new ItemStack(Material.SHEARS))));
            }
            case PICKAXE -> {
                removeAll(inventory, Kits::isPickaxe);
                roomPlayer.pickaxeTier(offer.level());
                deliver(player, item, Kits.pickaxe(offer.level(), names));
            }
            case AXE -> {
                removeAll(inventory, Kits::isAxe);
                roomPlayer.axeTier(offer.level());
                deliver(player, item, Kits.axe(offer.level(), team, names));
            }
            case POTION -> deliver(player, item, potion(item, tier));
            case ITEM -> deliver(player, item, names.named(plain(item, tier), bought));
            case SPECIAL -> deliver(player, item, names.named(new ItemStack(tier.material(), item.amount(), (short) item.data()),
                item.id().equals(Slingshot.ITEM_ID) ? messages.get(player, "slingshot-item") : bought));
        }
    }

    static ItemStack block(WoodTypes woods, UUID player, ShopItem item, Tier tier) {
        if (item.id().equals(WoodTypes.ITEM_ID)) {
            WoodChoice wood = woods.of(player);
            return new ItemStack(wood.material(), item.amount(), wood.data());
        }
        return new ItemStack(tier.material(), item.amount(), (short) item.data());
    }

    private ShopCatalog shop(Player player) {
        return shops.in(messages.language(player));
    }

    private void deliver(Player player, ShopItem item, ItemStack stack) {
        hotbarDelivery.give(player, item.id(), stack);
    }

    private static ItemStack plain(ShopItem item, Tier tier) {
        ItemStack stack = new ItemStack(tier.material(), item.amount(), (short) item.data());
        if (item.unbreakable()) {
            stack = Kits.unbreakable(stack);
        }
        item.resolvedEnchantments().forEach(stack::addUnsafeEnchantment);
        return stack;
    }

    private static ItemStack potion(ShopItem item, Tier tier) {
        ItemStack stack = new ItemStack(Material.POTION, 1, (short) item.data());
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.clearCustomEffects();
        meta.addCustomEffect(Invisibility.potionEffect(item.potion().type(), item.potion().seconds() * 20, item.potion().amplifier()), true);
        meta.setDisplayName(Colors.translate("&f" + tier.name()));
        stack.setItemMeta(meta);
        return stack;
    }

    private static void removeAll(PlayerInventory inventory, Predicate<Material> matches) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && matches.test(contents[slot].getType())) {
                inventory.setItem(slot, null);
            }
        }
    }

    static int count(PlayerInventory inventory, Material material) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
