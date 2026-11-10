**English** | [Português](README.pt-BR.md)

# Lobby

The main hub, where players arrive after logging in and choose a game. Lives in `servers:lobby` (`Muxe-Lobby`). The network can run several lobbies (`lobby-1`, `lobby-2`, …), all in the `lobby` group; each game will also have its own lobby servers.

The same plugin runs on every lobby, the main ones and each game's (such as `bedwars-lobby`); a game only adds what its lobby needs, as BedWars does in lobby mode. The lobby plugin handles the spawn and arrival, flight, protection, the scoreboard, the boss bar and `/jogar`. The hotbar, its menus (game selector, profile, lobby selector) and the player visibility come from Muxe-Core, so every lobby of the network gets them: see [Hotbar](../hotbar/README.md).

## User stories

- As a player, I want to arrive at a clean, safe spawn, see the network at a glance and choose what to play.
- As a VIP or staff member, I want to fly in the lobby and turn flight off when I want.
- As a builder, I want to switch to a build mode to edit the lobby, while regular players cannot break it.

## How it works

### Arrival

- After logging in, the proxy sends the player to the least loaded lobby (see [Network routing](../network-routing/README.md)).
- On join: Adventure mode, full health and hunger, experience reset, then teleported to the configured spawn (or the main world's spawn). Muxe-Core clears the inventory and the armor and gives the [hotbar](../hotbar/README.md).
- Players with `muxe.lobby.fly` arrive `arrival.fly-height` blocks (5) above the spawn, already flying. Everyone else arrives on the spawn. Falling below `protection.void-level` in one of the lobby's own worlds (those loaded when it starts) sends the player back the same way; worlds created later, such as arena setup worlds, handle their own void.
- `/fly` (`/voar`) turns flight on (`&eVocê ativou o &bmodo voar&e!`) or off (`&cVocê desativou o modo voar!`). Turning it off lands a player who is flying. Joining or falling into the void gives flight back to everyone with `muxe.lobby.fly`.
- The [parkour](../parkour/README.md) turns flight off while a player runs it and gives it back when the run ends. Nobody can take off during a run, even after `/fly`.
- Join and quit messages are hidden.
- Half a second after joining, the player hears a level-up sound and sees the title `&b&lMUXE` with the subtitle `&eBoas-vindas!`. This greeting comes from Muxe-Core and runs on every `LOBBY` server, never on auth or game servers. Every server clears whatever title is left when a player arrives, so leaving a lobby during the greeting never carries `Boas-vindas!` to the next server.
- Switching lobbies in the [lobby selector](../hotbar/README.md#lobby-selector), or using `/lobby`, shows no "connecting" message: the player is just moved. The same goes for `/jogar`, the game selector and game NPCs.

### Boss bar and scoreboard

- The boss bar shows `MUXE` in `&e&l` and switches to `&6&l` every half second (`bossbar.frames`, `bossbar.interval`).
- The scoreboard, the default tag, the boss bar frames and the command replies follow the player's [language](../language/README.md), and are redrawn as soon as the player uses `/idioma`. The English texts are in the lobby's `lang/en.yml`, at the same paths as `config.yml`. `/lobbyconfig` edits only the Portuguese `config.yml`; after an edit the scoreboard is rebuilt for every language, and the English title and lines keep coming from `lang/en.yml`.
- The scoreboard, titled `&b&lMUXE`, shows:

```text

Tag: Membro
Créditos: 0

Lobby: #1
Jogadores: 2211

muxe.com.br
```

- `{lobby}` is the number at the end of the server id (`lobby-4` shows `#4`), and `{players}` counts every player on the network's Spigot servers. Lines refresh every `scoreboard.update-interval`.
- `Tag` shows the tag the player displays (`&aVIP`, `&dUltra`, `&cNatal`...), as described in [Ranks and tags](../ranks-and-tags/README.md); it changes within a second of `/tag` or a new rank. `scoreboard.default-tag` is shown until the player's ranks are loaded. `Créditos` is the player's balance, described in [Credits and cosmetics](../credits/README.md); it is loaded on join and reloaded every 30 seconds. The values come from `PlayerInfoSource` (`RankInfoSource`).
- A plugin can replace these lines through `core.sidebars()`; the BedWars lobby shows its own scoreboard this way (see [BedWars](../bedwars/README.md)).

### Hotbar, menus and player visibility

The hotbar items (game selector, profile, cosmetics, visibility dye and lobby selector), the menus they open and the saved player visibility come from Muxe-Core and are the same on every lobby. See [Hotbar](../hotbar/README.md).

### NPCs, holograms and shop

NPCs, holograms and the shop with its cash registers come from Muxe-Core and work on every server, not only here. Whatever staff place in one lobby shows up in every lobby of the `lobby` group. See [NPCs and holograms](../npcs-and-holograms/README.md) and [Shop](../shop/README.md).

### Game NPCs

Twelve game NPCs stand in a row in front of the spawn, from x 12.5 to -9.5 at y 33.375, each turned towards the spawn and holding an item of its game. Above each one, its name in `&b&l` and `&e{players} jogando`, the players online on the live servers of its group (0 when none is alive). Clicking one sends the player to the game's lobby (or to the least loaded server of its group when the game has no lobby), like the game selector, or answers `&cNenhuma sala de &f{name} &cdisponível no momento.`

| NPC | Id | Group | In hand | Lines |
|---|---|---|---|---|
| HG | `hg` | `hg` | Mushroom soup | name, count |
| MURDER | `murder` | `murder` | Redstone | name, count |
| PVP | `pvp` | `pvp` | Iron chestplate | name, count |
| BUILD | `build` | `buildbattle` | Oak planks | name, count |
| TREINO: PONTE | `treinoponte` | `treinoponte` | Bottle o' enchanting | name, `&eClique aqui!` |
| DUELS | `duels` | `duels` | Diamond sword | `&d&lNOVIDADE!`, name, count |
| BED WARS | `bedwars` | `bedwars` | Bed | name, count |
| SKY WARS | `skywars` | `skywars` | Eye of ender | name, count |
| THE BRIDGE | `thebridge` | `thebridge` | White stained clay | name, count |
| PARTY | `party` | `party` | Cake | name, count |
| HIDE AND SEEK | `hideandseek` | `hideandseek` | Melon block | name, count |
| CTF | `ctf` | `ctf` | Red wool | name, count |

- They are ordinary game NPCs of the `lobby` group, so `lobby-1`, `lobby-2` and every later lobby show them.
- They live in `servers/lobby/placements/import.yml`, in the [import format](../npcs-and-holograms/README.md#importing-old-placements). The group of each NPC is its `group` key; change it there before importing, or later with `/npc acao <id> <group>`.
- Skins: every NPC wears the skin of its reference NPC, stored in `import.yml` as the signed Mojang texture (`skin.value` and `skin.signature`), which [Legal](../../legal/README.md) allows. To change one, run `/npc skin <id> <name>`, or edit the `skin` of the entry and import again.

#### Placing them

1. Copy `servers/lobby/placements/import.yml` to `plugins/Muxe-Core/import.yml` on one lobby (any one: the NPCs belong to the group).
2. Run `/npc importar` on that lobby, or restart it. It answers `&a12 itens de &fimport.yml &aimportados no grupo &flobby&a.`
3. Check with `/npc listar`. Importing again replaces the twelve NPCs with what is in the file.

### Protection

Players cannot take damage, lose hunger, break, place, drop or pick up items, or move items in their inventory, so the hotbar stays in place. Rain is cancelled. Fire never burns blocks, never spreads and never starts on its own (lava, lightning, spread); only staff in build mode can light a fire by hand, and that fire does not burn anything either (`FireRules`). The map is static: block physics is off, so blocks out of place (sugar cane on dirt, a flower without soil, a torch without a wall) never break or drop; leaves never decay, ice and snow never melt, and water and lava never flow. On startup the lobby sets `randomTickSpeed` to `0` and `doFireTick` to `false` on every world (`StaticWorld`): in 1.8, sugar cane, flowers, crops and saplings check their soil on random ticks and break without any event, and saplings would grow into trees. A fire lit by staff can still go out. No mob ever spawns in the lobby (natural spawns, spawners, eggs and breeding are cancelled); only entities spawned by a plugin (`SpawnReason.CUSTOM`) are allowed, and the NPCs and holograms are packets, not entities.

Players are always in Adventure mode: any change to another game mode is cancelled, except Creative for a player in build mode.

### Launch pads

Every slime block (`launch-pad.block`) in a lobby is a launch pad, with no setup: the lobby finds them by scanning each loaded chunk off the main thread (`PadScanner`, `ChunkScan`) and follows blocks placed or broken in build mode.

- Green sparkles (`HAPPY_VILLAGER`) rise from the top of each pad every `launch-pad.particles.interval`, only while a player is within `launch-pad.particles.view-radius` blocks.
- Stepping on a pad throws the player towards where they look: 20 blocks forward and 5 blocks up by default. `LaunchVelocity` turns `distance` and `height` into the velocity by replaying the 1.8 client physics (gravity, drag and air friction); each component is capped at 3.9, the most a velocity packet carries.
- The launch plays a firework taking off (`launch-pad.sound`). A player is launched once per `launch-pad.cooldown`. Flying players and players in build mode are not launched. Lobby players take no damage, so the landing does not hurt.

### Build mode

`/build` comes from Muxe-Core and works on every server, as described in the [Core API](../../development/core-api/README.md#build-mode). In the lobby:

- `/build on` clears the hotbar, switches to Creative and lets the player break, place and move items, with `&aModo construção ativado` in the action bar for as long as the mode is on.
- `/build off` returns to Adventure. Muxe-Core gives the hotbar back (with the current visibility dye) and the lobby lets players with `muxe.lobby.fly` fly again (`FlightBuildHook`).
- Build mode follows the player to the other servers: moving to another lobby keeps it on. Hotbar actions do nothing while it is on.

### AFK

A player who is not VIP and stands still for 5 minutes is moved to the AFK server and comes back here as soon as they move. See [AFK server](../afk/README.md).

## Commands

| Command | Description | Permission |
|---|---|---|
| `/jogar <mode>` (`/play`) | Sends the player to the game's lobby (`<mode>-lobby`) when one is alive, otherwise to the game group. | None |
| `/setspawn` (`/setlobby`) | Saves the current position as the spawn of this lobby. | `muxe.lobby.setspawn` |
| `/fly` (`/voar`) | Turns flight on or off. | `muxe.lobby.fly` |
| `/lobbyconfig ver\|definir\|adicionar\|remover\|local\|recarregar ...` | Views and changes the lobby `config.yml` in game; see [Changing the configuration in game](#changing-the-configuration-in-game). | `muxe.admin.config` |
| `/build <on\|off>` | Turns build mode on or off on any server; without an argument it toggles. Provided by Muxe-Core. | `muxe.afk.bypass` | Never sent to the [AFK server](../afk/README.md) for standing still. Seeded on `vip`. | op |
| `muxe.build` |

`/jogar` completes the configured games with tab.

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.lobby.fly` | Use `/fly`, arrive above the spawn, flying, and fly again after build mode. Seeded on `vip`, so every rank above VIP has it too. | op |
| `muxe.build` | Use `/build` (Muxe-Core) to break, place, drop and pick up items in Creative. | op |
| `muxe.lobby.setspawn` | Use `/setspawn`. | op |
| `muxe.admin.config` | Use `/lobbyconfig` (and `/coreconfig`, `/bwconfig`). Declared by Muxe-Core. | op |

## Changing the configuration in game

`/lobbyconfig` (permission `muxe.admin.config`) views and changes any key of the lobby `config.yml` without editing the file, with the subcommands of the [configuration editor](../../development/core-api/README.md#configuration-editor): `ver`, `definir`, `adicionar`, `remover`, `local` and `recarregar`.

- Live, with no restart: `spawn` (the `/setspawn` position, which `local spawn --novo` also writes) and `scoreboard.title` and `scoreboard.lines`, which change the scoreboard the players see on its next update.
- Everything else (`play.games`, `protection.*`, `arrival.*`, `launch-pad.*`, `bossbar.*`, `scoreboard.default-*` and `scoreboard.update-interval`, `messages.*`) is saved and the answer says it applies after a restart.

```text
/lobbyconfig ver scoreboard
/lobbyconfig definir scoreboard.title &b&lMUXE
/lobbyconfig adicionar play.games pvp
/lobbyconfig local spawn --novo
```

The join greeting is Muxe-Core configuration: `/coreconfig definir join-greeting.delay 1s` applies it live.

## Configuration

`plugins/Muxe-Lobby/config.yml`:

| Key | Default | Description |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Groups allowed in `/jogar`. |
| `protection.void-level` | `0` | Y below which players in the lobby's own worlds are sent back to spawn. |
| `arrival.fly-height` | `5` | Blocks above the spawn for players with `muxe.lobby.fly`. |
| `afk.enabled`, `afk.idle-after`, `afk.server-group`, `afk.check-interval`, `afk.origin-ttl` | `true`, `5m`, `afk`, `1s`, `12h` | The AFK watch; see [AFK server](../afk/README.md#configuration). |
| `launch-pad.enabled` | `true` | Turns the launch pads on. |
| `launch-pad.block` | `SLIME_BLOCK` | Block (a 1.8 `Material` name) that works as a launch pad. |
| `launch-pad.distance`, `launch-pad.height` | `20`, `5` | Blocks forward and up a launch reaches. |
| `launch-pad.cooldown` | `1s` | Time before the same player can be launched again. |
| `launch-pad.sound.sound`, `volume`, `pitch` | `FIREWORK_LAUNCH`, `1.0`, `1.0` | Sound played to the player when launched (a firework taking off). |
| `launch-pad.particles.interval`, `per-block`, `view-radius` | `250ms`, `1`, `32` | How often each pad sparkles, how many sparkles each time, and how close a player must be. |
| `bossbar.frames`, `bossbar.interval` | `&e&lMUXE`, `&6&lMUXE`, `500ms` | Boss bar texts and how long each one stays. |
| `scoreboard.title`, `scoreboard.lines` | See above | Scoreboard text. Placeholders: `{tag}`, `{credits}`, `{lobby}`, `{players}`. |
| `scoreboard.update-interval` | `1s` | How often the scoreboard refreshes. |
| `scoreboard.default-tag`, `scoreboard.default-credits` | `&7Membro`, `0` | Tag shown while the player's ranks are not loaded. `default-credits` is no longer used: the scoreboard shows the real balance. |
| `spawn.*` | not set | Written by `/setspawn`. |
| `messages.*` | Portuguese texts | Messages of `/jogar`, `/setspawn` and `/fly` (`fly-enabled`, `fly-disabled`); shared messages come from Muxe-Core. |

`spam`, `hotbar`, `lobby-selector` and the visibility and lobby selector messages are no longer read from this file: they live in `plugins/Muxe-Core/config.yml`, described in [Hotbar](../hotbar/README.md#configuration).

`plugins/Muxe-Core/config.yml`:

| Key | Default | Description |
|---|---|---|
| `join-greeting.server-kinds` | `LOBBY` | Server kinds that greet players on join. |
| `join-greeting.delay` | `500ms` | Wait after joining before the greeting. |
| `join-greeting.sound`, `volume`, `pitch` | `LEVEL_UP`, `1.0`, `1.0` | Sound played to the player (a 1.8 `Sound` name). Empty for no sound. |
| `join-greeting.fade-in`, `stay`, `fade-out` | `10`, `50`, `20` | Title timings in ticks. |
| `messages.join-title`, `messages.join-subtitle` | `&b&lMUXE`, `&eBoas-vindas!` | Title and subtitle of the greeting. |

The keys of the hotbar and its menus, NPCs, holograms and the shop are in `plugins/Muxe-Core/config.yml` too, described in [Hotbar](../hotbar/README.md#configuration), [NPCs and holograms](../npcs-and-holograms/README.md#configuration) and [Shop](../shop/README.md#configuration).

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.connect` | proxy | standard | Sent by `/jogar`. |

## Data

The `lobby` schema has no tables. The lobby migration `V2__drop_player_settings.sql` drops `lobby.player_settings`: the player visibility moved to `core.player_visibility` (see [Hotbar](../hotbar/README.md#data)). Muxe-Core starts before the lobby and copies the rows first.

## Metrics

The Spigot server metrics of Muxe-Core. The visibility metrics moved with it and are listed in [Hotbar](../hotbar/README.md#metrics).

## Integrations

- [Network routing](../network-routing/README.md) brings players here after login and moves them to other lobbies and games.
- The [Core API](../../development/core-api/README.md) provides the boss bar and the scoreboard.
- The [Hotbar](../hotbar/README.md), with its menus and the player visibility, comes from Muxe-Core.
- The [AFK server](../afk/README.md) receives the players who stand still here.
- [NPCs and holograms](../npcs-and-holograms/README.md) and the [Shop](../shop/README.md) come from Muxe-Core and are placed in the lobby by command.
- The [Parkour](../parkour/README.md) comes from Muxe-Core; staff mark its points in the lobby by command.

## Acceptance criteria

- [x] Players with the fly permission arrive 5 blocks above the spawn, flying; others arrive on it.
- [x] `/fly` turns flight on and off with the reference messages, lands a flying player, and needs `muxe.lobby.fly`.
- [x] The join greeting is enabled only on `LOBBY` servers, with the reference title and subtitle.
- [x] The game NPC file has the twelve NPCs at the exported spots and turns, with their group, item in hand and lines.
- [x] The boss bar alternates `&e&lMUXE` and `&6&lMUXE` every 10 ticks, and the scoreboard fills the reference lines.
- [x] The lobby configuration command changes the scoreboard template and the spawn live and leaves the other keys for a restart (`LobbyConfigCommandTest`, `LobbyReloadsTest`).
- [ ] Boss bar, scoreboard and flight after build mode work in game (checked in game).
- [x] The default launch reaches 20 blocks forward and 5 blocks up in the 1.8 client physics, towards where the player looks.
- [ ] Slime blocks sparkle green and launch the player in game (checked in game).
- [ ] The twelve game NPCs show on both lobbies with their lines at the reference heights, the count follows the game servers and a click sends the player (checked in game).

## Failures and recovery

- Proxy or Redis unavailable: `/jogar` shows a failure message and the player stays in the lobby.
- Server internals missing (another server version): the boss bar is skipped with one warning.
- Configured spawn world missing: the main world's spawn is used.

## Known limitations

- No game server group exists yet besides `skywars` and `bedwars`, so the other game NPCs show `0 jogando` and answer that no room is available.
- The limits of the hotbar and its menus are listed in [Hotbar](../hotbar/README.md#known-limitations).
