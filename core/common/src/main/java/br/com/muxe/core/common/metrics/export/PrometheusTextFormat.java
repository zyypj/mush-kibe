package br.com.muxe.core.common.metrics.export;

import br.com.muxe.core.common.metrics.MetricId;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.metrics.snapshot.MetricsSnapshot;
import br.com.muxe.core.common.metrics.snapshot.TimerSnapshot;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public final class PrometheusTextFormat {

    public static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private final Tags commonTags;

    public PrometheusTextFormat(Tags commonTags) {
        this.commonTags = commonTags;
    }

    public String render(MetricsSnapshot snapshot) {
        StringBuilder out = new StringBuilder(4096);
        renderFamily(out, snapshot.counters(), "counter", (id, value) -> sample(out, id.name(), id.tags(), value.doubleValue()));
        renderFamily(out, snapshot.gauges(), "gauge", (id, value) -> sample(out, id.name(), id.tags(), value));
        renderTimers(out, snapshot.timers());
        return out.toString();
    }

    private <V> void renderFamily(StringBuilder out, Map<MetricId, V> metrics, String type, BiConsumer<MetricId, V> writer) {
        String currentName = null;
        for (Map.Entry<MetricId, V> entry : sorted(metrics).entrySet()) {
            String name = entry.getKey().name();
            if (!name.equals(currentName)) {
                out.append("# TYPE ").append(name).append(' ').append(type).append('\n');
                currentName = name;
            }
            writer.accept(entry.getKey(), entry.getValue());
        }
    }

    private void renderTimers(StringBuilder out, Map<MetricId, TimerSnapshot> timers) {
        String currentName = null;
        for (Map.Entry<MetricId, TimerSnapshot> entry : sorted(timers).entrySet()) {
            MetricId id = entry.getKey();
            TimerSnapshot timer = entry.getValue();
            if (!id.name().equals(currentName)) {
                out.append("# TYPE ").append(id.name()).append(" histogram\n");
                currentName = id.name();
            }
            double[] bounds = timer.boundsSeconds();
            long[] cumulative = timer.cumulativeCounts();
            for (int index = 0; index < bounds.length; index++) {
                sample(out, id.name() + "_bucket", id.tags().and("le", formatNumber(bounds[index])), cumulative[index]);
            }
            sample(out, id.name() + "_bucket", id.tags().and("le", "+Inf"), timer.count());
            sample(out, id.name() + "_sum", id.tags(), timer.totalSeconds());
            sample(out, id.name() + "_count", id.tags(), timer.count());
            sample(out, id.name() + "_max", id.tags(), timer.maxSeconds());
        }
    }

    private void sample(StringBuilder out, String name, Tags tags, double value) {
        out.append(name);
        Tags all = commonTags.and(tags);
        if (!all.isEmpty()) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, String> tag : all.values().entrySet()) {
                if (!first) {
                    out.append(',');
                }
                out.append(tag.getKey()).append("=\"").append(escape(tag.getValue())).append('"');
                first = false;
            }
            out.append('}');
        }
        out.append(' ').append(formatNumber(value)).append('\n');
    }

    private static <V> Map<MetricId, V> sorted(Map<MetricId, V> metrics) {
        Map<MetricId, V> sorted = new TreeMap<>(Comparator.comparing(MetricId::name).thenComparing(id -> id.tags().values().toString()));
        sorted.putAll(metrics);
        return sorted;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String formatNumber(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
