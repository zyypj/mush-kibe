**English** | [Português](README.pt-BR.md)

# Accounts

Every player has one account, created the first time they reach the proxy and updated on every login. Accounts are the identity other features build on.

## User stories

- As a player, I want my progress tied to my name, so that I find it again every time I join.
- As an operator, I want two players to never share a name that differs only in letter case, so that offline-mode identities cannot be confused.

## How it works

- When a player logs into the proxy, the proxy dispatches `RecordLoginCommand(playerId, username)`.
- The first login creates the account with `first_seen_at` and `last_seen_at`. Later logins update the name and `last_seen_at`.
- If another account already uses the same name with different letter case, the login is refused with a message asking the player to use the exact name.
- Names must match `[A-Za-z0-9_]{3,16}`; anything else is refused.

## Commands

None.

## Permissions

None.

## Configuration

Messages in the proxy `config.yml`:

| Key | Description |
|---|---|
| `messages.name-case-conflict` | Shown when the name differs only in case from an existing account. |
| `messages.invalid-username` | Shown when the name is not allowed. |
| `messages.service-unavailable` | Shown when the account cannot be recorded. |

## Packets

None.

## Data

Schema `core`, owned by `core:common`:

| Table | Columns |
|---|---|
| `accounts` | `id` UUID primary key, `username`, `first_seen_at`, `last_seen_at`. Unique index on `LOWER(username)`. |

## Application

| Message | Kind | Result |
|---|---|---|
| `RecordLoginCommand` | Command | `Result<Account>`; failures `account.invalid_username`, `account.name_case_conflict`. |
| `FindAccountQuery` | Query | `Optional<Account>`. |

## Metrics

`muxe_cqrs_handle_seconds{message="record_login_command"}` and `muxe_db_transaction_seconds{operation="account.record_login"}`.

## Integrations

- The proxy's `LoginListener` records every login.
- Any plugin can ask `FindAccountQuery` through `MuxeCore.queries()`.

## Acceptance criteria

- [x] A new player gets an account on the first login.
- [x] A returning player keeps `first_seen_at` and gets a new `last_seen_at`.
- [x] A name that differs only in case from an existing account is refused, in code and by the database index.
- [x] An invalid name is refused without touching the database.

## Failures and recovery

- If PostgreSQL does not answer, the login is denied with `service-unavailable`. Nothing is half-written: the command runs in one transaction.
- Two simultaneous logins with names that differ only in case: the unique index makes one of them fail; that player is denied and can retry.

## Known limitations

- Players cannot change their name or merge accounts.
- Premium (original) accounts are not detected yet; every player is identified by the offline-mode UUID that the proxy provides.
