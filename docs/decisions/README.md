**English** | [Português](README.pt-BR.md)

# Architecture decision records

Short records of decisions that shape the architecture: the context, the decision and its consequences. They explain **why**, so future contributors do not have to guess or repeat old discussions.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-redis-pubsub-for-packets.md) | Redis Pub/Sub carries packets between servers | Accepted |
| [0002](0002-java-25-on-a-1-8-fork.md) | Java 25 everywhere, Spigot 1.8.8 API on a modern fork, Velocity 4 | Accepted |
| [0003](0003-postgresql-with-schemas-per-module.md) | PostgreSQL with Flyway migrations in one schema per module | Accepted |
| [0004](0004-own-metrics-registry.md) | A small metrics registry with Prometheus text export | Accepted |
| [0005](0005-signed-binary-envelopes.md) | Binary envelopes with explicit codecs, signed with HMAC-SHA256 | Accepted |
| [0006](0006-cqrs-with-in-process-buses.md) | CQRS with asynchronous in-process buses and `Result` values | Accepted |
| [0007](0007-jedis-as-redis-client.md) | Jedis as the Redis client | Accepted |
| [0008](0008-packet-npcs-and-click-interception.md) | NPCs, holograms and boss bars live only in packets | Accepted |
| [0009](0009-own-map-format-and-match-cells.md) | Maps use our own format and matches share one world in cells | Accepted |

## Writing a new ADR

1. Copy [`_template.md`](_template.md) and [`_template.pt-BR.md`](_template.pt-BR.md) to `NNNN-short-title.md` and `NNNN-short-title.pt-BR.md`, with the next number.
2. Fill in both languages and add the ADR to the table above, in both index files.
3. An accepted ADR is not edited to change its decision. A new ADR supersedes it, and the old one's status becomes "Superseded by NNNN".
