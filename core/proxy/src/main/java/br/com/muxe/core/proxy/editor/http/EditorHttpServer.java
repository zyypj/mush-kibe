package br.com.muxe.core.proxy.editor.http;

import br.com.muxe.core.proxy.editor.api.ApiRequest;
import br.com.muxe.core.proxy.editor.api.ApiResponse;
import br.com.muxe.core.proxy.editor.api.EditorApi;
import br.com.muxe.core.proxy.editor.security.RateLimiter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EditorHttpServer implements AutoCloseable {

    private static final String JSON = "application/json; charset=utf-8";

    private final HttpServer server;
    private final ExecutorService executor;
    private final EditorApi api;
    private final StaticAssets assets;
    private final RateLimiter requests;
    private final RateLimiter failures;
    private final int maxBodyBytes;
    private final String publicUrl;
    private final Logger logger;

    private EditorHttpServer(HttpServer server, ExecutorService executor, Options options) {
        this.server = server;
        this.executor = executor;
        this.api = options.api();
        this.assets = options.assets();
        this.requests = options.requests();
        this.failures = options.failures();
        this.maxBodyBytes = options.maxBodyBytes();
        this.publicUrl = options.publicUrl();
        this.logger = options.logger();
    }

    public static EditorHttpServer start(InetSocketAddress address, Options options) throws IOException {
        HttpServer server = HttpServer.create(address, 64);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        EditorHttpServer editor = new EditorHttpServer(server, executor, options);
        server.createContext("/", editor::dispatch);
        server.setExecutor(executor);
        server.start();
        return editor;
    }

    public InetSocketAddress address() {
        return server.getAddress();
    }

    @Override
    public void close() {
        server.stop(1);
        executor.shutdownNow();
    }

    private void dispatch(HttpExchange exchange) {
        try (exchange) {
            String path = exchange.getRequestURI().getRawPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                handleStatic(exchange, path);
            }
        } catch (IOException | RuntimeException exception) {
            logger.log(Level.FINE, "Permission editor request failed", exception);
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws IOException {
        SecurityHeaders.apply(exchange.getResponseHeaders(), true);
        String client = client(exchange);
        if (failures.isBlocked(client) || !requests.tryAcquire(client)) {
            send(exchange, ApiResponse.error(429, ApiResponse.TOO_MANY_REQUESTS, "Too many requests, wait a minute"));
            return;
        }
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        Optional<String> authorization = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Authorization"));
        ApiRequest request;
        if (method.equals("GET")) {
            request = new ApiRequest("GET", path, ApiRequest.parseQuery(exchange.getRequestURI().getRawQuery()), authorization, "");
        } else if (method.equals("POST")) {
            Optional<ApiResponse> rejected = rejectPost(exchange);
            if (rejected.isPresent()) {
                send(exchange, rejected.get());
                return;
            }
            Optional<String> body = readBody(exchange.getRequestBody());
            if (body.isEmpty()) {
                send(exchange, ApiResponse.error(413, ApiResponse.TOO_LARGE, "The request is too large"));
                return;
            }
            request = ApiRequest.post(path, authorization, body.get());
        } else {
            send(exchange, ApiResponse.error(405, ApiResponse.METHOD_NOT_ALLOWED, "Method not allowed"));
            return;
        }
        ApiResponse response = api.handle(request);
        if (response.status() == 401 || response.status() == 403) {
            failures.tryAcquire(client);
        }
        send(exchange, response);
    }

    private Optional<ApiResponse> rejectPost(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && !OriginCheck.allowed(origin, exchange.getRequestHeaders().getFirst("Host"), publicUrl)) {
            return Optional.of(ApiResponse.error(403, ApiResponse.BAD_ORIGIN, "Requests must come from the editor page"));
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            return Optional.of(ApiResponse.error(415, ApiResponse.UNSUPPORTED_MEDIA_TYPE, "Send application/json"));
        }
        String length = exchange.getRequestHeaders().getFirst("Content-Length");
        if (length != null) {
            try {
                if (Long.parseLong(length.trim()) > maxBodyBytes) {
                    return Optional.of(ApiResponse.error(413, ApiResponse.TOO_LARGE, "The request is too large"));
                }
            } catch (NumberFormatException exception) {
                return Optional.of(ApiResponse.error(400, ApiResponse.INVALID_REQUEST, "Invalid Content-Length"));
            }
        }
        return Optional.empty();
    }

    private Optional<String> readBody(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(maxBodyBytes + 1);
        if (bytes.length > maxBodyBytes) {
            return Optional.empty();
        }
        return Optional.of(new String(bytes, StandardCharsets.UTF_8));
    }

    private void handleStatic(HttpExchange exchange, String path) throws IOException {
        SecurityHeaders.apply(exchange.getResponseHeaders(), false);
        if (!requests.tryAcquire(client(exchange))) {
            sendText(exchange, 429, "Too many requests");
            return;
        }
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (!method.equals("GET") && !method.equals("HEAD")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        Optional<StaticAssets.Asset> asset = assets.find(path);
        if (asset.isEmpty()) {
            sendText(exchange, 404, "Not found");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", asset.get().contentType());
        if (method.equals("HEAD")) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        byte[] bytes = asset.get().bytes();
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void send(HttpExchange exchange, ApiResponse response) throws IOException {
        byte[] bytes = response.json().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(response.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String client(HttpExchange exchange) {
        InetSocketAddress remote = exchange.getRemoteAddress();
        return remote == null || remote.getAddress() == null ? "unknown" : remote.getAddress().getHostAddress();
    }

    public record Options(EditorApi api, StaticAssets assets, RateLimiter requests, RateLimiter failures, int maxBodyBytes, String publicUrl, Logger logger) {
    }
}
