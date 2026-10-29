package br.com.muxe.core.bukkit.nms;

import br.com.muxe.core.bukkit.hologram.HologramPixel;
import br.com.muxe.core.bukkit.hologram.HologramTransport;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PacketHologramTransport implements HologramTransport {

    public static final double CLIENT_STAND_HEIGHT = 1.975;
    public static final double NAME_TAG_OFFSET = 0.5;
    public static final double TEXT_TOP_ABOVE_LABEL = 0.09375;
    public static final double LABEL_HEIGHT = CLIENT_STAND_HEIGHT / 2 + NAME_TAG_OFFSET - TEXT_TOP_ABOVE_LABEL;

    private static final PacketHologramTransport INSTANCE = new PacketHologramTransport();

    private PacketHologramTransport() {
    }

    public static PacketHologramTransport instance() {
        return INSTANCE;
    }

    public static double standY(double labelY) {
        return labelY - LABEL_HEIGHT;
    }

    public static double clientTextTop(double smallStandY) {
        return smallStandY - CLIENT_STAND_HEIGHT / 2 + CLIENT_STAND_HEIGHT + NAME_TAG_OFFSET;
    }

    public static boolean isAvailable() {
        return Holder.HANDLES.isPresent();
    }

    @Override
    public SpawnedHologram spawn(Player viewer, List<HologramPixel> pixels) {
        if (viewer == null || pixels.isEmpty()) {
            return SpawnedHologram.NONE;
        }
        return Holder.HANDLES.<SpawnedHologram>map(handles -> handles.spawn(viewer, pixels)).orElse(SpawnedHologram.NONE);
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = Handles.resolve();
    }

    private static final class Handles {

        private final Method worldHandle;
        private final Method playerHandle;
        private final Field playerConnection;
        private final Method sendPacket;
        private final Constructor<?> armorStand;
        private final Method setLocation;
        private final Method setInvisible;
        private final Method setCustomName;
        private final Method setCustomNameVisible;
        private final Method setSmall;
        private final Method setGravity;
        private final Method entityId;
        private final Constructor<?> spawnPacket;
        private final Constructor<?> destroyPacket;

        private Handles(
            Method worldHandle,
            Method playerHandle,
            Field playerConnection,
            Method sendPacket,
            Constructor<?> armorStand,
            Method setLocation,
            Method setInvisible,
            Method setCustomName,
            Method setCustomNameVisible,
            Method setSmall,
            Method setGravity,
            Method entityId,
            Constructor<?> spawnPacket,
            Constructor<?> destroyPacket
        ) {
            this.worldHandle = worldHandle;
            this.playerHandle = playerHandle;
            this.playerConnection = playerConnection;
            this.sendPacket = sendPacket;
            this.armorStand = armorStand;
            this.setLocation = setLocation;
            this.setInvisible = setInvisible;
            this.setCustomName = setCustomName;
            this.setCustomNameVisible = setCustomNameVisible;
            this.setSmall = setSmall;
            this.setGravity = setGravity;
            this.entityId = entityId;
            this.spawnPacket = spawnPacket;
            this.destroyPacket = destroyPacket;
        }

        private static Optional<Handles> resolve() {
            try {
                String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
                String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
                String nms = "net.minecraft.server." + version + ".";
                Class<?> world = Class.forName(nms + "World");
                Class<?> entity = Class.forName(nms + "Entity");
                Class<?> entityLiving = Class.forName(nms + "EntityLiving");
                Class<?> entityArmorStand = Class.forName(nms + "EntityArmorStand");
                Class<?> entityPlayer = Class.forName(nms + "EntityPlayer");
                Field connection = entityPlayer.getField("playerConnection");
                return Optional.of(new Handles(
                    Class.forName(craftPackage + ".CraftWorld").getMethod("getHandle"),
                    Class.forName(craftPackage + ".entity.CraftPlayer").getMethod("getHandle"),
                    connection,
                    connection.getType().getMethod("sendPacket", Class.forName(nms + "Packet")),
                    entityArmorStand.getConstructor(world),
                    entity.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class),
                    entity.getMethod("setInvisible", boolean.class),
                    entity.getMethod("setCustomName", String.class),
                    entity.getMethod("setCustomNameVisible", boolean.class),
                    entityArmorStand.getMethod("setSmall", boolean.class),
                    entityArmorStand.getMethod("setGravity", boolean.class),
                    entity.getMethod("getId"),
                    Class.forName(nms + "PacketPlayOutSpawnEntityLiving").getConstructor(entityLiving),
                    Class.forName(nms + "PacketPlayOutEntityDestroy").getConstructor(int[].class)
                ));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Packet holograms are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private SpawnedHologram spawn(Player viewer, List<HologramPixel> pixels) {
            try {
                List<Object> stands = new ArrayList<>(pixels.size());
                for (HologramPixel pixel : pixels) {
                    stands.add(armorStand(pixel));
                }
                int[] ids = new int[stands.size()];
                for (int index = 0; index < ids.length; index++) {
                    ids[index] = (int) entityId.invoke(stands.get(index));
                }
                PacketHologram hologram = new PacketHologram(viewer, stands, ids);
                hologram.resend();
                return hologram;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Could not build a hologram for " + viewer.getName(), exception);
                return SpawnedHologram.NONE;
            }
        }

        private Object armorStand(HologramPixel pixel) throws ReflectiveOperationException {
            Location location = pixel.location();
            World world = location.getWorld();
            Object stand = armorStand.newInstance(world == null ? null : worldHandle.invoke(world));
            setLocation.invoke(stand, location.getX(), standY(location.getY()), location.getZ(), 0f, 0f);
            setInvisible.invoke(stand, true);
            setSmall.invoke(stand, true);
            setGravity.invoke(stand, false);
            setCustomName.invoke(stand, pixel.name());
            setCustomNameVisible.invoke(stand, true);
            return stand;
        }

        private void send(Player viewer, Object packet) throws ReflectiveOperationException {
            sendPacket.invoke(playerConnection.get(playerHandle.invoke(viewer)), packet);
        }

        private final class PacketHologram implements SpawnedHologram {

            private final Player viewer;
            private final List<Object> stands;
            private final int[] ids;
            private boolean removed;

            private PacketHologram(Player viewer, List<Object> stands, int[] ids) {
                this.viewer = viewer;
                this.stands = stands;
                this.ids = ids;
            }

            @Override
            public void resend() {
                if (removed || !viewer.isOnline()) {
                    return;
                }
                try {
                    for (Object stand : stands) {
                        send(viewer, spawnPacket.newInstance(stand));
                    }
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not send a hologram to " + viewer.getName(), exception);
                }
            }

            @Override
            public boolean owns(int entityId) {
                for (int id : ids) {
                    if (id == entityId) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void remove() {
                if (removed) {
                    return;
                }
                removed = true;
                if (!viewer.isOnline()) {
                    return;
                }
                try {
                    send(viewer, destroyPacket.newInstance((Object) ids));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not remove a hologram of " + viewer.getName(), exception);
                }
            }
        }
    }
}
