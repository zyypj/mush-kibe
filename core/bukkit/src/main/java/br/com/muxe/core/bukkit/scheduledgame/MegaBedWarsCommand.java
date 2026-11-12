package br.com.muxe.core.bukkit.scheduledgame;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import org.bukkit.entity.Player;

@Command(name = "megabw", description = "Joins the Mega Bed Wars while it is open.")
public final class MegaBedWarsCommand {

    public static final String GAME = "mega";

    private final ScheduledPlay play;

    public MegaBedWarsCommand(ScheduledPlay play) {
        this.play = play;
    }

    @Default
    public void join(Player player) {
        play.play(player, GAME);
    }
}
