**English** | [Português](README.pt-BR.md)

# AFK server

Players who are not VIP and stand still for 5 minutes in a lobby are moved to a small AFK server, where they cannot chat, and go back to the exact server they came from as soon as they move. Lives in `servers:lobby` (`Muxe-Lobby`, package `afk`), which watches the lobbies, and `servers:afk` (`Muxe-AFK`), which runs the AFK server. AFK during matches works differently and is not covered here yet.

## User stories

- As an operator, I want idle players out of the lobbies, so that the lobbies keep room and their counts show who is really playing.
- As a player who stepped away, I want to come back to the lobby I was in just by moving.
- As a VIP, I want to stay in the lobby however long I stand still.

## How it works

### In the lobbies

- Every server running Muxe-Lobby watches it: the main lobbies (`lobby-1`, `lobby-2`, …) and every game lobby (`bedwars-lobby-1`, …). Game servers, auth servers and the AFK server itself do not.
- A player counts as active when they arrive and whenever they walk, jump, chat, run a command, click a block or the air, or click in an inventory. Only turning the head, or falling straight down, does not count.
- Once a second (`afk.check-interval`) the lobby looks for players who have not been active for 5 minutes (`afk.idle-after`). Players with `muxe.afk.bypass` are skipped; VIP and every rank above it have it (it is seeded on `vip`, so `mvp`, `pro`, `ultra`, the partners and the staff inherit it; `admin` has `*`).
- An idle player is sent to the least loaded server of the `afk` group (`afk.server-group`) with room, with no message. Before that, the lobby stores which server they are on (`afk:return:<player>` in `MuxeCore.store()`, for `afk.origin-ttl`, 12 hours).
- When no AFK server is alive or has room, the player stays where they are. The countdown restarts after each attempt, so a player who is still in the lobby is tried again 5 minutes later.

### On the AFK server

- The player arrives on the map's spawn (`spawn`, 8.5, 63, 8.5 facing the sign), in Adventure mode, with full health and hunger and an empty inventory. Damage, hunger, breaking, placing, dropping, picking up, clicking blocks and inventories are blocked for everyone outside [build mode](../lobby/README.md).
- Players on the AFK server do not see each other. This also keeps them from pushing each other, which would count as moving.
- On arrival they see the title `&c&lVOCÊ ESTÁ AFK` with the subtitle `&7Mova-se para voltar ao lobby.`, and in the chat:

```text
VOCÊ ESTÁ AFK!
Mova-se para voltar ao lobby.
```

- Join and quit messages are hidden.
- Chat is closed: anything the player types is dropped without a reply. They still receive every message sent to them (announcements, staff messages). Commands still work, so `/lobby` also leaves.
- As soon as the player moves (walks or jumps; turning the head does not count) they are sent back to the server stored by the lobby. If that server is gone or full, or nothing was stored, they go to the least loaded main lobby (`return.fallback-group`).
- Movement in the first second after arriving (`return.arrival-grace`) is ignored, so the arrival teleport never sends the player back.
- A return is requested once. If the player is still on the AFK server 5 seconds later (`return.retry-after`), for example because the proxy had nowhere to send them, the next movement asks again.
- Leaving the AFK server forgets the stored server.

## Commands

None.

## Permissions

| Permission | Grants |
|---|---|
| `muxe.afk.bypass` | Never sent to the AFK server for standing still in a lobby. Seeded on `vip` by the core migration `V17__grant_afk_bypass_to_vip.sql`. |

## Configuration

Muxe-Lobby `config.yml`:

| Key | Default | Description |
|---|---|---|
| `afk.enabled` | `true` | Turns the AFK watch of the lobby on or off. |
| `afk.idle-after` | `5m` | How long a player must stay inactive to be sent away. |
| `afk.server-group` | `afk` | Group of the AFK servers. |
| `afk.check-interval` | `1s` | How often the lobby looks for idle players. |
| `afk.origin-ttl` | `12h` | How long the server the player came from is kept. |

Muxe-AFK `config.yml`:

