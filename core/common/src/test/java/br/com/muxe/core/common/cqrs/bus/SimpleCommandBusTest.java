package br.com.muxe.core.common.cqrs.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.cqrs.middleware.MetricsMiddleware;
import br.com.muxe.core.common.metrics.MetricId;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class SimpleCommandBusTest {

    private final SimpleMetricRegistry metrics = new SimpleMetricRegistry();
    private final SimpleCommandBus bus = new SimpleCommandBus(Runnable::run, List.of(new MetricsMiddleware(metrics)));

    @Test
    void dispatchesToTheRegisteredHandler() {
        bus.register(GreetCommand.class, command -> Result.success("Olá, " + command.name()));

        Result<String> result = bus.dispatch(new GreetCommand("Steve")).join();

        assertThat(result.orElseThrow()).isEqualTo("Olá, Steve");
    }

    @Test
    void recordsTheOutcomeOfEachCommand() {
        bus.register(GreetCommand.class, command -> Result.failure("greet.refused", "no"));

        bus.dispatch(new GreetCommand("Steve")).join();

        MetricId id = new MetricId("muxe_cqrs_handle_seconds", Tags.of("kind", "command", "message", "greet_command", "outcome", "failure"));
        assertThat(metrics.snapshot().timers()).containsKey(id);
    }

    @Test
    void failsWhenNoHandlerIsRegistered() {
        assertThatThrownBy(() -> bus.dispatch(new GreetCommand("Steve")).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(HandlerNotFoundException.class);
    }

    @Test
    void refusesTwoHandlersForTheSameCommand() {
        bus.register(GreetCommand.class, command -> Result.success("a"));

        assertThatThrownBy(() -> bus.register(GreetCommand.class, command -> Result.success("b")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void propagatesHandlerExceptions() {
        bus.register(GreetCommand.class, command -> {
            throw new IllegalArgumentException("bad name");
        });

        assertThatThrownBy(() -> bus.dispatch(new GreetCommand("Steve")).join())
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    record GreetCommand(String name) implements Command<Result<String>> {
    }
}
