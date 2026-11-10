package br.com.muxe.minigame.bedwars.lobby.top;

import br.com.muxe.minigame.bedwars.lobby.settings.Spot;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record TopSelectorSettings(
    Spot spot,
    double listOffset,
    double listSpacing,
    double footerOffset,
    int visibleModes,
    String header,
    String footer,
    String selected,
    String other,
    String generalName
) {

    public static Optional<TopSelectorSettings> from(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(new TopSelectorSettings(
            Spot.from(Objects.requireNonNull(section.getConfigurationSection("location"), "Missing lobby.tops.selector.location")),
            section.getDouble("list-offset", 0.375),
            section.getDouble("list-spacing", 0.28125),
            section.getDouble("footer-offset", 1.90625),
            Math.max(1, section.getInt("visible-modes", 5)),
            section.getString("header", ""),
            section.getString("footer", ""),
            section.getString("selected", "{mode}"),
            section.getString("other", "{mode}"),
            section.getString("general-name", "")
        ));
    }
}
