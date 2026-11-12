package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.core.bukkit.mission.tracker.MissionTracker;
import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.cosmetic.message.KillAnnouncer;
import br.com.muxe.minigame.api.cosmetic.message.LifetimeTally;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant.State;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.Death.Cause;
import br.com.muxe.minigame.api.runtime.Death;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.timeline.Timeline;
import br.com.muxe.minigame.api.timeline.Timeline.Upcoming;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena.TeamBase;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.bedwars.custom.CustomRules;
import br.com.muxe.minigame.bedwars.custom.CustomShop;
import br.com.muxe.minigame.bedwars.game.BedDefense;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent;
import br.com.muxe.minigame.bedwars.game.ChestAccess;
import br.com.muxe.minigame.bedwars.game.ChestDeposits;
import br.com.muxe.minigame.bedwars.game.CompactTower;
import br.com.muxe.minigame.bedwars.game.Cooldowns;
import br.com.muxe.minigame.bedwars.game.ForgeHeights;
import br.com.muxe.minigame.bedwars.game.ForgeLayout;
import br.com.muxe.minigame.bedwars.game.Levels;
import br.com.muxe.minigame.bedwars.level.LevelBar;
import br.com.muxe.minigame.bedwars.lobby.LevelChat;
import br.com.muxe.minigame.bedwars.mission.MissionRules;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.settings.MatchSounds;
import br.com.muxe.minigame.bedwars.shop.ResourcePool;
import br.com.muxe.minigame.bedwars.stats.application.command.RecordMatchCommand;
import br.com.muxe.minigame.bedwars.stats.application.query.FindPlayerStatsQuery;
import br.com.muxe.minigame.bedwars.stats.domain.MatchOutcome;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.setup.TeamPicker;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class BedWarsMatch implements MatchState {

    public static final String GAME = "bedwars";

    private static final int RESEND_EVERY = 3;
    private static final Map<Material, String> LOOT = new LinkedHashMap<>();
    private static final int BED_SEARCH = 2;
    private static final double CHEST_LABEL_RANGE = 48;
    private static final double CHEST_LABEL_GAP = 0.28125;
    private static final double CHEST_LABEL_TOP = 1.25;
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    static {
        LOOT.put(Material.IRON_INGOT, "iron");
        LOOT.put(Material.GOLD_INGOT, "gold");
        LOOT.put(Material.DIAMOND, "diamond");
        LOOT.put(Material.EMERALD, "emerald");
    }

    private final MatchRuntime<BedWarsMatch> runtime;
    private final BedWarsServices services;
    private final MuxeCore core;
    private final BedWarsSettings settings;
    private final MessageCatalog messages;
    private final Match match;
    private final Room room;
    private final RoomForges forges;
    private final KillAnnouncer announcer;
    private final LifetimeTally tally = new LifetimeTally();
    private final Map<UUID, BedbugHit> bedbugHits = new HashMap<>();
    private final List<String> npcs = new ArrayList<>();
    private final Map<UUID, Inventory> enderChests = new HashMap<>();
    private final Map<UUID, Long> storedXp = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Block, SpawnedHologram>> chestLabels = new HashMap<>();
    private List<Block> islandChests = List.of();
    private CustomRules rules = CustomRules.NONE;
    private CustomShop customShop;
    private final Map<TeamColor, Set<BedDefense.Pos>> defenses = new HashMap<>();
    private Timeline<BedWarsEvent> timeline;
    private int skipped;
    private MissionTracker missions;
    private final Cooldowns towerCooldowns = new Cooldowns(CompactTower.COOLDOWN);
    private final Set<Long> towerBlocks = new HashSet<>();
    private final List<BukkitTask> towers = new ArrayList<>();

    public BedWarsMatch(MatchRuntime<BedWarsMatch> runtime, BedWarsServices services, BedWarsArena arena) {
        this.runtime = runtime;
        this.services = services;
        this.core = services.core();
        this.settings = services.settings();
        this.messages = services.messages();
        this.match = runtime.match();
        this.room = new Room(match, arena);
        this.forges = new RoomForges(services.plugin(), services.localized(), services.languages(), services.displays());
        this.timeline = settings.timeline();
        this.announcer = new KillAnnouncer(services.messageSets(), id -> services.killMessages().of(id, match.code(), ThreadLocalRandom.current()), tally);
    }

    public Room room() {
        return room;
    }

    public MatchRuntime<BedWarsMatch> runtime() {
        return runtime;
    }

    public RoomForges forges() {
        return forges;
    }

    public CustomRules rules() {
        return rules;
    }

    public CustomShop shop() {
        if (customShop == null) {
            customShop = new CustomShop(rules);
        }
        return customShop;
    }

    @Override
    public void arrived(UUID player) {
        loadLevel(player);
    }

    @Override
    public void left(UUID player) {
        hideChestLabels(player);
        forges.hide(player);
        services.levelUps().forget(player);
    }

    @Override
    public void everySecond() {
        match.players().forEach(player -> showLevel(player.id()));
    }

    @Override
    public void fastTick() {
        forges.cycle();
    }

    @Override
    public void starting() {
        rules = CustomRules.of(runtime.control().options());
        customShop = null;
        timeline = rules.timeline(settings.timeline());
        skipped = 0;
        forges.rules(rules);
        missions = null;
        if (!rules.custom()) {
            missions = new MissionTracker(core, GAME, services.missions(), "room " + match.code(), services.plugin().getLogger());
            missions.begin(match.players().stream().map(Participant::id).toList());
        }
        room.emptyIslands().forEach(this::removeBed);
        BedWarsArena arena = room.arena();
        for (TeamColor color : room.islands()) {
            TeamBase base = arena.teams().get(color);
            base.iron().forEach(point -> forges.island(color, runtime.location(point), Material.IRON_INGOT, settings.forges().iron(match.mode()),
                layout(point, base.spawn())));
            base.gold().forEach(point -> forges.island(color, runtime.location(point), Material.GOLD_INGOT, settings.forges().gold(match.mode().id())));
            boolean teams = match.mode().teamSize() > 1;
            spawnNpc(color, "loja", base.shop(), services.localized().map(texts -> teams ? texts.holograms().shopTeam() : texts.holograms().shop()));
            spawnNpc(color, "melhorias", base.upgrades(), services.localized().map(texts -> teams ? texts.holograms().upgradesTeam() : texts.holograms().upgrades()));
        }
        arena.diamonds().forEach(point -> forges.shared(runtime.location(point), Material.DIAMOND));
        arena.emeralds().forEach(point -> forges.shared(runtime.location(point), Material.EMERALD));
        for (int index = 0; index < arena.extraShops().size(); index++) {
            spawnNpc(null, "loja-extra-" + index, arena.extraShops().get(index), services.localized().map(texts -> texts.holograms().shop()));
        }
        for (int index = 0; index < arena.extraUpgrades().size(); index++) {
            spawnNpc(null, "melhorias-extra-" + index, arena.extraUpgrades().get(index), services.localized().map(texts -> texts.holograms().upgrades()));
        }
        arena.extraIron().forEach(point -> forges.island(null, runtime.location(point), Material.IRON_INGOT, settings.forges().iron(1)));
        arena.extraGold().forEach(point -> forges.island(null, runtime.location(point), Material.GOLD_INGOT, settings.forges().gold()));
        if (rules.dragonEgg()) {
            for (TeamColor color : room.islands()) {
                if (room.bedAlive(color)) {
                    removeBed(color);
                    runtime.location(room.arena().teams().get(color).bed()).getBlock().setType(Material.DRAGON_EGG, false);
                }
            }
        }
    }

    @Override
    public void spawned(Participant participant) {
        runtime.player(participant.id()).ifPresent(player -> {
            Kits.spawnKit(player, room.player(participant.id()), room.team(participant.team()), services.settings(participant.id()).items());
            if (rules.creative()) {
                player.setGameMode(GameMode.CREATIVE);
            }
        });
    }

    @Override
    public void started() {
        findIslandChests();
    }

    @Override
    public void playing() {
        if (missions != null) {
            missions.check(id -> missionTotals(id, Optional.empty()));
        }
        for (BedWarsEvent event : timeline.dueAt(match.elapsed() + skipped)) {
            runEvent(event);
            if (match.phase() != MatchPhase.PLAYING) {
                return;
            }
        }
        islandEffects();
        forges.holograms(runtime.onlinePlayers(), runtime.seconds() % RESEND_EVERY == 0);
        updateChestLabels();
    }

    @Override
    public void died(Death death) {
        Participant victim = death.victim();
        if (death.cause() == Cause.TIMEOUT) {
            return;
        }
        if (death.cause() == Cause.QUIT) {
            death.killer().ifPresent(killer -> killer.addXp(settings.xp().finalKill()));
            return;
        }
        Instant now = core.clock().instant();
        Optional<Player> victimPlayer = runtime.player(victim.id());
        Map<Material, Integer> loot = victimPlayer.map(BedWarsMatch::resources).orElse(Map.of());
        BedbugHit bedbug = bedbugHits.remove(victim.id());
        death.killer().ifPresent(credited -> {
            if (bedbug != null && bedbug.team() == credited.team()
                && !bedbug.at().plusSeconds(services.missionRules().bedbugAssistSeconds()).isBefore(now)) {
                room.player(credited.id()).addBedbugKill();
            }
            int xp = death.finalKill() ? settings.xp().finalKill() : settings.xp().regularKill();
            credited.addXp(xp);
            runtime.player(credited.id()).ifPresent(killer -> {
                giveLoot(killer, loot);
                killer.sendMessage(messages.get(killer, "xp", "xp", String.valueOf(xp)));
                MatchSounds.play(death.finalKill() ? settings.sounds().finalKill() : settings.sounds().kill(), killer);
            });
        });
        room.died(victim);
    }

    @Override
    public Optional<Text> deathText(Death death, String victimName, Optional<String> killerName) {
        Participant victim = death.victim();
        if (death.killer().isPresent() && killerName.isPresent()) {
            return Optional.of(announcer.killText(death.killer().get().id(), killerName.get(), victim.id(), victimName, death.finalKill(), death.byVoid()));
        }
        return Optional.of(announcer.deathText(victim.id(), victimName, death.finalKill(), death.byVoid()));
    }

    @Override
    public void standingsChanged() {
        dropEnderChests();
    }

    @Override
    public void ending() {
        for (UUID id : runtime.online()) {
            hideChestLabels(id);
            forges.hide(id);
        }
    }

    @Override
    public void ended(Optional<TeamColor> winner) {
        if (missions != null) {
            missions.finish(id -> missionTotals(id, winner));
        }
        record(winner);
    }

    @Override
    public void closed() {
        towers.forEach(BukkitTask::cancel);
        towers.clear();
        npcs.forEach(services.npcs()::remove);
        npcs.clear();
        forges.clear();
        match.players().forEach(player -> services.killMessages().forget(player.id()));
    }

    @Override
    public Map<String, String> placeholders(Optional<Participant> viewer) {
        return Map.of("beds", String.valueOf(viewer.map(player -> room.player(player.id()).beds()).orElse(0)));
    }

    @Override
    public Optional<Upcoming> upcoming() {
        return timeline.next(match.elapsed() + skipped);
    }

    @Override
    public Optional<Text> skipEventText() {
        int now = match.elapsed() + skipped;
        Optional<Timeline.Entry<BedWarsEvent>> next = timeline.entries().stream().filter(entry -> entry.at() > now).findFirst();
        if (next.isEmpty()) {
            return Optional.empty();
        }
        skipped += next.get().at() - now;
        for (BedWarsEvent event : timeline.dueAt(next.get().at())) {
            runEvent(event);
            if (match.phase() != MatchPhase.PLAYING) {
                break;
            }
        }
        return Optional.of(next.get().label());
    }

    @Override
    public int score(String ranking, Participant player) {
        if (ranking.equals("beds")) {
            return room.player(player.id()).beds();
        }
        return MatchState.super.score(ranking, player);
    }

    @Override
    public Optional<Area> area() {
        return Optional.of(room.area());
    }

    @Override
    public String chatBadge(UUID player) {
        return LevelChat.badge(services.levelBadges(), chatLevel(player), services.preferences().of(player).showLevel());
    }

    public void languageChanged(UUID player) {
        hideChestLabels(player);
    }

    public boolean breakBed(Player breaker, Block block) {
        Optional<TeamColor> owner = bedOwner(block);
        if (owner.isEmpty()) {
            return false;
        }
        Participant player = match.player(breaker.getUniqueId()).orElse(null);
        if (player == null || owner.get() == player.team()) {
            breaker.sendMessage(messages.get(breaker, "own-bed"));
            return true;
        }
        if (!room.bedAlive(owner.get())) {
            return true;
        }
        if (breaker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            room.player(player.id()).addInvisibleBed();
        }
        List<Participant> lost = room.breakBed(owner.get(), player);
        Optional<BedFrame> frame = bedFrame(owner.get());
        if (block.getType() == Material.DRAGON_EGG) {
            block.setType(Material.AIR, false);
        } else {
            removeBed(owner.get());
        }
        frame.ifPresent(bed -> services.bedBreaks().broken(breaker, runtime.world(), bed, runtime.onlinePlayers()));
        MatchSounds.play(settings.sounds().bedDestroyed(), breaker);
        match.members(owner.get()).forEach(member -> Optional.ofNullable(Bukkit.getPlayer(member.id()))
            .ifPresent(victim -> MatchSounds.play(settings.sounds().bedLost(), victim)));
        runtime.broadcast(announcer.bedText(player.id(), runtime.coloredName(player), Colors.translate(owner.get().color()), match.teamText(owner.get())));
        breaker.sendMessage(messages.get(breaker, "xp", "xp", String.valueOf(settings.xp().bedDestroyed())));
        player.addXp(settings.xp().bedDestroyed());
        for (Participant gone : lost) {
            match.credit(player, true);
            player.addXp(settings.xp().finalKill());
            runtime.broadcast(announcer.killText(player.id(), runtime.coloredName(player), gone.id(), runtime.coloredName(gone), true, false));
        }
        runtime.checkEliminations();
        runtime.refreshBoards();
        return true;
    }

    public Optional<TeamColor> bedOwner(Block block) {
        if (block.getType() == Material.DRAGON_EGG && rules.dragonEgg()) {
            for (Map.Entry<TeamColor, TeamBase> entry : room.arena().teams().entrySet()) {
                Location totem = runtime.location(entry.getValue().bed());
                if (totem.getBlockX() == block.getX() && totem.getBlockY() == block.getY() && totem.getBlockZ() == block.getZ()) {
                    return Optional.of(entry.getKey());
                }
            }
            return Optional.empty();
        }
        if (block.getType() != Material.BED_BLOCK) {
            return Optional.empty();
        }
        for (Map.Entry<TeamColor, TeamBase> entry : room.arena().teams().entrySet()) {
            Location bed = runtime.location(entry.getValue().bed());
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

    public void endStonePlaced(Player placer, Block block) {
        match.player(placer.getUniqueId()).filter(player -> player.team() != null).ifPresent(player -> {
            TeamBase base = room.arena().teams().get(player.team());
            int radius = services.missionRules().endStoneRadius();
            if (base != null && runtime.location(base.bed()).distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) <= (radius + 0.5) * (radius + 0.5)) {
                room.player(player.id()).addEndStone();
            }
        });
    }

    public void bedbugHit(UUID victim, TeamColor team) {
        bedbugHits.put(victim, new BedbugHit(team, core.clock().instant()));
    }

    public boolean mayPlace(Location at) {
        BedWarsArena arena = room.arena();
        if (arena.buildLimit().isPresent() && at.getBlockY() > arena.buildLimit().getAsInt()) {
            return false;
        }
        int spawn = settings.timings().spawnRadius();
        int npc = settings.timings().npcRadius();
        int generator = settings.timings().generatorRadius();
        int island = settings.forges().fiveByFive(match.mode().id()) ? Math.max(generator, ForgeLayout.FIVE_BY_FIVE_RADIUS) : generator;
        for (TeamBase base : arena.teams().values()) {
            if (near(at, base.spawn(), spawn, -2, 5) || near(at, base.shop(), npc, -1, 4) || near(at, base.upgrades(), npc, -1, 4)) {
                return false;
            }
            for (Point point : base.iron()) {
                if (near(at, point, island, -2, 5)) {
                    return false;
                }
            }
            for (Point point : base.gold()) {
                if (near(at, point, island, -2, 5)) {
                    return false;
                }
            }
        }
        for (Point point : arena.diamonds()) {
            if (near(at, point, generator, -2, 5)) {
                return false;
            }
        }
        for (Point point : arena.emeralds()) {
            if (near(at, point, generator, -2, 5)) {
                return false;
            }
        }
        return true;
    }

    public Optional<Duration> towerCooldown(UUID player) {
        return towerCooldowns.remaining(player, core.clock().instant());
    }

    @SuppressWarnings("deprecation")
    public void buildTower(Player player, Block anchor) {
        Participant participant = match.player(player.getUniqueId()).orElseThrow();
        TeamColor color = participant.team();
        towerCooldowns.start(player.getUniqueId(), core.clock().instant());
        ForgeLayout.Facing facing = CompactTower.facing(player.getLocation().getYaw());
        byte ladder = CompactTower.ladderData(facing);
        World world = anchor.getWorld();
        int[] tick = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = services.plugin().getServer().getScheduler().runTaskTimer(services.plugin(), () -> {
            for (CompactTower.Placed placed : CompactTower.at(tick[0], anchor.getX(), anchor.getY(), anchor.getZ(), facing)) {
                Block block = world.getBlockAt(placed.x(), placed.y(), placed.z());
                if (block.getType() != Material.AIR || !mayPlace(block.getLocation())) {
                    continue;
                }
                if (placed.ladder()) {
                    block.setTypeIdAndData(Material.LADDER.getId(), ladder, false);
                } else {
                    block.setTypeIdAndData(Material.WOOL.getId(), (byte) color.woolData(), false);
                }
                match.placed(placed.x(), placed.y(), placed.z());
                towerBlocks.add(towerKey(placed.x(), placed.y(), placed.z()));
                world.playSound(block.getLocation(), Sound.CHICKEN_EGG_POP, 1f, CompactTower.SOUND_PITCH);
            }
            if (++tick[0] > CompactTower.LAST_TICK) {
                task[0].cancel();
                towers.remove(task[0]);
            }
        }, 0L, 1L);
        towers.add(task[0]);
    }

    public boolean isTowerBlock(Block block) {
        return towerBlocks.contains(towerKey(block.getX(), block.getY(), block.getZ()));
    }

    public void forgetTowerBlock(Block block) {
        towerBlocks.remove(towerKey(block.getX(), block.getY(), block.getZ()));
    }

    private static long towerKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }

    public boolean hasNpc(String npcId) {
        return npcs.contains(npcId);
    }

    public Optional<TeamColor> npcTeam(String npcId) {
        for (TeamColor color : room.islands()) {
            if (npcId.startsWith(match.code() + "-" + color.id() + "-")) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    public void upgraded(RoomTeam team, RoomTeam.Upgrade upgrade, int level) {
        for (Participant member : match.members(team.color())) {
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
            forges.forgeLevel(team.color(), level, base.iron().isEmpty() ? null : runtime.location(base.iron().getFirst()));
        }
        if (upgrade == RoomTeam.Upgrade.BED_PROTECTION) {
            defendBed(team.color(), level);
        }
    }

    @SuppressWarnings("deprecation")
    private void defendBed(TeamColor color, int level) {
        if (!room.bedAlive(color)) {
            return;
        }
        Block marked = runtime.location(room.arena().teams().get(color).bed()).getBlock();
        List<BedDefense.Pos> bed = new ArrayList<>();
        bed.add(new BedDefense.Pos(marked.getX(), marked.getY(), marked.getZ()));
        if (marked.getType() == Material.BED_BLOCK) {
            for (BlockFace side : SIDES) {
                Block next = marked.getRelative(side);
                if (next.getType() == Material.BED_BLOCK) {
                    bed.add(new BedDefense.Pos(next.getX(), next.getY(), next.getZ()));
                }
            }
        }
        Set<BedDefense.Pos> built = defenses.computeIfAbsent(color, ignored -> new HashSet<>());
        BedDefense.plan(bed, level).forEach((pos, layer) -> {
            Block block = runtime.world().getBlockAt(pos.x(), pos.y(), pos.z());
            if (block.getType() != Material.AIR && !built.contains(pos)) {
                return;
            }
            switch (layer) {
                case WOOD -> block.setType(Material.WOOD, false);
                case WOOL -> block.setTypeIdAndData(Material.WOOL.getId(), (byte) color.woolData(), false);
                case GLASS -> block.setTypeIdAndData(Material.STAINED_GLASS.getId(), (byte) color.woolData(), false);
                case END_STONE -> block.setType(Material.ENDER_STONE, false);
            }
            match.placed(pos.x(), pos.y(), pos.z());
            built.add(pos);
        });
    }

    public int chatLevel(UUID id) {
        long stored = storedXp.getOrDefault(id, 0L);
        long earned = match.player(id).map(Participant::xp).orElse(0L);
        return Levels.progress(Math.max(0, stored + earned)).level();
    }

    public ResourcePool resources(Player player, boolean teamDiamonds) {
        List<Inventory> sources = new ArrayList<>();
        sources.add(player.getInventory());
        sources.add(enderChest(player));
        match.player(player.getUniqueId()).map(Participant::team).ifPresent(team -> {
            sources.addAll(teamChests(team));
            if (teamDiamonds) {
                teammates(player, team).forEach(mate -> sources.add(mate.getInventory()));
            }
        });
        return new ResourcePool(sources);
    }

    public Inventory enderChest(Player player) {
        return enderChests.computeIfAbsent(player.getUniqueId(),
            id -> Bukkit.createInventory(null, InventoryType.ENDER_CHEST, messages.get(player, "ender-chest-title")));
    }

    public Optional<TeamColor> chestOwner(Location chest) {
        Map<TeamColor, Point> spawns = new EnumMap<>(TeamColor.class);
        room.arena().teams().forEach((color, base) -> {
            Location spawn = runtime.location(base.spawn());
            spawns.put(color, new Point(spawn.getX(), spawn.getY(), spawn.getZ(), 0, 0));
        });
        Point at = new Point(chest.getX(), chest.getY(), chest.getZ(), 0, 0);
        return ChestAccess.owner(at, spawns, settings.timings().islandRadius());
    }

    public boolean mayOpenChest(Player player, Location chest) {
        if (!match.isAlive(player.getUniqueId())) {
            return false;
        }
        if (canOpenChest(player, chest)) {
            return true;
        }
        Optional<TeamColor> owner = chestOwner(chest);
        player.sendMessage(messages.get(player, "chest-locked", "team-color", Colors.translate(owner.get().color()), "team", match.teamText(owner.get())));
        return false;
    }

    private void loadLevel(UUID id) {
        core.queries().ask(new FindPlayerStatsQuery(id)).whenComplete((stats, error) -> core.mainThread().execute(() -> {
            if (error != null) {
                services.plugin().getLogger().log(Level.WARNING, "Could not load the BedWars level of " + id, error);
                return;
            }
            storedXp.put(id, Math.max(0, stats.general().xp()));
            tally.load(id, stats.general().lifetime());
            showLevel(id);
        }));
    }

    private void showLevel(UUID id) {
        Long stored = storedXp.get(id);
        Optional<Player> player = runtime.player(id);
        if (stored == null || player.isEmpty()) {
            return;
        }
        int level = LevelBar.show(player.get(), stored + match.player(id).map(Participant::xp).orElse(0L)).level();
        services.levelUps().announce(player.get(), level);
    }

    private Optional<Map<String, Long>> missionTotals(UUID id, Optional<TeamColor> winner) {
        MissionRules rules = services.missionRules();
        return match.player(id).map(player -> room.missionTotals(player, winner, rules.fastWinSeconds(), rules.endStoneBlocks()));
    }

    private void runEvent(BedWarsEvent event) {
        switch (event.kind()) {
            case DIAMOND_TIER -> {
                forges.tier(Material.DIAMOND, event.tier());
                runtime.broadcast(generatorUpgrade(messages, Material.DIAMOND, event.tier()));
            }
            case EMERALD_TIER -> {
                forges.tier(Material.EMERALD, event.tier());
                runtime.broadcast(generatorUpgrade(messages, Material.EMERALD, event.tier()));
            }
            case BEDS_DESTROYED -> {
                for (TeamColor color : room.islands()) {
                    if (room.bedAlive(color)) {
                        room.breakBed(color, null).forEach(lost -> runtime.broadcast(announcer.deathText(lost.id(), runtime.coloredName(lost), true, false)));
                        removeBed(color);
                    }
                }
                runtime.announce("beds-destroyed-event");
                runtime.checkEliminations();
            }
            case DRAGONS -> runtime.announce("dragons-event");
            case GAME_END -> runtime.end(match.winner());
        }
    }

    static Text generatorUpgrade(MessageCatalog messages, Material generator, int tier) {
        String type = generator == Material.DIAMOND ? "generator-diamond" : "generator-emerald";
        return messages.text("generator-upgrade", "type", messages.text(type), "tier", BedWarsSettings.roman(tier));
    }

    private void islandEffects() {
        int radius = settings.timings().islandRadius();
        List<Player> online = runtime.onlinePlayers();
        for (RoomTeam team : room.teams().values()) {
            Location center = runtime.location(room.arena().teams().get(team.color()).spawn());
            boolean heal = team.level(RoomTeam.Upgrade.HEAL_POOL) > 0;
            boolean blindTrap = team.level(RoomTeam.Upgrade.TRAP_BLINDNESS) > 0;
            int fatigueTrap = team.level(RoomTeam.Upgrade.TRAP_FATIGUE);
            if (!heal && !blindTrap && fatigueTrap == 0) {
                continue;
            }
            for (Player player : online) {
                Participant participant = match.player(player.getUniqueId()).orElse(null);
                if (participant == null || !participant.isAlive() || player.getLocation().distanceSquared(center) > (double) radius * radius) {
                    continue;
                }
                if (participant.team() == team.color()) {
                    if (heal) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false), true);
                    }
                    continue;
                }
                if (!room.bedAlive(team.color())) {
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
        for (Participant member : match.members(team.color())) {
            Player player = Bukkit.getPlayer(member.id());
            if (player != null) {
                player.sendMessage(messages.get(player, "trap-triggered"));
            }
        }
    }

    private void record(Optional<TeamColor> winner) {
        List<MatchOutcome> outcomes = room.outcomes(winner, settings.xp());
        for (MatchOutcome outcome : outcomes) {
            long earnedBefore = match.player(outcome.playerId()).map(Participant::xp).orElse(0L);
            storedXp.computeIfPresent(outcome.playerId(), (id, stored) -> stored + Math.max(0, outcome.xp() - earnedBefore));
            showLevel(outcome.playerId());
        }
        if (outcomes.isEmpty() || match.matchId().isEmpty()) {
            return;
        }
        core.commands().dispatch(new RecordMatchCommand(match.matchId().get(), outcomes)).whenComplete((result, error) -> {
            if (error != null || !result.isSuccess()) {
                services.plugin().getLogger().log(Level.WARNING, "Could not save the stats of room " + match.code(), error);
            }
        });
    }

    private List<Inventory> teamChests(TeamColor team) {
        List<Inventory> inventories = new ArrayList<>();
        for (Block block : islandChests) {
            if (block.getType() != Material.ENDER_CHEST && ChestDeposits.isChest(block.getType())
                && chestOwner(center(block)).filter(team::equals).isPresent() && block.getState() instanceof Chest chest) {
                inventories.add(chest.getBlockInventory());
            }
        }
        return inventories;
    }

    private List<Player> teammates(Player player, TeamColor team) {
        List<Player> mates = new ArrayList<>();
        for (Participant member : match.members(team)) {
            Player mate = Bukkit.getPlayer(member.id());
            if (mate != null && !mate.equals(player) && member.isAlive() && mate.getWorld().equals(runtime.world())) {
                mates.add(mate);
            }
        }
        return mates;
    }

    private void findIslandChests() {
        int radius = settings.timings().islandRadius();
        World world = runtime.world();
        Set<Block> found = new LinkedHashSet<>();
        for (TeamBase base : room.arena().teams().values()) {
            Location spawn = runtime.location(base.spawn());
            for (int chunkX = (spawn.getBlockX() - radius) >> 4; chunkX <= (spawn.getBlockX() + radius) >> 4; chunkX++) {
                for (int chunkZ = (spawn.getBlockZ() - radius) >> 4; chunkZ <= (spawn.getBlockZ() + radius) >> 4; chunkZ++) {
                    for (BlockState state : world.getChunkAt(chunkX, chunkZ).getTileEntities()) {
                        if (ChestDeposits.isChest(state.getType()) && state.getLocation().distanceSquared(spawn) <= (double) radius * radius) {
                            found.add(state.getBlock());
                        }
                    }
                }
            }
        }
        islandChests = List.copyOf(found);
    }

    private void updateChestLabels() {
        for (Player viewer : runtime.onlinePlayers()) {
            Map<Block, SpawnedHologram> shown = chestLabels.computeIfAbsent(viewer.getUniqueId(), id -> new HashMap<>());
            Set<Block> wanted = new HashSet<>();
            boolean labels = services.preferences().of(viewer.getUniqueId()).chestDeposit().showsHolograms();
            if (labels && match.isAlive(viewer.getUniqueId())) {
                for (Block chest : islandChests) {
                    Location at = center(chest);
                    if (at.distanceSquared(viewer.getLocation()) <= CHEST_LABEL_RANGE * CHEST_LABEL_RANGE
                        && (chest.getType() == Material.ENDER_CHEST || canOpenChest(viewer, at))) {
                        wanted.add(chest);
                    }
                }
            }
            shown.entrySet().removeIf(entry -> {
                if (wanted.contains(entry.getKey())) {
                    return false;
                }
                entry.getValue().remove();
                return true;
            });
            for (Block chest : wanted) {
                shown.computeIfAbsent(chest, block -> PacketHologramTransport.instance().spawn(viewer,
                    TextHolograms.standard(CHEST_LABEL_GAP).layout(center(block).add(0, CHEST_LABEL_TOP, 0), messages.lines(viewer, "chest-hologram"))));
            }
        }
    }

    private void hideChestLabels(UUID id) {
        Map<Block, SpawnedHologram> shown = chestLabels.remove(id);
        if (shown != null) {
            shown.values().forEach(SpawnedHologram::remove);
        }
    }

    private boolean canOpenChest(Player player, Location chest) {
        Optional<Participant> viewer = match.player(player.getUniqueId()).filter(Participant::isAlive);
        if (viewer.isEmpty()) {
            return false;
        }
        Optional<TeamColor> owner = chestOwner(chest);
        return ChestAccess.canOpen(owner, viewer.get().team(), owner.map(color -> match.status(color).isEliminated()).orElse(true));
    }

    private void dropEnderChests() {
        for (Participant player : match.players()) {
            if (player.state() != State.ELIMINATED || player.team() == null) {
                continue;
            }
            Inventory chest = enderChests.remove(player.id());
            if (chest == null) {
                continue;
            }
            new ArrayList<>(chest.getViewers()).forEach(HumanEntity::closeInventory);
            Location at = enderChestDrop(player.team());
            for (ItemStack item : chest.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    runtime.world().dropItem(at, item).setVelocity(new Vector());
                }
            }
            chest.clear();
        }
    }

    private Location enderChestDrop(TeamColor team) {
        TeamBase base = room.arena().teams().get(team);
        Point point = base.iron().isEmpty() ? base.spawn() : base.iron().getFirst();
        return runtime.location(point).add(0, ForgeHeights.DROP_ABOVE_POINT, 0);
    }

    private void spawnNpc(TeamColor team, String kind, Point point, Localized<List<String>> lines) {
        String id = match.code() + "-" + (team == null ? "neutro" : team.id()) + "-" + kind;
        String color = team == null ? "&f" : team.color();
        services.npcLines().put(id, lines.map(texts -> texts.stream().map(line -> line.replace("{color}", color)).toList()));
        services.npcs().put(new Npc(id, runtime.location(point), Optional.empty()));
        npcs.add(id);
    }

    @SuppressWarnings("deprecation")
    private Optional<BedFrame> bedFrame(TeamColor color) {
        Block bed = runtime.location(room.arena().teams().get(color).bed()).getBlock();
        if (bed.getType() != Material.BED_BLOCK) {
            return Optional.empty();
        }
        return Optional.of(BedFrame.ofBlock(bed.getX(), bed.getY(), bed.getZ(), bed.getData()));
    }

    private ForgeLayout layout(Point forge, Point spawn) {
        if (settings.forges().fiveByFive(match.mode().id())) {
            return ForgeLayout.fiveByFive(spawn.x() - forge.x(), spawn.z() - forge.z());
        }
        Block reference = runtime.location(forge).getBlock();
        ForgeLayout layout = ForgeLayout.detect(new ForgeLayout.Blocks() {
            @Override
            public boolean slab(int dx, int dy, int dz) {
                String name = reference.getRelative(dx, dy, dz).getType().name();
                return name.contains("SLAB") || name.contains("STEP");
            }

            @Override
            public boolean solid(int dx, int dy, int dz) {
                return reference.getRelative(dx, dy, dz).getType().isSolid();
            }
        }, spawn.x() - forge.x(), spawn.z() - forge.z());
        if (layout.shape() == ForgeLayout.Shape.SINGLE) {
            services.plugin().getLogger().warning("Room " + match.code() + ": the iron forge at " + forge.describe() + " has no 2x3 or 3x3 slab floor, so it drops on one spot");
        }
        return layout;
    }

    private void removeBed(TeamColor color) {
        Point marked = room.arena().teams().get(color).bed();
        List<Point> beds = new ArrayList<>();
        for (int dx = -BED_SEARCH; dx <= BED_SEARCH; dx++) {
            for (int dy = -BED_SEARCH; dy <= BED_SEARCH; dy++) {
                for (int dz = -BED_SEARCH; dz <= BED_SEARCH; dz++) {
                    Point near = Point.block(marked.blockX() + dx, marked.blockY() + dy, marked.blockZ() + dz);
                    if (runtime.location(near).getBlock().getType() == Material.BED_BLOCK) {
                        beds.add(near);
                    }
                }
            }
        }
        Optional<Point> found = TeamPicker.nearestFree(marked, beds, List.of());
        if (found.isEmpty()) {
            services.plugin().getLogger().warning("Room " + match.code() + " found no bed of the empty team " + color.id() + " near " + marked.describe());
            return;
        }
        Block bed = runtime.location(found.get()).getBlock();
        for (BlockFace side : SIDES) {
            Block next = bed.getRelative(side);
            if (next.getType() == Material.BED_BLOCK) {
                next.setType(Material.AIR, false);
            }
        }
        bed.setType(Material.AIR, false);
    }

    private boolean near(Location at, Point point, int radius, int below, int above) {
        Location center = runtime.location(point);
        int dx = Math.abs(at.getBlockX() - center.getBlockX());
        int dz = Math.abs(at.getBlockZ() - center.getBlockZ());
        int dy = at.getBlockY() - center.getBlockY();
        return dx <= radius && dz <= radius && dy >= below && dy <= above;
    }

    private void giveLoot(Player killer, Map<Material, Integer> loot) {
        loot.forEach((material, amount) -> {
            Map<Integer, ItemStack> left = killer.getInventory().addItem(services.settings(killer.getUniqueId()).items().named(new ItemStack(material, amount)));
            left.values().forEach(item -> killer.getWorld().dropItem(killer.getLocation(), item));
            String type = LOOT.get(material);
            killer.sendMessage(messages.get(killer, "loot-" + type + (amount == 1 ? "-one" : "-many"), "amount", String.valueOf(amount)));
        });
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

    private static Location center(Block block) {
        return block.getLocation().add(0.5, 0, 0.5);
    }

    private record BedbugHit(TeamColor team, Instant at) {
    }
}
