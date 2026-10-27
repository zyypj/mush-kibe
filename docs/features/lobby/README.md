**English** | [Português](README.pt-BR.md)

# Lobby

The main hub, where players arrive after logging in and choose a game. Lives in `servers:lobby` (`Muxe-Lobby`). The network can run several lobbies (`lobby-1`, `lobby-2`, …), all in the `lobby` group; each game will also have its own lobby servers.

## User stories

- As a player, I want to arrive at a clean, safe spawn, see the network at a glance and choose what to play.
- As a player, I want to switch to another main lobby, and to hide other players when the lobby is crowded.
- As a staff member, I want to fly in the lobby.
- As a builder, I want to switch to a build mode to edit the lobby, while regular players cannot break it.

## How it works

### Arrival

- After logging in, the proxy sends the player to the least loaded lobby (see [Network routing](../network-routing/README.md)).
- On join: Adventure mode, inventory and armor cleared, full health and hunger, experience reset, then teleported to the configured spawn (or the main world's spawn).
- Players with `muxe.lobby.fly` arrive `arrival.fly-height` blocks (5) above the spawn, already flying. Everyone else arrives on the spawn. Falling below `protection.void-level` sends the player back the same way.
- Join and quit messages are hidden.
- Half a second after joining, the player hears a level-up sound and sees the title `&b&lMUXE` with the subtitle `&eBoas-vindas!`. This greeting comes from Muxe-Core and runs on every `LOBBY` server, never on auth or game servers.
- Switching lobbies in the selector, or using `/lobby`, shows no "connecting" message: the player is just moved.

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
- Using hotbar items three times within `spam.window` (1 second) blocks them for `spam.cooldown` (3 seconds). While blocked, the player sees `&cAguarde 2,9s para usar novamente.`

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

### NPCs, holograms and shop

NPCs, holograms and the shop with its cash registers come from Muxe-Core and work on every server, not only here. Whatever staff place in one lobby shows up in every lobby of the `lobby` group. See [NPCs and holograms](../npcs-and-holograms/README.md) and [Shop](../shop/README.md).

### Protection

Players cannot take damage, lose hunger, break, place, drop or pick up items, or move items in their inventory. Rain is cancelled.

Players are always in Adventure mode: any change to another game mode is cancelled, except Creative for a player in build mode.

### Build mode

`/build` comes from Muxe-Core and works on every server, as described in the [Core API](../../development/core-api/README.md#build-mode). In the lobby:

- `/build on` clears the hotbar, switches to Creative and lets the player break, place and move items, with `&aModo construção ativado` in the action bar for as long as the mode is on.
- `/build off` gives the lobby items back (with the current visibility dye), returns to Adventure and lets players with `muxe.lobby.fly` fly again.
- Build mode follows the player to the other servers: moving to another lobby keeps it on. Hotbar actions do nothing while it is on.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/jogar <mode>` (`/play`) | Sends the player to a game group. | None |
| `/setspawn` | Saves the current position as the lobby spawn. | `muxe.lobby.setspawn` |
| `/build <on\|off>` | Turns build mode on or off on any server; without an argument it toggles. Provided by Muxe-Core. | `muxe.build` |

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.lobby.fly` | Arrive above the spawn, flying. | op |
| `muxe.build` | Use `/build` (Muxe-Core) to break, place, drop and pick up items in Creative. | op |
| `muxe.lobby.setspawn` | Use `/setspawn`. | op |

## Configuration

`plugins/Muxe-Lobby/config.yml`:

| Key | Default | Description |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Groups allowed in `/jogar`. |
| `protection.void-level` | `0` | Y below which players are sent back to spawn. |
| `arrival.fly-height` | `5` | Blocks above the spawn for players with `muxe.lobby.fly`. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam for hotbar items. NPCs and cash registers use the one of Muxe-Core. |
| `bossbar.frames`, `bossbar.interval` | `&e&lMUXE`, `&6&lMUXE`, `500ms` | Boss bar texts and how long each one stays. |
| `scoreboard.title`, `scoreboard.lines` | See above | Scoreboard text. Placeholders: `{tag}`, `{credits}`, `{lobby}`, `{players}`. |
| `scoreboard.update-interval` | `1s` | How often the scoreboard refreshes. |
| `scoreboard.default-tag`, `scoreboard.default-credits` | `&7Membro`, `0` | Shown until tags and credits exist. |
| `hotbar.<item>.slot`, `hotbar.<item>.item` | See above | Slot (1 to 9) and item of each hotbar item. `hotbar.visibility` has one item per option (`all`, `friends`, `none`). |
| `lobby-selector.group` | `lobby` | Group listed in the selector. |
| `lobby-selector.title`, `current`, `other`, `previous-page`, `next-page` | See above | Selector title and items. Placeholders: `{page}`, `{pages}`, `{number}`, `{online}`, `{max}`. |
| `spawn.*` | not set | Written by `/setspawn`. |
| `messages.*` | Portuguese texts | Messages of this plugin; shared messages come from Muxe-Core. |

`plugins/Muxe-Core/config.yml`:

| Key | Default | Description |
|---|---|---|
| `join-greeting.server-kinds` | `LOBBY` | Server kinds that greet players on join. |
| `join-greeting.delay` | `500ms` | Wait after joining before the greeting. |
| `join-greeting.sound`, `volume`, `pitch` | `LEVEL_UP`, `1.0`, `1.0` | Sound played to the player (a 1.8 `Sound` name). Empty for no sound. |
| `join-greeting.fade-in`, `stay`, `fade-out` | `10`, `50`, `20` | Title timings in ticks. |
| `messages.join-title`, `messages.join-subtitle` | `&b&lMUXE`, `&eBoas-vindas!` | Title and subtitle of the greeting. |

Items use the [`ItemBuilder` format](../../development/core-api/README.md#items). The keys of NPCs, holograms and the shop are in `plugins/Muxe-Core/config.yml`, described in [NPCs and holograms](../npcs-and-holograms/README.md#configuration) and [Shop](../shop/README.md#configuration).

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.connect` | proxy | standard | Sent by `/jogar` and the lobby selector. |

## Data

| Table | Content |
|---|---|
| `lobby.player_settings` | `player_id`, `visibility` (`ALL`, `FRIENDS`, `NONE`), `updated_at`. Written when a player changes the visibility, read on join. |

Commands and queries: `ChangeVisibilityCommand`, `FindVisibilityQuery` (players without a row see everyone).

## Metrics

The Spigot server metrics of Muxe-Core, plus `muxe_db_transaction_seconds{operation="lobby.change_visibility"|"lobby.find_visibility"}`.

## Integrations

- [Network routing](../network-routing/README.md) brings players here after login and moves them to other lobbies and games.
- The [Core API](../../development/core-api/README.md) provides the boss bar, scoreboard, menus and items.
- [NPCs and holograms](../npcs-and-holograms/README.md) and the [Shop](../shop/README.md) come from Muxe-Core and are placed in the lobby by command.

## Acceptance criteria

- [x] Players with the fly permission arrive 5 blocks above the spawn, flying; others arrive on it.
- [x] The hotbar items sit in slots 1, 2, 5, 8 and 9 with the reference names, and the visibility dye cycles through the three options.
- [x] The lobby selector lists lobbies in number order, centered, lime for the current one and gray for the others, with the reference name and lore.
- [x] Three quick uses start a 3-second cooldown with the remaining time as `2,9`.
- [x] The join greeting is enabled only on `LOBBY` servers, with the reference title and subtitle.
- [x] The boss bar alternates `&e&lMUXE` and `&6&lMUXE` every 10 ticks, and the scoreboard fills the reference lines.
- [x] The visibility choice is saved in PostgreSQL and read back.
- [ ] Boss bar, scoreboard, hotbar, selector and visibility work in game (checked in game).

## Failures and recovery

- Proxy or Redis unavailable: `/jogar` and the selector show a failure message and the player stays in the lobby.
- PostgreSQL unavailable: the visibility starts as "show everyone" and changes are not saved; the lobby keeps working.
- Server internals missing (another server version): the boss bar is skipped with one warning.
- Configured spawn world missing: the main world's spawn is used.

## Known limitations

- The game selector, profile and cosmetics items do nothing yet.
- Tags, credits and friends do not exist yet; the scoreboard shows default values and "Only friends" hides everyone.
