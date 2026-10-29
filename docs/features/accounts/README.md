**English** | [Português](README.pt-BR.md)

# Accounts

Every player has one account, created the first time they reach the proxy and updated on every login. Accounts are the identity other features build on.

## User stories

- As a player, I want my progress tied to my name, so that I find it again every time I join.
- As an operator, I want two players to never share a name that differs only in letter case, so that offline-mode identities cannot be confused.
- As a premium player, I want to keep my progress when I change my name at Mojang, and to get my name back if someone else is using it here.

## How it works

- When a player logs into the proxy, the proxy dispatches `RecordLoginCommand(playerId, username, premium)`.
- The first login creates the account with `first_seen_at`, `last_seen_at` and whether it is **premium** (verified by Mojang) or cracked. Later logins update the name and `last_seen_at`; the premium flag never changes.
- Premium accounts use the player's Mojang UUID; cracked accounts use the offline-mode UUID derived from the name. The flag decides how the account logs in from then on, as described in [Authentication](../authentication/README.md).

### Name ownership

A Mojang-verified identity always wins a name:

| Situation | What happens |
|---|---|
| A premium player changes name at Mojang and comes back | Same Mojang UUID, same account: the name is updated and all progress is kept. |
| A premium player logs in with a name held by another account (premium or cracked, in any letter case) | The name moves to the premium player. The other account keeps its data but loses its name until its owner logs in again. |
| A cracked player logs in with a name held by another account in another letter case | Refused with `name-case-conflict`. |
| A cracked player tries a name held by a premium account | The proxy forces online mode and Velocity refuses the connection. |

An account without a name keeps its id, premium flag and data. A premium owner who renamed gets the new name on the next login. A cracked account that lost its name to the premium owner can no longer log in with that name.
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
| `accounts` | `id` UUID primary key, `username` (empty while the name is released), `premium`, `first_seen_at`, `last_seen_at`. Unique index on `LOWER(username)`. |

## Application

| Message | Kind | Result |
|---|---|---|
| `RecordLoginCommand` | Command | `Result<RecordedLogin>`: the account and, when a premium login took the name, the id of the account that lost it. Failures `account.invalid_username`, `account.name_case_conflict`. |
| `FindAccountQuery` | Query | `Optional<Account>` by id. |
| `FindAccountByUsernameQuery` | Query | `Optional<Account>` by name, ignoring case. |

## Metrics

`muxe_cqrs_handle_seconds{message="record_login_command"}`, `muxe_db_transaction_seconds{operation="account.record_login"}` and `muxe_accounts_names_reclaimed_total`, counted by the proxy each time a premium player takes a name from another account (also logged with both ids).

## Integrations

- The proxy's `LoginListener` records every login.
- Any plugin can ask `FindAccountQuery` through `MuxeCore.queries()`.
- [Account](../account/README.md) shows accounts with `/account`, `/accountmenu` and the profile head, and [Ranks and tags](../ranks-and-tags/README.md) finds players by name for its commands.

## Acceptance criteria

- [x] A new player gets an account on the first login.
- [x] A returning player keeps `first_seen_at` and gets a new `last_seen_at`.
- [x] A name that differs only in case from an existing account is refused, in code and by the database index.
- [x] An invalid name is refused without touching the database.
- [x] The premium flag is stored on the first login and never changes.
- [x] A premium player who renamed keeps the same account.
- [x] A premium login takes its name from any other account, premium or cracked, in any letter case, and the other account keeps its data without a name.
- [x] A cracked login never takes a name from another account.
- [x] A name can be released and taken by another account in the same transaction (integration test).

## Failures and recovery

- If PostgreSQL does not answer, the login is denied with `service-unavailable`. Nothing is half-written: the command runs in one transaction.
- Two simultaneous logins with names that differ only in case: the unique index makes one of them fail; that player is denied and can retry.

## Known limitations

- Players cannot change their name or merge accounts.
- A cracked player who loses a name to its premium owner keeps the data under the old account but cannot reach it with a new name. Staff can move it manually; there is no command for that yet.
