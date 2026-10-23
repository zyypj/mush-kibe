package br.com.muxe.minigame.engine.task;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class MatchTaskScope implements AutoCloseable {

    private final Plugin plugin;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private boolean closed;

    public MatchTaskScope(Plugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask later(Runnable task, long delayTicks) {
        return track(plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public BukkitTask repeat(Runnable task, long delayTicks, long periodTicks) {
        return track(plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    public int size() {
        return tasks.size();
    }

    @Override
    public void close() {
        closed = true;
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    private BukkitTask track(BukkitTask task) {
        if (closed) {
            task.cancel();
            throw new IllegalStateException("Match task scope is closed");
        }
        tasks.add(task);
        return task;
    }
}
