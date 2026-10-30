package br.com.muxe.minigame.bedwars.cosmetic.preview;

import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.common.config.source.DurationParser;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record PreviewSettings(
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    BedFrame bed,
    Duration timeout,
    Duration returnDelay,
    String actionBar,
    String title,
    List<String> lines
) {

    public PreviewSettings {
        lines = List.copyOf(lines);
    }

    public static PreviewSettings from(ConfigurationSection section) {
        ConfigurationSection spawn = section(section, "spawn");
        ConfigurationSection foot = section(section, "bed.foot");
        ConfigurationSection head = section(section, "bed.head");
        ConfigurationSection scoreboard = section(section, "scoreboard");
        return new PreviewSettings(
            section.getString("world", "previa_quebra_de_cama"),
            spawn.getDouble("x"),
            spawn.getDouble("y"),
            spawn.getDouble("z"),
            (float) spawn.getDouble("yaw"),
            (float) spawn.getDouble("pitch"),
            BedFrame.of(foot.getInt("x"), foot.getInt("y"), foot.getInt("z"), head.getInt("x"), head.getInt("z")),
            DurationParser.parse(section.getString("timeout", "120s")),
            DurationParser.parse(section.getString("return-delay", "2s")),
            section.getString("action-bar", "&eQuebre a cama!"),
            scoreboard.getString("title", "&b&lMUXE"),
            scoreboard.getStringList("lines")
        );
    }

    public boolean isBed(int blockX, int blockY, int blockZ) {
        int footX = (int) Math.floor(bed.foot().x());
        int footY = (int) Math.floor(bed.foot().y());
        int footZ = (int) Math.floor(bed.foot().z());
        int headX = (int) Math.floor(bed.head().x());
        int headZ = (int) Math.floor(bed.head().z());
        return blockY == footY && (blockX == footX && blockZ == footZ || blockX == headX && blockZ == headZ);
    }

    public List<String> lines(String name) {
        return lines.stream().map(line -> line.replace("{name}", name)).toList();
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
