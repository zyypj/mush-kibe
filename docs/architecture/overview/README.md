**English** | [Português](README.pt-BR.md)

# Architecture overview

The principles behind the code, how it is layered, and how a request travels through it.

## Principles

| Principle | How it shows up in Muxe |
|---|---|
| **Single responsibility** | Small classes with one reason to change: `EnvelopeCodec` only encodes envelopes, `IdempotencyGuard` only decides duplicates, `PacketPublisher` only publishes. |
| **Open/closed** | New packets, commands, queries and middlewares are added by registering them, without editing the buses. |
| **Liskov substitution** | Every port has interchangeable implementations: `MessageTransport` (Redis or in-memory), `IdempotencyStore` (Redis or memory), `Transactions` (JDBC or direct). |
| **Interface segregation** | Narrow interfaces: `ServerLoad` exposes three numbers, `PacketContext` exposes the packet, its metadata and `reply`. |
| **Dependency inversion** | Application code depends on ports (`AccountRepository`, `PacketBus`, `Transactions`), and the bootstrap wires concrete implementations. |
| **CQRS** | State changes are `Command`s, reads are `Query`s, each with exactly one handler and its own bus. |
| **Scalability** | Stateless servers coordinated through Redis and PostgreSQL; any server type can run in many copies. |
| **Maintainability** | Enforced module rules, sub-packages by role, no hidden magic, tests at every layer, documentation in two languages. |

## Layers

```text
┌──────────────────────────────────────────────────────────────┐
│ Platform adapters   listener · command · bootstrap           │  Bukkit / Velocity
├──────────────────────────────────────────────────────────────┤
│ Application         commands · queries · handlers · flows    │
├──────────────────────────────────────────────────────────────┤
│ Domain              records · rules · repository ports       │  plain Java
├──────────────────────────────────────────────────────────────┤
│ Infrastructure      postgres · redis · crypto · transports   │
└──────────────────────────────────────────────────────────────┘
```

- **Domain** holds the business vocabulary (`Account`, `Credential`, `Match`, `ArenaInstance`) and the rules that never change with the platform.
- **Application** coordinates the domain through commands and queries. It defines what happens inside a transaction and which `Result` is returned.
- **Infrastructure** implements the ports with real technology.
- **Platform adapters** translate Bukkit and Velocity events into commands and queries, and results back into messages for the player.

Dependencies always point down. Nothing in the domain or application imports Bukkit or Velocity, so they are tested with plain JUnit.

## CQRS

```text
Command ──► CommandBus ──► middlewares ──► CommandHandler ──► Transactions ──► Repository
Query   ──► QueryBus   ──► middlewares ──► QueryHandler   ──► Transactions.readOnly
```

- **Commands** express intent (`RecordLoginCommand`, `RegisterCredentialCommand`) and return a `Result<T>`: success with a value, or failure with a `Problem` code.
- **Queries** never change state (`FindAccountQuery`, `HasCredentialQuery`).
- **Handlers** are registered once, by message class. Registering two handlers for the same message fails at startup.
- **Buses are asynchronous.** `dispatch` and `ask` return a `CompletableFuture` and run handlers on the core's virtual-thread executor, so platform code never blocks the main thread.
- **Middlewares** wrap every handler. `MetricsMiddleware` records `muxe_cqrs_handle_seconds` with the message name and outcome (`success`, `failure`, `error`). New cross-cutting concerns (auditing, tracing) are new middlewares.
- **Read models can diverge from write models** as features grow: a query handler may read from Redis or a denormalized table while commands write to the normalized tables.

## Runtime

`CoreRuntime` (in `core:common`) wires everything a server needs, in a fixed order, and closes it in reverse order:

1. Metrics registry and JVM metrics.
2. Executors: a virtual-thread executor for I/O and handlers, and a scheduler.
3. PostgreSQL pool, then the `core` schema migrations.
4. Redis connection and the Redis transport.
5. Packet registry, idempotency guard, envelope codec and the packet bus.
6. Server directory, CQRS buses and the account module.
7. Heartbeat and the metrics HTTP endpoint.

If any step fails, everything already opened is closed and startup fails with `RuntimeStartupException`. The Spigot core shuts the server down (configurable), and the proxy stops: a node that cannot reach PostgreSQL or Redis must not accept players.

`core:bukkit` and `core:proxy` only add what is platform-specific: reading the configuration file, the main-thread guard, the `ServerLoad`, commands and listeners.

## Following a request: logging in

```text
Player types /login senha  (auth server, main thread)
  └─ AuthCommands ─► AuthFlow.login
       └─ CommandBus.dispatch(VerifyCredentialCommand)          virtual thread
            └─ VerifyCredentialHandler
                 ├─ Transactions.readOnly ─► PostgresCredentialRepository.find
                 ├─ Pbkdf2PasswordHasher.matches
                 └─ Transactions.inTransaction ─► update last login
       └─ back on the main thread: AuthFlow.complete
            └─ PacketBus.publish(proxy, PlayerAuthenticatedPacket)  signed envelope over Redis
                 └─ proxy: ProxyPacketHandlers.onAuthenticated
                      ├─ AccessPolicy.isTrustedAuthOrigin(origin)
                      ├─ AuthenticationGate.authenticate(player)
                      └─ ServerRouter.connect(player, lobby group)
```

## Extending the platform

| To add | Do this |
|---|---|
| A packet | A record implementing `Packet` with a `static final PacketType<…> TYPE` (id, codec, delivery policy). Subscribe with `packets().subscribe(TYPE, handler)`. Document it in the feature. |
| A command or query | A record implementing `Command<R>`/`Query<R>`, a handler class, and a registration in the feature's `*Module`. |
| A table | A Flyway migration in `src/main/resources/db/migration/<module>/` and a repository in `infrastructure/postgres`. Run `migrations().migrate("<module>", classLoader)` on enable. |
| A reusable helper | Add it to `core:bukkit` or `core:common` and document it in the [Core API](../../development/core-api/README.md). |
| A metric | Ask `metrics()` for a counter, timer or gauge with a `muxe_` name, and document it in [Observability](../observability/README.md). |
| A server plugin | A module under `servers/` or `minigames/`, a `*Bootstrap`, and one line in the root `build.gradle.kts`. See [Development](../../development/README.md). |
