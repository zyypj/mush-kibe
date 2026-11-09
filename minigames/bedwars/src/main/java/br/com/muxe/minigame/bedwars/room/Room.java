package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena.TeamBase;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.bedwars.game.XpRewards;
import br.com.muxe.minigame.bedwars.mission.BedWarsGoal;
import br.com.muxe.minigame.bedwars.stats.domain.MatchOutcome;
import br.com.muxe.minigame.bedwars.stats.domain.MatchResult;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class Room {

    static final double MAP_MARGIN = 12;

    private final Match match;
    private final BedWarsArena arena;
    private final Map<TeamColor, RoomTeam> teams = new LinkedHashMap<>();
    private final Map<UUID, RoomPlayer> players = new HashMap<>();

    public Room(Match match, BedWarsArena arena) {
        this.match = match;
        this.arena = arena;
        arena.teams().keySet().forEach(color -> teams.put(color, new RoomTeam(color)));
    }

    public Match match() {
        return match;
    }

    public String code() {
        return match.code();
    }

    public BedWarsArena arena() {
        return arena;
    }

    public RoomPlayer player(UUID id) {
        return players.computeIfAbsent(id, RoomPlayer::new);
    }

    public Map<TeamColor, RoomTeam> teams() {
        return Collections.unmodifiableMap(teams);
    }

    public RoomTeam team(TeamColor color) {
        return teams.get(color);
    }

    public boolean bedAlive(TeamColor color) {
        return match.team(color).respawns();
    }

    public List<TeamColor> islands() {
        return List.copyOf(teams.keySet());
    }

    public void died(Participant participant) {
        player(participant.id()).died();
    }

    public List<Participant> breakBed(TeamColor color, Participant breaker) {
        match.team(color).stopRespawning(breaker == null ? null : breaker.id());
        if (breaker != null) {
            player(breaker.id()).addBed();
        }
        List<Participant> lost = new ArrayList<>();
        for (Participant member : match.members(color)) {
            player(member.id()).loseBed();
            if (member.state() == Participant.State.DISCONNECTED) {
                match.eliminate(member);
                lost.add(member);
            }
        }
        return lost;
    }

    public Area area() {
        List<Point> points = new ArrayList<>();
        for (TeamBase base : arena.teams().values()) {
            points.addAll(List.of(base.spawn(), base.bed(), base.shop(), base.upgrades()));
            points.addAll(base.iron());
            points.addAll(base.gold());
        }
        points.addAll(arena.diamonds());
        points.addAll(arena.emeralds());
        points.addAll(arena.extraShops());
        points.addAll(arena.extraUpgrades());
        points.addAll(arena.extraIron());
        points.addAll(arena.extraGold());
        if (points.isEmpty()) {
            points.add(arena.spectator());
        }
        double minX = points.stream().mapToDouble(Point::x).min().orElseThrow();
        double minZ = points.stream().mapToDouble(Point::z).min().orElseThrow();
        double maxX = points.stream().mapToDouble(Point::x).max().orElseThrow();
        double maxZ = points.stream().mapToDouble(Point::z).max().orElseThrow();
        Area area = new Area(match.cell().toWorldX(minX) - MAP_MARGIN, match.cell().toWorldZ(minZ) - MAP_MARGIN,
            match.cell().toWorldX(maxX) + MAP_MARGIN, match.cell().toWorldZ(maxZ) + MAP_MARGIN);
        List<Point> emeralds = arena.emeralds();
        if (emeralds.isEmpty()) {
            return area;
        }
        double middleX = emeralds.stream().mapToDouble(Point::x).average().orElseThrow();
        double middleZ = emeralds.stream().mapToDouble(Point::z).average().orElseThrow();
        return area.middleAt(match.cell().toWorldX(middleX), match.cell().toWorldZ(middleZ));
    }

    public List<MatchOutcome> outcomes(Optional<TeamColor> winner, XpRewards rewards) {
        List<MatchOutcome> outcomes = new ArrayList<>();
        int minutes = match.elapsed() / 60;
        for (Participant participant : match.players()) {
            if (participant.team() == null || !match.ranked(participant.id())) {
                continue;
            }
            RoomPlayer player = player(participant.id());
            boolean won = winner.filter(participant.team()::equals).isPresent();
            MatchResult result = winner.isEmpty() ? MatchResult.NONE : won ? MatchResult.WIN : MatchResult.LOSS;
            int teammates = Math.max(0, match.team(participant.team()).members().size() - 1);
            long xp = participant.xp() + rewards.endOfMatch(minutes, teammates, won);
            outcomes.add(new MatchOutcome(participant.id(), match.mode().id(), player.beds(), player.bedsLost(), participant.kills(), participant.deaths(),
                participant.assists(), participant.finalKills(), participant.finalDeaths(), participant.finalAssists(), result, xp));
        }
        return outcomes;
    }

    public Map<String, Long> missionTotals(Participant participant, Optional<TeamColor> winner, int fastWinSeconds, int endStoneBlocks) {
        RoomPlayer player = player(participant.id());
        boolean won = participant.team() != null && winner.filter(participant.team()::equals).isPresent();
        Map<String, Long> goals = new LinkedHashMap<>();
        goals.put(BedWarsGoal.KILL_STREAK.name(), (long) participant.bestKillStreak());
        goals.put(BedWarsGoal.INVISIBLE_BED.name(), (long) player.invisibleBeds());
        goals.put(BedWarsGoal.END_STONE_DEFENSE.name(), player.endStones() >= endStoneBlocks ? 1L : 0L);
        goals.put(BedWarsGoal.CLEAN_WIN.name(), won && !player.usedExplosives() ? 1L : 0L);
        goals.put(BedWarsGoal.BEDBUG_KILLS.name(), (long) player.bedbugKills());
        goals.put(BedWarsGoal.EMERALDS_IN_A_LIFE.name(), (long) player.bestEmeralds());
        goals.put(BedWarsGoal.FIREBALL_DAMAGE.name(), (long) Math.floor(player.fireballDamage()));
        goals.put(BedWarsGoal.FINAL_KILLS.name(), (long) participant.finalKills());
        goals.put(BedWarsGoal.EXPLOSIVE_BLOCKS.name(), (long) player.explosiveBlocks());
        goals.put(BedWarsGoal.FAST_WIN.name(), won && match.elapsed() <= fastWinSeconds ? 1L : 0L);
        goals.put(BedWarsGoal.CLAY_BOUGHT.name(), (long) player.clayBought());
        return goals;
    }
}
