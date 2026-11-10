**English** | [Português](README.pt-BR.md)

# Game engine

The part of the platform that runs matches inside each game server. Each server runs many matches of the same game at once, so the engine's main job is keeping them isolated and their lifecycles predictable.

Code: `minigames/engine/src/main/java/br/com/muxe/minigame/engine`.

## Concepts

| Concept | Class | What it is |
|---|---|---|
| Game mode | `GameMode` | The rules of one variant of a game, such as BedWars solo: id, display name, minimum and maximum players, and players per team (`team-size`, `1` by default). Loaded from the game's `config.yml` by `GameModeLoader`. |
| Start countdown | `StartCountdown` | The countdown before a match, the same in every game and mode. See below. |
| Engine | `MinigameEngine` | Holds the modes of one game and registers its metrics, which read what the game reports through `track(activeMatches, readyArenas)`. |

## Match lifecycle

The engine gives the low-level pieces every game needs (modes, the start countdown, arena setup and storage, match cells, team assignment, room codes). The [minigame API](../minigame-api/README.md) runs the matches with them (`WAITING → STARTING → PLAYING → ENDING`): joining and leaving, teams, deaths, respawns, spectators, the scoreboard, titles, matchmaking and the server wiring. Games are built on the minigame API, not on the engine directly.

## Start countdown

`StartCountdown` follows the same rules in every game and mode:

| Players | What happens |
|---|---|
| Below the minimum | The room waits. |
| The minimum is reached | A 30-second countdown starts. |
| The room fills up | The countdown drops to 10 seconds. It is never raised back. |
| A player leaves and the room drops below the minimum | The countdown is cancelled and the room waits again. |

- Modes whose minimum equals the maximum (1v1, 2v2, 3v3, 4v4, 4s) are full when they reach the minimum, so they count 10 seconds.
- `players(count)` reports `STARTED`, `SHORTENED`, `CANCELLED` or `NONE`.
- `tick()`, called once a second, reports the remaining seconds and when the countdown finishes.

## Health below the name

`HealthDisplay` shows the health of the players alive under their names, as `20 ❤`, and as a yellow number to the right of the name in the tab list, in every minigame. The game calls `show(scoreboard, alive)` for each viewer's scoreboard a few times a second (BedWars every 5 ticks), with the names of the players alive and their health rounded up (`points(health)`). A player missing from the list loses both at once. The label (the heart) comes from the game's messages.

## Level up

`LevelUps` congratulates a player who reaches a new level, in every minigame. The game creates it with its message (with `{level}`) and the badge of each level, and calls `announce(player, level)` whenever it shows the level. The first level seen for a player is only remembered; a higher one sends the message with the badge of the new level, once even when several levels are skipped. `forget(player)` drops the player when they leave.

## Languages

- `TeamColor` has a Portuguese and an English name. `label()` is a `Text` and `displayName(Language)` renders it. The team letters do not change.
- `GameModeLoader.modes(texts)` reads the modes from the game's `LocalizedConfig`. `ModeNames.from(texts)` gives each mode's display name per language, used by the connector and the play-again item.
- `LevelUps(messages.text("level-up"), badges, languages)` congratulates each player in their own language.
- The arena setup (`minigame-setup.yml`, `/arena`) is staff-only and stays in Portuguese.

## Arena setup

Each game describes what an arena needs in a `SetupSchema`, and the engine gives every game the same `/arena` command, holograms and checklist.

