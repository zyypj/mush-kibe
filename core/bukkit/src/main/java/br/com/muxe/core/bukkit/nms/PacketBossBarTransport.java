package br.com.muxe.core.bukkit.nms;

import br.com.muxe.core.bukkit.bossbar.SpawnedBossBar;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PacketBossBarTransport {

    private static final PacketBossBarTransport INSTANCE = new PacketBossBarTransport();

    private PacketBossBarTransport() {
    }

    public static PacketBossBarTransport instance() {
        return INSTANCE;
    }

    public static boolean isAvailable() {
        return Holder.HANDLES.isPresent();
    }

    public SpawnedBossBar spawn(Player viewer, Location location, String text) {
        return Holder.HANDLES.<SpawnedBossBar>map(handles -> handles.spawn(viewer, location, text)).orElse(SpawnedBossBar.NONE);
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = ServerInternals.resolve("Boss bars").flatMap(Handles::resolve);
    }

    private static final class Handles {

        private final ServerInternals internals;
        private final Constructor<?> wither;
        private final Method setLocation;
        private final Method setInvisible;
        private final Method setCustomName;
        private final Method entityId;
        private final Method dataWatcher;
        private final Constructor<?> spawnPacket;
        private final Constructor<?> metadataPacket;
        private final Constructor<?> teleportPacket;
        private final Constructor<?> destroyPacket;

        private Handles(ServerInternals internals) throws ReflectiveOperationException {
            this.internals = internals;
            Class<?> entity = internals.nms("Entity");
            this.wither = ServerInternals.constructor(internals.nms("EntityWither"), internals.nms("World"));
            this.setLocation = entity.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            this.setInvisible = entity.getMethod("setInvisible", boolean.class);
            this.setCustomName = entity.getMethod("setCustomName", String.class);
            this.entityId = entity.getMethod("getId");
            this.dataWatcher = entity.getMethod("getDataWatcher");
            this.spawnPacket = ServerInternals.constructor(internals.nms("PacketPlayOutSpawnEntityLiving"), internals.nms("EntityLiving"));
            this.metadataPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityMetadata"), int.class, internals.nms("DataWatcher"), boolean.class);
            this.teleportPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityTeleport"), entity);
            this.destroyPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityDestroy"), int[].class);
        }

        private static Optional<Handles> resolve(ServerInternals internals) {
            try {
                return Optional.of(new Handles(internals));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Boss bars are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private SpawnedBossBar spawn(Player viewer, Location location, String text) {
            try {
                Object entity = wither.newInstance(internals.world(location.getWorld()));
                setLocation.invoke(entity, location.getX(), location.getY(), location.getZ(), 0f, 0f);
                setInvisible.invoke(entity, true);
                setCustomName.invoke(entity, text);
                internals.send(viewer, spawnPacket.newInstance(entity));
                return new PacketBossBar(viewer, entity, (int) entityId.invoke(entity));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not show a boss bar to " + viewer.getName(), exception);
                return SpawnedBossBar.NONE;
            }
        }

        private final class PacketBossBar implements SpawnedBossBar {

            private final Player viewer;
            private final Object entity;
            private final int id;
            private boolean removed;

            private PacketBossBar(Player viewer, Object entity, int id) {
                this.viewer = viewer;
                this.entity = entity;
                this.id = id;
            }

            @Override
            public void move(Location location) {
                if (removed || !viewer.isOnline()) {
                    return;
                }
                try {
                    setLocation.invoke(entity, location.getX(), location.getY(), location.getZ(), 0f, 0f);
                    internals.send(viewer, teleportPacket.newInstance(entity));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not move the boss bar of " + viewer.getName(), exception);
                }
            }

            @Override
            public void rename(String text) {
                if (removed || !viewer.isOnline()) {
                    return;
                }
                try {
                    setCustomName.invoke(entity, text);
                    internals.send(viewer, metadataPacket.newInstance(id, dataWatcher.invoke(entity), true));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not rename the boss bar of " + viewer.getName(), exception);
                }
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
                    internals.send(viewer, destroyPacket.newInstance((Object) new int[] {id}));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not remove the boss bar of " + viewer.getName(), exception);
                }
            }
        }
    }
}
