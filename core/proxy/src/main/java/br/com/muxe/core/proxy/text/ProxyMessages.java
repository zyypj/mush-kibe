package br.com.muxe.core.proxy.text;

import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.proxy.language.LocalizedConfig;
import com.velocitypowered.api.proxy.Player;
import java.util.Collection;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ProxyMessages {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final LocalizedConfig texts;
    private final LanguageLookup languages;

    public ProxyMessages(ConfigSource source) {
        this(LocalizedConfig.standardOnly(source), LanguageLookup.DEFAULT);
    }

    public ProxyMessages(LocalizedConfig texts, LanguageLookup languages) {
        this.texts = texts;
        this.languages = languages;
    }

    public Component get(String key, String... placeholders) {
        return get(Language.DEFAULT, key, placeholders);
    }

    public Component get(Player viewer, String key, String... placeholders) {
        return get(viewer.getUniqueId(), key, placeholders);
    }

    public Component get(UUID viewer, String key, String... placeholders) {
        return get(languages.of(viewer), key, placeholders);
    }

    public Component get(Language language, String key, String... placeholders) {
        ConfigSource source = texts.in(language);
        String path = "messages." + key;
        String text = source.find(path)
            .map(value -> value instanceof Collection<?> ? String.join("\n", source.stringList(path)) : String.valueOf(value))
            .orElse("&cMissing message: " + key);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            text = text.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
        }
        return SERIALIZER.deserialize(text);
    }
}
