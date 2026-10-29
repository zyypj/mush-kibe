package br.com.muxe.core.proxy.motd;

import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.common.text.TextWidth;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public record MotdSettings(List<String> lines, int maxPlayers) {

    public static final int DEFAULT_MAX_PLAYERS = 3000;

    private static final String LINE_BREAK = "&r\n";

    public MotdSettings {
        lines = List.copyOf(lines);
    }

    public static MotdSettings from(ConfigSource source) {
        List<String> centered = source.stringList("motd.lines").stream()
            .map(line -> TextWidth.center(line, TextWidth.SERVER_LIST_WIDTH))
            .toList();
        return new MotdSettings(centered, source.integer("motd.max-players", DEFAULT_MAX_PLAYERS));
    }

    public Component description() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(String.join(LINE_BREAK, lines));
    }
}
