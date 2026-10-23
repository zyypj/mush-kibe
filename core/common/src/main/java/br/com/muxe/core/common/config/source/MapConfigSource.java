package br.com.muxe.core.common.config.source;

import java.util.Map;
import java.util.Optional;

public final class MapConfigSource implements ConfigSource {

    private final Map<?, ?> root;

    public MapConfigSource(Map<?, ?> root) {
        this.root = root == null ? Map.of() : root;
    }

    @Override
    public Optional<Object> find(String path) {
        Object current = root;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(segment)) {
                return Optional.empty();
            }
            current = map.get(segment);
        }
        return Optional.ofNullable(current);
    }
}
