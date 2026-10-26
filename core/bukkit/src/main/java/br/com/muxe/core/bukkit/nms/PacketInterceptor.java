package br.com.muxe.core.bukkit.nms;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class PacketInterceptor implements Listener {

    private static final String HANDLER_PREFIX = "muxe_entity_use_";
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static final String MINECRAFT_HANDLER = "packet_handler";

    private final Handles handles;
    private final EntityUseListener listener;
    private final String handler;

    private PacketInterceptor(Handles handles, EntityUseListener listener) {
        this.handles = handles;
        this.listener = listener;
        this.handler = handlerName(NEXT_ID.incrementAndGet());
    }

    static String handlerName(int id) {
        return HANDLER_PREFIX + id;
    }

    public static Optional<PacketInterceptor> start(Plugin plugin, EntityUseListener listener) {
        Optional<PacketInterceptor> interceptor = ServerInternals.resolve("Entity clicks").flatMap(Handles::resolve).map(handles -> new PacketInterceptor(handles, listener));
        interceptor.ifPresent(started -> {
            plugin.getServer().getPluginManager().registerEvents(started, plugin);
            plugin.getServer().getOnlinePlayers().forEach(started::inject);
        });
        return interceptor;
    }

    public void stop() {
        Bukkit.getOnlinePlayers().forEach(this::eject);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        eject(event.getPlayer());
    }

    private void inject(Player player) {
        handles.channel(player).ifPresent(channel -> channel.eventLoop().execute(() -> {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(handler) == null && pipeline.get(MINECRAFT_HANDLER) != null) {
                pipeline.addBefore(MINECRAFT_HANDLER, handler, new UseHandler(player));
            }
        }));
    }

    private void eject(Player player) {
        handles.channel(player).ifPresent(channel -> channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(handler) != null) {
                channel.pipeline().remove(handler);
            }
        }));
    }

    private final class UseHandler extends ChannelDuplexHandler {

        private final Player player;

        private UseHandler(Player player) {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            if (handles.useEntity.isInstance(message)) {
                try {
                    int entityId = handles.entityId.getInt(message);
                    Object action = handles.action.get(message);
                    if (action != null) {
                        listener.onUse(player, entityId, EntityUse.valueOf(((Enum<?>) action).name()));
                    }
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not read an entity click of " + player.getName(), exception);
                }
            }
            super.channelRead(context, message);
        }
    }

    private static final class Handles {

        private final ServerInternals internals;
        private final Field networkManager;
        private final Field channel;
        private final Class<?> useEntity;
        private final Field entityId;
        private final Field action;

        private Handles(ServerInternals internals) throws ReflectiveOperationException {
            this.internals = internals;
            Class<?> connection = internals.nms("PlayerConnection");
            Class<?> manager = internals.nms("NetworkManager");
            this.networkManager = fieldOfType(connection, manager);
            this.channel = fieldOfType(manager, Channel.class);
            this.useEntity = internals.nms("PacketPlayInUseEntity");
            this.entityId = fieldOfType(useEntity, int.class);
            this.action = fieldOfType(useEntity, internals.nms("PacketPlayInUseEntity$EnumEntityUseAction"));
        }

        private static Optional<Handles> resolve(ServerInternals internals) {
            try {
                return Optional.of(new Handles(internals));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Entity clicks are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private Optional<Channel> channel(Player player) {
            try {
                Object manager = networkManager.get(internals.connection(player));
                return Optional.ofNullable((Channel) channel.get(manager));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not reach the connection of " + player.getName(), exception);
                return Optional.empty();
            }
        }

        private static Field fieldOfType(Class<?> owner, Class<?> type) throws NoSuchFieldException {
            for (Field field : owner.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && field.getType().equals(type)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new NoSuchFieldException(owner.getSimpleName() + " has no field of type " + type.getSimpleName());
        }
    }
}
