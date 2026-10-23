**English** | [Português](0003-postgresql-with-schemas-per-module.pt-BR.md)

# 0003. PostgreSQL with Flyway migrations in one schema per module

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

Player data must survive restarts, be consistent across servers and support transactions. Several plugins own data, and they evolve independently.

## Decision

- PostgreSQL is the source of truth.
- Each module owns a PostgreSQL schema with its own name and its own Flyway history (`<module>.schema_history`), with migrations in `db/migration/<module>/`.
- Data access is plain JDBC through HikariCP, with hand-written SQL in repositories.

## Alternatives considered

- **MySQL/MariaDB:** common in Minecraft, but PostgreSQL offers transactional DDL, expression indexes (such as `LOWER(username)`), better concurrency and richer types.
- **One shared schema and one migration history:** couples every plugin's release to a single ordered history.
- **An ORM (Hibernate, Ebean):** hides query cost and adds weight and reflection to every plugin jar.
- **A custom migration runner:** less code to shade, but Flyway brings locking, checksums and validation that are easy to get wrong.

## Consequences

- Plugins can add tables without coordinating migration numbers with each other.
- Flyway must be shaded with merged service files; a smoke test proved that relocation works only with `duplicatesStrategy = INCLUDE` in Shadow 9.
- Queries are explicit and reviewable; repositories need integration tests.
