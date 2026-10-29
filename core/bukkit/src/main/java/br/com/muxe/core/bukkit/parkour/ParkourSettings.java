package br.com.muxe.core.bukkit.parkour;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.effect.FireworkBurst;
import br.com.muxe.core.bukkit.hotbar.HotbarItem;
import br.com.muxe.core.common.placement.domain.ParkourPoint;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public record ParkourSettings(
    Map<ParkourItem, HotbarItem> items,
    Map<ParkourPoint.Type, List<DisplayLabel>> labels,
    String noRecord,
    FireworkEffect finishFirework
) {

    public static final String NUMBER = "{number}";
    public static final String RECORD = "{record}";

    public ParkourSettings {
        items = Map.copyOf(items);
        labels = Map.copyOf(labels);
        Objects.requireNonNull(noRecord, "noRecord");
        Objects.requireNonNull(finishFirework, "finishFirework");
        Set<Integer> slots = new HashSet<>();
        for (ParkourItem item : ParkourItem.values()) {
            HotbarItem configured = Objects.requireNonNull(items.get(item), "parkour.items." + key(item));
            if (!slots.add(configured.slot())) {
                throw new IllegalArgumentException("Two parkour items share slot " + (configured.slot() + 1));
            }
        }
        for (ParkourPoint.Type type : ParkourPoint.Type.values()) {
            Objects.requireNonNull(labels.get(type), "parkour.holograms." + key(type));
        }
    }

    public static ParkourSettings from(ConfigurationSection parkour) {
        Map<ParkourItem, HotbarItem> items = new EnumMap<>(ParkourItem.class);
        ConfigurationSection itemSection = section(parkour, "items");
        for (ParkourItem item : ParkourItem.values()) {
            items.put(item, HotbarItem.from(section(itemSection, key(item))));
        }
        Map<ParkourPoint.Type, List<DisplayLabel>> labels = new EnumMap<>(ParkourPoint.Type.class);
        ConfigurationSection holograms = section(parkour, "holograms");
        for (ParkourPoint.Type type : ParkourPoint.Type.values()) {
            labels.put(type, labels(holograms, key(type)));
        }
        return new ParkourSettings(
            items,
            labels,
            parkour.getString("no-record", "..."),
            FireworkBurst.effect(section(parkour, "finish-firework"))
        );
    }

    public List<DisplayLabel> labelsFor(ParkourPoint point) {
        String number = Integer.toString(point.number());
        return labels.get(point.type()).stream().map(label -> new DisplayLabel(label.text().replace(NUMBER, number), label.height())).toList();
    }

    public Map<Integer, ItemStack> buildItems() {
        Map<Integer, ItemStack> built = new HashMap<>();
        items.values().forEach(item -> built.put(item.slot(), item.item().build()));
        return built;
    }

    public Optional<ParkourItem> itemAt(int slot, ItemStack held) {
        if (held == null) {
            return Optional.empty();
        }
        return items.entrySet().stream()
            .filter(entry -> entry.getValue().slot() == slot && entry.getValue().item().material() == held.getType())
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private static List<DisplayLabel> labels(ConfigurationSection holograms, String key) {
        List<DisplayLabel> labels = new ArrayList<>();
        for (Map<?, ?> line : holograms.getMapList(key)) {
            Object text = Objects.requireNonNull(line.get("text"), holograms.getCurrentPath() + "." + key + " needs a text on every line");
            Object height = Objects.requireNonNull(line.get("height"), holograms.getCurrentPath() + "." + key + " needs a height on every line");
            labels.add(new DisplayLabel(text.toString(), ((Number) height).doubleValue()));
        }
        return labels;
    }

    private static ConfigurationSection section(ConfigurationSection parent, String key) {
        return Objects.requireNonNull(parent.getConfigurationSection(key), parent.getCurrentPath() + "." + key);
    }

    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
