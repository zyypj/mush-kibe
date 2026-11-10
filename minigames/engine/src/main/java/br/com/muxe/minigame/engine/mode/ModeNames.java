package br.com.muxe.minigame.engine.mode;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface ModeNames {

    String MODES = "modes";
    String DISPLAY_NAME = "display-name";

    Text of(String mode);

    static ModeNames from(LocalizedConfig texts) {
        Localized<Map<String, String>> names = texts.map(root -> read(root.getConfigurationSection(MODES)));
        return mode -> language -> names.in(language).getOrDefault(mode, names.standard().getOrDefault(mode, mode));
    }

    static ModeNames of(List<GameMode> modes) {
        Map<String, String> names = new HashMap<>();
        modes.forEach(mode -> names.put(mode.id(), mode.displayName()));
        return mode -> Text.plain(names.getOrDefault(mode, mode));
    }

    static ModeNames plain(Function<String, String> names) {
        return mode -> Text.plain(names.apply(mode));
    }

    default String in(Language language, String mode) {
        return of(mode).in(language);
    }

    static Map<String, String> read(ConfigurationSection modes) {
        Map<String, String> names = new HashMap<>();
        if (modes != null) {
            for (String id : modes.getKeys(false)) {
                names.put(id, modes.getString(id + "." + DISPLAY_NAME, id));
            }
        }
        return names;
    }
}
