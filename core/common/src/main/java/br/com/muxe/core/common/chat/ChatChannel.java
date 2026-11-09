package br.com.muxe.core.common.chat;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ChatChannel {
    DEFAULT("default", "padrao"),
    PARTY("party"),
    CLAN("clan");

    private final List<String> names;

    ChatChannel(String... names) {
        this.names = List.of(names);
    }

    public String id() {
        return names.getFirst();
    }

    public List<String> names() {
        return names;
    }

    public static Optional<ChatChannel> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String plain = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (ChatChannel channel : values()) {
            if (channel.names.contains(plain)) {
                return Optional.of(channel);
            }
        }
        return Optional.empty();
    }
}
