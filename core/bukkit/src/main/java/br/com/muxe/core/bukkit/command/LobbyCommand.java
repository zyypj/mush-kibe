package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.preference.LobbyConfirmations;
import br.com.muxe.core.bukkit.preference.PreferenceService;
import br.com.muxe.core.common.preference.domain.Preference;
import java.util.Optional;
import org.bukkit.entity.Player;

@Command(name = "lobby", aliases = {"hub", "l"}, description = "Sends you back to a lobby.")
public final class LobbyCommand {

    public static final String CONFIRM = "lobby-confirm";

    private final MuxeCore core;
    private final String lobbyGroup;
    private final PreferenceService preferences;
    private final LobbyConfirmations confirmations;

    public LobbyCommand(MuxeCore core, String lobbyGroup, PreferenceService preferences, LobbyConfirmations confirmations) {
        this.core = core;
        this.lobbyGroup = lobbyGroup;
        this.preferences = preferences;
        this.confirmations = confirmations;
    }

    @Default
    public void send(Player player) {
        if (preferences.of(player.getUniqueId()).on(Preference.CONFIRM_LOBBY) && !confirmations.confirmed(player.getUniqueId(), core.clock().instant())) {
            player.sendMessage(core.messages().get(player, CONFIRM));
            return;
        }
        Optional<String> lobby = core.servers().lobbyFor(core.identity(), lobbyGroup);
        if (lobby.isEmpty()) {
            player.sendMessage(core.messages().get(player, lobbyGroup.equals(core.identity().group()) ? "no-other-lobby" : "connect-failed"));
            return;
        }
        core.network().connectToServer(player.getUniqueId(), lobby.get()).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(core.messages().get(player, "connect-failed")));
            return null;
        });
    }
}
