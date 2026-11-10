package br.com.muxe.core.bukkit.hotbar;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class Hotbar {

    private final Localized<HotbarSettings> settings;
    private final LanguageLookup languages;

    public Hotbar(HotbarSettings settings) {
        this(Localized.same(settings), LanguageLookup.DEFAULT);
    }

    public Hotbar(Localized<HotbarSettings> settings, LanguageLookup languages) {
        this.settings = settings;
        this.languages = languages;
    }

    public void give(Player player, Visibility visibility) {
        HotbarSettings current = settingsOf(player);
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(current.gameSelector().slot(), current.gameSelector().item().build());
        inventory.setItem(current.profile().slot(), profile(player, current).build());
        inventory.setItem(current.cosmetics().slot(), current.cosmetics().item().build());
        inventory.setItem(current.lobbySelector().slot(), current.lobbySelector().item().build());
        showVisibility(player, visibility);
    }

    public void refresh(Player player, Visibility visibility) {
        HotbarSettings current = settingsOf(player);
        replace(player, current.gameSelector().slot(), current.gameSelector().item());
        replace(player, current.profile().slot(), profile(player, current));
        replace(player, current.cosmetics().slot(), current.cosmetics().item());
        replace(player, current.lobbySelector().slot(), current.lobbySelector().item());
        replace(player, current.visibilitySlot(), current.visibilityItem(visibility));
    }

    public void showVisibility(Player player, Visibility visibility) {
        HotbarSettings current = settingsOf(player);
        player.getInventory().setItem(current.visibilitySlot(), current.visibilityItem(visibility).build());
    }

    public Optional<HotbarAction> actionFor(int slot, ItemStack held) {
        if (held == null) {
            return Optional.empty();
        }
        return settings.standard().actionAt(slot).filter(action -> expectedMaterial(action, held));
    }

    private HotbarSettings settingsOf(Player player) {
        return settings.in(languages.of(player.getUniqueId()));
    }

    private static ItemBuilder profile(Player player, HotbarSettings current) {
        return current.profile().item().skullOwner(player.getName());
    }

    private static void replace(Player player, int slot, ItemBuilder item) {
        ItemStack held = player.getInventory().getItem(slot);
        if (held != null && held.getType() == item.material()) {
            player.getInventory().setItem(slot, item.build());
        }
    }

    private boolean expectedMaterial(HotbarAction action, ItemStack held) {
        HotbarSettings standard = settings.standard();
        return switch (action) {
            case GAME_SELECTOR -> held.getType() == standard.gameSelector().item().material();
            case PROFILE -> held.getType() == standard.profile().item().material();
            case COSMETICS -> held.getType() == standard.cosmetics().item().material();
            case VISIBILITY -> held.getType() == standard.visibilityItem(Visibility.ALL).material();
            case LOBBY_SELECTOR -> held.getType() == standard.lobbySelector().item().material();
        };
    }
}
