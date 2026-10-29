**English** | [Português](README.pt-BR.md)

# Network administration

Staff commands to see the network from inside the game. Lives in `core:bukkit` (`Muxe-Core`) and is available on every Spigot server.

## User stories

- As a staff member, I want to list every live server with its players, so that I know the state of the network.
- As a staff member, I want to find which server a player is on.
- As an operator, I want a quick view of packet health without opening the monitoring system.

## How it works

- `/muxe servers` lists the servers in the local server directory: id, group, kind and players.
- `/muxe find <player>` sends a `player.locate_request` to the proxy and shows the answer. If the proxy does not answer within `messaging.request-timeout`, the reason is shown.
- `/muxe metrics` shows totals of packets published, received, duplicated, rejected, handler failures and the current TPS.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/muxe servers` | Lists live servers. | `muxe.admin` |
| `/muxe find <player>` | Shows which server a player is on. | `muxe.admin` |
| `/muxe metrics` | Shows a summary of packet metrics and TPS. | `muxe.admin` |

`/muxe find` completes the names of online players with tab.

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.admin` | Every `/muxe` command. | op |

## Configuration

Messages under `messages.admin.*` in Muxe-Core's `config.yml`, plus `messages.no-permission` and the [command messages](../../development/commands/README.md#messages).

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.locate_request` | proxy | standard, max age 5 s | Ask where a player is. |
| `player.locate_response` | requester | standard, max age 5 s | The answer. |

## Data

None.

## Metrics

`muxe_requests_total{type="player.locate_request"}` and `muxe_request_seconds{type="player.locate_request"}`.

## Integrations

The proxy answers `player.locate_request`.

## Acceptance criteria

- [ ] `/muxe servers` lists every server that sent a heartbeat in the last 10 seconds (checked in game).
- [ ] `/muxe find` shows the server of an online player and says when the player is offline (checked in game).
- [x] Request and response travel through Redis and time out when nobody answers (integration and unit tests).

## Failures and recovery

- Proxy unreachable: `/muxe find` shows the timeout reason; nothing else is affected.

## Known limitations

- The server list comes from the local directory, so a server that just started may take up to 2 seconds to appear.
