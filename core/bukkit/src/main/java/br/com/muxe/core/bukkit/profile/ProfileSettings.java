package br.com.muxe.core.bukkit.profile;

import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.common.permission.domain.HeldRank;
import br.com.muxe.core.common.permission.domain.RemainingTime;
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
    long credits,
    String rankLine,
    String permanentRank
) {

    public ProfileSettings {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(dateFormat, "dateFormat");
        Objects.requireNonNull(unknownFirstLogin, "unknownFirstLogin");
        Objects.requireNonNull(rankLine, "rankLine");
        Objects.requireNonNull(permanentRank, "permanentRank");
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
            section.getLong("default-credits", 0),
            section.getString("ranks.line", "&7Rank: {rank} &7({time})"),
            section.getString("ranks.permanent", "Vitalício")
        );
    }

    public String firstLogin(Optional<Instant> firstSeenAt) {
        return firstSeenAt.map(dateFormat::format).orElse(unknownFirstLogin);
    }

    public List<String> rankLines(List<HeldRank> ranks, Instant now) {
        return ranks.stream()
            .map(rank -> rankLine
                .replace("{rank}", rank.display())
                .replace("{time}", rank.remaining(now).map(RemainingTime::format).orElse(permanentRank)))
            .toList();
    }
}
