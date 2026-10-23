package br.com.muxe.core.bukkit.thread;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class BukkitMainThreadExecutor implements Executor {

    private final Plugin plugin;

    public BukkitMainThreadExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        if (!plugin.isEnabled()) {
            plugin.getLogger().log(Level.FINE, "Dropping main thread task scheduled while disabled");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
