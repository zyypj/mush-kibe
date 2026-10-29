package br.com.muxe.core.proxy.tablist;

import br.com.muxe.core.common.config.source.ConfigSource;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public record TabListSettings(List<String> header, List<String> footer) {

    public TabListSettings {
        header = List.copyOf(header);
        footer = List.copyOf(footer);
    }

    public static TabListSettings from(ConfigSource source) {
        return new TabListSettings(source.stringList("tab-list.header"), source.stringList("tab-list.footer"));
    }

    public Component headerComponent() {
        return render(header);
    }

    public Component footerComponent() {
        return render(footer);
    }

    private static Component render(List<String> lines) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(String.join("\n", lines));
    }
}
