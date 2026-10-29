package br.com.muxe.core.bukkit.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class ServerInternals {

    private final String nmsPackage;
    private final String craftPackage;
    private final Method worldHandle;
    private final Method playerHandle;
    private final Field playerConnection;
    private final Method sendPacket;

    private ServerInternals(String nmsPackage, String craftPackage) throws ReflectiveOperationException {
        this.nmsPackage = nmsPackage;
        this.craftPackage = craftPackage;
        this.worldHandle = craft("CraftWorld").getMethod("getHandle");
        this.playerHandle = craft("entity.CraftPlayer").getMethod("getHandle");
        this.playerConnection = nms("EntityPlayer").getField("playerConnection");
        this.sendPacket = playerConnection.getType().getMethod("sendPacket", nms("Packet"));
    }

    static Optional<ServerInternals> resolve(String feature) {
        try {
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
            return Optional.of(new ServerInternals("net.minecraft.server." + version + ".", craftPackage + "."));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Bukkit.getLogger().log(Level.WARNING, "[Muxe] " + feature + " are unavailable on this server: " + exception);
            return Optional.empty();
        }
    }

    Class<?> nms(String name) throws ClassNotFoundException {
        return Class.forName(nmsPackage + name);
    }

    Class<?> craft(String name) throws ClassNotFoundException {
        return Class.forName(craftPackage + name);
    }

    Object world(World world) throws ReflectiveOperationException {
        return worldHandle.invoke(world);
    }

    Object player(Player player) throws ReflectiveOperationException {
        return playerHandle.invoke(player);
    }

    Object connection(Player player) throws ReflectiveOperationException {
        return playerConnection.get(player(player));
    }

    void send(Player player, Object packet) throws ReflectiveOperationException {
        sendPacket.invoke(connection(player), packet);
    }

    static Constructor<?> constructor(Class<?> type, Class<?>... parameters) throws NoSuchMethodException {
        return type.getConstructor(parameters);
    }

    static Object constant(Class<?> enumType, String name) {
        for (Object constant : enumType.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        throw new IllegalStateException(enumType.getName() + " has no " + name);
    }
}
