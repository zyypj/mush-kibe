package br.com.muxe.core.bukkit.prompt;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.ContainerAnvil;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class AnvilEdits implements Listener {

    public static final int RESULT_SLOT = 2;

    private final Plugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private AnvilEdits(Plugin plugin) {
        this.plugin = plugin;
    }

    public static AnvilEdits start(Plugin plugin) {
        AnvilEdits started = new AnvilEdits(plugin);
        plugin.getServer().getPluginManager().registerEvents(started, plugin);
        return started;
    }

    public void edit(Player player, String current, UnaryOperator<String> resultName, Consumer<String> onAnswer, Runnable onCancel) {
        player.closeInventory();
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        EditContainer container = new EditContainer(handle, current, resultName);
        int windowId = handle.nextContainerCounter();
        handle.playerConnection.sendPacket(new PacketPlayOutOpenWindow(windowId, "minecraft:anvil", new ChatComponentText(""), 0));
        handle.activeContainer = container;
        container.windowId = windowId;
        container.addSlotListener(handle);
        Inventory top = container.getBukkitView().getTopInventory();
        top.setItem(0, ItemBuilder.of(Material.PAPER).name(current).build());
        container.refreshResult();
        sessions.put(player.getUniqueId(), new Session(container, top, onAnswer, onCancel));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !session.inventory().equals(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != RESULT_SLOT || session.container().typed().isBlank()) {
            return;
        }
        String answer = session.container().typed().trim();
        sessions.remove(player.getUniqueId());
        session.inventory().clear();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            if (player.isOnline()) {
                session.onAnswer().accept(answer);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null && session.inventory().equals(event.getView().getTopInventory())) {
            sessions.remove(event.getPlayer().getUniqueId());
            session.inventory().clear();
            if (event.getPlayer() instanceof Player player) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        session.onCancel().run();
                    }
                });
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.inventory().clear();
        }
    }

    private record Session(EditContainer container, Inventory inventory, Consumer<String> onAnswer, Runnable onCancel) {
    }

    private static final class EditContainer extends ContainerAnvil {

        private final UnaryOperator<String> resultName;
        private String typed;

        private EditContainer(EntityPlayer handle, String current, UnaryOperator<String> resultName) {
            super(handle.inventory, handle.world, new BlockPosition(0, 0, 0), handle);
            this.resultName = resultName;
            this.typed = current;
            this.checkReachable = false;
        }

        private String typed() {
            return typed == null ? "" : typed;
        }

        @Override
        public boolean a(EntityHuman human) {
            return true;
        }

        @Override
        public void a(String name) {
            super.a(name);
            typed = name;
            refreshResult();
        }

        @Override
        public void e() {
            super.e();
            refreshResult();
        }

        private void refreshResult() {
            if (resultName == null) {
                return;
            }
            String text = typed();
            if (text.isBlank()) {
                getSlot(RESULT_SLOT).set(null);
            } else {
                getSlot(RESULT_SLOT).set(CraftItemStack.asNMSCopy(ItemBuilder.of(Material.PAPER).name(Colors.translate(resultName.apply(text))).build()));
            }
            a = 0;
        }
    }
}
