package br.com.muxe.minigame.api.customroom;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.ModeContext;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Stepper;
import br.com.muxe.core.common.customroom.schema.RoomEditor;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CustomRoom {

    public static final String TEAM_SIZE = "team-size";

    private final String code;
    private final String game;
    private final UUID leader;
    private final RoomSchema schema;
    private final Map<UUID, TeamColor> chosen = new LinkedHashMap<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Set<UUID> members = new HashSet<>();
    private GameMode mode;
    private List<String> maps;
    private RoomOptions options;
    private boolean leaderArrived;
    private int closingIn = -1;

    public CustomRoom(String code, String game, UUID leader, RoomSchema schema, GameMode mode, List<String> maps, RoomOptions options, Collection<UUID> members) {
        this.code = code;
        this.game = game;
        this.leader = leader;
        this.schema = schema;
        this.mode = mode;
        this.maps = List.copyOf(maps);
        this.options = options;
        this.members.addAll(members);
        this.members.add(leader);
    }

    public String code() {
        return code;
    }

    public String game() {
        return game;
    }

    public UUID leader() {
        return leader;
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public RoomSchema schema() {
        return schema;
    }

    public GameMode baseMode() {
        return mode;
    }

    public void mode(GameMode changed) {
        mode = changed;
        chosen.clear();
    }

    public List<String> maps() {
        return maps;
    }

    public void maps(List<String> changed) {
        maps = List.copyOf(changed);
    }

    public RoomOptions options() {
        return options;
    }

    public void options(RoomOptions changed) {
        options = changed;
    }

    public ModeContext context() {
        return new ModeContext(mode.id(), mode.teamSize(), mode.maxPlayers(), mode.teams());
    }

    public RoomEditor editor() {
        return new RoomEditor(schema, context());
    }

    public int teamSize() {
        RoomEditor editor = editor();
        return schema.entries(Stepper.class).stream().filter(entry -> entry.key().equals(TEAM_SIZE)).findFirst()
            .map(entry -> (int) editor.stepper(entry, options))
            .orElse(mode.teamSize());
    }

    public List<TeamColor> teams(List<TeamColor> arenaTeams) {
        RoomEditor editor = editor();
        List<TeamColor> active = new ArrayList<>();
        for (int index = 0; index < arenaTeams.size(); index++) {
            if (editor.teamEnabled(index + 1, options)) {
                active.add(arenaTeams.get(index));
            }
        }
        if (active.isEmpty() && !arenaTeams.isEmpty()) {
            active.add(arenaTeams.getFirst());
        }
        return active;
    }

    public GameMode effectiveMode(List<TeamColor> arenaTeams) {
        int size = Math.max(1, teamSize());
        int teams = Math.max(1, teams(arenaTeams).size());
        return new GameMode(mode.id(), mode.displayName(), 1, size * teams, size);
    }

    public Map<TeamColor, String> teamNames(List<TeamColor> arenaTeams) {
        RoomEditor editor = editor();
        Map<TeamColor, String> names = new LinkedHashMap<>();
        for (int index = 0; index < arenaTeams.size(); index++) {
            int number = index + 1;
            TeamColor color = arenaTeams.get(index);
            editor.teamName(number, options).ifPresent(name -> names.put(color, name));
        }
        return names;
    }

    public Choice choose(UUID player, Optional<TeamColor> team, List<TeamColor> activeTeams) {
        if (team.isEmpty()) {
            chosen.remove(player);
            spectators.add(player);
            return Choice.SPECTATOR;
        }
        TeamColor wanted = team.get();
        if (!activeTeams.contains(wanted)) {
            return Choice.UNAVAILABLE;
        }
        if (wanted.equals(chosen.get(player))) {
            return Choice.JOINED;
        }
        if (playersOf(wanted) >= teamSize()) {
            return Choice.FULL;
        }
        spectators.remove(player);
        chosen.put(player, wanted);
        return Choice.JOINED;
    }

    public long playersOf(TeamColor team) {
        return chosen.values().stream().filter(team::equals).count();
    }

    public int spectatorCount() {
        return spectators.size();
    }

    public Optional<TeamColor> choiceOf(UUID player) {
        return Optional.ofNullable(chosen.get(player));
    }

    public Map<UUID, TeamColor> chosen() {
        return Map.copyOf(chosen);
    }

    public Set<UUID> spectators() {
        return Set.copyOf(spectators);
    }

    public void left(UUID player) {
        chosen.remove(player);
        spectators.remove(player);
    }

    public void keepChoicesOf(Collection<UUID> present) {
        chosen.keySet().retainAll(present);
        spectators.retainAll(present);
    }

    public Set<UUID> members() {
        return Set.copyOf(members);
    }

    public List<UUID> newMembers(Collection<UUID> partyMembers) {
        List<UUID> fresh = partyMembers.stream().filter(member -> !members.contains(member)).toList();
        members.addAll(fresh);
        return fresh;
    }

    public List<UUID> formerMembers(Collection<UUID> partyMembers) {
        List<UUID> gone = members.stream().filter(member -> !partyMembers.contains(member) && !member.equals(leader)).toList();
        gone.forEach(members::remove);
        return gone;
    }

    public boolean leaderArrived() {
        return leaderArrived;
    }

    public void leaderArrived(boolean value) {
        leaderArrived = value;
    }

    public boolean closing() {
        return closingIn >= 0;
    }

    public void closeIn(int seconds) {
        closingIn = seconds;
    }

    public boolean closeTick() {
        if (closingIn < 0) {
            return false;
        }
        closingIn--;
        return closingIn <= 0;
    }

    public enum Choice {
        JOINED,
        SPECTATOR,
        FULL,
        UNAVAILABLE
    }
}
