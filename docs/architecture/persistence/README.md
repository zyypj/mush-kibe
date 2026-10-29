**English** | [Português](README.pt-BR.md)

# Persistence

How Muxe stores data. PostgreSQL is the source of truth; Redis is a transport and cache that can be emptied at any time without losing anything.

Code: `core/common/src/main/java/br/com/muxe/core/common/persistence`.

## Building blocks

| Class | Responsibility |
|---|---|
| `DatabaseConfig` | URL, credentials, pool size, connection timeout, whether to run migrations. |
| `Database` | A HikariCP pool over `PGSimpleDataSource`, with pool metrics. |
| `Transactions` | The port used by handlers: `inTransaction(operation, work)` and `readOnly(operation, work)`. |
| `JdbcTransactions` | The JDBC implementation. |
| `TransactionContext` | Gives repositories the connection of the current transaction. |
| `MigrationRunner` | Runs the Flyway migrations of one module in its own schema. |
| `ExpiringStore` | Short-lived values in Redis (`RedisExpiringStore`), every one with a time to live. Keys get the `muxe:` prefix. |

## Transactions

```java
return transactions.inTransaction("account.record_login", () -> {
    Optional<Account> holder = accounts.findByUsername(command.username());
    accounts.save(account);
    return Result.success(account);
});
```

- The connection is bound with a Java 25 **`ScopedValue`** for the duration of the work. Repositories call `TransactionContext.connection()`; calling it outside a transaction fails with a clear message.
- A transaction started inside another one **joins** it, so a handler can compose repository calls freely.
- The work commits when it returns and rolls back when it throws. Checked exceptions become `PersistenceException`; runtime exceptions propagate unchanged.
- `readOnly` marks the connection read-only, so PostgreSQL can optimize it and accidental writes fail.
- **Never on the main thread.** On Spigot, `JdbcTransactions` uses a `MainThreadGuard` and throws `BlockingOnMainThreadException` if called from the server thread. Handlers run on the CQRS buses' virtual threads.
- Every transaction is timed in `muxe_db_transaction_seconds{operation, outcome}`. The operation name is a stable, low-cardinality string such as `auth.register`.

## Repositories

- The interface lives in the feature's `domain` package; the implementation in `infrastructure/postgres`.
- SQL is written by hand, in text blocks, with `PreparedStatement`. No ORM: the cost of each query is visible.
- Tables are always schema-qualified (`core.accounts`, `auth.credentials`).
- Unit tests use in-memory repositories with `DirectTransactions`; integration tests use the real repository against a PostgreSQL container.

## Migrations

- Each module owns a PostgreSQL **schema** with the same name and its migrations in `src/main/resources/db/migration/<module>/`, named `V<n>__<description>.sql`.
- `MigrationRunner.migrate(module, classLoader)` creates the schema if needed and keeps the history in `<module>.schema_history`.
- The core migrates the `core` schema on startup when `database.run-migrations` is true. A plugin migrates its own schema on enable: `core.migrations().migrate("auth", getClass().getClassLoader())`.
- Flyway uses a PostgreSQL lock, so several servers starting together never apply the same migration twice.
- A migration that was released is never edited. Changes are new migrations.
- In production, migrations can be applied by a single designated server (or a deploy step) with a privileged user, and disabled everywhere else, as described in [Security](../security/README.md).

| Schema | Owner | Tables |
|---|---|---|
| `core` | `core:common` | `accounts` (V1), `accounts.premium` (V2), nullable `accounts.username` for released names (V3), `npc_placements`, `hologram_placements`, `register_placements` (placed per server group) and `shop_promotion` (one row for the network) (V4), `player_visibility` (each player's visibility choice, copied from the old `lobby.player_settings`) (V5), `social_head_placements` (social media heads placed per server group) (V6), `parkour_placements` (parkour start, checkpoints and end placed per server group) and `parkour_records` (each player's best parkour time per server group) (V7) |
| `auth` | `servers:auth` | `credentials` |
| `lobby` | `servers:lobby` | None. `player_settings` (V1) was dropped by V2 after its rows moved to `core.player_visibility`. |

## Kinds of data

| Kind | Examples | Strategy |
|---|---|---|
| Critical | Credentials, coins, purchases, rewards | Written in a PostgreSQL transaction **before** the player is told it happened. |
| Non-critical | Preferences, toggles | Applied in memory and written in the background. A crash may lose the last seconds. |
| Match state | Teams, kills, timers | Memory only, on the server running the match. |
| Ephemeral network state | Server status, idempotency claims, login sessions, premium lookups | Redis or packets, always with an expiration. |

## Rules

- **Never block the main thread.** Database and Redis calls run outside it; results that touch the game are scheduled back.
- **Critical data is confirmed only after commit.**
- **Balances change atomically in SQL** (`UPDATE … SET coins = coins - 100 WHERE coins >= 100`), never by writing a value read earlier.
- **Uniqueness is enforced by the database**, not only by code. Example: `accounts_username_lower_key` makes two accounts whose names differ only in case impossible, even under a race.
- **Durable data never lives only in Redis.** Every Redis key that is not a cache has an expiration.
- **Redis keys share a prefix:** `muxe:<area>:<id>`, such as `muxe:idempotency:auth.authenticated:…`.
- **No queries inside loops.** Load what is needed in one query.
- **Every frequent query is backed by an index.**
- **Pools stay small.** Each PostgreSQL connection is a process; 4 connections per server is the default.

## Planned: session ownership

To support saving player data across servers safely, the plan is that each online player's data has exactly one owner server at a time, recorded in PostgreSQL with a generation number that only increases, and that every write carries the owner's generation so that a frozen or stale server can never overwrite newer data. Transfers will flush and release the session before the destination takes it. This is not implemented yet; it will be documented here when it is.
