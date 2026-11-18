package br.com.muxe.core.bukkit.permission.display;

import br.com.muxe.core.common.preference.domain.TagFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NameTagTeams implements Listener, Runnable {

    public static final long PERIOD_TICKS = 5L;

    private final Server server;
    private final BiFunction<UUID, TagFormat, NameTag> tags;
    private final Function<UUID, TagFormat> formats;
    private final Map<UUID, Map<TagFormat, NameTag>> wanted = new HashMap<>();
    private final Map<Scoreboard, Map<String, String>> applied = new WeakHashMap<>();

    public NameTagTeams(Server server, BiFunction<UUID, TagFormat, NameTag> tags, Function<UUID, TagFormat> formats) {
        this.server = server;
        this.tags = tags;
        this.formats = formats;
    }

    public void update(Player player) {
        if (player.isOnline()) {
            wanted.remove(player.getUniqueId());
            run();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        wanted.remove(event.getPlayer().getUniqueId());
        String name = event.getPlayer().getName();
        applied.forEach((board, entries) -> {
            String team = entries.remove(name);
            if (team != null) {
                leave(board, team, name);
            }
        });
    }

    @Override
    public void run() {
        List<Player> online = List.copyOf(server.getOnlinePlayers());
        Map<Scoreboard, TagFormat> boards = new IdentityHashMap<>();
        for (Player player : online) {
            boards.merge(player.getScoreboard(), formats.apply(player.getUniqueId()), NameTagTeams::shared);
        }
        boards.forEach((board, format) -> apply(board, online, format));
    }

    static TagFormat shared(TagFormat one, TagFormat other) {
        return one == other ? one : TagFormat.DEFAULT;
    }

    public void clear() {
        applied.forEach((board, entries) -> entries.forEach((name, team) -> leave(board, team, name)));
        applied.clear();
        wanted.clear();
    }

    private void apply(Scoreboard board, List<Player> online, TagFormat format) {
        Map<String, String> entries = applied.computeIfAbsent(board, ignored -> new HashMap<>());
        Set<String> present = new HashSet<>();
        for (Player player : online) {
            UUID id = player.getUniqueId();
            NameTag tag = wanted.computeIfAbsent(id, ignored -> new EnumMap<>(TagFormat.class)).computeIfAbsent(format, chosen -> tags.apply(id, chosen));
            String name = player.getName();
            present.add(name);
            Team team = board.getTeam(tag.team());
            if (team == null) {
                team = board.registerNewTeam(tag.team());
                team.setCanSeeFriendlyInvisibles(true);
            }
            if (!tag.prefix().equals(team.getPrefix())) {
                team.setPrefix(tag.prefix());
            }
            if (!tag.suffix().equals(team.getSuffix())) {
                team.setSuffix(tag.suffix());
            }
            String previous = entries.put(name, tag.team());
            if (!tag.team().equals(previous) || !team.hasEntry(name)) {
                if (previous != null && !previous.equals(tag.team())) {
                    leave(board, previous, name);
                }
                team.addEntry(name);
            }
        }
        entries.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) {
                return false;
            }
            leave(board, entry.getValue(), entry.getKey());
            return true;
        });
    }

    private static void leave(Scoreboard board, String teamName, String entry) {
        Team team = board.getTeam(teamName);
        if (team == null) {
            return;
        }
        if (team.hasEntry(entry)) {
            team.removeEntry(entry);
        }
        if (team.getSize() == 0) {
            team.unregister();
        }
    }
}
