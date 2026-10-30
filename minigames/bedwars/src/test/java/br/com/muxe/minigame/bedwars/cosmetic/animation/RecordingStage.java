package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.bukkit.Material;

final class RecordingStage implements Stage {

    final List<FakeActor> actors = new ArrayList<>();
    final Map<String, Integer> particles = new HashMap<>();
    final List<String> sounds = new ArrayList<>();
    final List<String> blocks = new ArrayList<>();
    private final Predicate<int[]> solid;

    RecordingStage(Predicate<int[]> solid) {
        this.solid = solid;
    }

    @Override
    public Actor armorStand(Vec position, float yaw, StandStyle style) {
        return add("ArmorStand", position);
    }

    @Override
    public Actor mob(String type, Vec position, float yaw) {
        return add(type, position);
    }

    @Override
    public Actor fallingBlock(Vec position, Material material, int data) {
        return add("FallingBlock:" + material + ":" + data, position);
    }

    @Override
    public Actor item(Vec position, Material material, int data) {
        return add("Item:" + material, position);
    }

    @Override
    public Actor firework(Vec position, List<Burst> bursts) {
        return add("Firework", position);
    }

    @Override
    public Actor snowball(Vec position, Vec velocity) {
        return add("Snowball", position);
    }

    @Override
    public boolean solid(int x, int y, int z) {
        return solid.test(new int[] {x, y, z});
    }

    @Override
    public void particle(String type, Vec position, int count, Vec spread, double speed, int... data) {
        particles.merge(type, 1, Integer::sum);
    }

    @Override
    public void sound(String name, Vec position, float volume, float pitch) {
        sounds.add(name);
    }

    @Override
    public void block(int x, int y, int z, Material material, int data) {
        blocks.add(x + "," + y + "," + z + "=" + material);
    }

    long alive() {
        return actors.stream().filter(actor -> !actor.removed()).count();
    }

    private FakeActor add(String type, Vec position) {
        FakeActor actor = new FakeActor(type, position);
        actors.add(actor);
        return actor;
    }

    static final class FakeActor implements Actor {

        final String type;
        Vec position;
        boolean removed;
        final Map<Integer, Object> watched = new HashMap<>();

        FakeActor(String type, Vec position) {
            this.type = type;
            this.position = position;
        }

        @Override
        public void teleport(Vec position, float yaw, float pitch) {
            this.position = position;
        }

        @Override
        public void velocity(Vec perTick) {
        }

        @Override
        public void equip(Slot slot, Material material, int data) {
        }

        @Override
        public void equipSkull(Slot slot, String texture) {
        }

        @Override
        public void watch(int index, Object value) {
            watched.put(index, value);
        }

        @Override
        public void pose(Pose pose) {
        }

        @Override
        public void ride(Actor vehicle) {
        }

        @Override
        public void status(int opcode) {
        }

        @Override
        public void remove() {
            removed = true;
        }

        @Override
        public boolean removed() {
            return removed;
        }
    }
}
