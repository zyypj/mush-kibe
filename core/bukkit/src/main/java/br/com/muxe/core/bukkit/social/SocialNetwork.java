package br.com.muxe.core.bukkit.social;

import br.com.muxe.core.bukkit.item.SkullTexture;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record SocialNetwork(String id, String label, Optional<String> texture, List<String> message) {

    public SocialNetwork {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(texture, "texture");
        message = List.copyOf(message);
    }

    public static SocialNetwork from(ConfigurationSection section) {
        return new SocialNetwork(
            section.getName(),
            section.getString("label", ""),
            Optional.ofNullable(SkullTexture.value(section.getString("texture"))),
            section.getStringList("message")
        );
    }
}
