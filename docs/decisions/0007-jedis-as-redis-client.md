**English** | [Português](0007-jedis-as-redis-client.pt-BR.md)

# 0007. Jedis as the Redis client

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

The Redis client is shaded into every plugin jar. Spigot 1.8 servers ship their own Netty, and the client must support binary Pub/Sub, `SET NX PX` and TLS.

## Decision

Use Jedis 8: `RedisClient` (pooled) for commands and a dedicated `Jedis` connection for the subscription, so the transport can force a reconnect when a health-check ping fails.

## Alternatives considered

- **Lettuce:** fully asynchronous, but brings Netty and Project Reactor, which must be relocated to avoid clashing with the server's Netty and make each jar several megabytes larger.
- **Redisson:** many high-level features Muxe does not need, and an even larger footprint.

## Consequences

- Blocking calls are fine because they run on virtual threads.
- The client is used only inside `core:common` (`RedisConnector`, the Redis transport and idempotency store), so it can be replaced without touching features.
