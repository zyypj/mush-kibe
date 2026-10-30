package br.com.muxe.core.bukkit.cosmetic.animation;

import java.util.List;
import org.bukkit.Material;

public interface Stage {

    Actor armorStand(Vec position, float yaw, StandStyle style);

    Actor mob(String type, Vec position, float yaw);

    Actor fallingBlock(Vec position, Material material, int data);

    Actor item(Vec position, Material material, int data);

    Actor firework(Vec position, List<Burst> bursts);

    void particle(String type, Vec position, int count, Vec spread, double speed, int... data);

    void sound(String name, Vec position, float volume, float pitch);

    void block(int x, int y, int z, Material material, int data);

    interface Actor {

        void teleport(Vec position, float yaw, float pitch);

        void velocity(Vec perTick);

        void equip(Slot slot, Material material, int data);

        void pose(Pose pose);

        void ride(Actor vehicle);

        void status(int opcode);

        void remove();

        boolean removed();
    }

    enum Slot {
        HAND,
        BOOTS,
        LEGGINGS,
        CHESTPLATE,
        HELMET
    }

    record StandStyle(boolean invisible, boolean small, boolean arms, boolean basePlate, boolean marker) {

        public static final StandStyle HIDDEN = new StandStyle(true, false, false, false, true);
        public static final StandStyle HIDDEN_SMALL = new StandStyle(true, true, false, false, true);
        public static final StandStyle VISIBLE_ARMS = new StandStyle(false, false, true, false, false);
    }

    record Pose(Vec head, Vec body, Vec leftArm, Vec rightArm, Vec leftLeg, Vec rightLeg) {

        public static final Pose STANDING = new Pose(Vec.ZERO, Vec.ZERO, new Vec(-10, 0, -10), new Vec(-15, 0, 10), new Vec(-1, 0, -1), new Vec(1, 0, 1));

        public Pose withHead(Vec value) {
            return new Pose(value, body, leftArm, rightArm, leftLeg, rightLeg);
        }

        public Pose withRightArm(Vec value) {
            return new Pose(head, body, leftArm, value, leftLeg, rightLeg);
        }
    }

    record Burst(int type, List<Integer> colors, List<Integer> fades, boolean flicker, boolean trail) {

        public Burst {
            colors = List.copyOf(colors);
            fades = List.copyOf(fades);
        }
    }
}
