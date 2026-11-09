package br.com.muxe.core.bukkit.customroom;

import br.com.muxe.core.bukkit.customroom.menu.MapSelector;
import br.com.muxe.core.bukkit.customroom.menu.ModeSelector;
import br.com.muxe.core.bukkit.customroom.menu.OptionMenus;
import br.com.muxe.core.bukkit.customroom.menu.PresetMenu;
import br.com.muxe.core.bukkit.prompt.AnvilEdits;

public record CustomRoomKit(CustomRoomSettings settings, CatalogDirectory catalogs, AnvilEdits anvils, ModeSelector modes, MapSelector maps,
                            OptionMenus options, PresetMenu presets) {
}
