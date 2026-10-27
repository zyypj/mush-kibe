package br.com.muxe.core.bukkit.hotbar;

import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class Hotbar {

    private final HotbarSettings settings;

    public Hotbar(HotbarSettings settings) {
        this.settings = settings;
    }

    public void give(Player player, Visibility visibility) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(settings.gameSelector().slot(), settings.gameSelector().item().build());
        inventory.setItem(settings.profile().slot(), settings.profile().item().skullOwner(player.getName()).build());
        inventory.setItem(settings.cosmetics().slot(), settings.cosmetics().item().build());
        inventory.setItem(settings.lobbySelector().slot(), settings.lobbySelector().item().build());
        showVisibility(player, visibility);
    }

    public void showVisibility(Player player, Visibility visibility) {
        player.getInventory().setItem(settings.visibilitySlot(), settings.visibilityItem(visibility).build());
    }

    public Optional<HotbarAction> actionFor(int slot, ItemStack held) {
        if (held == null) {
            return Optional.empty();
        }
        return settings.actionAt(slot).filter(action -> expectedMaterial(action, held));
    }

    private boolean expectedMaterial(HotbarAction action, ItemStack held) {
        return switch (action) {
            case GAME_SELECTOR -> held.getType() == settings.gameSelector().item().material();
            case PROFILE -> held.getType() == settings.profile().item().material();
            case COSMETICS -> held.getType() == settings.cosmetics().item().material();
            case VISIBILITY -> held.getType() == settings.visibilityItem(Visibility.ALL).material();
            case LOBBY_SELECTOR -> held.getType() == settings.lobbySelector().item().material();
        };
    }
}
