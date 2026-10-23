package br.com.muxe.core.common.metrics.export;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PrometheusTextFormatTest {

    @Test
    void rendersCountersGaugesAndHistograms() {
        SimpleMetricRegistry registry = new SimpleMetricRegistry();
        registry.counter("muxe_packets_published_total", Tags.of("type", "server.status")).add(3);
        registry.gauge("muxe_network_players_online", () -> 42);
        registry.timer("muxe_packets_handle_seconds", Tags.of("type", "player.connect")).record(Duration.ofMillis(3));

        String text = new PrometheusTextFormat(Tags.of("server", "lobby-1")).render(registry.snapshot());

        assertThat(text)
            .contains("# TYPE muxe_packets_published_total counter")
            .contains("muxe_packets_published_total{server=\"lobby-1\",type=\"server.status\"} 3")
            .contains("muxe_network_players_online{server=\"lobby-1\"} 42")
            .contains("# TYPE muxe_packets_handle_seconds histogram")
            .contains("muxe_packets_handle_seconds_bucket{le=\"0.0025\",server=\"lobby-1\",type=\"player.connect\"} 0")
            .contains("muxe_packets_handle_seconds_bucket{le=\"0.005\",server=\"lobby-1\",type=\"player.connect\"} 1")
            .contains("muxe_packets_handle_seconds_bucket{le=\"+Inf\",server=\"lobby-1\",type=\"player.connect\"} 1")
            .contains("muxe_packets_handle_seconds_count{server=\"lobby-1\",type=\"player.connect\"} 1");
    }

    @Test
    void escapesLabelValues() {
        SimpleMetricRegistry registry = new SimpleMetricRegistry();
        registry.counter("muxe_test_total", Tags.of("reason", "say \"hi\"")).increment();

        String text = new PrometheusTextFormat(Tags.EMPTY).render(registry.snapshot());

        assertThat(text).contains("muxe_test_total{reason=\"say \\\"hi\\\"\"} 1");
    }
}
