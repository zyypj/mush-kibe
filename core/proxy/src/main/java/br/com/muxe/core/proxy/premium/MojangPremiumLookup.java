package br.com.muxe.core.proxy.premium;

import br.com.muxe.core.common.account.domain.Usernames;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MojangPremiumLookup implements PremiumLookup {

    public static final URI DEFAULT_ENDPOINT = URI.create("https://api.mojang.com/users/profiles/minecraft/");

    private static final int FOUND = 200;
    private static final int NO_CONTENT = 204;
    private static final int NOT_FOUND = 404;

    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;
    private final Logger logger;

    public MojangPremiumLookup(HttpClient client, URI endpoint, Duration timeout, Logger logger) {
        this.client = client;
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.logger = logger;
    }

    @Override
    public PremiumStatus lookup(String username) {
        if (!Usernames.isValid(username)) {
            return PremiumStatus.CRACKED;
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint.resolve(username))
            .timeout(timeout)
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return switch (status) {
                case FOUND -> PremiumStatus.PREMIUM;
                case NO_CONTENT, NOT_FOUND -> PremiumStatus.CRACKED;
                default -> unknown(username, "HTTP " + status);
            };
        } catch (IOException exception) {
            return unknown(username, exception.toString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unknown(username, "interrupted");
        }
    }

    private PremiumStatus unknown(String username, String reason) {
        logger.log(Level.WARNING, "Premium lookup of " + username + " failed: " + reason);
        return PremiumStatus.UNKNOWN;
    }
}
