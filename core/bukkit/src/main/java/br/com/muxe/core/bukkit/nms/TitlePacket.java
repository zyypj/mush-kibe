package br.com.muxe.core.bukkit.nms;

import java.util.List;

public sealed interface TitlePacket {

    static List<TitlePacket> showing(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return List.of(new Times(fadeIn, stay, fadeOut), new Subtitle(subtitle), new Main(title));
    }

    static List<TitlePacket> clearing() {
        return List.of(new Times(0, 0, 0), new Subtitle(""), new Reset());
    }

    record Times(int fadeIn, int stay, int fadeOut) implements TitlePacket {
    }

    record Subtitle(String text) implements TitlePacket {
    }

    record Main(String text) implements TitlePacket {
    }

    record Reset() implements TitlePacket {
    }
}
