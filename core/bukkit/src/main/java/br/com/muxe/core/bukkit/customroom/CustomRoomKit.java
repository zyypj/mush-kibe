package br.com.muxe.core.bukkit.customroom;

import br.com.muxe.core.bukkit.customroom.menu.MapSelector;
import br.com.muxe.core.bukkit.customroom.menu.ModeSelector;
import br.com.muxe.core.bukkit.customroom.menu.OptionMenus;
import br.com.muxe.core.bukkit.customroom.menu.PresetMenu;
import br.com.muxe.core.bukkit.prompt.AnvilEdits;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.Objects;

public record CustomRoomKit(CustomRoomSettings settings, CatalogDirectory catalogs, AnvilEdits anvils, ModeSelector modes, MapSelector maps,
                            OptionMenus options, PresetMenu presets, Localized<CustomRoomSettings> localized) {

    public CustomRoomKit {
        Objects.requireNonNull(localized, "localized");
    }

    public CustomRoomKit(CustomRoomSettings settings, CatalogDirectory catalogs, AnvilEdits anvils, ModeSelector modes, MapSelector maps,
                         OptionMenus options, PresetMenu presets) {
        this(settings, catalogs, anvils, modes, maps, options, presets, Localized.same(settings));
    }

    public CustomRoomSettings settings(Language language) {
        return localized.in(language);
    }
}
