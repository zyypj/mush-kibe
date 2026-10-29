package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record AccountSettings(
    DateTimeFormatter chatDates,
    DateTimeFormatter menuDates,
    String title,
    int rows,
    ConfiguredButton head,
    String rankLine,
    List<String> headFooter,
    ConfiguredButton close,
    List<ConfiguredButton> staffButtons
) {

    public AccountSettings {
        Objects.requireNonNull(chatDates, "chatDates");
        Objects.requireNonNull(menuDates, "menuDates");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(rankLine, "rankLine");
        headFooter = List.copyOf(headFooter);
        Objects.requireNonNull(close, "close");
        staffButtons = List.copyOf(staffButtons);
        List<ConfiguredButton> all = new ArrayList<>(staffButtons);
        all.add(head);
        all.add(close);
        ConfiguredButton.requireFit(all, rows);
    }

    public static AccountSettings from(ConfigurationSection section) {
        ZoneId zone = ZoneId.of(section.getString("time-zone", "America/Sao_Paulo"));
        return new AccountSettings(
            DateTimeFormatter.ofPattern(section.getString("date-format", "dd/MM/yyyy HH:mm")).withZone(zone),
            DateTimeFormatter.ofPattern(section.getString("menu-date-format", "dd/MM/yyyy HH:mm:ss")).withZone(zone),
            section.getString("title", "Conta: {player}"),
            section.getInt("rows", 6),
            ConfiguredButton.from(required(section, "head")),
            section.getString("rank-line", "&7Rank: {rank}"),
            section.getStringList("head-footer"),
            ConfiguredButton.from(required(section, "close")),
            ConfiguredButton.list(section.getConfigurationSection("staff-buttons"))
        );
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        return Objects.requireNonNull(section.getConfigurationSection(path), "Missing configuration section " + section.getCurrentPath() + "." + path);
    }
}
