package br.com.muxe.core.proxy.premium;

import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CachedPremiumLookup implements PremiumLookup {

    private static final String KEY_PREFIX = "auth:premium:";

    private final PremiumLookup remote;
    private final ExpiringStore store;
    private final Duration timeToLive;
    private final MetricRegistry metrics;
    private final Logger logger;

    public CachedPremiumLookup(PremiumLookup remote, ExpiringStore store, Duration timeToLive, MetricRegistry metrics, Logger logger) {
        this.remote = remote;
        this.store = store;
        this.timeToLive = timeToLive;
        this.metrics = metrics;
        this.logger = logger;
    }

    @Override
    public PremiumStatus lookup(String username) {
        String key = KEY_PREFIX + username.toLowerCase(Locale.ROOT);
        Optional<PremiumStatus> cached = readCache(key);
        if (cached.isPresent()) {
            record(cached.get(), "cache");
            return cached.get();
        }
        PremiumStatus status = remote.lookup(username);
        record(status, "remote");
        if (status != PremiumStatus.UNKNOWN) {
            writeCache(key, status);
        }
        return status;
    }

    private Optional<PremiumStatus> readCache(String key) {
        try {
            return store.get(key).map(PremiumStatus::valueOf);
        } catch (RuntimeException exception) {
            logger.log(Level.FINE, "Premium cache unavailable: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private void writeCache(String key, PremiumStatus status) {
        try {
            store.put(key, status.name(), timeToLive);
        } catch (RuntimeException exception) {
            logger.log(Level.FINE, "Premium cache unavailable: " + exception.getMessage());
        }
    }

    private void record(PremiumStatus status, String source) {
        metrics.counter("muxe_auth_premium_lookups_total", Tags.of("result", status.tag(), "source", source)).increment();
    }
}
