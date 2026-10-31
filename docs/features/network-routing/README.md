**English** | [Português](README.pt-BR.md)

# Network routing

How players are placed on servers and moved between them. Lives in `core:proxy` (`muxe-core-proxy`) and the `/lobby` command of `core:bukkit`.

## User stories

- As a player, I want to land on a working server and be moved to a lobby after logging in.
- As a player, I want `/lobby` to take me back to a lobby from anywhere.
- As an operator, I want players spread across servers of the same group, and nobody to bypass authentication.

## How it works

- **Gate:** a player is authenticated when the account is premium, when a [login session](../authentication/README.md#2-automatic-authentication-proxy-at-login) is resumed, or when the proxy receives `auth.authenticated` from a trusted auth server. Until then, every connection to a server outside the auth group is denied with `must-authenticate`.
- **Closed network:** while no auth server is alive, nobody can log in, not even premium players or resumed sessions. The proxy refuses the login before anything else runs, so the player sees the `no-auth-server` screen on "Failed to connect to the server" and no account is recorded.
- **Initial server:** authenticated players start in the least loaded lobby; everyone else starts in the least loaded auth server that has room. If no lobby is alive, an authenticated player starts on the least loaded auth server instead and sees `no-lobby-server` once connected. A player with no server to join is disconnected with `no-lobby-server` or `no-auth-server`. Unauthenticated players never fall back to a lobby.
- **After authenticating on the auth server:** the proxy sends the player to the least loaded lobby. If none is alive, the player stays on the auth server and sees `no-lobby-server`.
- **When the player's server goes down or kicks them:** the proxy picks where the player goes next (`Fallbacks`), never the server they left.
  - A logged-in player goes to the least loaded lobby and sees `server-moved`. A player kicked on purpose by a server that is still up also goes to a lobby, and sees that server's kick reason instead.
  - If no other lobby is alive, even when the auth server is, the logged-in player is disconnected with the `server-restarting` screen. The same screen is shown when every server is down.
  - A player still logging in moves to another auth server only when theirs is closing; with no other auth server they get `server-restarting`. A kick on purpose by an auth server that is still up (a wrong password, the login time running out) works as before.
  - A server counts as closing when it already left the server list (its core announced the shutdown), when the kick has no reason, or when the reason is the Bukkit shutdown message (`Server closed`).
- **Entering a game:** the game selector, game NPCs and `/jogar` send the player to the game's lobby group (`<game>-lobby`, such as `bedwars-lobby`) when one of its servers is alive, and to the game group itself otherwise.
- **Moving later:** any server can publish `player.connect` to send a player to a server id or to the least loaded server of a group. The proxy ignores it for unauthenticated players, and tells the player `no-server` when there is no destination.
- **Least loaded:** among alive servers of the group with room, the one with fewest players; ties are broken by id.
- **Disconnect:** the gate forgets the player.
- **Server list:** the proxy answers every ping with the `motd.lines` (a bar in the rainbow colors with `&b&lMUXE` in the middle, then `&a&lVENHA JOGAR`) and shows `motd.max-players` (3000) as the maximum. Each line is padded with spaces so it sits in the middle of the server list (`TextWidth.center`, 271 pixels). The online count stays the real one, and the icon is Velocity's `server-icon.png`.
- **Tab list:** every time a player connects to a server (auth, lobby or game), the proxy sets the tab list header (`&b&lMUXE`) and footer (`&eWebsite &bmuxe.com.br`, `&eLoja &bloja.muxe.com.br`, `&eDiscord &bmuxe.com.br/discord`), each with a blank line around it. The player names in the list keep the rank prefix from the [ranks](../ranks-and-tags/README.md).

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
| `motd.lines` | The MUXE bar and `&a&lVENHA JOGAR` | The two lines of the server list. Write them without padding: the proxy centers them. |
| `motd.max-players` | `3000` | Maximum shown in the server list. |
| `tab-list.header`, `tab-list.footer` | The MUXE title; the website, shop and Discord lines | Lines above and below the player list, on every server. YAML lists, `""` for a blank line. |
| `messages.no-auth-server` | Six-line screen: could not connect, reason, retry hint, a blank line, where to follow the news and the team's Discord | Shown when the login is refused because no auth server is alive. A YAML list, one item per line. |
| `messages.server-restarting` | Five-line screen: disconnected, reason (server restarting), retry hint, a blank line and the Discord for more information | Shown when the player's server goes down and there is no other lobby, or no server at all. A YAML list, one item per line. |
| `messages.server-moved` | `&eO servidor em que você estava está reiniciando. Você foi levado para outro servidor.` | Chat message after being moved because the server went down. |
| `messages.no-lobby-server`, `no-server`, `must-authenticate` | Portuguese texts | Messages shown by the proxy. |
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
- [x] While no auth server is alive the network refuses every login, and a full auth server still counts as up.
- [x] A player whose server goes down moves to another lobby, and is disconnected with `server-restarting` when only the auth server, or nothing, is left.
- [ ] After login the player arrives in a lobby (checked in game).
- [ ] Restarting a lobby moves its players to the other lobby (checked in game).
- [ ] Premium players and resumed sessions start directly in a lobby (checked in game).
- [x] The server list shows the centered MUXE lines and 3000 as the maximum, keeping the real online count.
- [ ] The MOTD looks centered in the 1.8 client (checked in game).
- [x] Every server the player connects to gets the tab list header and footer.
- [ ] The tab list matches the reference on every server (checked in game).

## Failures and recovery

- **No auth server alive:** logins are refused for everyone with `no-auth-server`; players already online stay where they are. Logins open again within seconds of an auth server reporting its status.
- **No server of the target group alive:** the player stays where they are and sees a message.
- **The player's server goes down:** they move to another lobby, or are disconnected with `server-restarting` when there is none.
- **Redis down:** routing requests from servers fail; players stay on their current server. The directory recovers within seconds of the reconnection.
- **Proxy restart:** players reconnect and authenticate again.

## Known limitations

- Choosing a server is by player count only; there is no party, queue or region awareness yet.
- With several proxies, the gate lives in each proxy's memory, but login sessions live in Redis, so a cracked player reconnecting through another proxy within the session duration is still let in.
