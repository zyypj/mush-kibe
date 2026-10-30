package br.com.muxe.minigame.engine.setup;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ArenaFiles {

    public static final String EXTENSION = ".yml";

    private static final String POINTS = "points";
    private static final String NUMBERS = "numbers";

    private final Path folder;

    public ArenaFiles(Path folder) {
        this.folder = folder;
    }

    public Path file(String name) {
        return folder.resolve(name + EXTENSION);
    }

    public boolean exists(String name) {
        return Files.isRegularFile(file(name));
    }

    public List<String> names() throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> listing = Files.list(folder)) {
            return listing
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(EXTENSION))
                .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                .filter(ArenaDraft::isValidName)
                .sorted()
                .toList();
        }
    }

    public void write(ArenaDraft draft, boolean ready) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", draft.name());
        yaml.set("mode", draft.mode());
        yaml.set("display-name", draft.displayName());
        yaml.set("ready", ready);
        yaml.set("teams", draft.teams().stream().map(TeamColor::id).toList());
        for (Map.Entry<String, List<Point>> entry : draft.allPoints().entrySet()) {
            yaml.set(POINTS + "." + entry.getKey(), entry.getValue().stream().map(ArenaFiles::point).toList());
        }
        for (Map.Entry<String, Double> entry : draft.allNumbers().entrySet()) {
            yaml.set(NUMBERS + "." + entry.getKey(), entry.getValue());
        }
        Files.createDirectories(folder);
        Path partial = folder.resolve(draft.name() + EXTENSION + ".part");
        Files.writeString(partial, yaml.saveToString());
        Files.move(partial, file(draft.name()), StandardCopyOption.REPLACE_EXISTING);
    }

    public Optional<StoredArena> read(String name) throws IOException {
        Path file = file(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(file));
        } catch (InvalidConfigurationException exception) {
            throw new IOException(file + " is not valid YAML: " + exception.getMessage(), exception);
        }
        String mode = yaml.getString("mode");
        if (mode == null) {
            throw new IOException(file + " has no mode");
        }
        ArenaDraft draft = new ArenaDraft(name, mode, yaml.getString("display-name", name));
        List<TeamColor> teams = new ArrayList<>();
        for (String team : yaml.getStringList("teams")) {
            teams.add(TeamColor.find(team).orElseThrow(() -> new IOException(file + " has an unknown team " + team)));
        }
        draft.restoreTeams(teams);
        ConfigurationSection points = yaml.getConfigurationSection(POINTS);
        if (points != null) {
            for (String key : points.getKeys(true)) {
                if (points.isList(key)) {
                    draft.restorePoints(key, points.getMapList(key).stream().map(ArenaFiles::point).toList());
                }
            }
        }
        ConfigurationSection numbers = yaml.getConfigurationSection(NUMBERS);
        if (numbers != null) {
            for (String key : numbers.getKeys(true)) {
                if (!numbers.isConfigurationSection(key)) {
                    draft.restoreNumber(key, numbers.getDouble(key));
                }
            }
        }
        return Optional.of(new StoredArena(draft, yaml.getBoolean("ready", false)));
    }

    private static Map<String, Object> point(Point point) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("x", point.x());
        values.put("y", point.y());
        values.put("z", point.z());
        values.put("yaw", (double) point.yaw());
        values.put("pitch", (double) point.pitch());
        return values;
    }

    private static Point point(Map<?, ?> values) {
        return new Point(number(values, "x"), number(values, "y"), number(values, "z"), (float) number(values, "yaw"), (float) number(values, "pitch"));
    }

    private static double number(Map<?, ?> values, String key) {
        return values.get(key) instanceof Number number ? number.doubleValue() : 0;
    }

    public record StoredArena(ArenaDraft draft, boolean ready) {
    }
}
