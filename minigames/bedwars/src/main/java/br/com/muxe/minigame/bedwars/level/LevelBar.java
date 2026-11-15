package br.com.muxe.minigame.bedwars.level;

import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import org.bukkit.entity.Player;

public final class LevelBar {

    private LevelBar() {
    }

    public static BedWarsLevels.Progress show(Player player, long totalXp) {
        BedWarsLevels.Progress progress = BedWarsLevels.progress(Math.max(0, totalXp));
        player.setLevel(progress.level());
        player.setExp(progress.fill());
        return progress;
    }
}
