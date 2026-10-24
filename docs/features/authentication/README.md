**English** | [Português](README.pt-BR.md)

# Authentication

How players prove who they are. Premium (original) accounts are verified by Mojang and skip passwords. Cracked accounts solve an anti-bot challenge and then register or log in on the auth server. A password login is remembered for two hours from the same IP.

Lives in `core:proxy` (premium detection, sessions, the gate) and `servers:auth` (`Muxe-Auth`: challenge, `/registrar`, `/logar`).

## User stories

- As a premium player, I want to join without typing a password, because Mojang already verified me.
- As a cracked player, I want to create a password, so that nobody else can play with my name.
- As a cracked player, I want to reconnect within two hours from the same place without logging in again.
- As an operator, I want bots stopped before they reach the password step, and repeated wrong passwords stopped too.
- As a staff member, I want to choose where players appear on the auth server and where the challenge code is drawn, without editing files.

## How it works

### 1. Login mode (proxy, before login)

When a connection starts, the proxy decides its **login mode**:

| Situation | Mode |
|---|---|
| An account with this name exists and is premium | Premium |
| An account with this name exists and is cracked, and the name is not premium at Mojang | Cracked |
| An account with this name exists and is cracked, but the name is now premium at Mojang | Premium: the Mojang owner takes the name back |
| No account, and Mojang has a profile with this name | Premium |
| No account, and Mojang has no profile with this name | Cracked |
| No account, and Mojang could not be reached | Connection refused with `service-unavailable` |

