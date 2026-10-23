package br.com.muxe.core.bukkit.thread;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import org.bukkit.Bukkit;

public final class BukkitMainThreadGuard implements MainThreadGuard {

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }
}
