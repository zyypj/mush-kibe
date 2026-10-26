package br.com.muxe.lobby.shop.register;

import br.com.muxe.core.bukkit.display.Display;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.location.ConfigLocation;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcService;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopRegister {

    public static final String ID = "shop-register";
    public static final String LOCATION_PATH = "shop.register.location";

    private final JavaPlugin plugin;
    private final RegisterSettings settings;
    private final ConfigLocation location;
    private final DisplayService displays;
    private final NpcService seller;

    public ShopRegister(JavaPlugin plugin, RegisterSettings settings, Consumer<Player> onClick) {
        this.plugin = plugin;
        this.settings = settings;
        this.location = new ConfigLocation(plugin, LOCATION_PATH);
        this.displays = DisplayService.start(plugin);
        this.seller = NpcService.start(plugin, npc -> List.of());
        this.displays.onClick((player, display) -> onClick.accept(player));
        this.seller.onClick((player, npc) -> onClick.accept(player));
    }

    public void place() {
        Optional<Location> anchor = location.location();
        if (anchor.isEmpty()) {
            displays.remove(ID);
            seller.remove(ID);
            plugin.getLogger().warning("The shop register has no location or its world is not loaded. Use /loja caixa to place it.");
            return;
        }
        displays.put(new Display(ID, anchor.get(), settings.parts(), settings.labels()));
        if (settings.seller()) {
            Location standing = anchor.get().clone();
            standing.setPitch(0f);
            seller.put(new Npc(ID, standing, settings.sellerSkin()));
        } else {
            seller.remove(ID);
        }
    }

    public void moveTo(Location target) {
        location.set(target);
        place();
    }

    public void stop() {
        displays.stop();
        seller.stop();
    }
}
