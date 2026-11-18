package br.com.muxe.core.common.preference.domain;

import java.util.Arrays;
import java.util.Optional;

public enum Preference {
    CHAT("chat", Kind.TOGGLE, "true"),
    CONFIRM_LOBBY("confirm-lobby", Kind.TOGGLE, "false"),
    PRIVATE_MESSAGES("private-messages", Kind.PRIVATE_MESSAGES, PrivateMessages.PUBLIC.name()),
    PARTY_INVITES("party-invites", Kind.TOGGLE, "true"),
    CLAN_INVITES("clan-invites", Kind.TOGGLE, "true"),
    FRIEND_REQUESTS("friend-requests", Kind.TOGGLE, "true"),
    STATS_VISIBLE("stats-visible", Kind.TOGGLE, "true"),
    SITE_DISCORD("site-discord", Kind.TOGGLE, "true"),
    SITE_FREEZE_STATS("site-freeze-stats", Kind.TOGGLE, "true"),
    TAG_FORMAT("tag-format", Kind.TAG_FORMAT, TagFormat.DEFAULT.name());

    private final String key;
    private final Kind kind;
    private final String defaultValue;

    Preference(String key, Kind kind, String defaultValue) {
        this.key = key;
        this.kind = kind;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public Kind kind() {
        return kind;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public boolean accepts(String value) {
        return switch (kind) {
            case TOGGLE -> value.equals("true") || value.equals("false");
            case PRIVATE_MESSAGES -> PrivateMessages.parse(value).isPresent();
            case TAG_FORMAT -> TagFormat.parse(value).isPresent();
        };
    }

    public static Optional<Preference> byKey(String key) {
        return Arrays.stream(values()).filter(preference -> preference.key.equals(key)).findFirst();
    }

    public enum Kind {
        TOGGLE,
        PRIVATE_MESSAGES,
        TAG_FORMAT
    }
}
