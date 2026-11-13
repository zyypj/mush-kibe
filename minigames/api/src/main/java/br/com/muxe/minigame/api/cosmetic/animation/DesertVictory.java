package br.com.muxe.minigame.api.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.core.bukkit.cosmetic.animation.Scene;
import br.com.muxe.core.bukkit.cosmetic.animation.SceneAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.BlockChange;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.BlockType;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Mount;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Slot;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import br.com.muxe.minigame.api.cosmetic.animation.DesertRules.Patches;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class DesertVictory implements SceneAnimation {

    static final int LENGTH = 200;
    static final int WAVE_FIRST_TICK = 2;
    static final int SCAN_BELOW = 12;
    static final int SCAN_ABOVE = 28;
    static final int MAX_HEIGHT = 255;
    static final int CACTUS_FIRST_DELAY = 1;
    static final int CACTUS_GROW_DELAY = 5;
    static final int CACTUS_LAST_DELAY = 6;
    static final int CACTUS_MAX_HEIGHT = 3;
    static final double SEAT_ABOVE_GROUND = 1.78125;
    static final double VEHICLE_BELOW_SEAT = 0.78125;
    static final double CARPET_BELOW_SEAT = 0.55;
    static final int LAUNCH_FIRST = 2;
    static final int LAUNCH_EVERY = 10;
    static final int LAUNCH_LAST = 192;
    static final int WINGS_FIRST = 140;
    static final int WINGS_EVERY = 20;
    static final int WINGS_LAST = 180;
    static final double MOVING_SPEED = 0.05;
    static final double TRAIL_BEHIND = 4.4;
    static final double TRAIL_SPACING = 0.125;
    static final double TRAIL_HALF_WIDTH = 0.9;
    static final int TRAIL_DOTS = 8;
    static final int IDLE_DUST_EVERY = 6;
    static final double SPARK_CHANCE = 0.8;
    static final double PALE_DUST_CHANCE = 0.3;
    static final int RAIN_FIRST = 1;
    static final int RAIN_EVERY = 4;
    static final int RAIN_LAST = 197;
    static final double RAIN_MIN_HEIGHT = 5.1;
    static final double RAIN_MAX_HEIGHT = 9.9;
    static final double RAIN_SIDEWAYS = 0.14;
    static final double RAIN_LIFT = 0.2;
    static final int DROP_MIN_LIFE = 60;
    static final int DROP_MAX_LIFE = 90;
    static final double ORB_CHANCE = 1.0 / 7;
    static final Material[] RAIN = {
        Material.GOLD_BLOCK,
        Material.GOLD_INGOT, Material.GOLD_INGOT,
        Material.GOLD_NUGGET, Material.GOLD_NUGGET,
        Material.GOLDEN_APPLE
    };
    static final int RIPPLE_STANDS = 15;
    static final int CARPET_LENGTH = 5;
    static final double CARPET_SPACING = 0.625;
    static final int CARPET_YELLOW = 4;
    static final int CARPET_PURPLE = 10;
    static final int CARPET_RED = 14;
    static final int CARPET_CENTRE = 7;
    static final int FIRST_PURPLE = 4;
    static final int SECOND_PURPLE = 10;
    static final double RIPPLE_BASE = -1.203;
    static final double RIPPLE_AMPLITUDE = 0.0725;
    static final double RIPPLE_RATE = 0.3507;
    static final double RIPPLE_SLOPE = 1.45;
    static final double RIPPLE_PHASE = 0.846;
    static final int POND_RINGS = 3;
    static final int OASIS_MIN_DISTANCE = 10;
    static final int OASIS_SEARCH = 48;
    static final int OASIS_START_TICK = 22;
    static final int FOUNTAIN_FIRST = 40;
    static final int FOUNTAIN_LAST_NUGGET = 190;
    static final int FOUNTAIN_WATER_FIRST = 58;
    static final int FOUNTAIN_WATER_EVERY = 20;
    static final double FOUNTAIN_NUGGET_CHANCE = 0.46;
    static final double FOUNTAIN_NUGGET_LIFT = 0.45;
    static final float[] SPLASH_PITCHES = {1.40f, 1.43f, 1.44f, 1.48f};

    @Override
    public String id() {
        return "deserto";
    }

    @Override
    public void script(Scene scene, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec origin = scene.origin();
        int originX = (int) Math.floor(origin.x());
        int originY = (int) Math.floor(origin.y());
        int originZ = (int) Math.floor(origin.z());
        timeline.lasts(LENGTH);
        Drops drops = new Drops(stage, random);
        scheduleWave(stage, timeline, random, DesertRules.box(scene.area(), originX, originZ), originX, originY, originZ);
        List<int[]> oases = new ArrayList<>();
        List<Riders.Rider> riders = Riders.of(scene, stage);
        for (Riders.Rider rider : riders) {
            Vec ground = rider.ground();
            oases.add(scheduleOasis(stage, timeline, random, drops, (int) Math.floor(ground.x()), (int) Math.floor(ground.y()), (int) Math.floor(ground.z()), rider.facing(), List.copyOf(oases)));
        }
        CarpetFlight leader = null;
        for (Riders.Rider rider : riders) {
            CarpetFlight flight = scheduleCarpet(rider, stage, timeline, random, leader == null);
            leader = leader == null ? flight : leader;
        }
        CarpetFlight rainCarpet = leader;
        timeline.repeat(RAIN_FIRST, RAIN_LAST, RAIN_EVERY, tick -> rain(random, drops, rainCarpet, scene.area()));
        timeline.repeat(0, LENGTH - 1, 1, tick -> drops.step());
        timeline.at(LENGTH, drops::removeAll);
    }

    static double ripple(int tick, double forward) {
        return RIPPLE_BASE + RIPPLE_AMPLITUDE * Math.sin(RIPPLE_RATE * tick - RIPPLE_SLOPE * forward - RIPPLE_PHASE);
    }

    static int carpetColor(int stand) {
        if (stand == CARPET_CENTRE) {
            return CARPET_RED;
        }
        return stand == FIRST_PURPLE || stand == SECOND_PURPLE ? CARPET_PURPLE : CARPET_YELLOW;
    }

    static Vec standOffset(int stand) {
        double lateral = (stand / CARPET_LENGTH - 1) * CARPET_SPACING;
        double forward = (stand % CARPET_LENGTH - CARPET_LENGTH / 2) * CARPET_SPACING;
        return new Vec(lateral, 0, forward);
    }

    private static void scheduleWave(Stage stage, Timeline timeline, RandomGenerator random, int[] box, int originX, int originY, int originZ) {
        List<int[]> order = DesertRules.order(box, originX, originZ, random);
        Patches patches = new Patches(box, random);
        int bottom = Math.max(0, originY - SCAN_BELOW);
        int top = Math.min(MAX_HEIGHT, originY + SCAN_ABOVE);
        int batches = DesertRules.batches(order.size());
        timeline.repeat(WAVE_FIRST_TICK, WAVE_FIRST_TICK + batches - 1, 1, batch -> {
            List<BlockChange> changes = new ArrayList<>();
            for (int[] column : DesertRules.batch(order, batch)) {
                Scan scan = new Scan(column[0], column[1], bottom, top);
                convertColumn(stage, timeline, random, patches, scan, WAVE_FIRST_TICK + batch, changes);
            }
            if (!changes.isEmpty()) {
                stage.blocks(changes);
            }
        });
    }

    private static void convertColumn(Stage stage, Timeline timeline, RandomGenerator random, Patches patches, Scan scan, int tick, List<BlockChange> changes) {
        BlockType[] original = new BlockType[scan.top() - scan.bottom() + 1];
        for (int index = 0; index < original.length; index++) {
            original[index] = stage.type(scan.x(), scan.bottom() + index, scan.z());
        }
        BlockType[] converted = DesertRules.convert(original, scan.bottom(), patches.clay(scan.x(), scan.z()));
        for (int index = 0; index < original.length; index++) {
            if (!converted[index].equals(original[index])) {
                changes.add(new BlockChange(scan.x(), scan.bottom() + index, scan.z(), converted[index].material(), converted[index].data()));
            }
        }
        int spot = DesertRules.cactusSpot(converted, random);
        if (spot >= 0) {
            growCactus(stage, timeline, random, scan.x(), scan.bottom() + spot + 1, scan.z(), tick);
        }
    }

    private static void growCactus(Stage stage, Timeline timeline, RandomGenerator random, int x, int y, int z, int tick) {
        int height = 1 + random.nextInt(CACTUS_MAX_HEIGHT);
        int when = tick + CACTUS_FIRST_DELAY;
        for (int block = 0; block < height; block++) {
            int level = y + block;
            timeline.at(when, () -> place(stage, new Placement(0, x, level, z, Material.CACTUS, 0)));
            when += block % 2 == 0 ? CACTUS_GROW_DELAY : CACTUS_LAST_DELAY;
        }
    }

    private static void place(Stage stage, Placement placement) {
        stage.block(placement.x(), placement.y(), placement.z(), placement.material(), placement.data());
        breakEffect(stage, placement);
    }

    private static void breakEffect(Stage stage, Placement placement) {
        Vec centre = new Vec(placement.x() + 0.5, placement.y() + 0.5, placement.z() + 0.5);
        stage.particle("BLOCK_CRACK", centre, 10, new Vec(0.25, 0.25, 0.25), 0.1, Kit.blockId(placement.material(), placement.data()));
    }

    private static int[] scheduleOasis(Stage stage, Timeline timeline, RandomGenerator random, Drops drops, int originX, int originY, int originZ, Vec facing, List<int[]> others) {
        int[] centre = oasisCentre(originX, originZ, facing, others);
        Oasis oasis = new Oasis(centre[0], originY, centre[1], random);
        for (int ring = 0; ring <= Oasis.RADIUS; ring++) {
            int current = ring;
            timeline.at(Oasis.ringTick(ring), () -> buildRing(stage, oasis, current));
        }
        for (int ring = 1; ring <= POND_RINGS; ring++) {
            int current = ring;
            timeline.at(Oasis.pondTick(ring), () -> fillPond(stage, oasis, current));
        }
        for (int splash = 0; splash < SPLASH_PITCHES.length; splash++) {
            float pitch = SPLASH_PITCHES[splash];
            timeline.at(OASIS_START_TICK + 2 * splash, () -> stage.sound("random.splash", oasis.centre(), 0.6f, pitch));
        }
        Map<Integer, List<Placement>> byTick = new TreeMap<>();
        for (Placement placement : oasis.placements()) {
            byTick.computeIfAbsent(placement.tick(), tick -> new ArrayList<>()).add(placement);
        }
        byTick.forEach((tick, placements) -> timeline.at(tick, () -> placements.forEach(placement -> place(stage, placement))));
        scheduleFountain(stage, timeline, random, drops, oasis);
        return centre;
    }

    static int[] oasisCentre(int originX, int originZ, Vec facing, List<int[]> others) {
        double heading = Math.atan2(facing.z(), facing.x());
        int[] first = ahead(originX, originZ, heading, OASIS_MIN_DISTANCE);
        for (int distance = OASIS_MIN_DISTANCE; distance <= OASIS_SEARCH; distance++) {
            int[] centre = ahead(originX, originZ, heading, distance);
            if (others.stream().noneMatch(other -> Math.hypot(centre[0] - other[0], centre[1] - other[1]) < 2 * (Oasis.RADIUS + 1))) {
                return centre;
            }
        }
        return first;
    }

    private static int[] ahead(int originX, int originZ, double heading, int distance) {
        return new int[] {originX + (int) Math.round(Math.cos(heading) * distance), originZ + (int) Math.round(Math.sin(heading) * distance)};
    }

    private static void buildRing(Stage stage, Oasis oasis, int ring) {
        List<BlockChange> changes = new ArrayList<>();
        int top = oasis.startY();
        if (ring == 0) {
            for (int depth = 4; depth >= 0; depth--) {
                changes.add(new BlockChange(oasis.centreX(), top - depth, oasis.centreZ(), Material.GOLD_BLOCK, 0));
            }
            breakEffect(stage, new Placement(0, oasis.centreX(), top, oasis.centreZ(), Material.GOLD_BLOCK, 0));
        }
        for (Oasis.Column column : ring == 0 ? List.<Oasis.Column>of() : oasis.ring(ring)) {
            for (int depth = 4; depth >= 2; depth--) {
                changes.add(new BlockChange(column.x(), top - depth, column.z(), Material.SANDSTONE, 0));
            }
            Material surface = column.surface() == Oasis.Surface.GRASS ? Material.GRASS : Material.SAND;
            changes.add(new BlockChange(column.x(), top - 1, column.z(), surface, 0));
            changes.add(new BlockChange(column.x(), top, column.z(), Material.AIR, 0));
        }
        stage.blocks(changes);
        stage.sound("dig.sand", oasis.centre(), 1.5f, (float) (0.70 + 0.079 * ring));
    }

    private static void fillPond(Stage stage, Oasis oasis, int ring) {
        List<BlockChange> changes = new ArrayList<>();
        for (Oasis.Column column : oasis.water(ring)) {
            changes.add(new BlockChange(column.x(), oasis.startY() - 2, column.z(), Material.STATIONARY_WATER, 0));
            changes.add(new BlockChange(column.x(), oasis.startY() - 1, column.z(), Material.STATIONARY_WATER, 0));
        }
        stage.blocks(changes);
    }

    private static void scheduleFountain(Stage stage, Timeline timeline, RandomGenerator random, Drops drops, Oasis oasis) {
        Vec top = oasis.fountainTop();
        Vec spray = oasis.centre().add(0, 2.1, 0);
        timeline.at(FOUNTAIN_FIRST, () -> stage.sound("random.levelup", top, 1.5f, 1.49f));
        timeline.repeat(FOUNTAIN_FIRST, LENGTH - 1, 2, tick -> stage.particle("WATER_SPLASH", spray, 20, new Vec(0.15, 0.5, 0.15), 1));
        timeline.repeat(FOUNTAIN_WATER_FIRST, LENGTH - 1, FOUNTAIN_WATER_EVERY, tick -> stage.sound("liquid.water", top, 0.6f, 1.49f));
        timeline.repeat(FOUNTAIN_FIRST, FOUNTAIN_LAST_NUGGET, 2, tick -> {
            if (random.nextDouble() < FOUNTAIN_NUGGET_CHANCE) {
                Vec velocity = new Vec(Kit.between(random, -RAIN_SIDEWAYS, RAIN_SIDEWAYS), FOUNTAIN_NUGGET_LIFT, Kit.between(random, -RAIN_SIDEWAYS, RAIN_SIDEWAYS));
                drops.spawn(Material.GOLD_NUGGET, top, velocity);
            }
        });
    }

    private static CarpetFlight scheduleCarpet(Riders.Rider rider, Stage stage, Timeline timeline, RandomGenerator random, boolean leader) {
        Vec seat = rider.ground().add(0, SEAT_ABOVE_GROUND, 0);
        float yaw = rider.yaw();
        CarpetFlight flight = new CarpetFlight(seat, yaw);
        Mount[] mount = new Mount[1];
        Carpet carpet = new Carpet();
        timeline.at(0, () -> {
            mount[0] = stage.mount(seat, yaw, rider.seated());
            carpet.spawn(stage);
            carpet.place(flight);
            stage.sound("mob.enderdragon.wings", seat, 1, 1.49f);
            stage.sound("fireworks.twinkle", seat, 1, 1.19f);
            stage.particle("FIREWORKS_SPARK", seat, 20, new Vec(0.6, 0.1, 0.6), 0.1);
        });
        timeline.repeat(1, LENGTH - 1, 1, tick -> {
            flight.step(mount[0].pushing(), mount[0].lookYaw(), mount[0].lookPitch());
            mount[0].move(flight.position(), flight.yaw());
            carpet.place(flight);
            trail(stage, random, flight, tick);
        });
        if (leader) {
            timeline.repeat(LAUNCH_FIRST, LAUNCH_LAST, LAUNCH_EVERY, tick -> stage.sound("fireworks.launch", flight.position(), 3, 1));
        }
        timeline.repeat(WINGS_FIRST, WINGS_LAST, WINGS_EVERY, tick -> stage.sound("mob.enderdragon.wings", flight.position(), 0.4f, (float) Kit.between(random, 1.75, 1.83)));
        timeline.at(LENGTH, carpet::remove);
        return flight;
    }

    private static void rain(RandomGenerator random, Drops drops, CarpetFlight flight, Area area) {
        Vec vehicle = flight.position().add(0, -VEHICLE_BELOW_SEAT, 0);
        for (int copy = 0; copy < area.copies(); copy++) {
            for (Material material : RAIN) {
                Vec position = rainSpot(area, vehicle, random);
                Vec velocity = new Vec(Kit.between(random, -RAIN_SIDEWAYS, RAIN_SIDEWAYS), RAIN_LIFT, Kit.between(random, -RAIN_SIDEWAYS, RAIN_SIDEWAYS));
                drops.spawn(material, position, velocity);
            }
        }
    }

    static Vec rainSpot(Area area, Vec vehicle, RandomGenerator random) {
        return area.pick(random, vehicle.y() + Kit.between(random, RAIN_MIN_HEIGHT, RAIN_MAX_HEIGHT));
    }

    private static void trail(Stage stage, RandomGenerator random, CarpetFlight flight, int tick) {
        Vec centre = flight.position().add(0, -CARPET_BELOW_SEAT, 0);
        if (flight.speed() > MOVING_SPEED) {
            Vec direction = Flight.direction(flight.heading(), 0);
            Vec side = new Vec(-direction.z(), 0, direction.x());
            for (double lateral : new double[] {-TRAIL_HALF_WIDTH, TRAIL_HALF_WIDTH}) {
                for (int dot = 0; dot < TRAIL_DOTS; dot++) {
                    Vec at = centre.add(direction.multiply(-(TRAIL_BEHIND + dot * TRAIL_SPACING))).add(side.multiply(lateral));
                    gold(stage, random, at);
                }
            }
        } else if (tick % IDLE_DUST_EVERY == 0) {
            gold(stage, random, centre);
        }
        if (random.nextDouble() < SPARK_CHANCE) {
            stage.particle("FIREWORKS_SPARK", centre, 4, new Vec(0.1, 0.1, 0.1), 0.05);
        }
    }

    private static void gold(Stage stage, RandomGenerator random, Vec position) {
        if (random.nextDouble() < PALE_DUST_CHANCE) {
            Kit.dust(stage, position, 0.886, 0.784, 0.47);
        } else {
            Kit.dust(stage, position, 1.0, 0.784, 0.0);
        }
    }

    record Scan(int x, int z, int bottom, int top) {
    }

    record Placement(int tick, int x, int y, int z, Material material, int data) {
    }

    static final class CarpetFlight {

        static final double RISE = 2.45;
        static final double RISE_RATE = 0.2;
        static final double TOP_SPEED = 0.98;
        static final double SPEED_EASING = 0.15;
        static final double COAST_DRAG = 0.85;
        static final double HEADING_EASING = 0.21;
        static final double YAW_EASING = 0.5;
        static final double CLIMB_EASING = 0.2;
        static final double FLOOR_BELOW_START = 0.72;

        private Vec position;
        private double heading;
        private double yaw;
        private double speed;
        private double climb;
        private double lift = RISE;
        private final double floor;
        private int ticks;

        CarpetFlight(Vec seat, float yaw) {
            this.position = seat;
            this.heading = yaw;
            this.yaw = yaw;
            this.floor = seat.y() - FLOOR_BELOW_START;
        }

        void step(boolean pushing, float lookYaw, float lookPitch) {
            heading += HEADING_EASING * Flight.wrap(lookYaw - heading);
            yaw += YAW_EASING * Flight.wrap(lookYaw - yaw);
            speed = pushing ? speed + SPEED_EASING * (TOP_SPEED - speed) : speed * COAST_DRAG;
            double pitch = Math.toRadians(lookPitch);
            double flat = speed * Math.cos(pitch);
            climb += CLIMB_EASING * (-speed * Math.sin(pitch) - climb);
            double rise = lift * RISE_RATE;
            lift -= rise;
            double height = position.y() + climb + rise;
            if (height < floor) {
                height = floor;
                climb = 0;
            }
            Vec direction = Flight.direction(heading, 0);
            position = new Vec(position.x() + direction.x() * flat, height, position.z() + direction.z() * flat);
            ticks++;
        }

        Vec position() {
            return position;
        }

        double heading() {
            return heading;
        }

        float yaw() {
            return (float) yaw;
        }

        double speed() {
            return speed;
        }

        double floor() {
            return floor;
        }

        int ticks() {
            return ticks;
        }
    }

    private static final class Carpet {

        private final List<Actor> stands = new ArrayList<>();

        void spawn(Stage stage) {
            for (int stand = 0; stand < RIPPLE_STANDS; stand++) {
                Actor actor = stage.armorStand(Vec.ZERO, 0, StandStyle.HIDDEN);
                actor.equip(Slot.HELMET, Material.CARPET, carpetColor(stand));
                stands.add(actor);
            }
        }

        void place(CarpetFlight flight) {
            float yaw = flight.yaw();
            Vec forward = Flight.direction(yaw, 0);
            Vec right = new Vec(-forward.z(), 0, forward.x());
            double vehicle = flight.position().y() - VEHICLE_BELOW_SEAT;
            for (int stand = 0; stand < stands.size(); stand++) {
                Vec offset = standOffset(stand);
                Vec at = new Vec(
                    flight.position().x() + right.x() * offset.x() + forward.x() * offset.z(),
                    vehicle + ripple(flight.ticks(), offset.z()),
                    flight.position().z() + right.z() * offset.x() + forward.z() * offset.z());
                stands.get(stand).teleport(at, yaw, 0);
            }
        }

        void remove() {
            stands.forEach(Actor::remove);
        }
    }

    private static final class Drops {

        private final Stage stage;
        private final RandomGenerator random;
        private final List<Drop> drops = new ArrayList<>();

        Drops(Stage stage, RandomGenerator random) {
            this.stage = stage;
            this.random = random;
        }

        void spawn(Material material, Vec position, Vec velocity) {
            Actor actor = stage.item(position, material, 0);
            int life = DROP_MIN_LIFE + random.nextInt(DROP_MAX_LIFE - DROP_MIN_LIFE + 1);
            drops.add(new Drop(actor, Body.item(position, velocity), life));
        }

        void step() {
            Iterator<Drop> iterator = drops.iterator();
            while (iterator.hasNext()) {
                Drop drop = iterator.next();
                drop.age++;
                if (drop.age >= drop.life) {
                    drop.actor.remove();
                    iterator.remove();
                    continue;
                }
                boolean wasLanded = drop.body.landed();
                boolean touched = drop.body.step(stage);
                if (!wasLanded) {
                    drop.actor.teleport(drop.body.position(), 0, 0);
                }
                if (touched && random.nextDouble() < ORB_CHANCE) {
                    stage.sound("random.orb", drop.body.position(), 0.3f, (float) Kit.between(random, 1.5, 2.0));
                }
            }
        }

        void removeAll() {
            drops.forEach(drop -> drop.actor.remove());
            drops.clear();
        }

        private static final class Drop {

            private final Actor actor;
            private final Body body;
            private final int life;
            private int age;

            Drop(Actor actor, Body body, int life) {
                this.actor = actor;
                this.body = body;
                this.life = life;
            }
        }
    }

    static final class Oasis {

        static final int RADIUS = 8;
        static final int FIRST_RING_TICK = 10;
        static final int RING_TICKS = 2;
        static final int POND_FIRST_TICK = 22;
        static final double POND_BASE = 2.5;
        static final double POND_SPREAD = 1.4;
        static final double GRASS_BASE = 4.5;
        static final double GRASS_SPREAD = 1.8;
        static final double PALM_MIN_RADIUS = 4.6;
        static final double PALM_MAX_RADIUS = 5.2;
        static final int PALMS = 3;
        static final int PALM_FIRST_LOG_TICK = 33;
        static final int PALM_LOG_EVERY = 3;
        static final int PALM_MIN_LOGS = 6;
        static final int PALM_STRAIGHT_LOGS = 3;
        static final int PALM_LEAN_EVERY = 2;
        static final int JUNGLE = 3;
        static final int JUNGLE_LEAVES = 7;
        static final int LILY_FIRST_TICK = 30;
        static final int LILY_LATER_TICK = 32;
        static final int LILIES = 3;
        static final int COVER_FIRST_TICK = 35;
        static final int COVER_TICKS = 10;
        static final double COVER_MIN_DISTANCE = 3.5;
        static final double COVER_MAX_DISTANCE = 5.5;
        static final int TALL_GRASS = 19;
        static final int FERNS = 6;
        static final int POPPIES = 2;
        static final int DANDELIONS = 3;
        static final int CANE_COLUMNS = 7;
        static final int CANE_FIRST_TICK = 38;
        static final int CANE_TICKS = 4;
        static final int CANE_EVERY = 3;
        static final int CANE_MIN_HEIGHT = 2;
        static final int[][] CROWN_OFFSETS = {
            {0, 1, 0, 0},
            {-1, 1, -1, 1}, {-1, 1, 0, 1}, {-1, 1, 1, 1}, {0, 1, -1, 1}, {0, 1, 1, 1}, {1, 1, -1, 1}, {1, 1, 0, 1}, {1, 1, 1, 1},
            {-2, 1, 0, 2}, {2, 1, 0, 2}, {0, 1, -2, 2}, {0, 1, 2, 2},
            {-2, 0, -2, 2}, {-2, 0, 2, 2}, {2, 0, -2, 2}, {2, 0, 2, 2},
            {-3, 0, 0, 3}, {3, 0, 0, 3}, {0, 0, -3, 3}, {0, 0, 3, 3}
        };

        private final int centreX;
        private final int centreZ;
        private final int startY;
        private final List<Column> columns = new ArrayList<>();
        private final List<Placement> placements = new ArrayList<>();

        Oasis(int centreX, int startY, int centreZ, RandomGenerator random) {
            this.centreX = centreX;
            this.startY = startY;
            this.centreZ = centreZ;
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    double distance = Math.hypot(dx, dz);
                    if (distance < RADIUS + 1) {
                        columns.add(new Column(centreX + dx, centreZ + dz, distance, (int) Math.floor(distance), surface(distance, random)));
                    }
                }
            }
            Set<Cell> reserved = new HashSet<>();
            addLilies(random);
            addCane(random, reserved);
            addPalms(random, reserved);
            addCover(random, reserved);
        }

        static int ringTick(int ring) {
            return FIRST_RING_TICK + RING_TICKS * ring;
        }

        static int pondTick(int ring) {
            return POND_FIRST_TICK + RING_TICKS * ring;
        }

        static Surface surface(double distance, RandomGenerator random) {
            double pond = random.nextDouble();
            double grass = random.nextDouble();
            if (distance > 0 && distance < POND_BASE + POND_SPREAD * pond) {
                return Surface.WATER;
            }
            return distance < GRASS_BASE + GRASS_SPREAD * grass ? Surface.GRASS : Surface.SAND;
        }

        int centreX() {
            return centreX;
        }

        int centreZ() {
            return centreZ;
        }

        int startY() {
            return startY;
        }

        Vec centre() {
            return new Vec(centreX + 0.5, startY, centreZ + 0.5);
        }

        Vec fountainTop() {
            return new Vec(centreX + 0.5, startY + 1.2, centreZ + 0.5);
        }

        List<Column> columns() {
            return columns;
        }

        List<Column> ring(int ring) {
            return columns.stream().filter(column -> column.ring() == ring).toList();
        }

        List<Column> water(int ring) {
            return ring(ring).stream().filter(column -> column.surface() == Surface.WATER).toList();
        }

        List<Placement> placements() {
            return placements;
        }

        private List<Column> dry() {
            return new ArrayList<>(columns.stream().filter(column -> column.ring() > 0 && column.surface() != Surface.WATER).toList());
        }

        private void addLilies(RandomGenerator random) {
            List<Column> pads = new ArrayList<>(columns.stream()
                .filter(column -> column.surface() == Surface.WATER && column.ring() <= 2).toList());
            Collections.shuffle(pads, random);
            for (int pad = 0; pad < Math.min(LILIES, pads.size()); pad++) {
                Column column = pads.get(pad);
                int tick = pad == 0 ? LILY_FIRST_TICK : LILY_LATER_TICK;
                placements.add(new Placement(tick, column.x(), startY, column.z(), Material.WATER_LILY, 0));
            }
        }

        private void addCane(RandomGenerator random, Set<Cell> reserved) {
            Set<Cell> water = new HashSet<>();
            columns.stream().filter(column -> column.surface() == Surface.WATER).forEach(column -> water.add(new Cell(column.x(), column.z())));
            List<Column> shore = new ArrayList<>(dry().stream().filter(column -> touches(water, column)).toList());
            Collections.shuffle(shore, random);
            for (int index = 0; index < Math.min(CANE_COLUMNS, shore.size()); index++) {
                Column column = shore.get(index);
                reserved.add(new Cell(column.x(), column.z()));
                int height = CANE_MIN_HEIGHT + random.nextInt(2);
                int first = CANE_FIRST_TICK + random.nextInt(CANE_TICKS);
                for (int block = 0; block < height; block++) {
                    placements.add(new Placement(first + CANE_EVERY * block, column.x(), startY + block, column.z(), Material.SUGAR_CANE_BLOCK, 0));
                }
            }
        }

        private static boolean touches(Set<Cell> water, Column column) {
            return water.contains(new Cell(column.x() + 1, column.z())) || water.contains(new Cell(column.x() - 1, column.z()))
                || water.contains(new Cell(column.x(), column.z() + 1)) || water.contains(new Cell(column.x(), column.z() - 1));
        }

        private void addPalms(RandomGenerator random, Set<Cell> reserved) {
            double start = random.nextDouble() * 360;
            for (int palm = 0; palm < PALMS; palm++) {
                Vec offset = Kit.horizontal(start + palm * 360.0 / PALMS, Kit.between(random, PALM_MIN_RADIUS, PALM_MAX_RADIUS));
                int logs = PALM_MIN_LOGS + random.nextInt(2);
                int crownDelay = 2 + random.nextInt(2);
                addPalm(centreX + (int) Math.round(offset.x()), centreZ + (int) Math.round(offset.z()), logs, crownDelay, reserved);
            }
        }

        private void addPalm(int baseX, int baseZ, int logs, int crownDelay, Set<Cell> reserved) {
            int x = baseX;
            int z = baseZ;
            for (int log = 0; log < logs; log++) {
                if (log >= PALM_STRAIGHT_LOGS && (log - PALM_STRAIGHT_LOGS) % PALM_LEAN_EVERY == 0) {
                    int toX = centreX - x;
                    int toZ = centreZ - z;
                    if (Math.abs(toX) >= Math.abs(toZ)) {
                        x += Integer.signum(toX);
                    } else {
                        z += Integer.signum(toZ);
                    }
                }
                reserved.add(new Cell(x, z));
                placements.add(new Placement(PALM_FIRST_LOG_TICK + PALM_LOG_EVERY * log, x, startY + log, z, Material.LOG, JUNGLE));
            }
            int crownTick = PALM_FIRST_LOG_TICK + PALM_LOG_EVERY * (logs - 1) + crownDelay;
            int crownY = startY + logs - 1;
            for (int[] leaf : CROWN_OFFSETS) {
                placements.add(new Placement(crownTick + leaf[3], x + leaf[0], crownY + leaf[1], z + leaf[2], Material.LEAVES, JUNGLE_LEAVES));
            }
        }

        private void addCover(RandomGenerator random, Set<Cell> reserved) {
            List<Column> candidates = new ArrayList<>(dry().stream()
                .filter(column -> column.distance() >= COVER_MIN_DISTANCE && column.distance() <= COVER_MAX_DISTANCE)
                .filter(column -> !reserved.contains(new Cell(column.x(), column.z()))).toList());
            Collections.shuffle(candidates, random);
            List<Placement> cover = new ArrayList<>();
            repeatCover(cover, TALL_GRASS, Material.LONG_GRASS, 1);
            repeatCover(cover, FERNS, Material.LONG_GRASS, 2);
            repeatCover(cover, POPPIES, Material.RED_ROSE, 0);
            repeatCover(cover, DANDELIONS, Material.YELLOW_FLOWER, 0);
            for (int index = 0; index < Math.min(cover.size(), candidates.size()); index++) {
                Column column = candidates.get(index);
                Placement plant = cover.get(index);
                int tick = COVER_FIRST_TICK + random.nextInt(COVER_TICKS);
                placements.add(new Placement(tick, column.x(), startY, column.z(), plant.material(), plant.data()));
            }
        }

        private static void repeatCover(List<Placement> cover, int count, Material material, int data) {
            for (int index = 0; index < count; index++) {
                cover.add(new Placement(0, 0, 0, 0, material, data));
            }
        }

        record Cell(int x, int z) {
        }

        enum Surface {
            WATER,
            GRASS,
            SAND
        }

        record Column(int x, int z, double distance, int ring, Surface surface) {
        }
    }
}
