package br.com.muxe.core.bukkit.customroom.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.customroom.schema.Icon;
import br.com.muxe.core.common.customroom.schema.RoomLore.Shown;
import java.util.List;
import org.bukkit.Material;

public final class RoomIcons {

    private RoomIcons() {
    }

    public static ItemBuilder item(Icon icon) {
        Material material = Material.matchMaterial(icon.material());
        ItemBuilder builder = ItemBuilder.of(material == null ? Material.STONE : material, icon.data()).amount(icon.amount()).glow(icon.glow());
        return icon.texture().isBlank() ? builder : builder.skullTexture(icon.texture());
    }

    public static ItemBuilder item(Shown shown) {
        return item(shown.icon()).name(Colors.translate(shown.name())).lore(shown.lore().stream().map(Colors::translate).toList());
    }

    public static ItemBuilder item(Icon icon, String name, List<String> lore) {
        return item(icon).name(Colors.translate(name)).lore(lore.stream().map(Colors::translate).toList());
    }
}
