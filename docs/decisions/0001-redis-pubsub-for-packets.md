**English** | [Português](0001-redis-pubsub-for-packets.pt-BR.md)

# 0001. Redis Pub/Sub carries packets between servers

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

Servers must exchange messages: heartbeats, routing requests, authentication results, player lookups and, later, game and store events. Messages must flow even when a server has no players, and broadcasts to many servers must be cheap.

## Decision

Packets travel over Redis Pub/Sub, behind the `MessageTransport` interface. Each server subscribes only to its own channels (broadcast, its group, its id, and the proxy channel on proxies).

## Alternatives considered

- **Plugin Messaging:** needs a player connected on both ends and goes through the proxy. It stops working exactly when a server is empty.
- **A message broker (RabbitMQ, NATS, Kafka):** durable delivery and consumer groups, but one more piece of infrastructure to run and secure. Redis is already needed for shared state.
- **Redis Streams:** durable and replayable, but more complex to consume correctly. It remains an option behind `MessageTransport` if at-least-once delivery becomes necessary.

## Consequences

- Delivery is at most once. Effects that must not be lost are written to PostgreSQL first, and packets only notify. See [Messaging](../architecture/messaging/README.md#what-delivery-guarantees).
- Idempotency, expiration and signatures are implemented by Muxe on top of the transport.
- Access to Redis must be private and authenticated.
- Tests use the in-memory transport for logic and a Redis container for the real transport.
