package br.com.muxe.core.bukkit.nms;

import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcBody;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PacketNpcTransport {

    private static final int SKIN_LAYERS_INDEX = 10;
    private static final byte ALL_SKIN_LAYERS = 127;
    private static final float DEGREES_TO_BYTE = 256f / 360f;
    private static final int HAND_SLOT = 0;

    private static final PacketNpcTransport INSTANCE = new PacketNpcTransport();

    private PacketNpcTransport() {
    }

    public static PacketNpcTransport instance() {
        return INSTANCE;
    }

    public static boolean isAvailable() {
        return Holder.HANDLES.isPresent();
    }

    public NpcBody create(Npc npc) {
        return Holder.HANDLES.<NpcBody>map(handles -> handles.create(npc)).orElse(NpcBody.NONE);
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = ServerInternals.resolve("NPCs").flatMap(Handles::resolve);
    }

    private static final class Handles {

        private final ServerInternals internals;
        private final Method minecraftServer;
        private final Constructor<?> gameProfile;
        private final Method profileProperties;
        private final Constructor<?> property;
        private final Constructor<?> interactManager;
        private final Constructor<?> entityPlayer;
        private final Method setLocation;
        private final Method entityId;
        private final Method dataWatcher;
        private final Method watch;
        private final Constructor<?> infoPacket;
        private final Class<?> entityPlayerType;
        private final Object addPlayer;
        private final Object removePlayer;
        private final Constructor<?> spawnPacket;
        private final Constructor<?> headRotationPacket;
        private final Constructor<?> destroyPacket;
        private final Method nmsCopy;
        private final Constructor<?> equipmentPacket;

        private Handles(ServerInternals internals) throws ReflectiveOperationException {
            this.internals = internals;
            Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyType = Class.forName("com.mojang.authlib.properties.Property");
            Class<?> entity = internals.nms("Entity");
            Class<?> world = internals.nms("World");
            this.entityPlayerType = internals.nms("EntityPlayer");
            Class<?> infoType = internals.nms("PacketPlayOutPlayerInfo");
            Class<?> infoAction = internals.nms("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            this.minecraftServer = internals.craft("CraftServer").getMethod("getServer");
            this.gameProfile = ServerInternals.constructor(profileType, UUID.class, String.class);
            this.profileProperties = profileType.getMethod("getProperties");
            this.property = ServerInternals.constructor(propertyType, String.class, String.class, String.class);
            this.interactManager = ServerInternals.constructor(internals.nms("PlayerInteractManager"), world);
            this.entityPlayer = ServerInternals.constructor(entityPlayerType, internals.nms("MinecraftServer"), internals.nms("WorldServer"), profileType, internals.nms("PlayerInteractManager"));
            this.setLocation = entity.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            this.entityId = entity.getMethod("getId");
            this.dataWatcher = entity.getMethod("getDataWatcher");
            this.watch = internals.nms("DataWatcher").getMethod("watch", int.class, Object.class);
            this.infoPacket = ServerInternals.constructor(infoType, infoAction, Array.newInstance(entityPlayerType, 0).getClass());
            this.addPlayer = ServerInternals.constant(infoAction, "ADD_PLAYER");
            this.removePlayer = ServerInternals.constant(infoAction, "REMOVE_PLAYER");
            this.spawnPacket = ServerInternals.constructor(internals.nms("PacketPlayOutNamedEntitySpawn"), internals.nms("EntityHuman"));
            this.headRotationPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityHeadRotation"), entity, byte.class);
            this.destroyPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityDestroy"), int[].class);
            this.nmsCopy = internals.craft("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
            this.equipmentPacket = ServerInternals.constructor(internals.nms("PacketPlayOutEntityEquipment"), int.class, int.class, internals.nms("ItemStack"));
        }

        private static Optional<Handles> resolve(ServerInternals internals) {
            try {
                return Optional.of(new Handles(internals));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] NPCs are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private NpcBody create(Npc npc) {
            try {
                Location location = npc.location();
                Object profile = gameProfile.newInstance(npc.profileId(), npc.profileName());
                if (npc.skin().isPresent()) {
                    NpcSkin skin = npc.skin().get();
                    Object properties = profileProperties.invoke(profile);
                    properties.getClass().getMethod("put", Object.class, Object.class)
                        .invoke(properties, "textures", property.newInstance("textures", skin.value(), skin.signature()));
                }
                Object world = internals.world(location.getWorld());
                Object player = entityPlayer.newInstance(minecraftServer.invoke(Bukkit.getServer()), world, profile, interactManager.newInstance(world));
                setLocation.invoke(player, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                watch.invoke(dataWatcher.invoke(player), SKIN_LAYERS_INDEX, ALL_SKIN_LAYERS);
                Optional<Object> hand = npc.hand().isPresent() ? Optional.of(nmsCopy.invoke(null, npc.hand().get())) : Optional.empty();
                return new PacketNpc(player, (int) entityId.invoke(player), (byte) (location.getYaw() * DEGREES_TO_BYTE), hand);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Could not build NPC " + npc.id(), exception);
                return NpcBody.NONE;
            }
        }

        private Object info(Object action, Object player) throws ReflectiveOperationException {
            Object players = Array.newInstance(entityPlayerType, 1);
            Array.set(players, 0, player);
            return infoPacket.newInstance(action, players);
        }

        private final class PacketNpc implements NpcBody {

            private final Object player;
            private final int id;
            private final byte headYaw;
            private final Optional<Object> hand;

            private PacketNpc(Object player, int id, byte headYaw, Optional<Object> hand) {
                this.player = player;
                this.id = id;
                this.headYaw = headYaw;
                this.hand = hand;
            }

            @Override
            public int entityId() {
                return id;
            }

            @Override
            public void show(Player viewer) {
                send(viewer, "show", () -> {
                    internals.send(viewer, info(addPlayer, player));
                    internals.send(viewer, spawnPacket.newInstance(player));
                    internals.send(viewer, headRotationPacket.newInstance(player, headYaw));
                    if (hand.isPresent()) {
                        internals.send(viewer, equipmentPacket.newInstance(id, HAND_SLOT, hand.get()));
                    }
                });
            }

            @Override
            public void leaveTabList(Player viewer) {
                send(viewer, "update the tab list for", () -> internals.send(viewer, info(removePlayer, player)));
            }

            @Override
            public void hide(Player viewer) {
                send(viewer, "hide", () -> {
                    internals.send(viewer, destroyPacket.newInstance((Object) new int[] {id}));
                    internals.send(viewer, info(removePlayer, player));
                });
            }

            private void send(Player viewer, String action, PacketAction packets) {
                if (!viewer.isOnline()) {
                    return;
                }
                try {
                    packets.run();
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not " + action + " an NPC to " + viewer.getName(), exception);
                }
            }
        }
    }

    @FunctionalInterface
    private interface PacketAction {
        void run() throws ReflectiveOperationException;
    }
}
