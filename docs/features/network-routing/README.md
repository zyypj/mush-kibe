**English** | [Português](README.pt-BR.md)

# Network routing

How players are placed on servers and moved between them. Lives in `core:proxy` (`muxe-core-proxy`) and the `/lobby` command of `core:bukkit`.

## User stories

- As a player, I want to land on a working server and be moved to a lobby after logging in.
- As a player, I want `/lobby` to take me back to a lobby from anywhere.
- As an operator, I want players spread across servers of the same group, and nobody to bypass authentication.

## How it works

- **Gate:** a player is authenticated when the account is premium, when a [login session](../authentication/README.md#2-automatic-authentication-proxy-at-login) is resumed, or when the proxy receives `auth.authenticated` from a trusted auth server. Until then, every connection to a server outside the auth group is denied with `must-authenticate`.
- **Initial server:** authenticated players start in the least loaded lobby; everyone else starts in the least loaded auth server that has room. If no lobby is alive, an authenticated player starts on the least loaded auth server instead and sees `no-lobby-server` once connected. A player with no server to join is disconnected with `no-lobby-server` or `no-auth-server`. Unauthenticated players never fall back to a lobby.
- **After authenticating on the auth server:** the proxy sends the player to the least loaded lobby. If none is alive, the player stays on the auth server and sees `no-lobby-server`.
- **Moving later:** any server can publish `player.connect` to send a player to a server id or to the least loaded server of a group. The proxy ignores it for unauthenticated players, and tells the player `no-server` when there is no destination.
- **Least loaded:** among alive servers of the group with room, the one with fewest players; ties are broken by id.
- **Disconnect:** the gate forgets the player.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/lobby` (`/hub`, `/l`) | Sends the player to a lobby. Available on every Spigot server. | None |

## Permissions

None.

## Configuration

Proxy `config.yml`:

| Key | Default | Description |
|---|---|---|
| `routing.auth-group` | `auth` | Group of the auth servers. |
| `routing.lobby-group` | `lobby` | Group players are sent to after authenticating. |
| `messages.no-auth-server`, `no-lobby-server`, `no-server`, `must-authenticate` | Portuguese texts | Messages shown by the proxy. |
| `authentication.*` | see [Authentication](../authentication/README.md#configuration) | Premium detection and login sessions. |

Muxe-Core `config.yml`:

| Key | Default | Description |
|---|---|---|
| `network.lobby-group` | `lobby` | Group used by `/lobby`. |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `server.status` | broadcast | `NONE` | Feeds the server directory used for routing. |
| `server.shutdown` | broadcast | standard | Removes a stopping server immediately. |
| `player.connect` | proxy | standard | Move a player to a server or group. |
| `auth.authenticated` | proxy | `DURABLE` | Opens the gate for a player. |

## Data

None. The gate lives in the proxy's memory.

## Metrics

`muxe_proxy_authenticated_players`, `muxe_network_servers_alive`, `muxe_network_players_online` and the packet metrics of the packets above.

## Integrations

- [Authentication](../authentication/README.md) publishes `auth.authenticated`.
- [Lobby](../lobby/README.md) publishes `player.connect` for `/jogar`.

## Acceptance criteria

- [x] Unauthenticated players can only reach auth servers.
- [x] Authenticated players can reach every server.
- [x] Only auth servers are trusted to authenticate players.
- [x] The least loaded server with room is chosen, and dead servers are ignored.
- [ ] After login the player arrives in a lobby (checked in game).
- [ ] Premium players and resumed sessions start directly in a lobby (checked in game).

## Failures and recovery

- **No server of the target group alive:** the player stays where they are and sees a message.
- **Redis down:** routing requests from servers fail; players stay on their current server. The directory recovers within seconds of the reconnection.
- **Proxy restart:** players reconnect and authenticate again.

## Known limitations

- Choosing a server is by player count only; there is no party, queue or region awareness yet.
- With several proxies, the gate lives in each proxy's memory, but login sessions live in Redis, so a cracked player reconnecting through another proxy within the session duration is still let in.
