package br.com.muxe.core.proxy;

import br.com.muxe.core.proxy.bootstrap.ProxyBootstrap;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MuxeProxyPlugin {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger = Logger.getLogger("muxe-proxy");
    private ProxyBootstrap bootstrap;

    @Inject
    public MuxeProxyPlugin(ProxyServer proxy, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        bootstrap = new ProxyBootstrap(this, proxy, dataDirectory, logger);
        try {
            bootstrap.enable();
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Muxe proxy failed to start, shutting down", exception);
            bootstrap.disable();
            proxy.shutdown();
        }
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (bootstrap != null) {
            bootstrap.disable();
        }
    }
}
