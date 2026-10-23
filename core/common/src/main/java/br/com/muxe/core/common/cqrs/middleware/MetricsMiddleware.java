package br.com.muxe.core.common.cqrs.middleware;

import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.result.Result;
import java.util.Locale;

public final class MetricsMiddleware implements HandlerMiddleware {

    private final MetricRegistry metrics;

    public MetricsMiddleware(MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public <R> R around(MessageKind kind, Object message, Invocation<R> next) throws Exception {
        long start = System.nanoTime();
        String outcome = "error";
        try {
            R result = next.proceed();
            outcome = result instanceof Result<?> value && value.isFailure() ? "failure" : "success";
            return result;
        } finally {
            Tags tags = Tags.of("kind", kind.tag(), "message", nameOf(message), "outcome", outcome);
            metrics.timer("muxe_cqrs_handle_seconds", tags).record(System.nanoTime() - start);
        }
    }

    private static String nameOf(Object message) {
        String simpleName = message.getClass().getSimpleName();
        return simpleName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
