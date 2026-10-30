package br.com.muxe.core.bukkit.cosmetic.stage;

import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityFallingBlock;
import net.minecraft.server.v1_8_R3.EntityFireworks;
import net.minecraft.server.v1_8_R3.EntityItem;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntitySnowball;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public final class PacketStage implements Stage, AutoCloseable {

    private static final int OBJECT_ITEM = 2;
    private static final int OBJECT_FALLING_BLOCK = 70;
    private static final int OBJECT_FIREWORK = 76;
    private static final int OBJECT_SNOWBALL = 61;

    private final WorldServer world;
    private final Supplier<Collection<Player>> viewers;
    private final List<PacketActor> actors = new ArrayList<>();
    private final Map<BlockPosition, Boolean> changed = new LinkedHashMap<>();
    private boolean closed;

    public PacketStage(World world, Supplier<Collection<Player>> viewers) {
        this.world = ((CraftWorld) world).getHandle();
        this.viewers = viewers;
    }

    @Override
    public Actor armorStand(Vec position, float yaw, StandStyle style) {
        EntityArmorStand stand = new EntityArmorStand(world, position.x(), position.y(), position.z());
        stand.setLocation(position.x(), position.y(), position.z(), yaw, 0);
        stand.setInvisible(style.invisible());
        stand.setSmall(style.small());
        stand.setArms(style.arms());
        stand.setBasePlate(!style.basePlate());
        stand.setGravity(true);
        stand.n(style.marker());
        return living(stand, yaw);
    }

    @Override
    public Actor mob(String type, Vec position, float yaw) {
        Entity entity = Objects.requireNonNull(EntityTypes.createEntityByName(type, world), "Unknown entity type " + type);
        if (!(entity instanceof EntityLiving living)) {
            throw new IllegalArgumentException(type + " is not a living entity");
        }
        living.setLocation(position.x(), position.y(), position.z(), yaw, 0);
        return living(living, yaw);
    }

    @Override
    public Actor fallingBlock(Vec position, Material material, int data) {
        IBlockData block = state(material, data);
        EntityFallingBlock falling = new EntityFallingBlock(world, position.x(), position.y(), position.z(), block);
        return object(falling, new PacketPlayOutSpawnEntity(falling, OBJECT_FALLING_BLOCK, Block.getCombinedId(block)), false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Actor item(Vec position, Material material, int data) {
        EntityItem item = new EntityItem(world, position.x(), position.y(), position.z(), CraftItemStack.asNMSCopy(new ItemStack(material, 1, (short) data)));
        return object(item, new PacketPlayOutSpawnEntity(item, OBJECT_ITEM, 1), true);
    }

    @Override
    public Actor firework(Vec position, List<Burst> bursts) {
        ItemStack rocket = new ItemStack(Material.FIREWORK);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        for (Burst burst : bursts) {
            meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.values()[Math.clamp(burst.type(), 0, FireworkEffect.Type.values().length - 1)])
                .withColor(burst.colors().stream().map(Color::fromRGB).toList())
                .withFade(burst.fades().stream().map(Color::fromRGB).toList())
                .flicker(burst.flicker())
                .trail(burst.trail())
                .build());
        }
        meta.setPower(1);
        rocket.setItemMeta(meta);
        EntityFireworks firework = new EntityFireworks(world, position.x(), position.y(), position.z(), CraftItemStack.asNMSCopy(rocket));
        return object(firework, new PacketPlayOutSpawnEntity(firework, OBJECT_FIREWORK), true);
    }

    @Override
    public Actor snowball(Vec position, Vec velocity) {
        EntitySnowball snowball = new EntitySnowball(world, position.x(), position.y(), position.z());
        Actor actor = object(snowball, new PacketPlayOutSpawnEntity(snowball, OBJECT_SNOWBALL), false);
        actor.velocity(velocity);
        return actor;
    }

    @Override
    public boolean solid(int x, int y, int z) {
        return world.getType(new BlockPosition(x, y, z)).getBlock().getMaterial().isSolid();
    }

    @Override
    public void particle(String type, Vec position, int count, Vec spread, double speed, int... data) {
        send(new PacketPlayOutWorldParticles(EnumParticle.valueOf(type), true, (float) position.x(), (float) position.y(), (float) position.z(),
            (float) spread.x(), (float) spread.y(), (float) spread.z(), (float) speed, count, data));
    }

    @Override
    public void sound(String name, Vec position, float volume, float pitch) {
        send(new PacketPlayOutNamedSoundEffect(name, position.x(), position.y(), position.z(), volume, pitch));
    }

    @Override
    public void block(int x, int y, int z, Material material, int data) {
        BlockPosition position = new BlockPosition(x, y, z);
        changed.putIfAbsent(position, Boolean.TRUE);
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(world, position);
        packet.block = state(material, data);
        send(packet);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (PacketActor actor : actors) {
            actor.remove();
        }
        for (BlockPosition position : changed.keySet()) {
            send(new PacketPlayOutBlockChange(world, position));
        }
        actors.clear();
        changed.clear();
    }

    void send(Packet<?> packet) {
        for (Player viewer : viewers.get()) {
            if (viewer.isOnline()) {
                ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }

    private Actor living(EntityLiving entity, float yaw) {
        PacketActor actor = new PacketActor(this, entity);
        send(new PacketPlayOutSpawnEntityLiving(entity));
        actor.headYaw(yaw);
        actors.add(actor);
        return actor;
    }

    private Actor object(Entity entity, Packet<?> spawn, boolean metadata) {
        PacketActor actor = new PacketActor(this, entity);
        send(spawn);
        if (metadata) {
            send(new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), true));
        }
        actors.add(actor);
        return actor;
    }

    @SuppressWarnings("deprecation")
    private static IBlockData state(Material material, int data) {
        return Block.getById(material.getId()).fromLegacyData(data);
    }
}
