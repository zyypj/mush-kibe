package br.com.muxe.core.bukkit.placement;

import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.common.placement.domain.HeldItem;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlacementImport {

    public static final String FILE = "import.yml";
    public static final String DONE_SUFFIX = ".done";

    public record Outcome(boolean fileFound, int imported) {

        public static final Outcome NO_FILE = new Outcome(false, 0);
    }

    private PlacementImport() {
    }

    public static List<Placement> read(ConfigurationSection root, String group, Logger logger) {
        List<Placement> placements = new ArrayList<>();
        ConfigurationSection npcs = root.getConfigurationSection("npcs");
        if (npcs != null) {
            for (String id : npcs.getKeys(false)) {
                ConfigurationSection npc = npcs.getConfigurationSection(id);
                try {
                    placements.add(npc(group, id, npc));
                } catch (RuntimeException exception) {
                    logger.warning("Skipping NPC '" + id + "' of " + FILE + ": " + exception.getMessage());
                }
            }
        }
        ConfigurationSection registers = root.getConfigurationSection("registers");
        if (registers != null) {
            for (String id : registers.getKeys(false)) {
                ConfigurationSection register = registers.getConfigurationSection(id);
                try {
                    placements.add(new RegisterPlacement(group, id, position(register, "location").orElseThrow(() -> new IllegalArgumentException("missing location")),
                        position(register, "seller")));
                } catch (RuntimeException exception) {
                    logger.warning("Skipping register '" + id + "' of " + FILE + ": " + exception.getMessage());
                }
            }
        }
        ConfigurationSection parkour = root.getConfigurationSection("parkour");
        if (parkour != null) {
            parkourPoint(parkour, "start", ParkourPoint.start(), group, logger).ifPresent(placements::add);
            ConfigurationSection checkpoints = parkour.getConfigurationSection("checkpoints");
            if (checkpoints != null) {
                for (String number : checkpoints.getKeys(false)) {
                    try {
                        parkourPoint(checkpoints, number, ParkourPoint.checkpoint(Integer.parseInt(number)), group, logger).ifPresent(placements::add);
                    } catch (RuntimeException exception) {
                        logger.warning("Skipping parkour checkpoint '" + number + "' of " + FILE + ": " + exception.getMessage());
                    }
                }
            }
            parkourPoint(parkour, "end", ParkourPoint.end(), group, logger).ifPresent(placements::add);
        }
        return placements;
    }

    public static CompletableFuture<List<Placement>> withSkins(List<Placement> placements, Function<String, CompletableFuture<Optional<SkinTexture>>> skins, Logger logger) {
        List<CompletableFuture<Placement>> filled = new ArrayList<>(placements.size());
        for (Placement placement : placements) {
            if (placement instanceof NpcPlacement npc && npc.skin().isEmpty() && !npc.skinName().isBlank()) {
                filled.add(skins.apply(npc.skinName())
                    .<Placement>thenApply(found -> {
                        if (found.isEmpty()) {
                            logger.warning("NPC '" + npc.id() + "' of " + FILE + ": skin '" + npc.skinName() + "' not found, it keeps the default skin");
                        }
                        return npc.wearing(npc.skinName(), found);
                    })
                    .exceptionally(error -> {
                        logger.warning("NPC '" + npc.id() + "' of " + FILE + ": could not fetch skin '" + npc.skinName() + "', it keeps the default skin: " + error);
                        return npc;
                    }));
            } else {
                filled.add(CompletableFuture.completedFuture(placement));
            }
        }
        return CompletableFuture.allOf(filled.toArray(CompletableFuture[]::new))
            .thenApply(ignored -> filled.stream().map(CompletableFuture::join).toList());
    }

    public static CompletableFuture<Outcome> run(File dataFolder, PlacementSync sync, Function<String, CompletableFuture<Optional<SkinTexture>>> skins, Logger logger) {
        File file = new File(dataFolder, FILE);
        if (!file.isFile()) {
            return CompletableFuture.completedFuture(Outcome.NO_FILE);
        }
        List<Placement> read = read(YamlConfiguration.loadConfiguration(file), sync.group(), logger);
        return withSkins(read, skins, logger).thenCompose(placements -> save(placements, sync, file, dataFolder, logger));
    }

    private static CompletableFuture<Outcome> save(List<Placement> placements, PlacementSync sync, File file, File dataFolder, Logger logger) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (Placement placement : placements) {
            chain = chain.thenCompose(ignored -> sync.save(placement)).thenAccept(result -> {
                if (result.isFailure()) {
                    throw new IllegalStateException("Could not import " + placement.kind() + " '" + placement.id() + "'");
                }
            });
        }
        return chain.whenComplete((ignored, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "The import of " + FILE + " stopped; it will run again on the next start", error);
                return;
            }
            File done = new File(dataFolder, FILE + DONE_SUFFIX);
            if (done.exists() && !done.delete() || !file.renameTo(done)) {
                logger.warning("Imported " + FILE + " but could not rename it; delete it to avoid importing it again");
            }
            logger.info("Imported " + placements.size() + " NPCs, registers and parkour points from " + FILE + " into group '" + sync.group() + "'");
        }).thenApply(ignored -> new Outcome(true, placements.size()));
    }

    private static NpcPlacement npc(String group, String id, ConfigurationSection section) {
        Optional<SkinTexture> skin = Optional.ofNullable(section.getString("skin.value"))
            .flatMap(value -> Optional.ofNullable(section.getString("skin.signature")).map(signature -> new SkinTexture(value, signature)));
        return new NpcPlacement(
            group,
            id,
            position(section, "location").orElseThrow(() -> new IllegalArgumentException("missing location")),
            section.getString("name", id),
            NpcAction.parse(section.getString("group", NpcAction.NONE_WORD)),
            section.getString("skin-name", ""),
            skin,
            section.getStringList("lines"),
            hand(section)
        );
    }

    private static Optional<HeldItem> hand(ConfigurationSection section) {
        String material = section.getString("hand.material", "");
        if (material.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new HeldItem(material, section.getInt("hand.data", 0)));
    }

    private static Optional<ParkourPointPlacement> parkourPoint(ConfigurationSection section, String key, ParkourPoint point, String group, Logger logger) {
        if (!section.isConfigurationSection(key)) {
            return Optional.empty();
        }
        try {
            return position(section, key).map(position -> new ParkourPointPlacement(group, point, position));
        } catch (RuntimeException exception) {
            logger.warning("Skipping parkour point '" + point.id() + "' of " + FILE + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Position> position(ConfigurationSection section, String path) {
        return StoredLocation.from(new BukkitConfigSource(section), path)
            .map(stored -> new Position(stored.world(), stored.x(), stored.y(), stored.z(), stored.yaw(), stored.pitch()));
    }
}
