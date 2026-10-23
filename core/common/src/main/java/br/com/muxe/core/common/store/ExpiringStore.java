package br.com.muxe.core.common.store;

import java.time.Duration;
import java.util.Optional;

public interface ExpiringStore {

    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);

    boolean delete(String key);
}
