package br.com.muxe.core.common.party.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PartyNotice(List<UUID> recipients, String key, List<String> placeholders, String click, String raw) {

    public PartyNotice {
        recipients = List.copyOf(recipients);
        placeholders = List.copyOf(placeholders);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(click, "click");
        Objects.requireNonNull(raw, "raw");
        if (placeholders.size() % 2 != 0) {
            throw new IllegalArgumentException("Placeholders come in name and value pairs");
        }
    }

    public static PartyNotice of(Collection<UUID> recipients, String key, String... placeholders) {
        return new PartyNotice(List.copyOf(recipients), key, Arrays.asList(placeholders), "", "");
    }

    public static PartyNotice raw(Collection<UUID> recipients, String text) {
        return new PartyNotice(List.copyOf(recipients), "", List.of(), "", text);
    }

    public PartyNotice clicking(String command) {
        return new PartyNotice(recipients, key, placeholders, command, raw);
    }

    public PartyNotice to(Collection<UUID> others) {
        return new PartyNotice(List.copyOf(others), key, placeholders, click, raw);
    }

    public boolean isRaw() {
        return key.isEmpty();
    }

    public Optional<String> clickCommand() {
        return click.isEmpty() ? Optional.empty() : Optional.of(click);
    }

    public Optional<String> value(String name) {
        for (int index = 0; index + 1 < placeholders.size(); index += 2) {
            if (placeholders.get(index).equals(name)) {
                return Optional.of(placeholders.get(index + 1));
            }
        }
        return Optional.empty();
    }
}
