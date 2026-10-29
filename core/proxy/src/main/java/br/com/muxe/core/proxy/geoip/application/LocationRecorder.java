package br.com.muxe.core.proxy.geoip.application;

import br.com.muxe.core.common.account.application.command.SaveAccountLocationCommand;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.proxy.geoip.lookup.GeoLocator;
import br.com.muxe.core.proxy.geoip.lookup.PublicAddresses;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LocationRecorder {

    private final CommandBus commands;
    private final Supplier<GeoLocator> locator;
    private final Executor executor;
    private final Logger logger;

    public LocationRecorder(CommandBus commands, Supplier<GeoLocator> locator, Executor executor, Logger logger) {
        this.commands = commands;
        this.locator = locator;
        this.executor = executor;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> record(UUID playerId, InetAddress address) {
        if (!PublicAddresses.isPublic(address)) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> locate(address), executor)
            .thenCompose(location -> location.isUnknown()
                ? CompletableFuture.completedFuture(false)
                : commands.dispatch(new SaveAccountLocationCommand(playerId, location)).thenApply(Result::isSuccess))
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not save the location of " + playerId + ": " + rootType(error));
                return false;
            });
    }

    private AccountLocation locate(InetAddress address) {
        try {
            return locator.get().locate(address);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String rootType(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }
}
