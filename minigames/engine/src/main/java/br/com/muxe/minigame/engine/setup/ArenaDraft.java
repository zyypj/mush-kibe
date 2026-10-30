package br.com.muxe.minigame.engine.setup;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

public final class ArenaDraft {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{3,32}");

    private final String name;
    private String mode;
    private String displayName;
    private final List<TeamColor> teams = new ArrayList<>();
    private final Map<String, List<Point>> points = new LinkedHashMap<>();
    private final Map<String, Double> numbers = new LinkedHashMap<>();

    public ArenaDraft(String name, String mode, String displayName) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Arena name must match " + NAME.pattern() + ", got: " + name);
        }
        this.name = name;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    public static ArenaDraft create(String name, String mode, SetupSchema schema) {
        ArenaDraft draft = new ArenaDraft(name, mode, schema.displayNameFromName(name));
        draft.teams.addAll(schema.teams(mode).fixed());
        return draft;
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    public static String key(SetupStep step, TeamColor team) {
        if (step.perTeam() != (team != null)) {
            throw new IllegalArgumentException("Step " + step.id() + (step.perTeam() ? " needs a team" : " has no team"));
        }
        return team == null ? step.id() : step.id() + "." + team.id();
    }

    public String name() {
        return name;
    }

    public String mode() {
        return mode;
    }

    public String displayName() {
        return displayName;
    }

    public void rename(String newDisplayName) {
        this.displayName = Objects.requireNonNull(newDisplayName, "displayName");
    }

    public List<TeamColor> teams() {
        return Collections.unmodifiableList(teams);
    }

    public boolean addTeam(TeamColor team, TeamRule rule) {
        if (!rule.isChosen() || teams.contains(team) || teams.size() >= rule.count()) {
            return false;
        }
        teams.add(team);
        return true;
    }

    public boolean removeTeam(TeamColor team, TeamRule rule) {
        if (!rule.isChosen() || !teams.remove(team)) {
            return false;
        }
        String suffix = "." + team.id();
        points.keySet().removeIf(key -> key.endsWith(suffix) || key.contains(suffix + "."));
        numbers.keySet().removeIf(key -> key.endsWith(suffix));
        return true;
    }

    public void restoreTeams(List<TeamColor> saved) {
        teams.clear();
        saved.stream().distinct().forEach(teams::add);
    }

    public List<Point> points(String key) {
        return Collections.unmodifiableList(points.getOrDefault(key, List.of()));
    }

    public List<Point> points(SetupStep step, TeamColor team) {
        if (step.kind() == StepKind.REGION) {
            List<Point> corners = new ArrayList<>();
            corner(step, team, 1).ifPresent(corners::add);
            corner(step, team, 2).ifPresent(corners::add);
            return corners;
        }
        return points(key(step, team));
    }

    public Optional<Point> corner(SetupStep step, TeamColor team, int corner) {
        List<Point> found = points(cornerKey(key(step, team), corner));
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    public static String cornerKey(String key, int corner) {
        if (corner != 1 && corner != 2) {
            throw new IllegalArgumentException("A region has corners 1 and 2, got " + corner);
        }
        return key + ".pos" + corner;
    }

    public OptionalDouble number(String key) {
        Double value = numbers.get(key);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public Map<String, List<Point>> allPoints() {
        return Collections.unmodifiableMap(points);
    }

    public Map<String, Double> allNumbers() {
        return Collections.unmodifiableMap(numbers);
    }

    public int count(SetupStep step, TeamColor team) {
        String key = key(step, team);
        if (step.kind() == StepKind.HEIGHT) {
            return numbers.containsKey(key) ? 1 : 0;
        }
        return points(step, team).size();
    }

    public Change set(SetupStep step, TeamColor team, Point point) {
        String key = key(step, team);
        if (step.kind() == StepKind.HEIGHT) {
            boolean existed = numbers.containsKey(key);
            numbers.put(key, (double) point.blockY());
            return new Change(existed ? Change.Type.REPLACED : Change.Type.SET, 1, 1);
        }
        if (step.kind() == StepKind.REGION) {
            int corner = corner(step, team, 1).isEmpty() ? 1 : corner(step, team, 2).isEmpty() ? 2 : 1;
            if (corner == 1 && count(step, team) == 2) {
                points.remove(cornerKey(key, 2));
            }
            return setCorner(step, team, corner, point);
        }
        List<Point> list = points.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (step.maximum() == 1) {
            boolean existed = !list.isEmpty();
            list.clear();
            list.add(point);
            return new Change(existed ? Change.Type.REPLACED : Change.Type.SET, 1, 1);
        }
        if (list.size() >= step.maximum()) {
            return new Change(Change.Type.FULL, list.size(), list.size());
        }
        list.add(point);
        return new Change(Change.Type.ADDED, list.size(), list.size());
    }

    public Change setCorner(SetupStep step, TeamColor team, int corner, Point point) {
        if (step.kind() != StepKind.REGION) {
            throw new IllegalArgumentException("Step " + step.id() + " is not a region");
        }
        String key = cornerKey(key(step, team), corner);
        boolean existed = points.containsKey(key);
        points.put(key, new ArrayList<>(List.of(point)));
        return new Change(existed ? Change.Type.REPLACED : Change.Type.SET, corner, count(step, team));
    }

    public boolean remove(SetupStep step, TeamColor team, Point near) {
        String key = key(step, team);
        if (step.kind() == StepKind.HEIGHT) {
            return numbers.remove(key) != null;
        }
        if (step.kind() == StepKind.REGION) {
            boolean first = points.remove(cornerKey(key, 1)) != null;
            boolean second = points.remove(cornerKey(key, 2)) != null;
            return first || second;
        }
        List<Point> list = points.get(key);
        if (list == null || list.isEmpty()) {
            return false;
        }
        if (step.maximum() == 1 || step.kind() == StepKind.REGION || near == null) {
            points.remove(key);
            return true;
        }
        list.remove(list.stream().min(Comparator.comparingDouble(point -> point.distanceSquared(near))).orElseThrow());
        if (list.isEmpty()) {
            points.remove(key);
        }
        return true;
    }

    public void restorePoints(String key, List<Point> saved) {
        if (!saved.isEmpty()) {
            points.put(key, new ArrayList<>(saved));
        }
    }

    public void restoreNumber(String key, double value) {
        numbers.put(key, value);
    }

    public record Change(Type type, int index, int count) {

        public enum Type {
            SET,
            REPLACED,
            ADDED,
            FULL
        }
    }
}
