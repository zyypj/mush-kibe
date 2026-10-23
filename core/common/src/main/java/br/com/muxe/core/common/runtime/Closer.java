package br.com.muxe.core.common.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Closer implements AutoCloseable {

    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private final Logger logger;

    Closer(Logger logger) {
        this.logger = logger;
    }

    <T extends AutoCloseable> T register(T resource) {
        resources.push(resource);
        return resource;
    }

    @Override
    public void close() {
        while (!resources.isEmpty()) {
            AutoCloseable resource = resources.pop();
            try {
                resource.close();
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to close " + resource.getClass().getSimpleName(), exception);
            }
        }
    }
}
