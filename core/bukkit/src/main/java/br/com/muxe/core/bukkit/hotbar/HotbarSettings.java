package br.com.muxe.core.bukkit.hotbar;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public record HotbarSettings(
    Set<ServerKind> serverKinds,
    HotbarItem gameSelector,
    HotbarItem profile,
    HotbarItem cosmetics,
    int visibilitySlot,
    Map<Visibility, ItemBuilder> visibilityItems,
    HotbarItem lobbySelector
) {

    public HotbarSettings {
        serverKinds = Set.copyOf(serverKinds);
        visibilityItems = Map.copyOf(visibilityItems);
        for (Visibility visibility : Visibility.values()) {
            Objects.requireNonNull(visibilityItems.get(visibility), "hotbar.visibility." + key(visibility));
        }
        Set<Integer> slots = new HashSet<>();
        for (int slot : new int[] {gameSelector.slot(), profile.slot(), cosmetics.slot(), visibilitySlot, lobbySelector.slot()}) {
            if (!slots.add(slot)) {
                throw new IllegalArgumentException("Two hotbar items share slot " + (slot + 1));
            }
        }
    }

    public static HotbarSettings from(ConfigurationSection hotbar) {
        ConfigurationSection visibility = section(hotbar, "visibility");
        Map<Visibility, ItemBuilder> items = new EnumMap<>(Visibility.class);
        for (Visibility option : Visibility.values()) {
            items.put(option, ItemBuilder.from(section(visibility, key(option))));
        }
        Set<ServerKind> kinds = EnumSet.noneOf(ServerKind.class);
        hotbar.getStringList("server-kinds").forEach(kind -> kinds.add(ServerKind.valueOf(kind.toUpperCase(Locale.ROOT))));
        return new HotbarSettings(
            kinds,
            HotbarItem.from(section(hotbar, "game-selector")),
            HotbarItem.from(section(hotbar, "profile")),
            HotbarItem.from(section(hotbar, "cosmetics")),
            HotbarItem.slotOf(visibility),
            items,
            HotbarItem.from(section(hotbar, "lobby-selector"))
        );
    }

    public boolean appliesTo(ServerKind kind) {
        return serverKinds.contains(kind);
    }

    public Optional<HotbarAction> actionAt(int slot) {
        if (slot == gameSelector.slot()) {
            return Optional.of(HotbarAction.GAME_SELECTOR);
        }
        if (slot == profile.slot()) {
            return Optional.of(HotbarAction.PROFILE);
        }
        if (slot == cosmetics.slot()) {
            return Optional.of(HotbarAction.COSMETICS);
        }
        if (slot == visibilitySlot) {
            return Optional.of(HotbarAction.VISIBILITY);
        }
        if (slot == lobbySelector.slot()) {
            return Optional.of(HotbarAction.LOBBY_SELECTOR);
        }
        return Optional.empty();
    }

    public ItemBuilder visibilityItem(Visibility visibility) {
        return visibilityItems.get(visibility);
    }

    private static String key(Visibility visibility) {
        return visibility.name().toLowerCase(Locale.ROOT);
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
