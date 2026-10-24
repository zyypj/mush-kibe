**English** | [Português](README.pt-BR.md)

# Lobby

The main hub, where players arrive after logging in and choose a game. Lives in `servers:lobby` (`Muxe-Lobby`). The network can run several lobbies (`lobby-1`, `lobby-2`, …), all in the `lobby` group; each game will also have its own lobby servers.

## User stories

- As a player, I want to arrive at a clean, safe spawn, see the network at a glance and choose what to play.
- As a player, I want to switch to another main lobby, and to hide other players when the lobby is crowded.
- As a staff member, I want to fly in the lobby and to place NPCs that send players to each game.
- As a builder, I want to edit the lobby with permission, while regular players cannot break it.

## How it works

### Arrival

- After logging in, the proxy sends the player to the least loaded lobby (see [Network routing](../network-routing/README.md)).
- On join: Adventure mode, inventory and armor cleared, full health and hunger, experience reset, then teleported to the configured spawn (or the main world's spawn).
- Players with `muxe.lobby.fly` arrive `arrival.fly-height` blocks (5) above the spawn, already flying. Everyone else arrives on the spawn. Falling below `protection.void-level` sends the player back the same way.
- Join and quit messages are hidden.

### Boss bar and scoreboard

- The boss bar shows `MUXE` in `&e&l` and switches to `&6&l` every half second (`bossbar.frames`, `bossbar.interval`).
- The scoreboard, titled `&b&lMUXE`, shows:

```text

Tag: Membro
Créditos: 0

Lobby: #1
Jogadores: 2211

muxe.com.br
```

- `{lobby}` is the number at the end of the server id (`lobby-4` shows `#4`), and `{players}` counts every player on the network's Spigot servers. Lines refresh every `scoreboard.update-interval`.
- There is no tag or credit system yet: every player shows `scoreboard.default-tag` and `scoreboard.default-credits`. The values come from `PlayerInfoSource`, where those systems will plug in.

### Hotbar

| Slot | Item | Name | Action |
|---|---|---|---|
| 1 | Compass | `&aSelecionar jogo` | None yet |
| 2 | Player's head | `&aMeu perfil` | None yet |
| 5 | Chest | `&aCosméticos` | None yet |
| 8 | Dye | `&fJogadores: …` | Player visibility, see below |
| 9 | Nether star | `&aSelecionar lobby` | Opens the lobby selector |

- Items are used with a right click. They cannot be moved, dropped or taken.
- Using items or NPCs three times within `spam.window` (1 second) blocks them for `spam.cooldown` (3 seconds). While blocked, the player sees `&cAguarde 2,9s para usar novamente.`

### Lobby selector

- Title `Selecionar lobby (1/1)`. Every live lobby of `lobby-selector.group` appears in number order, centered on the middle row: one stained glass pane each, lime for the lobby the player is in, gray for the others.
- Each pane is named `&aLobby Principal #<number>` with the lore `&fJogadores &a<online>/<max>`.
- Clicking another lobby sends the player there through the proxy. Clicking the current lobby or a full one only shows a message.
- More than 21 lobbies spread over pages with arrows.

### Player visibility

The dye cycles through three options and the choice is saved, so it follows the player to every lobby:

| Option | Item | Message |
|---|---|---|
| Show everyone | Lime dye, `&fJogadores: &aAtivados` | `&aVocê agora está vendo todos os jogadores.` |
| Only friends | Cyan dye, `&fJogadores: &bApenas amigos` | `&bVocê agora está vendo apenas seus amigos.` |
| Hide everyone | Gray dye, `&fJogadores: &7Desativados` | `&cVocê escondeu todos os jogadores.` |

There is no friend system yet, so "Only friends" hides everyone. Friends will come from `FriendDirectory`.

### Game NPCs

- Staff place NPCs with `/npc criar <id> <grupo> <skin> <nome>` where they stand. The skin is the one of an original player name, fetched from Mojang once and stored.
- Above each NPC, the lines of `npcs.lines` show the game name and how many players are in its group.
- Clicking (left or right) sends the player to the least loaded server of the group, or says that no room is available.
- NPCs exist only in packets, are shown to players within 48 blocks and do not appear in the tab list. See [ADR 0008](../../decisions/0008-packet-npcs-and-click-interception.md).

### Protection

Players cannot take damage, lose hunger, break, place, drop or pick up items, or move items in their inventory; `muxe.lobby.build` allows all of that. Rain is cancelled.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/jogar <mode>` (`/play`) | Sends the player to a game group. | None |
| `/setspawn` | Saves the current position as the lobby spawn. | `muxe.lobby.setspawn` |
| `/npc criar <id> <grupo> <skin> <nome>` | Creates an NPC where you stand. | `muxe.lobby.npc` |
| `/npc mover <id>` | Moves an NPC to where you stand. | `muxe.lobby.npc` |
| `/npc skin <id> <skin>` | Changes the skin to the one of an original player name. | `muxe.lobby.npc` |
| `/npc nome <id> <nome>` | Changes the name shown above the NPC. | `muxe.lobby.npc` |
| `/npc remover <id>` | Removes an NPC. | `muxe.lobby.npc` |
| `/npc listar` | Lists the NPCs. | `muxe.lobby.npc` |

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.lobby.fly` | Arrive above the spawn, flying. | op |
| `muxe.lobby.build` | Break, place, drop and pick up items, and move items in the inventory. | op |
| `muxe.lobby.setspawn` | Use `/setspawn`. | op |
| `muxe.lobby.npc` | Use `/npc`. | op |

## Configuration

`plugins/Muxe-Lobby/config.yml`:

| Key | Default | Description |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Groups allowed in `/jogar`. |
| `protection.void-level` | `0` | Y below which players are sent back to spawn. |
| `arrival.fly-height` | `5` | Blocks above the spawn for players with `muxe.lobby.fly`. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam for hotbar items and NPCs. |
| `bossbar.frames`, `bossbar.interval` | `&e&lMUXE`, `&6&lMUXE`, `500ms` | Boss bar texts and how long each one stays. |
| `scoreboard.title`, `scoreboard.lines` | See above | Scoreboard text. Placeholders: `{tag}`, `{credits}`, `{lobby}`, `{players}`. |
| `scoreboard.update-interval` | `1s` | How often the scoreboard refreshes. |
| `scoreboard.default-tag`, `scoreboard.default-credits` | `&7Membro`, `0` | Shown until tags and credits exist. |
| `hotbar.<item>.slot`, `hotbar.<item>.item` | See above | Slot (1 to 9) and item of each hotbar item. `hotbar.visibility` has one item per option (`all`, `friends`, `none`). |
| `lobby-selector.group` | `lobby` | Group listed in the selector. |
| `lobby-selector.title`, `current`, `other`, `previous-page`, `next-page` | See above | Selector title and items. Placeholders: `{page}`, `{pages}`, `{number}`, `{online}`, `{max}`. |
| `npcs.lines` | `&b&l{name}`, `&e{players} jogando` | Lines above every NPC. |
| `spawn.*` | not set | Written by `/setspawn`. |
| `messages.*` | Portuguese texts | Messages of this plugin; shared messages come from Muxe-Core. |

Items use the [`ItemBuilder` format](../../development/core-api/README.md#items). NPCs are saved in `plugins/Muxe-Lobby/npcs.yml` by `/npc`.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.connect` | proxy | standard | Sent by `/jogar`, the NPCs and the lobby selector. |

## Data

| Table | Content |
|---|---|
| `lobby.player_settings` | `player_id`, `visibility` (`ALL`, `FRIENDS`, `NONE`), `updated_at`. Written when a player changes the visibility, read on join. |

Commands and queries: `ChangeVisibilityCommand`, `FindVisibilityQuery` (players without a row see everyone).

## Metrics

The Spigot server metrics of Muxe-Core, plus `muxe_db_transaction_seconds{operation="lobby.change_visibility"|"lobby.find_visibility"}`.

## Integrations

- [Network routing](../network-routing/README.md) brings players here after login and moves them to other lobbies and games.
- The [Core API](../../development/core-api/README.md) provides the boss bar, scoreboard, menus, items, holograms and NPCs.

## Acceptance criteria

- [x] Players with the fly permission arrive 5 blocks above the spawn, flying; others arrive on it.
- [x] The hotbar items sit in slots 1, 2, 5, 8 and 9 with the reference names, and the visibility dye cycles through the three options.
- [x] The lobby selector lists lobbies in number order, centered, lime for the current one and gray for the others, with the reference name and lore.
- [x] Three quick uses start a 3-second cooldown with the remaining time as `2,9`.
- [x] The boss bar alternates `&e&lMUXE` and `&6&lMUXE` every 10 ticks, and the scoreboard fills the reference lines.
- [x] The visibility choice is saved in PostgreSQL and read back.
- [x] NPCs survive being saved to and read from `npcs.yml`; broken entries are skipped.
- [ ] Boss bar, scoreboard, hotbar, selector, visibility and NPC clicks work in game (checked in game).

## Failures and recovery

- Proxy or Redis unavailable: `/jogar`, the selector and the NPCs show a failure message and the player stays in the lobby.
- PostgreSQL unavailable: the visibility starts as "show everyone" and changes are not saved; the lobby keeps working.
- Server internals missing (another server version): boss bar, NPCs or holograms are skipped with one warning each.
- Configured spawn or NPC world missing: the main world's spawn is used and the NPC is not shown.

## Known limitations

- The game selector, profile and cosmetics items do nothing yet.
- Tags, credits and friends do not exist yet; the scoreboard shows default values and "Only friends" hides everyone.
- NPCs look in a fixed direction.
