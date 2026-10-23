**English** | [Português](README.pt-BR.md)

# Authentication

Players register and log in with a password on the auth server before they can reach any other server. Lives in `servers:auth` (`Muxe-Auth`).

## User stories

- As a new player, I want to create a password, so that nobody else can play with my name.
- As a returning player, I want to log in with my password and go straight to the lobby.
- As an operator, I want repeated wrong passwords to be stopped, so that accounts cannot be brute-forced.

## How it works

1. The proxy sends every new connection to the least loaded auth server.
2. On join, the player is hidden from other players, teleported to the world spawn and frozen. The server checks whether the player has a password and asks for `/register` or `/login`.
3. `/register <password> <password>` validates the password (matching confirmation, length, not equal to the name), hashes it and stores it.
4. `/login <password>` checks the password. A wrong password shows how many attempts are left; reaching the limit kicks the player.
5. On success the auth server publishes `auth.authenticated` to the proxy, which opens the gate and sends the player to a lobby.
6. A player who does not authenticate in time is kicked.

While unauthenticated, a player cannot move (only look around), chat, run other commands, interact, drop or pick up items, open inventories, break or place blocks, take damage or lose hunger.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/register <password> <password>` (`/registrar`) | Creates the account password. | None |
| `/login <password>` (`/logar`) | Logs in. | None |

## Permissions

None.

## Configuration

`plugins/Muxe-Auth/config.yml`:

| Key | Default | Description |
|---|---|---|
| `security.pbkdf2-iterations` | `600000` | PBKDF2-HMAC-SHA256 iterations for new hashes. Raising it upgrades old hashes at their next login. |
| `security.login-timeout` | `60s` | Time to authenticate before being kicked. |
| `security.max-attempts` | `5` | Wrong passwords allowed per window. |
| `security.attempt-window` | `5m` | Window for counting wrong passwords. |
| `password.min-length` | `6` | Minimum password length. |
| `password.max-length` | `64` | Maximum password length. |
| `messages.*` | Portuguese texts | Every message shown to the player. |

Required on the auth server's `spigot.yml`: `commands.log: false`, so passwords are never written to the console.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `auth.authenticated` | proxy | `DURABLE`, 5 min window, max age 15 s | Tells the proxy the player registered (`REGISTER`) or logged in (`LOGIN`). |

## Data

Schema `auth`, owned by `servers:auth`:

| Table | Columns |
|---|---|
| `credentials` | `player_id` UUID primary key, `password_hash` (`pbkdf2_sha256$<iterations>$<salt>$<hash>`), `created_at`, `updated_at`, `last_login_at`. |

## Application

| Message | Kind | Result |
|---|---|---|
| `RegisterCredentialCommand` | Command | `Result<Void>`; failures `auth.already_registered`, `auth.passwords_do_not_match`, `auth.password_too_short`, `auth.password_too_long`, `auth.password_equals_username`. |
| `VerifyCredentialCommand` | Command | `Result<Void>`; failures `auth.not_registered`, `auth.wrong_password`. Records the login and rehashes weak hashes. |
| `HasCredentialQuery` | Query | `Boolean`. |

## Metrics

`muxe_cqrs_handle_seconds` for the three messages above, `muxe_db_transaction_seconds{operation="auth.*"}`, and the packet metrics of `auth.authenticated`.

## Integrations

- [Network routing](../network-routing/README.md): the proxy gate that waits for `auth.authenticated`.
- [Accounts](../accounts/README.md): the account is created by the proxy before the player reaches the auth server.

## Acceptance criteria

- [x] A new player can register and is then authenticated.
- [x] A registered player cannot register again.
- [x] Weak passwords are refused before hashing.
- [x] The right password logs in and records the login time; the wrong one is refused.
- [x] After the maximum number of wrong passwords within the window, the player is blocked.
- [x] Hashes made with fewer iterations are upgraded at login.
- [ ] Unauthenticated players cannot move, chat or run other commands (checked in game).
- [ ] After authenticating, the player is sent to a lobby (checked in game).

## Failures and recovery

- **Database unavailable:** the player is kicked with `service-unavailable`; nothing is half-written.
- **Proxy does not receive `auth.authenticated`** (Redis down): the publish fails and the player is kicked with `service-unavailable`, so they never wait forever. They log in again when Redis is back.
- **Player disconnects while a command runs:** the result is ignored and the session is cleared.

## Known limitations

- Wrong-password limits are counted per player on each auth server, in memory; they reset when the server restarts.
- No password change, recovery or premium-account auto-login yet.
- Spigot logs commands by default; operators must set `commands.log: false`.
