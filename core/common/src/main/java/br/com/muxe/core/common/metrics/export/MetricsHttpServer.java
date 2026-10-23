package br.com.muxe.core.common.metrics.export;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class MetricsHttpServer implements AutoCloseable {

    public static final String PATH = "/metrics";

    private final HttpServer server;

    private MetricsHttpServer(HttpServer server) {
        this.server = server;
    }

    public static MetricsHttpServer start(InetSocketAddress address, Supplier<String> body, Executor executor) throws IOException {
        HttpServer server = HttpServer.create(address, 0);
        server.createContext("/", exchange -> handle(exchange, body));
        server.setExecutor(executor);
        server.start();
        return new MetricsHttpServer(server);
    }

    public InetSocketAddress address() {
        return server.getAddress();
    }

    private static void handle(HttpExchange exchange, Supplier<String> body) throws IOException {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod()) || !PATH.equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", PrometheusTextFormat.CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
