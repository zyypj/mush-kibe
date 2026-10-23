**English** | [Português](README.pt-BR.md)

# Network architecture

How the Muxe network is split into servers and how players move between them.

## Servers

```text
                      Velocity proxy (muxe-core-proxy)
                                 │
          ┌──────────────────────┼───────────────────────┐
          ▼                      ▼                       ▼
   auth-1 … auth-N        lobby-1 … lobby-N       skywars-1 … / bedwars-1 …
   Muxe-Core + Auth       Muxe-Core + Lobby       Muxe-Core + game
```

| Group | Kind | Role |
|---|---|---|
| `proxy` | `PROXY` | The only public entry point. Picks servers, guards authentication, answers player lookups. |
| `auth` | `AUTH` | Where every player starts. `/register` or `/login`, then the player is sent to a lobby. |
| `lobby` | `LOBBY` | The main lobby, where players choose a game with `/jogar`. |
| `skywars`, `bedwars`, … | `MINIGAME` | One game per server. Each server runs many matches of the same game. |

Every process connects to the shared infrastructure:

| Service | Role |
|---|---|
| PostgreSQL | Source of truth. See [Persistence](../persistence/README.md). |
| Redis | Packet transport, idempotency claims and, in the future, shared cache. See [Messaging](../messaging/README.md). |

## Identity

Each process has a `ServerIdentity`, read from its `config.yml`:

| Field | Example | Rule |
|---|---|---|
| `server.id` | `lobby-2` | Unique in the network, equal to the server name in `velocity.toml`. |
| `server.group` | `lobby` | Servers that run the same thing share a group. |
| `server.kind` | `LOBBY` | `PROXY`, `AUTH`, `LOBBY` or `MINIGAME`. |

## Discovery: the server directory

Servers find each other without any static list besides `velocity.toml`:

1. Every process (including the proxy) publishes a `server.status` packet every **2 seconds** with its id, group, kind, online and maximum players and whether it accepts players.
2. Every process keeps a `ServerDirectory` built from those packets. A server that has not reported for **10 seconds** is considered dead.
3. A server that stops cleanly publishes `server.shutdown`, and is removed immediately.
4. Out-of-order statuses are ignored: a status older than the one already known for that server changes nothing.

The proxy uses the directory to pick the least loaded server of a group that has room. A server known to Velocity but absent from the directory is not used.

## Player journey

```text
connect ─► proxy picks the least loaded auth server
        ─► /register or /login on the auth server
        ─► auth publishes auth.authenticated to the proxy
        ─► proxy opens the gate and sends the player to the least loaded lobby
        ─► /jogar skywars on the lobby publishes player.connect to the proxy
        ─► proxy sends the player to the least loaded skywars server
```

Until the proxy receives `auth.authenticated` for a player, it refuses every connection to a server outside the auth group. See [Network routing](../../features/network-routing/README.md).

## Scaling

- **Horizontal.** Any group can run more servers: add the server to `velocity.toml`, give it a unique `server.id`, start it. It joins the directory with its first heartbeat.
- **Isolation.** Each game runs in its own servers, so a heavy match or a crash in one game never slows down another.
- **Independent restarts.** A server can be restarted without touching the others; players are routed to its siblings while it is down.
- **Many proxies.** Several Velocity instances can run behind a TCP load balancer. Packets to `proxy` reach every proxy, and each one acts only on the players it holds.

## Not decided yet

- How a game lobby distributes players into match servers when lobbies and matches are split into different servers.
- Automatic start and stop of servers as demand changes.
- Session ownership between servers, described as planned in [Persistence](../persistence/README.md#planned-session-ownership).
