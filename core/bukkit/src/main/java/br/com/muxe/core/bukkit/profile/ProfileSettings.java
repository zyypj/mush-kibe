package br.com.muxe.core.bukkit.profile;

import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record ProfileSettings(
    String title,
    int rows,
    ConfiguredButton head,
    List<ConfiguredButton> buttons,
    DateTimeFormatter dateFormat,
    String unknownFirstLogin,
    long credits
) {

    public ProfileSettings {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(dateFormat, "dateFormat");
        Objects.requireNonNull(unknownFirstLogin, "unknownFirstLogin");
        buttons = List.copyOf(buttons);
        List<ConfiguredButton> all = new ArrayList<>(buttons);
        all.add(head);
        ConfiguredButton.requireFit(all, rows);
    }

    public static ProfileSettings from(ConfigurationSection section) {
        String pattern = section.getString("first-login.format", "dd/MM/yyyy HH:mm");
        ZoneId zone = ZoneId.of(section.getString("first-login.time-zone", "America/Sao_Paulo"));
        return new ProfileSettings(
            section.getString("title", "Meu perfil"),
            section.getInt("rows", 6),
            ConfiguredButton.from(Objects.requireNonNull(section.getConfigurationSection("head"), "Missing configuration section " + section.getCurrentPath() + ".head")),
            ConfiguredButton.list(section.getConfigurationSection("buttons")),
            DateTimeFormatter.ofPattern(pattern).withZone(zone),
            section.getString("first-login.unknown", "-"),
            section.getLong("default-credits", 0)
        );
    }

    public String firstLogin(Optional<Instant> firstSeenAt) {
        return firstSeenAt.map(dateFormat::format).orElse(unknownFirstLogin);
    }
}
