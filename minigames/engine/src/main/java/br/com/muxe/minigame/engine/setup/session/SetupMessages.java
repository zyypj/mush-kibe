package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SetupMessages {

    public static final String RESOURCE = "/minigame-setup.yml";

    private SetupMessages() {
    }

    public static MessageCatalog defaults() {
        try (InputStream input = SetupMessages.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException(RESOURCE + " is missing from the plugin jar");
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
            return MessageCatalog.from(yaml.getConfigurationSection("messages"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static MessageCatalog withOverrides(MessageCatalog plugin) {
        return defaults().merge(plugin);
    }
}
