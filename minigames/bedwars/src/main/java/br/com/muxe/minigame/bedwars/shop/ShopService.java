package br.com.muxe.minigame.bedwars.shop;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.room.Kits;
import br.com.muxe.minigame.bedwars.room.RoomPlayer;
import br.com.muxe.minigame.bedwars.room.RoomRuntime;
import br.com.muxe.minigame.bedwars.room.RoomTeam;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Category;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tab;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tier;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog.Entry;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public final class ShopService {

    public static final List<Integer> ITEM_SLOTS = List.of(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43);

    private static final int PANE_FIRST = 9;
    private static final int PANE_GREEN = 13;
    private static final int PANE_GRAY = 7;
    private static final int SHOP_ROWS = 6;
    private static final int UPGRADE_ROWS = 3;

    private final ShopCatalog shop;
    private final UpgradeCatalog upgrades;
    private final MessageCatalog messages;

    public ShopService(ShopCatalog shop, UpgradeCatalog upgrades, MessageCatalog messages) {
        this.shop = shop;
        this.upgrades = upgrades;
        this.messages = messages;
    }

    public void open(Player player, RoomRuntime runtime, boolean upgradeMenu) {
        if (upgradeMenu) {
            openUpgrades(player, runtime);
        } else {
            openShop(player, runtime, ShopCatalog.QUICK_BUY);
        }
    }

    public void openShop(Player player, RoomRuntime runtime, String categoryId) {
        Optional<RoomPlayer> found = runtime.room().player(player.getUniqueId());
        if (found.isEmpty() || !found.get().isAlive()) {
            return;
        }
        RoomPlayer roomPlayer = found.get();
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
        List<String> ids = quickBuy ? shop.quickBuy() : category.items();
        for (int index = 0; index < ids.size() && index < ITEM_SLOTS.size(); index++) {
            ShopItem item = shop.items().get(ids.get(index));
            menu.button(ITEM_SLOTS.get(index), icon(player, runtime, roomPlayer, item), context -> {
                if (context.isShiftClick()) {
                    return;
                }
                buy(context, runtime, item);
                openShop(context.player(), runtime, categoryId);
            });
        }
        if (quickBuy) {
            menu.button(shop.editor().slot(), ItemBuilder.of(shop.editor().material()).name(shop.editor().name()).lore(shop.editor().lore()));
        }
        menu.build().open(player);
    }

    public void openUpgrades(Player player, RoomRuntime runtime) {
        Optional<RoomPlayer> found = runtime.room().player(player.getUniqueId());
        if (found.isEmpty() || !found.get().isAlive()) {
            return;
        }
        RoomTeam team = runtime.room().team(found.get().team());
        int diamonds = count(player.getInventory(), Material.DIAMOND);
        Menu.Builder menu = Menu.builder(upgrades.title()).rows(UPGRADE_ROWS);
        for (Entry entry : upgrades.entries()) {
            int level = team.level(entry.upgrade());
            ItemBuilder icon = ItemBuilder.of(entry.material())
                .name(entry.name(upgrades.texts(), level, diamonds))
                .lore(entry.lore(upgrades.texts(), level, diamonds));
            menu.button(entry.slot(), icon, context -> {
                upgrade(context, runtime, team, entry);
                openUpgrades(context.player(), runtime);
            });
        }
        menu.build().open(player);
    }

    private void upgrade(ClickContext context, RoomRuntime runtime, RoomTeam team, Entry entry) {
        Player player = context.player();
        int level = team.level(entry.upgrade());
        if (entry.maxed(level)) {
            return;
        }
        int price = entry.nextPrice(level);
        if (count(player.getInventory(), Material.DIAMOND) < price) {
            player.sendMessage(Colors.translate(upgrades.texts().cannotAfford()));
            return;
        }
        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, price));
        team.upgrade(entry.upgrade(), level + 1);
        runtime.upgraded(team, entry.upgrade(), level + 1);
        String message = messages.get("upgrade-bought", "player", runtime.coloredName(runtime.room().player(player.getUniqueId()).orElseThrow()),
            "upgrade", entry.boughtName(level + 1));
        runtime.room().members(team.color()).forEach(member -> {
            Player online = player.getServer().getPlayer(member.id());
            if (online != null) {
                online.sendMessage(message);
            }
        });
    }

    private ItemBuilder icon(Player player, RoomRuntime runtime, RoomPlayer roomPlayer, ShopItem item) {
        Offer offer = Offer.of(item, roomPlayer);
        Tier tier = offer.tier();
        boolean affordable = count(player.getInventory(), tier.currency().material()) >= tier.price();
        boolean locked = item.id().equals("rastreador") && runtime.room().teams().values().stream()
            .anyMatch(team -> team.bedAlive() && team.color() != roomPlayer.team());
        boolean inQuickBuy = shop.quickBuy().contains(item.id());
        ItemBuilder icon = ItemBuilder.of(tier.material(), item.type() == ShopCatalog.Type.POTION || item.type() == ShopCatalog.Type.SPECIAL ? item.data() : 0)
            .amount(item.amount())
            .name(offer.name(shop.texts(), affordable && !locked))
            .lore(offer.lore(shop.texts(), affordable, inQuickBuy, locked));
        boolean glow = !item.enchantments().isEmpty() || item.isTiered() || item.type() == ShopCatalog.Type.POTION;
        return glow ? icon.glow() : icon;
    }

    private void buy(ClickContext context, RoomRuntime runtime, ShopItem item) {
        Player player = context.player();
        RoomPlayer roomPlayer = runtime.room().player(player.getUniqueId()).orElseThrow();
        RoomTeam team = runtime.room().team(roomPlayer.team());
        Offer offer = Offer.of(item, roomPlayer);
        if (offer.state() == Offer.State.MAXED) {
            player.sendMessage(Colors.translate(shop.texts().maxed()));
            return;
        }
        if (offer.state() == Offer.State.OWNED) {
            player.sendMessage(Colors.translate(shop.texts().owned()));
            return;
        }
        if (item.type() == ShopCatalog.Type.SPECIAL) {
            player.sendMessage(messages.get("coming-soon"));
            return;
        }
        Tier tier = offer.tier();
        PlayerInventory inventory = player.getInventory();
        if (count(inventory, tier.currency().material()) < tier.price()) {
            player.sendMessage(Colors.translate(shop.texts().cannotAfford().replace("{currency}", tier.currency().many())));
            return;
        }
        boolean needsSlot = item.type() != ShopCatalog.Type.ARMOR && item.type() != ShopCatalog.Type.PICKAXE && item.type() != ShopCatalog.Type.AXE;
        if (needsSlot && inventory.firstEmpty() < 0) {
            player.sendMessage(messages.get("inventory-full"));
            return;
        }
        inventory.removeItem(new ItemStack(tier.currency().material(), tier.price()));
        give(player, roomPlayer, team, item, offer);
        player.sendMessage(messages.get("bought", "item", offer.boughtName()));
    }

    private void give(Player player, RoomPlayer roomPlayer, RoomTeam team, ShopItem item, Offer offer) {
        PlayerInventory inventory = player.getInventory();
        Tier tier = offer.tier();
        switch (item.type()) {
            case WOOL -> inventory.addItem(Kits.wool(team.color(), item.amount()));
            case BLOCK -> inventory.addItem(new ItemStack(tier.material(), item.amount(), (short) item.data()));
            case SWORD -> {
                inventory.remove(Material.WOOD_SWORD);
                inventory.addItem(Kits.sword(tier.material(), team));
            }
            case ARMOR -> {
                roomPlayer.armorTier(item.armorTier());
                Kits.armor(player, roomPlayer, team);
            }
            case SHEARS -> {
                roomPlayer.giveShears();
                inventory.addItem(Kits.unbreakable(new ItemStack(Material.SHEARS)));
            }
            case PICKAXE -> {
                removeAll(inventory, Kits::isPickaxe);
                roomPlayer.pickaxeTier(offer.level());
                inventory.addItem(Kits.pickaxe(offer.level()));
            }
            case AXE -> {
                removeAll(inventory, Kits::isAxe);
                roomPlayer.axeTier(offer.level());
                inventory.addItem(Kits.axe(offer.level(), team));
            }
            case POTION -> inventory.addItem(potion(item, tier));
            case ITEM -> inventory.addItem(plain(item, tier));
            case SPECIAL -> {
            }
        }
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
        meta.addCustomEffect(new PotionEffect(item.potion().type(), item.potion().seconds() * 20, item.potion().amplifier()), true);
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
