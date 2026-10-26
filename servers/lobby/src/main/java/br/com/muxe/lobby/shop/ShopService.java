package br.com.muxe.lobby.shop;

import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.menu.MenuHolder;
import br.com.muxe.core.bukkit.prompt.ChatPrompts;
import br.com.muxe.core.bukkit.text.ChatLines;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.lobby.shop.catalog.ShopCatalog;
import br.com.muxe.lobby.shop.catalog.ShopCategory;
import br.com.muxe.lobby.shop.catalog.ShopProduct;
import br.com.muxe.lobby.shop.checkout.CheckoutSettings;
import br.com.muxe.lobby.shop.checkout.LoadingRing;
import br.com.muxe.lobby.shop.checkout.Purchase;
import br.com.muxe.lobby.shop.menu.ShopMenus;
import br.com.muxe.lobby.shop.menu.ShopNavigator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class ShopService implements ShopNavigator {

    private final Plugin plugin;
    private final ShopMenus menus;
    private final CheckoutSettings checkout;
    private final MessageCatalog messages;
    private final ChatPrompts prompts;
    private final SpamGuard spam;
    private final Map<UUID, BukkitTask> loading = new ConcurrentHashMap<>();

    public ShopService(Plugin plugin, ShopSettings settings, MessageCatalog messages, ChatPrompts prompts, SpamGuard spam) {
        this.plugin = plugin;
        this.menus = new ShopMenus(settings.catalog(), settings.checkout());
        this.checkout = settings.checkout();
        this.messages = messages;
        this.prompts = prompts;
        this.spam = spam;
    }

    public ShopCatalog catalog() {
        return menus.catalog();
    }

    public void open(Player player) {
        category(player, catalog().defaultCategory());
    }

    public void openFeatured(Player player) {
        SpamDecision decision = spam.use(player.getUniqueId());
        if (!decision.allowed()) {
            player.sendMessage(messages.get("spam-cooldown", "seconds", CooldownFormat.seconds(decision.remaining())));
            return;
        }
        page(player, catalog().featured());
    }

    @Override
    public void category(Player player, ShopCategory category) {
        menus.category(category, player.getName(), this).open(player);
    }

    @Override
    public void page(Player player, ShopProduct product) {
        menus.page(product, this).open(player);
    }

    @Override
    public void method(Player player, Purchase purchase) {
        menus.method(purchase, this).open(player);
    }

    @Override
    public void supporter(Player player, Purchase purchase) {
        menus.supporter(purchase, this).open(player);
    }

    @Override
    public void useCode(Player player, Purchase purchase) {
        player.closeInventory();
        messages.lines("shop-code-ask").forEach(player::sendMessage);
        prompts.ask(player, checkout.codeTimeout(),
            code -> {
                player.sendMessage(messages.get("shop-code-accepted", "code", code));
                startLoading(player);
            },
            () -> player.sendMessage(messages.get("shop-code-cancelled")),
            () -> player.sendMessage(messages.get("shop-code-expired")));
    }

    @Override
    public void skipCode(Player player, Purchase purchase) {
        startLoading(player);
    }

    public void stop() {
        loading.values().forEach(BukkitTask::cancel);
        loading.clear();
    }

    private void startLoading(Player player) {
        UUID id = player.getUniqueId();
        cancelLoading(id);
        MenuHolder holder = menus.loading(0, closed -> cancelLoading(closed.getUniqueId())).open(player);
        long steps = LoadingRing.steps(checkout.durationTicks(), checkout.stepTicks());
        long[] step = {0};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            step[0]++;
            if (!player.isOnline()) {
                cancelLoading(id);
                return;
            }
            if (step[0] >= steps) {
                finish(player);
                return;
            }
            int lit = LoadingRing.litSlot(step[0]);
            for (int slot : LoadingRing.SLOTS) {
                holder.set(slot, MenuButton.of(slot == lit ? checkout.litPane() : checkout.dimPane()));
            }
        }, checkout.stepTicks(), checkout.stepTicks());
        loading.put(id, task);
    }

    private void finish(Player player) {
        cancelLoading(player.getUniqueId());
        player.closeInventory();
        ChatLines.send(player, messages.lines("shop-payment"), checkout.chatCenter(), checkout.paymentUrl());
    }

    private void cancelLoading(UUID id) {
        BukkitTask task = loading.remove(id);
        if (task != null) {
            task.cancel();
        }
    }
}
