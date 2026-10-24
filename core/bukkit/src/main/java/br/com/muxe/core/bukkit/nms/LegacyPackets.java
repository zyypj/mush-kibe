package br.com.muxe.core.bukkit.nms;

import com.google.gson.JsonObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LegacyPackets {

    private static final byte ACTION_BAR_POSITION = 2;

    private LegacyPackets() {
    }

    public static boolean isAvailable() {
        return Holder.HANDLES.isPresent();
    }

    public static boolean sendActionBar(Player player, String legacyText) {
        return Holder.HANDLES
            .map(handles -> handles.send(player, () -> handles.chatPacket.newInstance(handles.serialize.invoke(null, actionBarJson(legacyText)), ACTION_BAR_POSITION)))
            .orElse(false);
    }

    static String actionBarJson(String legacyText) {
        JsonObject component = new JsonObject();
        component.addProperty("text", legacyText == null ? "" : legacyText);
        return component.toString();
    }

    public static boolean sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return sendAll(player, TitlePacket.showing(title, subtitle, fadeIn, stay, fadeOut));
    }

    public static boolean clearTitle(Player player) {
        return sendAll(player, TitlePacket.clearing());
    }

    private static boolean sendAll(Player player, List<TitlePacket> packets) {
        return Holder.HANDLES.map(handles -> packets.stream().allMatch(packet -> handles.send(player, () -> handles.titlePacket(packet)))).orElse(false);
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = Handles.resolve();
    }

    @FunctionalInterface
    private interface PacketFactory {
        Object create() throws ReflectiveOperationException;
    }

    private static final class Handles {

        private final Method getHandle;
        private final Field playerConnection;
        private final Method sendPacket;
        private final Method serialize;
        private final Constructor<?> chatPacket;
        private final Constructor<?> titleTextPacket;
        private final Constructor<?> titleTimesPacket;
        private final Object titleAction;
        private final Object subtitleAction;
        private final Object resetAction;

        private Handles(
            Method getHandle,
            Field playerConnection,
            Method sendPacket,
            Method serialize,
            Constructor<?> chatPacket,
            Constructor<?> titleTextPacket,
            Constructor<?> titleTimesPacket,
            Object titleAction,
            Object subtitleAction,
            Object resetAction
        ) {
            this.getHandle = getHandle;
            this.playerConnection = playerConnection;
            this.sendPacket = sendPacket;
            this.serialize = serialize;
            this.chatPacket = chatPacket;
            this.titleTextPacket = titleTextPacket;
            this.titleTimesPacket = titleTimesPacket;
            this.titleAction = titleAction;
            this.subtitleAction = subtitleAction;
            this.resetAction = resetAction;
        }

        private static Optional<Handles> resolve() {
            try {
                String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
                String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
                String nms = "net.minecraft.server." + version + ".";
                Class<?> craftPlayer = Class.forName(craftPackage + ".entity.CraftPlayer");
                Class<?> entityPlayer = Class.forName(nms + "EntityPlayer");
                Field connection = entityPlayer.getField("playerConnection");
                Class<?> packet = Class.forName(nms + "Packet");
                Class<?> component = Class.forName(nms + "IChatBaseComponent");
                Class<?> serializer = Class.forName(nms + "IChatBaseComponent$ChatSerializer");
                Class<?> titlePacket = Class.forName(nms + "PacketPlayOutTitle");
                Class<?> titleActions = Class.forName(nms + "PacketPlayOutTitle$EnumTitleAction");
                return Optional.of(new Handles(
                    craftPlayer.getMethod("getHandle"),
                    connection,
                    connection.getType().getMethod("sendPacket", packet),
                    serializer.getMethod("a", String.class),
                    Class.forName(nms + "PacketPlayOutChat").getConstructor(component, byte.class),
                    titlePacket.getConstructor(titleActions, component),
                    titlePacket.getConstructor(int.class, int.class, int.class),
                    constant(titleActions, "TITLE"),
                    constant(titleActions, "SUBTITLE"),
                    constant(titleActions, "RESET")
                ));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Action bars and title timings are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private static Object constant(Class<?> enumType, String name) {
            for (Object constant : enumType.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(name)) {
                    return constant;
                }
            }
            throw new IllegalStateException(enumType.getName() + " has no " + name);
        }

        private Object titlePacket(TitlePacket packet) throws ReflectiveOperationException {
            return switch (packet) {
                case TitlePacket.Times times -> titleTimesPacket.newInstance(times.fadeIn(), times.stay(), times.fadeOut());
                case TitlePacket.Subtitle subtitle -> titleTextPacket.newInstance(subtitleAction, component(subtitle.text()));
                case TitlePacket.Main main -> titleTextPacket.newInstance(titleAction, component(main.text()));
                case TitlePacket.Reset reset -> titleTextPacket.newInstance(resetAction, null);
            };
        }

        private Object component(String legacyText) throws ReflectiveOperationException {
            String json = ComponentSerializer.toString(TextComponent.fromLegacyText(legacyText == null ? "" : legacyText));
            return serialize.invoke(null, json);
        }

        private boolean send(Player player, PacketFactory factory) {
            try {
                Object connection = playerConnection.get(getHandle.invoke(player));
                sendPacket.invoke(connection, factory.create());
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not send packet to " + player.getName(), exception);
                return false;
            }
        }
    }
}
