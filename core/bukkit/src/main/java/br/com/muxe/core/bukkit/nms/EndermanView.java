package br.com.muxe.core.bukkit.nms;

import net.minecraft.server.v1_8_R3.EntityEnderman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutCamera;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutGameStateChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class EndermanView {

    private static final int CHANGE_GAME_MODE = 3;
    private static final float SPECTATOR = 3f;

    private final Player viewer;
    private final EntityEnderman enderman;

    private EndermanView(Player viewer, EntityEnderman enderman) {
        this.viewer = viewer;
        this.enderman = enderman;
    }

    public static EndermanView open(Player viewer, Location eye) {
        EntityEnderman enderman = new EntityEnderman(((CraftWorld) eye.getWorld()).getHandle());
        enderman.setLocation(eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch());
        enderman.setInvisible(true);
        EndermanView view = new EndermanView(viewer, enderman);
        view.send(new PacketPlayOutSpawnEntityLiving(enderman));
        view.send(new PacketPlayOutGameStateChange(CHANGE_GAME_MODE, SPECTATOR));
        view.send(new PacketPlayOutCamera(enderman));
        return view;
    }

    public void move(Location eye) {
        enderman.setLocation(eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch());
        send(new PacketPlayOutEntityTeleport(enderman));
    }

    @SuppressWarnings("deprecation")
    public void close() {
        EntityPlayer handle = ((CraftPlayer) viewer).getHandle();
        send(new PacketPlayOutCamera(handle));
        send(new PacketPlayOutGameStateChange(CHANGE_GAME_MODE, viewer.getGameMode().getValue()));
        send(new PacketPlayOutEntityDestroy(enderman.getId()));
    }

    private void send(Packet<?> packet) {
        if (viewer.isOnline()) {
            ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(packet);
        }
    }
}
