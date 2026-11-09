package br.com.muxe.core.common.customroom.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record RoomSchema(String game, String root, Map<String, OptionPage> pages, Map<String, Currency> currencies) {

    public RoomSchema {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(root, "root");
        pages = Map.copyOf(new LinkedHashMap<>(pages));
        currencies = Collections.unmodifiableMap(new LinkedHashMap<>(currencies));
        if (!pages.containsKey(root)) {
            throw new IllegalArgumentException("The root page " + root + " of " + game + " does not exist");
        }
    }

    public Optional<OptionPage> page(String id) {
        return Optional.ofNullable(pages.get(id));
    }

    public Optional<Currency> currency(String id) {
        return Optional.ofNullable(currencies.get(id));
    }

    public Currency currencyOrFirst(String id) {
        return currency(id).orElseGet(() -> currencies.values().iterator().next());
    }

    public String nextCurrency(String id) {
        List<String> ids = new ArrayList<>(currencies.keySet());
        int index = ids.indexOf(id);
        return ids.get((index + 1) % ids.size());
    }

    public List<OptionEntry> entries() {
        List<OptionEntry> entries = new ArrayList<>();
        pages.values().forEach(page -> entries.addAll(page.entries()));
        return entries;
    }

    public <T extends OptionEntry> List<T> entries(Class<T> type) {
        return entries().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
