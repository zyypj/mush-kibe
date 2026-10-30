package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.core.bukkit.text.CenteredText;
import br.com.muxe.core.bukkit.text.ChatLines;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena.TeamBase;
import br.com.muxe.minigame.bedwars.game.Ranking;
import br.com.muxe.minigame.bedwars.game.Timeline.Event;
import br.com.muxe.minigame.bedwars.room.RoomPlayer.State;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.engine.match.StartCountdown.Change;
import br.com.muxe.minigame.engine.match.StartCountdown.Second;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class RoomRuntime {

    private static final int BLOCKS_PER_TICK = 4000;
    private static final int RESEND_EVERY = 3;
    private static final String HEALTH_OBJECTIVE = "muxe-vida";
    private static final Map<Material, String> LOOT = new LinkedHashMap<>();
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    static {
        LOOT.put(Material.IRON_INGOT, "iron");
        LOOT.put(Material.GOLD_INGOT, "gold");
        LOOT.put(Material.DIAMOND, "diamond");
        LOOT.put(Material.EMERALD, "emerald");
    }

    private final RoomService service;
    private final MuxeCore core;
    private final BedWarsSettings settings;
    private final MessageCatalog messages;
    private final Room room;
    private final World world;
    private final RoomView view;
    private final RoomForges forges;
    private final Map<UUID, Sidebar> sidebars = new HashMap<>();
    private final List<String> npcs = new ArrayList<>();
    private final Set<TeamColor> announcedEliminations = new HashSet<>();
    private int seconds;

    RoomRuntime(RoomService service, Room room, World world) {
        this.service = service;
        this.core = service.core();
        this.settings = service.settings();
        this.messages = service.messages();
        this.room = room;
        this.world = world;
        this.view = new RoomView(settings.scoreboard(), settings.timeline());
        this.forges = new RoomForges(service.plugin(), settings);
    }

    public Room room() {
        return room;
    }

    public World world() {
        return world;
    }

    public Location location(Point point) {
        return new Location(world, room.cell().toWorldX(point.x()), point.y(), room.cell().toWorldZ(point.z()), point.yaw(), point.pitch());
    }

    public List<Player> online() {
        List<Player> online = new ArrayList<>();
        for (RoomPlayer player : room.players()) {
            Player bukkit = Bukkit.getPlayer(player.id());
            if (bukkit != null && bukkit.getWorld().equals(world)) {
                online.add(bukkit);
            }
        }
        return online;
    }

    public void broadcast(String message) {
        online().forEach(player -> player.sendMessage(message));
    }

    public String coloredName(RoomPlayer player) {
        return Colors.translate(player.team() == null ? "&7" : player.team().color()) + player.name();
    }

    void addWaiting(Player player) {
        Change change = room.join(player.getUniqueId(), player.getName());
        reset(player, GameMode.ADVENTURE);
        player.teleport(location(room.arena().waitingSpawn()));
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(settings.hotbar().menu().slot(), named(Material.matchMaterial(settings.hotbar().menu().material()), settings.hotbar().menu().name()));
        inventory.setItem(settings.hotbar().leave().slot(), named(Material.matchMaterial(settings.hotbar().leave().material()), settings.hotbar().leave().name()));
        sidebars.put(player.getUniqueId(), Sidebar.create(player));
        countdownChanged(change);
        refreshScoreboards();
    }

    void removeWaiting(Player player) {
        Change change = room.leaveBeforeStart(player.getUniqueId());
        forget(player);
        countdownChanged(change);
        refreshScoreboards();
    }

    void forget(Player player) {
        sidebars.remove(player.getUniqueId());
        forges.hide(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    void second() {
        seconds++;
        switch (room.phase()) {
            case WAITING -> refreshScoreboards();
            case STARTING -> {
                Second second = room.countdown().tick();
                if (second.finished()) {
                    start();
                } else {
                    settings.countdownTitles().at(second.remaining()).ifPresent(this::title);
                    refreshScoreboards();
                }
            }
            case PLAYING -> playingSecond();
            case ENDING -> {
                room.tick();
                if (room.endingFor() == settings.timings().restartSeconds()) {
                    online().forEach(player -> core.network().connectToGroup(player.getUniqueId(), settings.lobbyGroup()));
                } else if (room.endingFor() >= settings.timings().restartSeconds() + 3) {
                    close();
                }
            }
        }
    }

    void forgeCycle() {
        if (room.phase() == Room.Phase.PLAYING) {
            forges.cycle();
        }
    }

    void start() {
        Instant now = core.clock().instant();
        room.start(List.of(), now);
        clearWaitingArea();
        for (Map.Entry<TeamColor, RoomTeam> entry : room.teams().entrySet()) {
            TeamBase base = room.arena().teams().get(entry.getKey());
            boolean occupied = !entry.getValue().members().isEmpty();
            if (!occupied) {
                removeBed(entry.getKey());
                continue;
            }
            base.iron().forEach(point -> forges.island(entry.getKey(), location(point), Material.IRON_INGOT, settings.forges().iron(room.mode().teamSize())));
            base.gold().forEach(point -> forges.island(entry.getKey(), location(point), Material.GOLD_INGOT, settings.forges().gold()));
            spawnNpc(entry.getKey(), "loja", base.shop(), room.mode().teamSize() > 1 ? settings.holograms().shopTeam() : settings.holograms().shop());
            spawnNpc(entry.getKey(), "melhorias", base.upgrades(), room.mode().teamSize() > 1 ? settings.holograms().upgradesTeam() : settings.holograms().upgrades());
        }
        room.arena().diamonds().forEach(point -> forges.shared(location(point), Material.DIAMOND));
        room.arena().emeralds().forEach(point -> forges.shared(location(point), Material.EMERALD));
        for (int index = 0; index < room.arena().extraShops().size(); index++) {
            spawnNpc(null, "loja-extra-" + index, room.arena().extraShops().get(index), settings.holograms().shop());
        }
        for (int index = 0; index < room.arena().extraUpgrades().size(); index++) {
            spawnNpc(null, "melhorias-extra-" + index, room.arena().extraUpgrades().get(index), settings.holograms().upgrades());
        }
        room.arena().extraIron().forEach(point -> forges.island(null, location(point), Material.IRON_INGOT, settings.forges().iron(1)));
        room.arena().extraGold().forEach(point -> forges.island(null, location(point), Material.GOLD_INGOT, settings.forges().gold()));
        for (Player player : online()) {
            room.player(player.getUniqueId()).ifPresent(roomPlayer -> spawn(player, roomPlayer));
            healthInTab(player);
            core.nameTags().refresh(player);
        }
        room.teams().keySet().forEach(color -> {
            if (room.status(color).isEliminated()) {
                announcedEliminations.add(color);
            }
        });
        refreshScoreboards();
    }

    public void die(Player victim, boolean voidDeath) {
        Optional<RoomPlayer> found = room.player(victim.getUniqueId());
        if (found.isEmpty() || !found.get().isAlive()) {
            return;
        }
        RoomPlayer dead = found.get();
        RoomTeam team = room.team(dead.team());
        boolean finalKill = !team.bedAlive();
        Instant now = core.clock().instant();
        Optional<RoomPlayer> killer = room.credit().killer(dead.id(), now, finalKill, team.bedBreaker())
            .flatMap(room::player)
            .filter(player -> player.team() != dead.team());
        String victimName = coloredName(dead);
        String key;
        if (killer.isPresent()) {
            key = finalKill ? "final-kill" : "kill";
        } else if (voidDeath) {
            key = finalKill ? "void-final-death" : "void-death";
        } else {
            key = finalKill ? "final-death" : "death";
        }
        broadcast(messages.get(key, "victim", victimName, "killer", killer.map(this::coloredName).orElse("")));
        Map<Material, Integer> loot = resources(victim);
        killer.ifPresent(credited -> {
            room.credit(credited, finalKill);
            Player bukkit = Bukkit.getPlayer(credited.id());
            if (bukkit != null) {
                giveLoot(bukkit, loot);
                bukkit.sendMessage(messages.get("xp", "xp", String.valueOf(finalKill ? settings.xp().finalKill() : settings.xp().regularKill())));
            }
        });
        room.die(dead, settings.timings().respawnSeconds());
        reset(victim, GameMode.SPECTATOR);
        victim.teleport(location(room.arena().spectator()));
        if (dead.state() == State.RESPAWNING) {
            Title.of(messages.get("died-title"), messages.get("respawn-subtitle", "seconds", String.valueOf(settings.timings().respawnSeconds()))).send(victim);
        } else {
            Title.of(messages.get("died-title"), messages.get("spectator-subtitle")).send(victim);
            checkEliminations();
        }
        refreshScoreboards();
    }

    public boolean breakBed(Player breaker, Block block) {
        Optional<TeamColor> owner = bedOwner(block);
        if (owner.isEmpty()) {
            return false;
        }
        RoomPlayer player = room.player(breaker.getUniqueId()).orElse(null);
        if (player == null || owner.get() == player.team()) {
            breaker.sendMessage(messages.get("own-bed"));
            return true;
        }
        if (!room.team(owner.get()).bedAlive()) {
            return true;
        }
        List<RoomPlayer> lost = room.breakBed(owner.get(), player);
        removeBed(owner.get());
        broadcast(messages.get("bed-destroyed", "team-color", Colors.translate(owner.get().color()), "team", owner.get().displayName(), "player", coloredName(player)));
        breaker.sendMessage(messages.get("xp", "xp", String.valueOf(settings.xp().bedDestroyed())));
        for (RoomPlayer gone : lost) {
            room.credit(player, true);
            broadcast(messages.get("final-kill", "victim", coloredName(gone), "killer", coloredName(player)));
        }
        checkEliminations();
        refreshScoreboards();
        return true;
    }

    public Optional<TeamColor> bedOwner(Block block) {
        if (block.getType() != Material.BED_BLOCK) {
            return Optional.empty();
        }
        for (Map.Entry<TeamColor, TeamBase> entry : room.arena().teams().entrySet()) {
            Location bed = location(entry.getValue().bed());
            if (bed.getBlockX() == block.getX() && bed.getBlockY() == block.getY() && bed.getBlockZ() == block.getZ()) {
                return Optional.of(entry.getKey());
            }
            for (BlockFace side : SIDES) {
                Block next = block.getRelative(side);
                if (next.getX() == bed.getBlockX() && next.getY() == bed.getBlockY() && next.getZ() == bed.getBlockZ()) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    void quit(Player player) {
        Optional<RoomPlayer> found = room.player(player.getUniqueId());
        forget(player);
        if (found.isEmpty()) {
            return;
        }
        RoomPlayer quitter = found.get();
        switch (room.phase()) {
            case WAITING, STARTING -> removeWaiting(player);
            case ENDING -> {
            }
            case PLAYING -> {
                if (!quitter.isPlaying()) {
                    return;
                }
                RoomTeam team = room.team(quitter.team());
                if (team.bedAlive()) {
                    room.disconnected(quitter, core.clock().instant().plusSeconds(settings.timings().rejoinSeconds()));
                } else {
                    Optional<RoomPlayer> killer = room.credit().killer(quitter.id(), core.clock().instant(), true, team.bedBreaker())
                        .flatMap(room::player).filter(other -> other.team() != quitter.team());
                    room.eliminate(quitter);
                    killer.ifPresent(credited -> room.credit(credited, true));
                    broadcast(messages.get(killer.isPresent() ? "final-kill" : "final-death", "victim", coloredName(quitter), "killer", killer.map(this::coloredName).orElse("")));
                    checkEliminations();
                }
                refreshScoreboards();
            }
        }
    }

    public boolean leavingWithBed(Player player) {
        return room.phase() == Room.Phase.PLAYING && room.player(player.getUniqueId())
            .filter(RoomPlayer::isPlaying)
            .map(roomPlayer -> room.team(roomPlayer.team()).bedAlive())
            .orElse(false);
    }

    boolean canReturn(UUID player, Instant now) {
        return room.phase() == Room.Phase.PLAYING && room.player(player)
            .filter(roomPlayer -> roomPlayer.state() == State.DISCONNECTED)
            .filter(roomPlayer -> roomPlayer.disconnectedUntil().isAfter(now))
            .filter(roomPlayer -> room.team(roomPlayer.team()).bedAlive())
            .isPresent();
    }

    void rejoin(Player player) {
        RoomPlayer roomPlayer = room.player(player.getUniqueId()).orElseThrow();
        room.reconnected(roomPlayer, settings.timings().respawnSeconds());
        reset(player, GameMode.SPECTATOR);
        player.teleport(location(room.arena().spectator()));
        sidebars.put(player.getUniqueId(), Sidebar.create(player));
        healthInTab(player);
        core.nameTags().refresh(player);
        broadcast(messages.get("reconnected", "player", coloredName(roomPlayer)));
        refreshScoreboards();
    }

    public void placed(Block block) {
        room.placed(block.getX(), block.getY(), block.getZ());
    }

    public boolean mayPlace(Location at) {
        if (room.arena().buildLimit().isPresent() && at.getBlockY() > room.arena().buildLimit().getAsInt()) {
            return false;
        }
        int spawn = settings.timings().spawnRadius();
        int npc = settings.timings().npcRadius();
        int generator = settings.timings().generatorRadius();
        for (TeamBase base : room.arena().teams().values()) {
            if (near(at, base.spawn(), spawn, -2, 5) || near(at, base.shop(), npc, -1, 4) || near(at, base.upgrades(), npc, -1, 4)) {
                return false;
            }
            for (Point point : base.iron()) {
                if (near(at, point, generator, -2, 5)) {
                    return false;
                }
            }
            for (Point point : base.gold()) {
                if (near(at, point, generator, -2, 5)) {
                    return false;
                }
            }
        }
        for (Point point : room.arena().diamonds()) {
            if (near(at, point, generator, -2, 5)) {
                return false;
            }
        }
        for (Point point : room.arena().emeralds()) {
            if (near(at, point, generator, -2, 5)) {
                return false;
            }
        }
        return true;
    }

    public boolean belowVoid(Location at) {
        return at.getY() <= room.arena().voidY();
    }

    public Location waitingSpawn() {
        return location(room.arena().waitingSpawn());
    }

    public Location spectatorSpawn() {
        return location(room.arena().spectator());
    }

    public Optional<String> npcRoom(String npcId) {
        return npcs.contains(npcId) ? Optional.of(room.code()) : Optional.empty();
    }

    public Optional<TeamColor> npcTeam(String npcId) {
        for (TeamColor color : room.teams().keySet()) {
            if (npcId.startsWith(room.code() + "-" + color.id() + "-")) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    public RoomForges forges() {
        return forges;
    }

    public void upgraded(RoomTeam team, RoomTeam.Upgrade upgrade, int level) {
        for (RoomPlayer member : room.members(team.color())) {
            Player player = Bukkit.getPlayer(member.id());
            if (player == null || !member.isAlive()) {
                continue;
            }
            switch (upgrade) {
                case SHARPNESS -> Kits.sharpen(player, team);
                case PROTECTION -> Kits.protect(player, team);
                case HASTE -> Kits.effects(player, team);
                default -> {
                }
            }
        }
        if (upgrade == RoomTeam.Upgrade.FORGE) {
            TeamBase base = room.arena().teams().get(team.color());
            forges.forgeLevel(team.color(), level, base.iron().isEmpty() ? null : location(base.iron().getFirst()));
        }
    }

    private void islandEffects() {
        int radius = settings.timings().islandRadius();
        for (RoomTeam team : room.teams().values()) {
            Location center = location(room.arena().teams().get(team.color()).spawn());
            boolean heal = team.level(RoomTeam.Upgrade.HEAL_POOL) > 0;
            boolean blindTrap = team.level(RoomTeam.Upgrade.TRAP_BLINDNESS) > 0;
            int fatigueTrap = team.level(RoomTeam.Upgrade.TRAP_FATIGUE);
            if (!heal && !blindTrap && fatigueTrap == 0) {
                continue;
            }
            for (Player player : online()) {
                RoomPlayer roomPlayer = room.player(player.getUniqueId()).orElse(null);
                if (roomPlayer == null || !roomPlayer.isAlive() || player.getLocation().distanceSquared(center) > (double) radius * radius) {
                    continue;
                }
                if (roomPlayer.team() == team.color()) {
                    if (heal) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false), true);
                    }
                    continue;
                }
                if (!team.bedAlive()) {
                    continue;
                }
                if (blindTrap) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0), true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 160, 0), true);
                    team.upgrade(RoomTeam.Upgrade.TRAP_BLINDNESS, 0);
                    alert(team);
                    blindTrap = false;
                }
                if (fatigueTrap > 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, fatigueTrap == 1 ? 140 : 200, 0), true);
                    team.upgrade(RoomTeam.Upgrade.TRAP_FATIGUE, 0);
                    alert(team);
                    fatigueTrap = 0;
                }
            }
        }
    }

    private void alert(RoomTeam team) {
        for (RoomPlayer member : room.members(team.color())) {
            Player player = Bukkit.getPlayer(member.id());
            if (player != null) {
                player.sendMessage(messages.get("trap-triggered"));
            }
        }
    }

    public void refreshScoreboards() {
        for (Player player : online()) {
            Sidebar sidebar = sidebars.get(player.getUniqueId());
            if (sidebar == null) {
                continue;
            }
            sidebar.title(Colors.translate(view.title()));
            sidebar.lines(view.lines(room, player.getUniqueId(), 0).stream().map(Colors::translate).toList());
        }
    }

    void close() {
        Location fallback = Bukkit.getWorlds().getFirst().getSpawnLocation();
        for (Player player : online()) {
            forget(player);
            player.teleport(fallback);
            core.nameTags().refresh(player);
        }
        npcs.forEach(service.npcs()::remove);
        npcs.clear();
        forges.clear();
        service.closed(this);
    }

    private void playingSecond() {
        room.tick();
        Instant now = core.clock().instant();
        for (Event event : settings.timeline().dueAt(room.elapsed())) {
            runEvent(event);
            if (room.phase() != Room.Phase.PLAYING) {
                return;
            }
        }
        for (RoomPlayer player : room.players()) {
            Player bukkit = Bukkit.getPlayer(player.id());
            if (player.state() == State.RESPAWNING && bukkit != null) {
                int left = player.respawnAt() - room.elapsed();
                if (left <= 0) {
                    room.respawned(player, now.plus(Duration.ofSeconds(settings.timings().respawnProtectionSeconds())));
                    spawn(bukkit, player);
                } else {
                    Title.of(messages.get("died-title"), messages.get("respawn-subtitle", "seconds", String.valueOf(left))).send(bukkit);
                }
            } else if (player.state() == State.DISCONNECTED && player.disconnectedUntil().isBefore(now)) {
                room.eliminate(player);
                broadcast(messages.get("final-death", "victim", coloredName(player)));
                checkEliminations();
            }
        }
        if (room.phase() == Room.Phase.PLAYING) {
            islandEffects();
            forges.holograms(online(), seconds % RESEND_EVERY == 0);
            refreshScoreboards();
        }
    }

    private void runEvent(Event event) {
        switch (event.kind()) {
            case DIAMOND_TIER -> {
                forges.tier(Material.DIAMOND, event.tier());
                broadcast(messages.get("generator-upgrade", "type", messages.get("generator-diamond"), "tier", BedWarsSettings.roman(event.tier())));
            }
            case EMERALD_TIER -> {
                forges.tier(Material.EMERALD, event.tier());
                broadcast(messages.get("generator-upgrade", "type", messages.get("generator-emerald"), "tier", BedWarsSettings.roman(event.tier())));
            }
            case BEDS_DESTROYED -> {
                for (TeamColor color : room.teams().keySet()) {
                    if (room.team(color).bedAlive()) {
                        room.breakBed(color, null).forEach(lost -> broadcast(messages.get("final-death", "victim", coloredName(lost))));
                        removeBed(color);
                    }
                }
                broadcast(messages.get("beds-destroyed-event"));
                checkEliminations();
            }
            case DRAGONS -> broadcast(messages.get("dragons-event"));
            case GAME_END -> end(room.winner());
        }
    }

    private void checkEliminations() {
        for (TeamColor color : room.teams().keySet()) {
            if (room.status(color).isEliminated() && announcedEliminations.add(color)) {
                broadcast(messages.get("team-eliminated", "team-color", Colors.translate(color.color()), "team", color.displayName()));
            }
        }
        if (room.phase() == Room.Phase.PLAYING && room.standing().size() <= 1) {
            end(room.winner());
        }
    }

    private void end(Optional<TeamColor> winner) {
        room.end();
        for (Player player : online()) {
            forges.hide(player.getUniqueId());
        }
        String time = clock(room.elapsed());
        List<String> lines;
        if (winner.isPresent()) {
            lines = new ArrayList<>();
            for (String line : messages.lines("result", "team-color", Colors.translate(winner.get().color()), "team", winner.get().displayName(), "time", time)) {
                if (line.contains("{top-final-kills}")) {
                    lines.addAll(rank(score(RoomPlayer::finalKills)));
                } else if (line.contains("{top-beds}")) {
                    lines.addAll(rank(score(RoomPlayer::beds)));
                } else {
                    lines.add(line);
                }
            }
        } else {
            lines = messages.lines("result-draw", "time", time);
        }
        for (Player player : online()) {
            ChatLines.send(player, lines, CenteredText.DEFAULT_CENTER, "");
        }
        refreshScoreboards();
    }

    private Map<UUID, Integer> score(ToIntFunction<RoomPlayer> stat) {
        Map<UUID, Integer> scores = new HashMap<>();
        room.players().forEach(player -> scores.put(player.id(), stat.applyAsInt(player)));
        return scores;
    }

    private List<String> rank(Map<UUID, Integer> scores) {
        List<Ranking.Entry> top = Ranking.top(scores, room.joinOrder());
        if (top.isEmpty()) {
            return messages.lines("result-nobody");
        }
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < top.size(); index++) {
            Ranking.Entry entry = top.get(index);
            String name = room.player(entry.player()).map(RoomPlayer::name).orElse("?");
            String colored = Colors.translate(core.permissions().display(entry.player()).color()) + name;
            lines.addAll(messages.lines("result-rank-" + (index + 1), "player", colored, "score", String.valueOf(entry.score())));
        }
        return lines;
    }

    private void spawn(Player player, RoomPlayer roomPlayer) {
        TeamBase base = room.arena().teams().get(roomPlayer.team());
        reset(player, GameMode.SURVIVAL);
        player.teleport(location(base.spawn()));
        Kits.spawnKit(player, roomPlayer, room.team(roomPlayer.team()));
    }

    private void spawnNpc(TeamColor team, String kind, Point point, List<String> lines) {
        String id = room.code() + "-" + (team == null ? "neutro" : team.id()) + "-" + kind;
        String color = team == null ? "&f" : team.color();
        service.npcLines().put(id, lines.stream().map(line -> line.replace("{color}", color)).toList());
        service.npcs().put(new Npc(id, location(point), Optional.empty()));
        npcs.add(id);
    }

    private void removeBed(TeamColor color) {
        Block bed = location(room.arena().teams().get(color).bed()).getBlock();
        if (bed.getType() != Material.BED_BLOCK) {
            return;
        }
        for (BlockFace side : SIDES) {
            Block next = bed.getRelative(side);
            if (next.getType() == Material.BED_BLOCK) {
                next.setType(Material.AIR, false);
            }
        }
        bed.setType(Material.AIR, false);
    }

    private void clearWaitingArea() {
        List<Point> corners = room.arena().waitingArea();
        if (corners.size() < 2) {
            return;
        }
        Location a = location(corners.get(0));
        Location b = location(corners.get(1));
        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
        List<int[]> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(new int[] {x, y, z});
                }
            }
        }
        service.batch(blocks, BLOCKS_PER_TICK, block -> {
            Block target = world.getBlockAt(block[0], block[1], block[2]);
            if (target.getType() != Material.AIR) {
                target.setType(Material.AIR, false);
            }
        });
    }

    private void countdownChanged(Change change) {
        if (change == Change.STARTED || change == Change.SHORTENED) {
            room.countdown().remaining().ifPresent(remaining -> settings.countdownTitles().at(remaining).ifPresent(this::title));
        }
    }

    private void title(String text) {
        Title title = Title.builder().title(Colors.translate(text)).subtitle("").timings(0, 25, 5).build();
        online().forEach(title::send);
    }

    private void healthInTab(Player player) {
        Sidebar sidebar = sidebars.get(player.getUniqueId());
        Scoreboard board = sidebar == null ? player.getScoreboard() : sidebar.scoreboard();
        Objective health = board.getObjective(HEALTH_OBJECTIVE);
        if (health == null) {
            health = board.registerNewObjective(HEALTH_OBJECTIVE, "health");
            health.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }

    private static Map<Material, Integer> resources(Player player) {
        Map<Material, Integer> found = new EnumMap<>(Material.class);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && LOOT.containsKey(item.getType())) {
                found.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return found;
    }

    private void giveLoot(Player killer, Map<Material, Integer> loot) {
        loot.forEach((material, amount) -> {
            Map<Integer, ItemStack> left = killer.getInventory().addItem(new ItemStack(material, amount));
            left.values().forEach(item -> killer.getWorld().dropItem(killer.getLocation(), item));
            String type = LOOT.get(material);
            killer.sendMessage(messages.get("loot-" + type + (amount == 1 ? "-one" : "-many"), "amount", String.valueOf(amount)));
        });
    }

    private static void reset(Player player, GameMode mode) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(mode);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setExp(0);
        player.setLevel(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Colors.translate(name));
        item.setItemMeta(meta);
        return item;
    }

    private boolean near(Location at, Point point, int radius, int below, int above) {
        Location center = location(point);
        int dx = Math.abs(at.getBlockX() - center.getBlockX());
        int dz = Math.abs(at.getBlockZ() - center.getBlockZ());
        int dy = at.getBlockY() - center.getBlockY();
        return dx <= radius && dz <= radius && dy >= below && dy <= above;
    }

    static String clock(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
