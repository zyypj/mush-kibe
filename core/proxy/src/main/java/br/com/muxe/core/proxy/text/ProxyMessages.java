package br.com.muxe.core.proxy.text;

import br.com.muxe.core.common.config.source.ConfigSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ProxyMessages {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigSource source;

    public ProxyMessages(ConfigSource source) {
        this.source = source;
    }

    public Component get(String key, String... placeholders) {
        String text = source.string("messages." + key, "&cMissing message: " + key);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            text = text.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
        }
        return SERIALIZER.deserialize(text);
    }
}
