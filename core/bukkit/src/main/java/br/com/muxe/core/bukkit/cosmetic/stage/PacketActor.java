package br.com.muxe.core.bukkit.cosmetic.stage;

import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Pose;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Slot;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.Vector3f;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

final class PacketActor implements Actor {

    private final PacketStage stage;
    private final Entity entity;
    private boolean removed;

    PacketActor(PacketStage stage, Entity entity) {
        this.stage = stage;
        this.entity = entity;
    }

    @Override
    public void teleport(Vec position, float yaw, float pitch) {
        if (removed) {
            return;
        }
        entity.setLocation(position.x(), position.y(), position.z(), yaw, pitch);
        stage.send(new PacketPlayOutEntityTeleport(entity));
        if (entity instanceof EntityLiving) {
            headYaw(yaw);
        }
    }

    @Override
    public void velocity(Vec perTick) {
        if (!removed) {
            stage.send(new PacketPlayOutEntityVelocity(entity.getId(), perTick.x(), perTick.y(), perTick.z()));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void equip(Slot slot, Material material, int data) {
        if (!removed) {
            ItemStack item = material == Material.AIR ? null : new ItemStack(material, 1, (short) data);
            stage.send(new PacketPlayOutEntityEquipment(entity.getId(), slot.ordinal(), CraftItemStack.asNMSCopy(item)));
        }
    }

    @Override
    public void pose(Pose pose) {
        if (removed || !(entity instanceof EntityArmorStand stand)) {
            return;
        }
        stand.setHeadPose(vector(pose.head()));
        stand.setBodyPose(vector(pose.body()));
        stand.setLeftArmPose(vector(pose.leftArm()));
        stand.setRightArmPose(vector(pose.rightArm()));
        stand.setLeftLegPose(vector(pose.leftLeg()));
        stand.setRightLegPose(vector(pose.rightLeg()));
        stage.send(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), true));
    }

    @Override
    public void ride(Actor vehicle) {
        if (!removed && vehicle instanceof PacketActor other) {
            stage.send(new PacketPlayOutAttachEntity(0, entity, other.entity));
        }
    }

    @Override
    public void status(int opcode) {
        if (!removed) {
            stage.send(new PacketPlayOutEntityStatus(entity, (byte) opcode));
        }
    }

    @Override
    public void remove() {
        if (!removed) {
            removed = true;
            stage.send(new PacketPlayOutEntityDestroy(entity.getId()));
        }
    }

    @Override
    public boolean removed() {
        return removed;
    }

    void headYaw(float yaw) {
        stage.send(new PacketPlayOutEntityHeadRotation(entity, (byte) (yaw * 256 / 360)));
    }

    private static Vector3f vector(Vec degrees) {
        return new Vector3f((float) degrees.x(), (float) degrees.y(), (float) degrees.z());
    }
}
