package br.com.muxe.core.bukkit.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public final class BlockUpdates {

    private BlockUpdates() {
    }

    public static void resend(Block block) {
        Holder.HANDLES.ifPresent(handles -> handles.resend(block));
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = ServerInternals.resolve("Block updates").flatMap(Handles::resolve);
    }

    private static final class Handles {

        private final ServerInternals server;
        private final Constructor<?> position;
        private final Method notify;

        private Handles(ServerInternals server) throws ReflectiveOperationException {
            this.server = server;
            Class<?> positionType = server.nms("BlockPosition");
            this.position = positionType.getConstructor(int.class, int.class, int.class);
            this.notify = server.nms("World").getMethod("notify", positionType);
        }

        private static Optional<Handles> resolve(ServerInternals server) {
            try {
                return Optional.of(new Handles(server));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Block updates are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private void resend(Block block) {
            try {
                notify.invoke(server.world(block.getWorld()), position.newInstance(block.getX(), block.getY(), block.getZ()));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not resend a block", exception);
            }
        }
    }
}
