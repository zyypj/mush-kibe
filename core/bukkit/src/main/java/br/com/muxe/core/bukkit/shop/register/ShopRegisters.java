package br.com.muxe.core.bukkit.shop.register;

import br.com.muxe.core.bukkit.display.Display;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.placement.Reconciler;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ShopRegisters {

    public static final String SELLER_PREFIX = "shop-seller:";

    private final Plugin plugin;
    private final RegisterSettings settings;
    private final DisplayService displays;
    private final NpcService sellers;
    private final Reconciler<RegisterPlacement> shown = new Reconciler<>(RegisterPlacement::id);
    private volatile List<String> lines = List.of();

    public ShopRegisters(Plugin plugin, RegisterSettings settings, Consumer<Player> onClick) {
        this(plugin, settings, onClick, Localized.same(List.of()), LanguageLookup.DEFAULT);
    }

    public ShopRegisters(Plugin plugin, RegisterSettings settings, Consumer<Player> onClick, Localized<List<String>> defaultLines, LanguageLookup languages) {
        this.plugin = plugin;
        this.settings = settings;
        this.displays = DisplayService.start(plugin);
        this.sellers = NpcService.start(plugin, npc -> List.of());
        this.displays.onClick((player, display) -> onClick.accept(player));
        this.sellers.onClick((player, npc) -> onClick.accept(player));
        this.displays.labelText((viewer, text) -> translate(defaultLines, languages.of(viewer.getUniqueId()), text));
    }

    public void refresh(Player viewer) {
        displays.refresh(viewer);
    }

    public static String sellerId(String registerId) {
        return SELLER_PREFIX + registerId;
    }

    public void show(List<RegisterPlacement> registers) {
        Reconciler.Changes<RegisterPlacement> changes = shown.next(registers);
        changes.removed().forEach(this::hide);
        changes.put().forEach(this::place);
    }

    public void label(List<String> value) {
        if (value.equals(lines)) {
            return;
        }
        lines = List.copyOf(value);
        shown.all().forEach(this::place);
    }

    public void stop() {
        displays.stop();
        sellers.stop();
    }

    private void place(RegisterPlacement register) {
        Optional<Location> anchor = Positions.resolve(register.position(), plugin.getServer()::getWorld);
        if (anchor.isEmpty()) {
            hide(register.id());
            plugin.getLogger().warning("Register '" + register.id() + "' is in world '" + register.position().world() + "', which is not loaded");
            return;
        }
        displays.put(new Display(register.id(), anchor.get(), settings.parts(register.position().yaw()), settings.labels(lines)));
        Optional<Location> seller = register.seller().flatMap(position -> Positions.resolve(position, plugin.getServer()::getWorld));
        if (seller.isPresent()) {
            sellers.put(new Npc(sellerId(register.id()), seller.get(), settings.sellerSkin()));
        } else {
            sellers.remove(sellerId(register.id()));
        }
    }

    private String translate(Localized<List<String>> defaults, Language language, String text) {
        List<String> standard = defaults.standard();
        if (!lines.equals(standard)) {
            return text;
        }
        int index = standard.indexOf(text);
        List<String> own = defaults.in(language);
        return index >= 0 && index < own.size() ? own.get(index) : text;
    }

    private void hide(String id) {
        displays.remove(id);
        sellers.remove(sellerId(id));
    }
}
