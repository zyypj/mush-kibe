package br.com.muxe.core.bukkit.placement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class Reconciler<T> {

    private final Function<T, String> id;
    private final Map<String, T> shown = new LinkedHashMap<>();

    public Reconciler(Function<T, String> id) {
        this.id = id;
    }

    public Changes<T> next(List<T> wanted) {
        Map<String, T> next = new LinkedHashMap<>();
        wanted.forEach(item -> next.put(id.apply(item), item));
        List<String> removed = shown.keySet().stream().filter(key -> !next.containsKey(key)).toList();
        List<T> put = next.entrySet().stream().filter(entry -> !Objects.equals(shown.get(entry.getKey()), entry.getValue())).map(Map.Entry::getValue).toList();
        shown.clear();
        shown.putAll(next);
        return new Changes<>(new ArrayList<>(put), removed);
    }

    public List<T> all() {
        return List.copyOf(shown.values());
    }

    public record Changes<T>(List<T> put, List<String> removed) {

        public Changes {
            put = List.copyOf(put);
            removed = List.copyOf(removed);
        }
    }
}
