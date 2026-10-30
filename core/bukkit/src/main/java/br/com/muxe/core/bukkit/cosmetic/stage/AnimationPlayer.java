package br.com.muxe.core.bukkit.cosmetic.stage;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class AnimationPlayer {

    private final Plugin plugin;

    public AnimationPlayer(Plugin plugin) {
        this.plugin = plugin;
    }

    public Playback play(BedAnimation animation, World world, BedFrame bed, Supplier<Collection<Player>> viewers, Runnable onFinish) {
        PacketStage stage = new PacketStage(world, viewers);
        Timeline timeline = new Timeline();
        animation.script(bed, stage, timeline, ThreadLocalRandom.current());
        Playback playback = new Playback(stage, onFinish);
        playback.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                timeline.run(playback.tick);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Bed animation " + animation.id() + " failed", exception);
                playback.finish();
                return;
            }
            playback.tick++;
            if (playback.tick > timeline.length()) {
                playback.finish();
            }
        }, 0L, 1L);
        return playback;
    }

    public static final class Playback {

        private final PacketStage stage;
        private final Runnable onFinish;
        private BukkitTask task;
        private int tick;
        private boolean finished;

        private Playback(PacketStage stage, Runnable onFinish) {
            this.stage = stage;
            this.onFinish = onFinish;
        }

        public boolean finished() {
            return finished;
        }

        public void cancel() {
            if (!finished) {
                finished = true;
                task.cancel();
                stage.close();
            }
        }

        private void finish() {
            if (!finished) {
                finished = true;
                task.cancel();
                stage.close();
                onFinish.run();
            }
        }
    }
}
