**English** | [Português](README.pt-BR.md)

# Observability

What the network measures and how to read it. Problems are seen with numbers instead of guesses.

Code: `core/common/src/main/java/br/com/muxe/core/common/metrics`.

## How metrics are exposed

Every server and proxy serves its metrics in the [Prometheus text format](https://prometheus.io/docs/instrumenting/exposition_formats/) at:

```text
http://<metrics.host>:<metrics.port>/metrics
```

| Key | Default | Description |
|---|---|---|
| `metrics.enabled` | `true` | Starts the HTTP endpoint. |
| `metrics.host` | `127.0.0.1` | Address to bind. Keep it private. |
| `metrics.port` | `9400` (`9410` on the proxy) | Must be unique per process on the same machine. |

Every sample carries the labels `server` and `group` of the process, so one Prometheus can scrape the whole network.

`/muxe metrics` shows a short summary in game.

## Instruments

| Instrument | Prometheus type | Notes |
|---|---|---|
| `Counter` | `counter` | Only goes up. Names end with `_total`. |
| `Timer` | `histogram` | Buckets from 0.5 ms to 10 s, plus `_sum`, `_count` and `_max`. Names end with `_seconds`. |
| Gauge | `gauge` | A value read when the endpoint is scraped. |

The registry (`SimpleMetricRegistry`) has no dependencies and uses `LongAdder`s, so recording is cheap and never blocks. The `MetricRegistry` interface allows replacing it (for example with Micrometer) without touching features. See [ADR 0004](../../decisions/0004-own-metrics-registry.md).

## Messaging

Labels: `type` is the packet id, `reason` the rejection reason, `outcome` the request result.

| Metric | Type | What it shows |
|---|---|---|
| `muxe_packets_published_total{type}` | counter | Packets published. |
| `muxe_packets_published_bytes_total{type}` | counter | Bytes published, envelope included. |
| `muxe_packets_publish_seconds{type}` | histogram | Time from publish call to Redis acknowledging it. |
| `muxe_packets_publish_failures_total{type}` | counter | Publishes that failed (encoding, size or Redis). |
| `muxe_packets_received_total{type}` | counter | Valid envelopes received for a known type. |
| `muxe_packets_received_bytes_total{type}` | counter | Bytes received. |
| `muxe_packets_latency_seconds{type}` | histogram | From the sender's clock to the receiver's clock. Includes clock skew between machines. |
| `muxe_packets_handle_seconds{type}` | histogram | Time spent in each handler. |
| `muxe_packets_handler_failures_total{type}` | counter | Handlers that threw. |
| `muxe_packets_duplicates_total{type}` | counter | Packets dropped by idempotency. |
| `muxe_packets_unhandled_total{type}` | counter | Packets received with no handler on this server. |
| `muxe_packets_rejected_total{reason}` | counter | `too_large`, `malformed`, `bad_magic`, `unsupported_protocol`, `bad_signature`, `unknown_type`, `expired`, `malformed_payload`, `late_response`, `idempotency_unavailable`. |
| `muxe_requests_total{type,outcome}` | counter | Requests by `success`, `timeout` or `failure`. |
| `muxe_request_seconds{type,outcome}` | histogram | Round trip of requests. |
| `muxe_requests_pending` | gauge | Requests waiting for a response. |
| `muxe_transport_connected` | gauge | 1 while the Redis subscription is active. |
| `muxe_transport_reconnects_total` | counter | Times the Redis subscription was lost. |

`bad_signature` above zero means a misconfigured server or someone publishing to Redis without the secret: investigate.

## Authentication

| Metric | Type | What it shows |
|---|---|---|
| `muxe_auth_login_modes_total{mode}` | counter | Login modes decided by the proxy: `premium`, `cracked`, `undecided`. |
| `muxe_auth_logins_total{mode}` | counter | How players were let in: `premium`, `session` or `password`. |
| `muxe_auth_premium_lookups_total{result,source}` | counter | Mojang lookups by result (`premium`, `cracked`, `unknown`) and source (`cache`, `remote`). |
| `muxe_auth_challenges_total{outcome}` | counter | Anti-bot challenge answers: `passed`, `wrong`, `failed`. |
| `muxe_accounts_names_reclaimed_total` | counter | Names a premium player took from another account. Each one is also logged with both ids. |

A growing share of `undecided` or `unknown` means Mojang is unreachable or rate-limiting the proxy.

## Application

| Metric | Type | What it shows |
|---|---|---|
| `muxe_cqrs_handle_seconds{kind,message,outcome}` | histogram | Every command and query. `outcome` is `success`, `failure` (a `Result.Failure`) or `error` (an exception). |

## Persistence

| Metric | Type | What it shows |
|---|---|---|
| `muxe_db_transaction_seconds{operation,outcome}` | histogram | Every transaction, by operation and `commit`, `rollback` or `error`. |
| `muxe_db_connections_active` | gauge | Connections in use. |
| `muxe_db_connections_idle` | gauge | Idle connections. |
| `muxe_db_connections_pending` | gauge | Threads waiting for a connection. Above zero for long means the pool is too small or a query is too slow. |
| `muxe_db_connections_total` | gauge | Connections in the pool. |

## Network

| Metric | Type | What it shows |
|---|---|---|
| `muxe_network_servers_alive` | gauge | Servers in this process's directory. |
| `muxe_network_players_online` | gauge | Players on game servers, as seen by this process. |
| `muxe_proxy_authenticated_players` | gauge | Players that passed authentication on this proxy. |

## Spigot servers

| Metric | Type | What it shows |
|---|---|---|
| `muxe_server_tps` | gauge | Ticks per second over the last 100 ticks (max 20). |
| `muxe_server_tick_interval_seconds` | histogram | Time between ticks. Above 0.05 s the server cannot keep 20 TPS. |
| `muxe_server_players_online` | gauge | Players on this server. |
| `muxe_server_players_max` | gauge | Slots of this server. |

## Minigames

| Metric | Type | What it shows |
|---|---|---|
| `muxe_matches_active{game}` | gauge | Matches currently open. |
| `muxe_arenas_available{game}` | gauge | Arena instances ready to be used. |
| `muxe_arenas_disabled{game}` | gauge | Arena instances disabled after failed resets. Above zero needs staff. |

## JVM

`jvm_memory_heap_used_bytes`, `jvm_memory_heap_committed_bytes`, `jvm_memory_heap_max_bytes`, `jvm_memory_non_heap_used_bytes`, `jvm_threads_live`, `jvm_threads_daemon`, `jvm_uptime_seconds`, `jvm_cpu_available_processors`, `jvm_gc_collections{gc}` and `jvm_gc_collection_seconds{gc}`.

## Rules

- **Measuring never slows the game down.** Recording is lock-free; gauges are read only when scraped.
- **Labels have low cardinality.** Packet types, operations and message names are fine; player names, UUIDs or match ids are not.
- **A feature documents its own metrics** here and in its feature documentation.
- **A number without a baseline means little.** Once the network runs, the normal value of each metric is recorded so changes stand out.
