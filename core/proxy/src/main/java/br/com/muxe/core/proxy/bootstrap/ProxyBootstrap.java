package br.com.muxe.core.proxy.bootstrap;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.settings.MuxeSettings;
import br.com.muxe.core.common.config.settings.SettingsLoader;
import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.common.runtime.CoreRuntime;
import br.com.muxe.core.common.runtime.PlatformHooks;
import br.com.muxe.core.proxy.auth.AccessPolicy;
import br.com.muxe.core.proxy.auth.AuthStatusResponder;
import br.com.muxe.core.proxy.auth.AuthenticationGate;
import br.com.muxe.core.proxy.config.AuthenticationSettings;
import br.com.muxe.core.proxy.config.ProxyConfigFile;
import br.com.muxe.core.proxy.config.RoutingSettings;
import br.com.muxe.core.proxy.listener.ConnectionListener;
import br.com.muxe.core.proxy.listener.LoginListener;
import br.com.muxe.core.proxy.listener.NetworkGateListener;
import br.com.muxe.core.proxy.listener.PreLoginListener;
import br.com.muxe.core.proxy.load.ProxyServerLoad;
import br.com.muxe.core.proxy.login.LoginModeResolver;
import br.com.muxe.core.proxy.packet.ProxyPacketHandlers;
import br.com.muxe.core.proxy.premium.CachedPremiumLookup;
import br.com.muxe.core.proxy.premium.MojangPremiumLookup;
import br.com.muxe.core.proxy.premium.PremiumLookup;
import br.com.muxe.core.proxy.routing.NetworkGate;
import br.com.muxe.core.proxy.routing.ServerRouter;
import br.com.muxe.core.proxy.session.LoginSessions;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.proxy.ProxyServer;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class ProxyBootstrap {

    private final Object plugin;
    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;
    private CoreRuntime runtime;

    public ProxyBootstrap(Object plugin, ProxyServer proxy, Path dataDirectory, Logger logger) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void enable() {
        MapConfigSource config = ProxyConfigFile.load(dataDirectory);
        MuxeSettings settings = new SettingsLoader(SecretResolver.system()).load(config);
        if (!settings.server().isProxy()) {
            throw new ConfigException("server.kind must be PROXY on Velocity");
        }
        RoutingSettings routing = RoutingSettings.from(config);
        AuthenticationSettings authentication = AuthenticationSettings.from(config);
        ProxyMessages messages = new ProxyMessages(config);

        runtime = CoreRuntime.start(settings, new PlatformHooks(logger, MainThreadGuard.none(), new ProxyServerLoad(proxy)));

        AuthenticationGate gate = new AuthenticationGate();
        AccessPolicy access = new AccessPolicy(gate, runtime.directory(), routing.authGroup());
        ServerRouter router = new ServerRouter(proxy, runtime.directory());
        LoginSessions sessions = new LoginSessions(runtime.store(), authentication.sessionDuration());
        LoginModeResolver resolver = new LoginModeResolver(runtime.queries(), premiumLookup(authentication), authentication.premiumAutoLogin());
        runtime.metrics().gauge("muxe_proxy_authenticated_players", gate::size);

        new ProxyPacketHandlers(proxy, gate, access, router, routing, sessions, messages, logger).register(runtime.packets());
        new AuthStatusResponder(gate, playerId -> proxy.getPlayer(playerId).isPresent()).register(runtime.packets());
        proxy.getEventManager().register(plugin, new PreLoginListener(resolver, runtime.executors().io(), messages, runtime.metrics(), logger));
        proxy.getEventManager().register(plugin, new NetworkGateListener(new NetworkGate(runtime.directory(), routing), messages));
        proxy.getEventManager().register(plugin, new LoginListener(runtime.commands(), gate, sessions, messages, runtime.metrics(), logger));
        proxy.getEventManager().register(plugin, new ConnectionListener(gate, access, router, routing, messages));

        logger.info("Muxe proxy ready as " + settings.server().id());
    }

    public void disable() {
        if (runtime != null) {
            runtime.close();
            runtime = null;
        }
    }

    private PremiumLookup premiumLookup(AuthenticationSettings authentication) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(authentication.premiumLookupTimeout())
            .executor(runtime.executors().io())
            .build();
        MojangPremiumLookup mojang = new MojangPremiumLookup(client, MojangPremiumLookup.DEFAULT_ENDPOINT, authentication.premiumLookupTimeout(), logger);
        return new CachedPremiumLookup(
            mojang,
            runtime.store(),
            authentication.premiumCacheDuration(),
            authentication.crackedCacheDuration(),
            runtime.metrics(),
            logger
        );
    }
}