| Class | What it is |
|---|---|
| `SetupSchema` | The game's modes, the team rule of each mode (`TeamRule`: fixed teams, or a number of colours to choose) and the steps of each mode. Its default methods read the mode and display name from the arena name: `1v1_slip` is mode `1v1`, map `Slip`. `teamAnchor(mode)` names the team step that places the others (BedWars: the spawn), and `linkedBlocks(mode)` the block steps found on their own around it (`LinkedBlock`: step, materials, radius; BedWars: the bed within 20 blocks). |
| `SetupStep` | One thing to mark: id, label, kind, whether it repeats per team, minimum and maximum, and hologram lines (`{team}`, `{index}`). |
| `StepKind` | `LOCATION` (where the player stands, centred, with the direction snapped to 45°), `CENTER` (the block under the player), `BLOCK` (the block the player looks at), `HEIGHT` (the player's Y) and `REGION` (Pos 1 and Pos 2, two opposite corners stored as `<step>.pos1` and `<step>.pos2`). |
| `ArenaDraft` | The arena being set up: mode, display name, teams, points and heights. |
| `SetupChecklist` | What is done and what is missing. An arena is ready when its teams are complete and every required step has its minimum. |
| `ArenaFiles` | Renders and parses the arena definition in expanded YAML, with `ready: true` once nothing is missing, and reads and writes `arenas/<name>.yml`. |
| `ArenaStore` | Where arenas and maps live. `FileArenaStore` keeps `arenas/<name>.yml` and `maps/<name>.mxw` in the plugin folder; `DatabaseArenaStore(game, commands, queries)` keeps both in PostgreSQL (`core.arenas`, through `SaveArenaCommand`, `FindArenaQuery` and `ListArenasQuery`), so every server of the game reads the same arenas. `saveDefinition` saves the points and keeps the stored map. |
| `ArenaSavedPacket` | `minigame.arena_saved` (game, name): sent after `/arena salvar` so the game servers reload that arena. |
| `TeamPicker` | Picks the team whose anchor point is nearest to a position, and the nearest free block that no other team owns (both halves of a bed count as owned). |
| `SetupService` | Opens setup worlds, applies the commands, shows the holograms and the action bar, and saves. |
| `TeamColor` | The nine team colours: Azul `&9`, Amarelo `&e`, Ciano `&b`, Cinza `&8`, Vermelho `&c`, Rosa `&d`, Verde `&a`, Branco `&f`, Laranja `&6`, each with its wool and leather colour. |

`/arena criar <name> [mode]` opens an arena:

1. If the store has a map with blocks for `<name>`, it is loaded from it.
2. Otherwise (no map, or one saved empty), if the server folder has a world folder named `<name>`, it is converted to the Muxe map format right away, stored and loaded.
3. Otherwise an empty world is created, and the player arrives at `0 60 0` to build or paste the map.

The setup world is a `muxe_setup_<name>` world, edited in creative and flying. Several staff members may edit it together. `SetupService.onEnter` and `onLeave` run when a player starts and stops editing; a game can use them, for example, to put staff in build mode on a lobby. Falling below Y 0 brings the editor back to the start of the setup world instead of leaving it, and chat stays inside each setup: editors only hear the editors of the same arena, and players outside a setup do not hear them.

| Command | What it does |
|---|---|
| `/arena` or `/arena ajuda` | The checklist, with one clickable line per step and team: `✔` done, `✘` missing, `○` optional. `[Definir]`, `[+]` and `[-]` run the command, and the hover says how to stand. When the schema has a team anchor, every other team step is one line with how many teams are done, and its buttons go to the nearest team. The checklist is sent again after every change. |
| `/arena definir <step> [team]` | Marks the step at the player's position. A single point is replaced and a list grows up to its maximum. Without the team, a team step goes to the team whose anchor is nearest. Marking a team's anchor also marks its linked blocks, such as the nearest bed. |
| `/arena definir <region> <1\|2>` | Marks Pos 1 or Pos 2 of a region; the help shows `[Pos 1]` and `[Pos 2]` buttons. Without the number, the first missing corner is marked. |
| `/arena remover <step> [team]` | Removes the point nearest to the player, or the whole step when it has a single point. Without the team, it uses the nearest team, like `definir`. |
| `/arena time <adicionar\|remover> <colour>` | Chooses the teams in modes whose teams are not fixed. Removing a team removes its points. |
| `/arena nome <name>` | Changes the display name of the map. |
| `/arena salvar` | Saves every chunk of the setup world and the points to the arena store. Points are also saved as they are marked, keeping the stored map. |
| `/arena sair` | Leaves the setup and goes back to where the player was. The world closes when its last editor leaves; map changes not saved are lost. |
| `/arena listar` | Lists the arenas, ready or incomplete, with a button to edit each one. |
| `/arena clonar <source> <name> [mode]` (`duplicar`) | Copies a saved arena, map and points included, under a new map name (`/arena clonar solo_majestic duplas_majestic`). Only the mode changes, and with it the number of players; the display name stays. The mode is the one given, else the prefix of the new name, else the source's. A mode with fixed teams keeps only its teams and their points; a mode with chosen teams keeps the first ones up to its count. It saves the clone ready or with what is missing (`/arena criar <name>` completes it) and tells the game servers with `minigame.arena_saved`. It copies the saved version, not unsaved changes of an arena open in the setup, and refuses a name already in use. |
| `/arena apagar <name> [confirmar]` | Deletes an arena and its map from the store (`DeleteArenaCommand`). Without `confirmar` it only asks, with a button; an arena open in the setup cannot be deleted. The game servers are told with `minigame.arena_saved` and drop it. |

- Every point shows a hologram to the editors, such as `SPAWN` with the team name, or `FERRO #2`.
- The action bar shows the next missing item and how many more are left, or that everything is ready.
- Points are saved to the arena file after every change.
- Permission: `muxe.minigame.setup`.
- Messages come from `minigame-setup.yml` inside the engine, under `messages`. A game overrides any of them in its own `config.yml`.

## Rooms

Pieces the [minigame API](../minigame-api/README.md) uses to run the rooms:

| Class | What it does |
|---|---|
| `RoomCodes` | Room codes like the reference: 6 characters, digits and lowercase letters, starting with a digit (`2v7bss`), never repeated on the server. |
| `TeamAssigner` | Splits the players of a room into the teams when the match starts. Parties (`PartyGroups`) stay together, the largest first; then the players with a preferred `TeamColor` (the favourite colour of the game, optional) go to that team when the arena has it and it has seats, grouped by colour: when more want it than it has seats, a draw with the injected `RandomGenerator` picks who gets them and the others fall through; everyone else goes to the team with the fewest players, in join order. A party takes the colour most of its members prefer (the first on a tie) if that team is still empty, and is never split. A party larger than a team spills into the next. Without preferences the result is the join-order one. |
| `PartyGroups` | The hook for the party system. `PartyGroups.NONE` until parties exist. |
| `CountdownTitles` | The titles of the start countdown, by second, read from `countdown.titles`. |
| `MatchWorld` | The match world of a game: one `muxe_<game>` world with its cells, plus a task that unloads chunks nobody is watching every 5 seconds (64 at most), keeping what matches changed. |
| `ArenaCatalog` | The arenas a server can play: every arena of its `ArenaStore` marked ready, with a map that fits a cell. `reload(name)` picks up an arena saved in the setup. |

## Worlds and maps

Matches never use world folders. See [ADR 0009](../../decisions/0009-own-map-format-and-match-cells.md).

| Class | Package | What it does |
|---|---|---|
| `MapTemplate` | `world.map` | A map in memory: its chunks, each one compressed. `chunk(x, z)` decompresses one. |
| `MapFiles` | `world.map` | Reads and writes `.mxw` files. |
| `AnvilImporter` | `world.anvil` | Turns a world folder (`region/*.mca`) into a `MapTemplate`, with no server. Entities are left out. |
| `EditableMap` | `world.map` | A map being edited in the setup. It takes every saved chunk, and `snapshot()` gives the map to write. |
| `MatchCells` | `world.cell` | Splits the match world into cells and serves each match its map around the cell centre. Chunks a match changes are kept until `release`: the server hands over a copy, which is served as is right away and compressed (fast level) on a background thread, so unloading never compresses on the main thread. |
| `Cell` | `world.cell` | A match's cell. Converts map coordinates to world coordinates and back. |
| `BlockBox`, `RegionEraser` | `world.cell`, `world.server` | Empties a box of blocks in one pass: zeroes the block arrays of the chunk sections it covers, removes the tile entities inside, relights each chunk once and resends each changed chunk as one packet to the players watching it. Used to remove waiting areas. |
| `MuxeWorlds` | `world.server` | Creates a void world named `muxe_<name>` whose chunks come from a `ChunkSource`. It also clears a cell, destroys a world and deletes leftover `muxe_` folders. |

- A world created by `MuxeWorlds` never writes region files, never keeps spawn chunks loaded and has autosave off.
- Its defaults: time fixed at noon, no weather, no mob spawning, no random ticks, no fire spread, no mob griefing.
- A match takes a cell with `MatchCells.allocate(map)`.
- When the match ends, `MuxeWorlds.clear(world, cell)` unloads the cell's chunks without saving, and `MatchCells.release(cell)` frees it.
- `MatchCells` refuses a map that does not fit the cell with its margin.
- Chunks are built with the server classes, so `minigames:engine` uses `muxe.server-internals`.

## Isolation

- Every event involving a player, entity, block or world must be routed to the match that owns it; interactions between matches are cancelled.
- Chat, tab list and visibility are scoped to the match.
- Matches run from the shared tasks of the minigame API's `MatchService` (every second and every 5 ticks), so nothing outlives a match.
- Async events such as chat read immutable snapshots built on the main thread, never the live match state.
- An error in one match's logic cancels that match only.

All matches of a server share the same process and main thread, so fatal problems (out of memory, infinite loop, blocking call) affect every match on it. That is why each game runs in its own servers. See [Network](../network/README.md).

## Results

A game saves the end of a match in **one PostgreSQL transaction** that first claims the match's unique id: saving the same id twice has no effect. BedWars gives each match a random id when it starts and keeps the claimed ids in `bedwars.recorded_matches`.

## Metrics

`muxe_matches_active{game}` and `muxe_arenas_available{game}`, read from what the game reports with `MinigameEngine.track`. See [Observability](../observability/README.md#minigames).

## Status

Implemented and tested: modes, match and arena lifecycles, the one-match-per-player rule, the arena pool, task scopes and metrics. Also implemented: the `.mxw` map format, importing world folders, the in-memory chunk loader and match cells (unit tests, plus a run on PandaSpigot 226). The match lifecycle, event routing per match and the shared gameplay rules live in the [minigame API](../minigame-api/README.md); results and rewards are still each game's.
