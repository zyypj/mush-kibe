**English** | [Português](README.pt-BR.md)

# NPCs and holograms

NPCs and text holograms that staff place by command, on any server. They live in Muxe-Core (`core:common` package `br.com.muxe.core.common.placement`, `core:bukkit` packages `placement`, `npc/placed` and `hologram/placed`), are stored in PostgreSQL per server group and show up on every server of that group.

## User stories

- As a staff member, I want to place an NPC that sends players to a game, opens the shop or just stands there, on any lobby or game server.
- As a staff member, I want to write text holograms in the world and edit them line by line, without restarting anything.
- As a staff member, I want what I place on one server to appear on every server of the same group, so that `lobby-1` and `lobby-7` always look the same.
- As a player, I want to click an NPC and be taken to the game it shows, with the number of players in it above its head.

## How it works

### Shared by the server group

- Every NPC, hologram, shop [cash register](../shop/README.md#cash-registers), [social media head](../social-heads/README.md) and [parkour](../parkour/README.md) point belongs to a server group (`server.group` in the Muxe-Core config). Every server of that group shows the same ones; other groups have their own. Ids are unique inside a group.
- Ids use lowercase letters, digits, `-` and `_`, up to 32 characters.
- A command saves the change in PostgreSQL (`SavePlacementCommand` or `RemovePlacementCommand`). Then the server where it ran reloads the group's placements and publishes `placement.changed` to the group; every other server of the group reloads them from PostgreSQL (`FindPlacementsQuery`). A server ignores its own packets, so it never reloads twice.
- On startup each server loads the placements of its group, so a server that starts later shows what was placed before.
- After a reload, only what changed is shown again: an NPC that did not change is not respawned.
- Placements are shown only if their world is loaded on that server.

### NPCs

- `/npc criar <id> <ação> <skin> <nome>` places an NPC where you stand, facing where you face (head level). The name can have spaces and `&` colors.
- The action is one word:

| Action | Word | Lines above the NPC | Click |
|---|---|---|---|
| Game | The group, such as `skywars` | `npcs.lines.game`: `&b&l{name}`, `&e{players} jogando` | Sends the player to the least loaded server of the group, or answers `&cNenhuma sala de &f{name} &cdisponível no momento.` |
| Shop | `loja` | `npcs.lines.shop`: `&b&l{name}`, `&eClique para abrir a loja` | Opens the [shop](../shop/README.md) on the Ranks tab. |
| None | `nenhuma` | `npcs.lines.none`: `&b&l{name}` | Nothing. |

- `{name}` is the NPC name and `{players}` the players online on the live servers of the game group.
- The skin is the one of an original player name, fetched from Mojang when the NPC is created or the skin changes, and stored with the NPC. When the name has no skin, the NPC keeps the default skin and staff see `&eSkin de &f{skin} &enão encontrada. O NPC fica com a skin padrão.`
- NPCs exist only in packets, are shown to players within 48 blocks and do not stay in the tab list. Left and right clicks both count. See [ADR 0008](../../decisions/0008-packet-npcs-and-click-interception.md).
- Clicks on game and shop NPCs share the anti-spam of Muxe-Core (`spam.*`): three clicks within 1 second block them for 3 seconds with `&cAguarde 2,9s para usar novamente.`

### Holograms

- `/holograma criar <id> <texto>` creates a text hologram where you stand. Its first line sits `holograms.top` (2.0) blocks above that spot and each next line goes `holograms.line-spacing` (0.28) blocks lower, so adding lines grows the hologram downward and the first line never moves.
- Lines use `&` colors. `{vazio}` as the whole text makes a blank line that keeps its space.
- Line numbers start at 1. `/holograma ver <id>` lists them. A hologram always has at least one line: removing the last one is refused, use `/holograma remover`.
- Holograms are invisible armor stands that exist only in packets, like the [display stands](../../development/core-api/README.md#display-stands).

### Importing old placements

On startup, if `plugins/Muxe-Core/import.yml` exists, Muxe-Core imports it into the server's group and renames it to `import.yml.done`. It reads NPCs in the old lobby `npcs.yml` format under `npcs:` (paste the old file there, one level in, since the old file kept them at the root; the `group` becomes the action, so a game group gives a game NPC), cash registers under `registers:` and the [parkour](../parkour/README.md#placing-a-course) start, checkpoints (keyed by number) and end under `parkour:` (each point with `world`, `x`, `y`, `z`, `yaw` and `pitch`):

```yaml
npcs:
  skywars:
    group: skywars
    name: "&bSkyWars"
    skin-name: SomePlayer
    skin:
      value: "..."
      signature: "..."
    location:
      world: lobby
      x: 10.5
      y: 33.0
      z: -4.5
      yaw: 90.0
      pitch: 0.0
registers:
  main:
    location:
      world: lobby
      x: 7.5
      y: 33.0
      z: -9.5
      yaw: 180.0
      pitch: 0.0
    seller:
      world: lobby
      x: 7.5
      y: 33.0
      z: -9.5
      yaw: 180.0
      pitch: 0.0
```

Broken entries are skipped with a warning. Entries with an id that already exists replace it. If saving fails, the file stays and the import runs again on the next start. Put the file on one server of the group only.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/npc criar <id> <ação> <skin> <nome>` | Creates an NPC where you stand. | `muxe.npc` |
| `/npc acao <id> <ação>` | Changes what the NPC does when clicked. | `muxe.npc` |
| `/npc mover <id>` | Moves an NPC to where you stand. | `muxe.npc` |
| `/npc skin <id> <skin>` | Changes the skin to the one of an original player name. | `muxe.npc` |
| `/npc nome <id> <nome>` | Changes the name shown above the NPC. | `muxe.npc` |
| `/npc remover <id>` | Removes an NPC. | `muxe.npc` |
| `/npc listar` | Lists the NPCs of this server group. | `muxe.npc` |
| `/holograma criar <id> <texto>` (`/hologram`, `/holo`) | Creates a hologram where you stand. | `muxe.hologram` |
| `/holograma adicionar <id> <texto>` | Adds a line at the end. | `muxe.hologram` |
| `/holograma linha <id> <número> <texto>` | Replaces a line. | `muxe.hologram` |
| `/holograma removerlinha <id> <número>` | Removes a line. | `muxe.hologram` |
| `/holograma ver <id>` | Shows the numbered lines. | `muxe.hologram` |
| `/holograma mover <id>` | Moves a hologram to where you stand. | `muxe.hologram` |
| `/holograma remover <id>` | Removes a hologram. | `muxe.hologram` |
| `/holograma listar` | Lists the holograms of this server group. | `muxe.hologram` |

Without arguments, or with a wrong one, each command shows its usage.

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.npc` | Use `/npc`. Replaces `muxe.lobby.npc`. | op |
| `muxe.hologram` | Use `/holograma`. | op |

## Configuration

`plugins/Muxe-Core/config.yml`:

| Key | Default | Description |
|---|---|---|
| `server.group` | `lobby` | The group whose placements this server shows and edits. |
| `npcs.lines.game` | `&b&l{name}`, `&e{players} jogando` | Lines above game NPCs. |
| `npcs.lines.shop` | `&b&l{name}`, `&eClique para abrir a loja` | Lines above shop NPCs. |
| `npcs.lines.none` | `&b&l{name}` | Lines above NPCs without an action. |
| `holograms.top` | `2.0` | Height of the first hologram line above where it was created. |
| `holograms.line-spacing` | `0.28` | Space between hologram lines. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam for NPC, cash register, hotbar, social media head and parkour item clicks. |
| `messages.npc-*`, `messages.hologram-*`, `messages.placement-failed`, `messages.spam-cooldown` | Portuguese texts | Command answers, usage lists and click messages. |
| `messages.game-connecting`, `messages.game-unavailable` | `&7Procurando uma sala de &b{name}&7...`, `&cNenhuma sala de &f{name} &cdisponível no momento.` | Shown when a game NPC is clicked, shared with the [game selector](../hotbar/README.md#game-selector). They replace `npc-connecting` and `npc-unavailable`. |

`/npc` no longer writes `plugins/Muxe-Lobby/npcs.yml`; use `import.yml` to bring old NPCs in.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `placement.changed` | the server group | standard | Something of the group was created, changed or removed; every server of the group reloads its placements from PostgreSQL. Carries the group. |
| `player.connect` | proxy | standard | Sent by game NPCs to move the player. |

## Data

| Table | Content |
|---|---|
| `core.npc_placements` | `server_group`, `id`, position (`world`, `x`, `y`, `z`, `yaw`, `pitch`), `name`, `action` (`NONE`, `GAME`, `SHOP`), `action_target` (the game group, empty otherwise), `skin_name`, `skin_value`, `skin_signature`. |
| `core.hologram_placements` | `server_group`, `id`, position, `lines` (text array, empty string for a blank line). |
| `core.register_placements` | Cash registers, see [Shop](../shop/README.md#data). |
| `core.social_head_placements` | Social media heads, see [Social media heads](../social-heads/README.md#data). |
| `core.parkour_placements` | Parkour points, see [Parkour](../parkour/README.md#data). |

The primary key of each table is `(server_group, id)`. Created by the core migration `V4__create_placements_and_promotion.sql`, except `core.social_head_placements`, created by `V6__create_social_head_placements.sql`, and `core.parkour_placements`, created by `V7__create_parkour.sql`.

Commands and queries: `SavePlacementCommand`, `RemovePlacementCommand` (fails with `placement.not_found` when the id does not exist), `FindPlacementsQuery(group)`, registered by `PlacementModule`.

## Metrics

`muxe_db_transaction_seconds{operation="placement.save"|"placement.remove"|"placement.find"}`, `muxe_cqrs_handle_seconds` for the commands and the query above, and the packet metrics of `placement.changed`.

## Integrations

- The [Core API](../../development/core-api/README.md) provides `NpcService`, `MojangSkins`, `DisplayService`, `LabelStack`, `GameConnector` (shared with the [game selector](../hotbar/README.md#game-selector)) and `ShopAccess`.
- The [Shop](../shop/README.md) uses the same storage and sync for its cash registers, and shop NPCs open it.
- The [social media heads](../social-heads/README.md) are another placement kind (`SOCIAL_HEAD`) with the same storage and sync.
- The [parkour](../parkour/README.md) start, checkpoints and end are another placement kind (`PARKOUR`) with the same storage, sync and `import.yml`.
- [Network routing](../network-routing/README.md) moves the players that click a game NPC.

## Acceptance criteria

- [x] NPC actions are read from one word: `loja`, `nenhuma` or a game group; only game actions keep a target.
- [x] Ids are short lowercase words; hologram lines are numbered from 1 and a hologram keeps at least one line.
- [x] NPCs, holograms and registers survive a round trip through PostgreSQL; saving again replaces, and other groups stay apart.
- [x] A change on one server shows up on every server of the group, removals too, and a server loads what was placed before it started.
- [x] Only changed placements are shown again, and missing ones are removed.
- [x] NPC lines are filled per action with `{name}` and `{players}`.
- [x] Hologram lines go down from the first line, which never moves.
- [x] The old `npcs.yml` format, registers and parkour points are read from `import.yml` into the server's group.
- [ ] Placing, editing and clicking NPCs and holograms works in game, and the change appears on a second server of the group (checked in game).

## Failures and recovery

- PostgreSQL unavailable when a command runs: staff see `&cNão foi possível salvar agora. Tente novamente em instantes.` and nothing changes.
- PostgreSQL unavailable on startup: nothing is shown, with a warning in the log. The next change in the group, or a restart, loads everything again.
- Redis unavailable: the change is saved and shown on the server where it was made; the other servers show it after their next reload.
- Mojang unavailable or unknown name: the NPC keeps the default skin; `/npc skin` can be run again later.
- Proxy unavailable or no live server in the game group: the player sees a failure message and stays where they are.
- World not loaded on a server: that NPC or hologram is skipped on that server with a warning.
- Server internals missing (another server version): NPCs and holograms are skipped with one warning each.

## Known limitations

- NPCs look in a fixed direction, the one you faced when placing or moving them; they do not turn to players.
- NPC lines are the same for every NPC of an action; only `{name}` and `{players}` change.
- Placements belong to a whole group: a single server of the group cannot have its own.
- Two staff members editing the same placement at the same time: the last save wins.
- The import file must be placed by hand and is only read on startup.