| Key | Default | Description |
|---|---|---|
| `spawn.world`, `x`, `y`, `z`, `yaw`, `pitch` | `afk`, 8.5, 63, 8.5, 0, 0 | Where players arrive. Without it, the center of the first world's spawn block. |
| `return.fallback-group` | `lobby` | Where a player goes when the server they came from is gone, full or unknown. |
| `return.arrival-grace` | `1s` | Movement ignored right after arriving. |
| `return.retry-after` | `5s` | When a return that did not happen can be requested again. |
| `messages.title`, `messages.subtitle` | `&c&lVOCÊ ESTÁ AFK`, `&7Mova-se para voltar ao lobby.` | Title shown on arrival. |
| `messages.arrived` | The two chat lines above | Chat lines on arrival, a YAML list. |

Muxe-AFK `lang/en.yml` holds the English `messages.*` at the same paths. Each player reads the title and the chat lines in their own [language](../language/README.md).

Muxe-Core `config.yml` of the AFK server: `server.group: afk` and `server.kind: AFK`. The `AFK` kind keeps the lobby hotbar, the greeting and the shop off that server.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.connect` | proxy | standard | Sends the idle player to the `afk` group, and back to their server or to a lobby. See [Network routing](../network-routing/README.md). |

## Data

| Store | Key | Value | Lifetime |
|---|---|---|---|
| `MuxeCore.store()` (Redis) | `afk:return:<player uuid>` | Id of the server the player came from | `afk.origin-ttl` (12 hours), removed when the player leaves the AFK server |

`perm_group_nodes` gets `muxe.afk.bypass` on `vip` (core migration `V17`).

## Metrics

None beyond the packet metrics of `player.connect`.

## Integrations

- [Lobby](../lobby/README.md): the watch runs inside Muxe-Lobby, on every main and game lobby.
- [Network routing](../network-routing/README.md): moves the player with `player.connect`.
- [Ranks and tags](../ranks-and-tags/README.md): `muxe.afk.bypass` comes with VIP.
- `IdleTracker`, `Displacement` and `AfkReturns` live in `core:common` (packages `activity` and `afk`), so other features can reuse them.

## Acceptance criteria

- [x] A non-VIP player inactive for 5 minutes in a lobby is sent to the `afk` group and the lobby they were in is stored.
- [x] A player active less than 5 minutes ago, a VIP, or anyone while no AFK server has room, stays in the lobby.
- [x] Moving on the AFK server sends the player back to the server they came from, or to the main lobby when it is gone, full or unknown.
- [x] Movement in the first second after arriving is ignored, and a return is requested only once until it fails or 5 seconds pass.
- [x] The title and the chat lines match the reference screenshots.
- [ ] A player standing still in `lobby-1` arrives on the AFK server after 5 minutes, sees the title and the chat lines, cannot chat, and walking takes them back to `lobby-1` (checked in game).
- [ ] The same from `bedwars-lobby-1` takes the player back to `bedwars-lobby-1` (checked in game).
- [ ] A VIP standing still in a lobby is never moved (checked in game).

## Failures and recovery

- **No AFK server alive or full:** idle players stay in the lobby and are tried again 5 minutes later.
- **The server the player came from went down:** moving takes them to the least loaded main lobby.
- **No lobby alive either:** the proxy answers `no-server`, the player stays on the AFK server, and moving after 5 seconds tries again.
- **Redis down:** the lobby cannot store the origin, and the move request does not reach the proxy, so the player stays in the lobby. On the AFK server, reading the origin fails and the return is tried again on the next movement.
- **The AFK server goes down:** the proxy moves its players to a lobby, as for any other server (see [Network routing](../network-routing/README.md)).

## Known limitations

- AFK in matches (with warnings) is a separate feature, not built yet.
- When the server the player came from is gone, they go to a main lobby, not to another lobby of the same game.
- Being pushed by another player in a lobby counts as moving.
- Every server of the network must run a Muxe-Core that knows the `AFK` kind before the AFK server starts; an older core cannot read its status and does not route players to it.
