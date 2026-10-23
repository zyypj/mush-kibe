**English** | [Português](README.pt-BR.md)

# Muxe

Muxe is an open-source Minecraft minigame network for **Minecraft 1.8**: a Velocity proxy, an authentication server, a lobby and minigame servers, all built as plugins in one Gradle monorepo.

It is an independent project inspired by the classic Brazilian minigame networks. It contains no code, assets, maps or trademarks from any other network. See [Legal and clean-room policy](docs/legal/README.md).

## Highlights

- **One monorepo, many plugins.** A shared core plus one plugin per server, with dependency rules enforced by the build.
- **Packets between servers over Redis.** Binary, HMAC-signed envelopes with request/response, idempotency (local, durable and exclusive), expiration and a metric for every step. See [Messaging](docs/architecture/messaging/README.md).
- **CQRS application layer.** Commands change state, queries read it, both go through buses with middleware and metrics. See [Architecture overview](docs/architecture/overview/README.md).
- **PostgreSQL as the source of truth.** HikariCP, versioned Flyway migrations per module and transactions that never run on the main thread. See [Persistence](docs/architecture/persistence/README.md).
- **Observability built in.** Prometheus metrics for packets, requests, commands, transactions, ticks and the JVM. See [Observability](docs/architecture/observability/README.md).
- **Documented in English and Portuguese**, for people and for AI agents.

## Stack

| Piece | Technology |
|---|---|
| Language | Java 25 |
| Game servers | Spigot API 1.8.8, running on a 1.8.8 fork that supports Java 25 (such as PandaSpigot) |
| Proxy | Velocity 4.3 |
| Database | PostgreSQL 18 |
| Messaging and shared cache | Redis 8 |
| Build | Gradle 9 with Kotlin DSL convention plugins |
| Tests | JUnit 6, AssertJ, Testcontainers |

## Quick start

```bash
cp .env.example .env
docker compose up -d
./gradlew build
```

On Windows use `gradlew.bat build`. The build compiles every module, runs all tests (including integration tests against real PostgreSQL and Redis containers) and assembles the plugins of each server into `build/servers/`. See [Development](docs/development/README.md) to run a local network.

## Current state

Early development. The platform (core, messaging, persistence, metrics, proxy routing, authentication and lobby basics) works and is tested. The minigames have their engine and plugin skeletons; gameplay is not implemented yet.

## Documentation

- [docs/](docs/README.md): architecture, development guide, process, decisions and features.
- [CONTRIBUTING.md](CONTRIBUTING.md): branches, commits, documentation and pull requests.
- [AGENTS.md](AGENTS.md): rules for AI agents working on this repository.
- [SECURITY.md](SECURITY.md): how to report a vulnerability.
