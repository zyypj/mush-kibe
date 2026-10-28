package br.com.muxe.core.proxy.editor.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.proxy.editor.EditorFixture;
import br.com.muxe.core.proxy.editor.security.RateLimiter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EditorHttpServerTest {

    private final EditorFixture fixture = new EditorFixture();
    private final HttpClient client = HttpClient.newHttpClient();
    private EditorHttpServer server;
    private String base;

    @AfterEach
    void close() {
        if (server != null) {
            server.close();
        }
        fixture.close();
    }

    @Test
    void thePageIsServedWithStrictSecurityHeaders() throws Exception {
        start(100, 100);

        HttpResponse<String> page = send(HttpRequest.newBuilder(uri("/")).GET());

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<script type=\"module\" src=\"app.js\"></script>");
        assertThat(page.headers().firstValue("Content-Security-Policy")).contains(SecurityHeaders.CONTENT_SECURITY_POLICY);
        assertThat(page.headers().firstValue("X-Frame-Options")).contains("DENY");
        assertThat(page.headers().firstValue("Referrer-Policy")).contains("no-referrer");
        assertThat(page.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(page.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        for (String file : StaticAssets.FILES) {
            assertThat(send(HttpRequest.newBuilder(uri("/" + file)).GET()).statusCode()).as(file).isEqualTo(200);
        }
    }

    @Test
    void onlyTheKnownFilesAreServed() throws Exception {
        start(100, 100);

        assertThat(send(HttpRequest.newBuilder(uri("/config.yml")).GET()).statusCode()).isEqualTo(404);
        assertThat(send(HttpRequest.newBuilder(uri("/%2e%2e/config.yml")).GET()).statusCode()).isEqualTo(404);
        assertThat(send(HttpRequest.newBuilder(uri("/")).DELETE()).statusCode()).isEqualTo(405);
    }

    @Test
    void theApiAnswersJsonThatIsNeverCached() throws Exception {
        start(100, 100);
        String token = fixture.adminToken();

        HttpResponse<String> data = send(HttpRequest.newBuilder(uri("/api/data")).header("Authorization", "Bearer " + token).GET());
        HttpResponse<String> anonymous = send(HttpRequest.newBuilder(uri("/api/data")).GET());

        assertThat(data.statusCode()).isEqualTo(200);
        assertThat(data.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("application/json"));
        assertThat(data.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(data.body()).contains("\"defaultGroup\":\"membro\"");
        assertThat(anonymous.statusCode()).isEqualTo(401);
    }

    @Test
    void savesMustBeJsonFromTheEditorItself() throws Exception {
        start(100, 100);
        String token = fixture.adminToken();
        String body = "{\"changes\":[{\"type\":\"delete-track\",\"id\":\"staff\"}]}";

        HttpResponse<String> form = send(HttpRequest.newBuilder(uri("/api/changes"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(body)));
        HttpResponse<String> foreign = send(HttpRequest.newBuilder(uri("/api/changes"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Origin", "https://evil.example")
            .POST(HttpRequest.BodyPublishers.ofString(body)));
        HttpResponse<String> saved = send(HttpRequest.newBuilder(uri("/api/changes"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Origin", base)
            .POST(HttpRequest.BodyPublishers.ofString(body)));

        assertThat(form.statusCode()).isEqualTo(415);
        assertThat(foreign.statusCode()).isEqualTo(403);
        assertThat(saved.statusCode()).isEqualTo(200);
        assertThat(fixture.sync.data().track("staff")).isEmpty();
    }

    @Test
    void largeBodiesAreRefused() throws Exception {
        start(100, 100);

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/changes"))
            .header("Authorization", "Bearer " + fixture.adminToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"changes\":[\"" + "x".repeat(5000) + "\"]}")));

        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    void tooManyRequestsFromOneAddressAreThrottled() throws Exception {
        start(3, 100);
        String token = fixture.adminToken();

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(send(HttpRequest.newBuilder(uri("/api/session")).header("Authorization", "Bearer " + token).GET()).statusCode()).isEqualTo(200);
        }

        assertThat(send(HttpRequest.newBuilder(uri("/api/session")).header("Authorization", "Bearer " + token).GET()).statusCode()).isEqualTo(429);
    }

    @Test
    void guessingTokensGetsTheAddressBlocked() throws Exception {
        start(100, 2);
        String token = fixture.adminToken();

        send(HttpRequest.newBuilder(uri("/api/session")).header("Authorization", "Bearer guess-1").GET());
        send(HttpRequest.newBuilder(uri("/api/session")).header("Authorization", "Bearer guess-2").GET());

        assertThat(send(HttpRequest.newBuilder(uri("/api/session")).header("Authorization", "Bearer " + token).GET()).statusCode()).isEqualTo(429);
        fixture.clock.advance(Duration.ofMinutes(10));
        assertThat(send(HttpRequest.newBuilder(uri("/api/session")).header("Authorization", "Bearer " + fixture.adminToken()).GET()).statusCode()).isEqualTo(200);
    }

    private void start(int requests, int failures) throws IOException {
        EditorHttpServer.Options options = new EditorHttpServer.Options(
            fixture.api,
            StaticAssets.load(getClass().getClassLoader()),
            new RateLimiter(requests, Duration.ofMinutes(1), fixture.clock),
            new RateLimiter(failures, Duration.ofMinutes(10), fixture.clock),
            4096,
            "http://editor.invalid:1031",
            Logger.getLogger("test")
        );
        server = EditorHttpServer.start(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), options);
        base = "http://127.0.0.1:" + server.address().getPort();
    }

    private URI uri(String path) {
        return URI.create(base + path);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
