**English** | [Português](0006-cqrs-with-in-process-buses.pt-BR.md)

# 0006. CQRS with asynchronous in-process buses and `Result` values

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

Plugin code tends to mix event handling, business rules and database access in the same class, which makes it hard to test and easy to block the main thread. Writes and reads also have different needs: writes need transactions and validation, reads need speed and may come from caches.

## Decision

- State changes are `Command`s and reads are `Query`s, each with exactly one handler, registered on a `CommandBus` or `QueryBus`.
- Buses run handlers asynchronously on virtual threads and return `CompletableFuture`s.
- Cross-cutting behavior (metrics today, auditing or tracing later) is added as middleware.
- Expected business failures are returned as `Result.failure(Problem)` with stable codes; exceptions are for bugs and infrastructure.

## Alternatives considered

- **Services called directly from listeners:** less ceremony, but blocking calls leak into the main thread and cross-cutting concerns are repeated.
- **Event sourcing:** strong audit trail, but far more complexity than the network needs today.

## Consequences

- Business logic is tested with plain JUnit and in-memory fakes.
- Platform code always deals with futures and must return to the main thread to touch Bukkit.
- Every command and query is measured for free.
