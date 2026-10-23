**English** | [Português](0004-own-metrics-registry.pt-BR.md)

# 0004. A small metrics registry with Prometheus text export

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

Every packet, request, command, transaction and tick must be measured without slowing the game down, on Spigot and on Velocity, and scraped by a standard monitoring stack.

## Decision

Muxe has its own `MetricRegistry` interface with counters, timers (fixed-bucket histograms) and gauges, implemented by `SimpleMetricRegistry` with `LongAdder`s, and exported in the Prometheus text format by an embedded JDK HTTP server.

## Alternatives considered

- **Micrometer with the Prometheus registry:** the standard on the JVM, but it adds several libraries to shade into every plugin, and Muxe only needs a small part of it.
- **Dropwizard Metrics:** same trade-off, with a less standard export.

## Consequences

- No dependency and full control over cost: recording is lock-free, gauges are read only when scraped.
- Features depend on `MetricRegistry`, so the implementation can be swapped for Micrometer later without changing them.
- Exporters other than Prometheus must be written if they are ever needed.
