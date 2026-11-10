package br.com.muxe.minigame.bedwars.lobby.top;

import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.minigame.bedwars.stats.domain.PlayerStats;
import br.com.muxe.minigame.bedwars.stats.domain.StatsPeriod;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

public record TopTexts(
    String header,
    String entry,
    String empty,
    String click,
    String periodSelected,
    String periodOther,
    String general,
    Map<StatsPeriod, String> periodNames,
    double labelAboveStand,
    int refreshSeconds,
    Optional<Sound> clickSound,
    String xpValue,
    Spacing spacing
) {

    public TopTexts {
        periodNames = Map.copyOf(periodNames);
    }

    public static TopTexts from(ConfigurationSection section) {
        Map<StatsPeriod, String> names = new EnumMap<>(StatsPeriod.class);
        ConfigurationSection configured = section.getConfigurationSection("period-names");
        for (StatsPeriod period : StatsPeriod.values()) {
            names.put(period, configured == null ? period.id() : configured.getString(period.name(), period.id()));
        }
        return new TopTexts(
            section.getString("header", "({page}/{pages})"),
            section.getString("entry", "{position}. {name} - {value}"),
            section.getString("empty", "{position}. - {value}"),
            section.getString("click", ""),
            section.getString("period-selected", "{period}"),
            section.getString("period-other", "{period}"),
            section.getString("general", PlayerStats.GENERAL),
            names,
            section.getDouble("label-above-stand", PacketHologramTransport.FULL_STAND_LABEL_HEIGHT),
            Math.max(10, section.getInt("refresh-seconds", 60)),
            sound(section.getString("click-sound", "CLICK")),
            section.getString("xp-value", "{xp} XP"),
            Spacing.from(section.getConfigurationSection("spacing"))
        );
    }

    public record Spacing(double title, double list, double entries, double click, double periods) {

        public static final Spacing REFERENCE = new Spacing(0.28125, 0.375, 0.28125, 0.375, 0.34375);

        static Spacing from(ConfigurationSection section) {
            if (section == null) {
                return REFERENCE;
            }
            return new Spacing(
                section.getDouble("title", REFERENCE.title()),
                section.getDouble("list", REFERENCE.list()),
                section.getDouble("entries", REFERENCE.entries()),
                section.getDouble("click", REFERENCE.click()),
                section.getDouble("periods", REFERENCE.periods())
            );
        }
    }

    public static Optional<Sound> sound(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Sound.valueOf(name.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
