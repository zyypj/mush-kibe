**English** | [Português](README.pt-BR.md)

# Network security

How the network protects its servers, services and players. The principle: players reach only the proxy, everything else is private, and every secret lives outside the repository.

## What is exposed

| Service | Reachable by players | Reachable by |
|---|---|---|
| Velocity | Yes, it is the only public entry point | Everyone |
| Spigot servers | No | The proxy only |
| PostgreSQL | No | The network's servers only |
| Redis | No | The network's servers only |
| Metrics endpoints | No | The monitoring system only |

## Proxy and game servers

- Spigot 1.8 servers do not support Velocity's modern forwarding, so the proxy uses **legacy (BungeeCord) forwarding**, or **BungeeGuard** forwarding when the server software supports it.
- Legacy forwarding has no signature. **The game servers' ports must be closed to the internet** by the firewall or bound to a private network. A Spigot server reachable from the internet lets anyone join as any player.
- `spigot.yml` has `settings.bungeecord: true` on every game server.

## Authentication

The network accepts players without a Minecraft account (offline mode), so the proxy cannot trust a username by itself. `velocity.toml` runs with `online-mode = false`, and the proxy decides per connection:

- **Premium names** (an existing premium account, or any name that exists at Mojang, even if a cracked account registered it here first) are forced into online mode: Velocity verifies the session with Mojang, so a cracked client cannot use a premium name. Premium players are authenticated without a password.
- **Names follow the Mojang-verified identity:** a premium login takes its name from any other account, and a renamed premium player keeps the same account because the Mojang UUID does not change. See [Accounts](../../features/accounts/README.md#name-ownership).
- **Cracked names** go to an auth server, solve a particle-drawn code (anti-bot) and then register or log in with a password.
- **Login sessions:** after a password login, the proxy keeps the SHA-256 of the player id and IP in Redis for 2 hours. A reconnection of the same account from the same IP within that time is authenticated without a password. The raw IP is never stored.
- If Mojang cannot be reached, new names are refused instead of being registered as cracked.
- Until a player is authenticated, the proxy **denies every connection to a server outside the auth group**, and ignores `player.connect` packets for that player.
- The proxy only accepts `auth.authenticated` from an origin that its server directory knows as an `AUTH` server of the auth group.
- The authenticated state is forgotten when the player disconnects.
- Names that differ only in letter case from an existing account are refused at login, because offline-mode identities are derived from the exact name.

Passwords:

- Hashed with **PBKDF2-HMAC-SHA256**, a random 16-byte salt per password and 600,000 iterations by default (`security.pbkdf2-iterations`). Raising the iterations upgrades each hash at its next successful login.
- Compared in constant time. Never logged: commands and records that hold passwords override `toString`.
- **Set `commands.log: false` in the auth server's `spigot.yml`**, otherwise Spigot prints every command, including `/login <password>`, to the console and logs.
- Wrong passwords are limited per player (5 per 5 minutes by default); the player is kicked when the limit is reached.
- Unauthenticated players cannot move, chat, use other commands, interact or see other players.

## Packets

- Every envelope is signed with HMAC-SHA256 using `messaging.secret`. Envelopes with a bad signature are rejected before being parsed. See [Messaging](../messaging/README.md#signatures).
- Envelopes have a maximum size and an expiration, which limits replay and memory abuse.
- **Access to Redis plus the secret means control of the network.** Both are protected: Redis is private and requires authentication; the secret is provided only through the environment.

## PostgreSQL

- Listens only on the private network and accepts connections only from the network's servers.
- Permissions are separated: servers use a user that can read and write data but cannot change the schema; migrations run with a separate user (`database.run-migrations: false` on the servers that do not migrate). Neither is a superuser.

## Redis

- Listens only on the private network, with `requirepass` or ACL users.
- With ACLs, each service gets its own user limited to the `muxe:*` keys and channels.

## Secrets

- Passwords and the messaging secret are never committed and never written in plain text in default configuration.
- Configuration values use `env:NAME` to read an environment variable or `file:/path` to read a file such as a Docker or Kubernetes secret.
- `.env` is ignored by git; `.env.example` documents the variables without values.
- A secret that was exposed, even briefly, is replaced. The messaging secret can be rotated without downtime with `messaging.previous-secrets`.

## Traffic between machines

While every service runs on the same machine, traffic stays local. Once PostgreSQL, Redis or the servers run on different machines, traffic uses TLS (`redis.ssl`, `sslmode` in the JDBC URL) unless it travels over an encrypted private network.
