package br.com.muxe.core.bukkit.nms;

import br.com.muxe.core.bukkit.display.DisplayStand;
import br.com.muxe.core.bukkit.display.StandBody;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PacketStandTransport {

    private static final int HAND_SLOT = 0;
    private static final int HELMET_SLOT = 4;

    private static final PacketStandTransport INSTANCE = new PacketStandTransport();

    private PacketStandTransport() {
    }

    public static PacketStandTransport instance() {
        return INSTANCE;
    }

    public static boolean isAvailable() {
        return Holder.HANDLES.isPresent();
    }

    public StandBody create(Location location, DisplayStand stand) {
        return Holder.HANDLES.<StandBody>map(handles -> handles.create(location, stand)).orElse(StandBody.NONE);
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = ServerInternals.resolve("Display stands").flatMap(Handles::resolve);
    }

    private static final class Handles {

        private final ServerInternals internals;
        private final Constructor<?> armorStand;
        private final Method bukkitEntity;
        private final Method setLocation;
        private final Method entityId;
        private final Method nmsCopy;
        private final Constructor<?> spawnPacket;
        private final Constructor<?> equipmentPacket;
        private final Constructor<?> teleportPacket;
        private final Constructor<?> destroyPacket;

        private Handles(ServerInternals internals) throws ReflectiveOperationException {
            this.internals = internals;
            Class<?> entity = internals.nms("Entity");
            Class<?> nmsItem = internals.nms("ItemStack");
            this.armorStand = ServerInternals.constructor(internals.nms("EntityArmorStand"), internals.nms("World"));
            this.bukkitEntity = entity.getMethod("getBukkitEntity");
            this.setLocation = entity.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            this.entityId = entity.getMethod("getId");
            this.nmsCopy = internals.craft("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
            this.spawnPacket = ServerInternals.constructor(internals.nms("PacketPlayOutSpawnEntityLiving"), internals.nms("EntityLiving"));
            this.equipmentPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityEquipment"), int.class, int.class, nmsItem);
            this.teleportPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityTeleport"), entity);
            this.destroyPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityDestroy"), int[].class);
        }

        private static Optional<Handles> resolve(ServerInternals internals) {
            try {
                return Optional.of(new Handles(internals));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Display stands are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private StandBody create(Location location, DisplayStand stand) {
            try {
                Object handle = armorStand.newInstance(internals.world(location.getWorld()));
                setLocation.invoke(handle, location.getX(), location.getY(), location.getZ(), location.getYaw(), 0f);
                ArmorStand view = (ArmorStand) bukkitEntity.invoke(handle);
                view.setVisible(false);
                view.setGravity(false);
                view.setBasePlate(false);
                view.setSmall(stand.small());
                view.setArms(stand.arms());
                view.setHeadPose(stand.headPose().toEulerAngle());
                view.setRightArmPose(stand.rightArmPose().toEulerAngle());
                List<Equipment> equipment = new ArrayList<>(2);
                if (stand.helmet().isPresent()) {
                    equipment.add(new Equipment(HELMET_SLOT, nms(stand.helmet().get())));
                }
                if (stand.hand().isPresent()) {
                    equipment.add(new Equipment(HAND_SLOT, nms(stand.hand().get())));
                }
                return new PacketStand(handle, (int) entityId.invoke(handle), List.copyOf(equipment));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Could not build a display stand", exception);
                return StandBody.NONE;
            }
        }

        private Object nms(ItemBuilder item) throws ReflectiveOperationException {
            return nmsCopy.invoke(null, item.build());
        }

        private record Equipment(int slot, Object item) {
        }

        private final class PacketStand implements StandBody {

            private final Object handle;
            private final int id;
            private final List<Equipment> equipment;

            private PacketStand(Object handle, int id, List<Equipment> equipment) {
                this.handle = handle;
                this.id = id;
                this.equipment = equipment;
            }

            @Override
            public int entityId() {
                return id;
            }

            @Override
            public void show(Player viewer) {
                send(viewer, "show", () -> {
                    internals.send(viewer, spawnPacket.newInstance(handle));
                    for (Equipment item : equipment) {
                        internals.send(viewer, equipmentPacket.newInstance(id, item.slot(), item.item()));
                    }
                });
            }

            @Override
            public void move(Location location, Collection<? extends Player> viewers) {
                try {
                    setLocation.invoke(handle, location.getX(), location.getY(), location.getZ(), location.getYaw(), 0f);
                    Object packet = teleportPacket.newInstance(handle);
                    for (Player viewer : viewers) {
                        send(viewer, "move", () -> internals.send(viewer, packet));
                    }
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not move a display stand", exception);
                }
            }

            @Override
            public void hide(Player viewer) {
                send(viewer, "hide", () -> internals.send(viewer, destroyPacket.newInstance((Object) new int[] {id})));
            }

            private void send(Player viewer, String action, PacketAction packets) {
                if (!viewer.isOnline()) {
                    return;
                }
                try {
                    packets.run();
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not " + action + " a display stand for " + viewer.getName(), exception);
                }
            }
        }
    }

    @FunctionalInterface
    private interface PacketAction {
        void run() throws ReflectiveOperationException;
    }
}
