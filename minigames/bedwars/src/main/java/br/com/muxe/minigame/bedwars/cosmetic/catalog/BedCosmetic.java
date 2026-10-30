package br.com.muxe.minigame.bedwars.cosmetic.catalog;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BedCosmetic(String id, String name, ItemBuilder icon, int stars, Optional<String> tag, Availability availability, List<String> description, boolean animated) {

    public static final int MAX_STARS = 5;

    public BedCosmetic {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        if (stars < 1 || stars > MAX_STARS) {
            throw new IllegalArgumentException("Cosmetic " + id + " needs 1 to 5 stars, got " + stars);
        }
        description = List.copyOf(description);
    }

    public boolean forSale() {
        return availability == Availability.SALE && animated;
    }
}