- **Premium** connections are forced into online mode: Velocity checks the player's session with Mojang. A cracked client using a premium name fails this check and cannot join.
- **Cracked** connections are forced into offline mode.
- The premium flag is stored in the account on its first login. Names held by cracked accounts are checked against Mojang again on every login, so a name that became premium goes to its owner. Who keeps a name is described in [Accounts](../accounts/README.md#name-ownership).
- Mojang answers are cached in Redis: premium answers for `authentication.premium-cache`, cracked answers for the shorter `authentication.cracked-cache`, so a name bought at Mojang is protected within minutes.
- With `authentication.premium-auto-login: false`, every new account is cracked.

### 2. Automatic authentication (proxy, at login)

After the account is recorded, the proxy authenticates the player immediately when:

- the account is **premium**, or
- the player has a valid **login session**: they logged in with a password less than `authentication.session-duration` ago (2 hours by default), with the same account and from the same IP.

Automatically authenticated players go straight to a lobby. Everyone else goes to an auth server.

### 3. Challenge (auth server)

1. On join the auth server asks the proxy whether the player is already authenticated (`auth.status_request`). If so, the challenge and password are skipped and the player sees `Você já está autenticado.` Otherwise the player is hidden from others, teleported to the configured spawn (or the world spawn). The player can walk around while authenticating.
2. A random code (4 digits by default) is drawn with holograms at the configured code position, turned to face the player, or 7 blocks in front of the player when no position is configured. Each digit is a grid of `⬛` name tags, white on top, light green in the body and dark green at the bottom. The holograms are sent only to that player, so players never see each other's codes. The chat says:
   - `DESAFIO:` / `Digite o código em frente no chat!`, with a hint that the code is in the holograms.
   - The action bar repeats `Digite o código em frente no chat!` every second.
3. The player types the code in the chat (case and spaces are ignored). Chat messages are never shown to other players.
4. A wrong code generates a new one. After `challenge.max-attempts` wrong answers the player is kicked.
5. The right code shows `Desafio completado com sucesso!` and moves to the password step.

The code is never sent as text: each hologram only carries a `⬛`, so a bot has to rebuild the picture from the positions to read it. A new code replaces the holograms at once, and they are sent again every 3 seconds in case the client dropped them.

### 4. Password (auth server)

- The chat shows `NUNCA COMPARTILHE A SUA SENHA COM NINGUÉM!` and `Faça o login utilizando /logar <senha>` (or `/registrar <senha> <senha>`), and the screen shows the title `MUXE` with `Use /logar <senha>`. Both are repeated every `reminder.interval`.
- `/registrar <senha> <senha>` validates the password (matching confirmation, length, not equal to the name), hashes it and stores it.
- `/logar <senha>` checks the password. A wrong password shows how many attempts are left; reaching the limit kicks the player.
- On success: `Você se autenticou com sucesso!` (or `Você se registrou com sucesso!`) and `Enviando ao lobby...`. The auth server publishes `auth.authenticated`; the proxy opens the gate, **remembers the login session** and sends the player to a lobby.
- A player who does not finish in `security.login-timeout` is kicked.

While unauthenticated, a player can only walk, type the code in the chat and use `/logar` and `/registrar`. They cannot chat with others, run other commands, see other players, interact with blocks or entities (item frames, armor stands, paintings), use buckets, drop or pick up items, open inventories, break or place blocks, take damage or lose hunger. Falling below `protection.void-level` sends the player back to the spawn. After authenticating, commands are released for everyone; building and interacting are released only for players with `muxe.auth.setup`. Chat stays closed for everyone on the auth server.

### 5. Configuring the auth server (staff)

1. Log in normally, then come back to the auth server with Velocity's `/server auth-1`. The proxy confirms you are authenticated, so you skip the challenge and, with `muxe.auth.setup`, can move freely.
2. Stand where players should appear, looking where they should look, and run `/authsetup spawn`.
3. Stand where the code should appear (it is centered on your eyes) and run `/authsetup codigo`. The code is turned to face the spawn automatically.
4. Run `/authsetup testar` to see a sample code for 10 seconds, as seen from the spawn.

Positions are saved in `plugins/Muxe-Auth/config.yml` under `locations`, and apply to the next players who join.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/registrar <senha> <senha>` (`/register`) | Creates the account password. | None |
| `/logar <senha>` (`/login`) | Logs in. | None |
| `/authsetup spawn` | Sets the spawn to your position and direction. | `muxe.auth.setup` |
| `/authsetup codigo` | Sets the challenge code position to your eyes. | `muxe.auth.setup` |
| `/authsetup testar` | Shows a sample code for 10 seconds, seen from the spawn. | `muxe.auth.setup` |
| `/authsetup info` | Shows both positions. | `muxe.auth.setup` |
| `/authsetup limpar <spawn\|codigo>` | Removes a position and goes back to the default. | `muxe.auth.setup` |

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.auth.setup` | `/authsetup`, and moving, building and interacting on the auth server after logging in. Only works once the player is authenticated, so a cracked client using an operator's name gets nothing. | op |

## Configuration

Proxy `config.yml`:

| Key | Default | Description |
|---|---|---|
| `authentication.premium-auto-login` | `true` | Detect premium names and let them in without a password. |
| `authentication.premium-lookup-timeout` | `3s` | Timeout of the Mojang lookup. |
| `authentication.premium-cache` | `6h` | How long "premium" answers from Mojang are cached in Redis. |
| `authentication.cracked-cache` | `10m` | How long "not premium" answers are cached. Short, so a name that becomes premium is protected quickly. |
| `authentication.session-duration` | `2h` | How long a password login is remembered for the same account and IP. |

`plugins/Muxe-Auth/config.yml`:

| Key | Default | Description |
|---|---|---|
| `challenge.enabled` | `true` | Ask for the code before the password. |
| `challenge.length` | `4` | Digits in the code. |
| `challenge.max-attempts` | `3` | Wrong codes before the player is kicked. |
| `challenge.distance` | `7` | Blocks between the player's eyes and the code. |
| `reminder.interval` | `5s` | How often the password prompt and title are repeated. |
| `protection.void-level` | `0` | Players below this height are sent back to the spawn. |
| `locations.spawn` | not set | Written by `/authsetup spawn`: world, x, y, z, yaw, pitch. Without it, the world spawn is used. |
| `locations.challenge-code` | not set | Written by `/authsetup codigo`. Without it, the code appears `challenge.distance` blocks in front of the player. |
| `security.pbkdf2-iterations` | `600000` | PBKDF2-HMAC-SHA256 iterations for new hashes. Raising it upgrades old hashes at their next login. |
| `security.login-timeout` | `60s` | Time to authenticate before being kicked. |
| `security.max-attempts` | `5` | Wrong passwords allowed per window. |
| `security.attempt-window` | `5m` | Window for counting wrong passwords. |
| `password.min-length` | `6` | Minimum password length. |
| `password.max-length` | `64` | Maximum password length. |
| `messages.*` | Portuguese texts | Every text shown to the player, including the title (`title`, `subtitle-login`, `subtitle-register`) and the action bar (`challenge-action-bar`). |

Required:

- `velocity.toml`: `online-mode = false`. Premium players are switched to online mode individually by the proxy.
- The auth server's `spigot.yml`: `commands.log: false`, so passwords are never written to the console.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `auth.authenticated` | proxy | `DURABLE`, 5 min window, max age 15 s | Tells the proxy the player registered (`REGISTER`) or logged in (`LOGIN`). |
| `auth.status_request` | proxy | standard, max age 5 s | Asks whether a player who just joined the auth server is already authenticated. Only the proxy the player is connected to answers. |
| `auth.status_response` | requester | standard, max age 5 s | The answer. No answer within `messaging.request-timeout` means "not authenticated". |

## Data

| Where | What |
|---|---|
| PostgreSQL `auth.credentials` | `player_id`, `password_hash` (`pbkdf2_sha256$<iterations>$<salt>$<hash>`), `created_at`, `updated_at`, `last_login_at`. |
| PostgreSQL `core.accounts.premium` | Whether the account logs in through Mojang. See [Accounts](../accounts/README.md). |
| Redis `muxe:auth:session:<player id>` | SHA-256 of the player id and IP of the last password login. Expires after `session-duration`. The raw IP is never stored. |
| Redis `muxe:auth:premium:<lowercase name>` | `PREMIUM` or `CRACKED` from Mojang. Expires after `premium-cache` or `cracked-cache`. |

## Application

| Message | Kind | Result |
|---|---|---|
| `RegisterCredentialCommand` | Command | `Result<Void>`; failures `auth.already_registered`, `auth.passwords_do_not_match`, `auth.password_too_short`, `auth.password_too_long`, `auth.password_equals_username`. |
| `VerifyCredentialCommand` | Command | `Result<Void>`; failures `auth.not_registered`, `auth.wrong_password`. Records the login and rehashes weak hashes. |
| `HasCredentialQuery` | Query | `Boolean`. |
| `FindAccountByUsernameQuery` | Query | `Optional<Account>`, used by the proxy to decide the login mode. |

## Metrics

| Metric | What it shows |
|---|---|
| `muxe_auth_login_modes_total{mode}` | Login modes decided by the proxy: `premium`, `cracked`, `undecided`. |
| `muxe_auth_logins_total{mode}` | How players were let in: `premium`, `session` (resumed) or `password` (sent to the auth server). |
| `muxe_auth_premium_lookups_total{result,source}` | Mojang lookups by `premium`/`cracked`/`unknown`, answered by `cache` or `remote`. |
| `muxe_auth_challenges_total{outcome}` | Challenge answers: `passed`, `wrong`, `failed`. |

Plus `muxe_cqrs_handle_seconds` for the commands and queries above, `muxe_db_transaction_seconds{operation="auth.*"}` and the packet metrics of `auth.authenticated`.

## Integrations

- [Network routing](../network-routing/README.md): the proxy gate and the choice of the first server.
- [Accounts](../accounts/README.md): the account and its premium flag.
- Core helpers: `Title`, `ActionBar` and `PixelHolograms`, described in [Core API](../../development/core-api/README.md).
- Mojang profile API (`api.mojang.com/users/profiles/minecraft/<name>`).

## Acceptance criteria

- [x] Premium accounts always log in through Mojang; cracked accounts stay cracked while the name is not premium and go to the Mojang owner once it is; new names follow Mojang; an unreachable Mojang refuses only new names.
- [x] Mojang answers are cached, cracked answers for less time than premium ones, and an unavailable cache does not block logins.
- [x] A password login is resumed only for the same account, from the same IP, within the session duration, and the IP is stored hashed.
- [x] The challenge accepts the right code ignoring case and spaces, renews the code after a wrong answer and fails after the maximum attempts.
- [x] Codes only use digits that the hologram font can draw.
- [x] The code hologram is shown once per code, replaced when the code changes, sent again periodically and removed when the challenge ends.
- [x] A new player can register; a registered player cannot register again; weak passwords are refused before hashing.
- [x] The right password logs in and records the login time; the wrong one is refused; the limit blocks the player.
- [ ] A premium player joins straight into a lobby without a password (checked in game).
- [ ] A cracked client using a premium name is refused by Velocity (checked in game).
- [ ] The code is readable in front of the player and the title and action bar appear (checked in game).
- [ ] Rejoining within two hours from the same IP skips the auth server (checked in game).
- [x] The auth server skips the flow only when the proxy says the player is authenticated; no answer or an answer about another player means not authenticated.
- [x] A configured code position is turned to face the viewer; without it, the code appears in front of the player.
- [ ] Staff can set the spawn and code position with `/authsetup` and preview it (checked in game).
- [x] Only authenticated staff may interact; unauthenticated players only run the login commands, even with a `plugin:` prefix.
- [ ] Unauthenticated players can walk but cannot interact, build, chat or see others (checked in game).

## Failures and recovery

- **Mojang unreachable:** existing accounts log in normally (cracked names cannot be re-checked until Mojang answers); new names are refused with `service-unavailable`, so a premium name can never be registered as cracked by accident.
- **Redis unavailable:** sessions cannot be resumed, so cracked players log in with their password; premium lookups go straight to Mojang.
- **Database unavailable:** the login is refused with `service-unavailable`; nothing is half-written.
- **Proxy does not receive `auth.authenticated`:** the publish fails and the player is kicked with `service-unavailable`, so they never wait forever.
- **Server without action bar support:** the action bar is skipped and everything else works; titles fall back to the server's default timings.

## Known limitations

- A name bought at Mojang can still be used by a cracked player for up to `cracked-cache` (10 minutes by default) after a cached "not premium" answer.
- When a cracked client tries a premium name, the refusal message is Velocity's own online-mode error.
- Premium players cannot yet choose to protect their account with a password too. This preference is planned and will reuse the same session rules.
- Wrong-password limits are counted per player on each auth server, in memory.
- No password change or recovery yet.
- Action bars and title timings use the 1.8 server internals through reflection; they are disabled with a warning if the server does not expose them.
