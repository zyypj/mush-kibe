**English** | [Português](README.pt-BR.md)

# Game engine

The part of the platform that runs matches inside each game server. Each server runs many matches of the same game at once, so the engine's main job is keeping them isolated and their lifecycles predictable.

Code: `minigames/engine/src/main/java/br/com/muxe/minigame/engine`.

## Concepts

| Concept | Class | What it is |
|---|---|---|
| Game mode | `GameMode` | The rules of one variant of a game, such as BedWars solo: id, display name, minimum and maximum players, and players per team (`team-size`, `1` by default). Loaded from the game's `config.yml` by `GameModeLoader`. |
| Start countdown | `StartCountdown` | The countdown before a match, the same in every game and mode. See below. |
| Arena instance | `ArenaInstance` | A playable copy of a map template, used by one match at a time. |
| Arena pool | `ArenaPool` | The instances of a server and which ones are available. |
| Match | `Match` | One game being played: its mode, arena, participants and state. |
| Match service | `MatchService` | Creates matches, adds and removes players, and moves matches through their lifecycle. |
| Task scope | `MatchTaskScope` | Bukkit tasks that belong to one match and are all cancelled when it closes. |
| Engine | `MinigameEngine` | Holds the modes, matches and arenas of one game and registers their metrics. |

## Match lifecycle

```text
WAITING → STARTING → RUNNING → ENDING → CLOSED
   ↑          │
   └──────────┘   (players left during the countdown)

WAITING, STARTING, RUNNING, ENDING → CANCELLED → CLOSED
```

| State | What happens |
|---|---|
| `WAITING` | Accepts players. Reaching the mode's minimum moves to `STARTING`. |
| `STARTING` | Countdown. Still accepts players until full. Dropping below the minimum goes back to `WAITING`. |
| `RUNNING` | The game is being played. No new players. |
| `ENDING` | The winner is decided, results are saved, players see the outcome. |
| `CANCELLED` | Stopped by a failure or a shutdown. |
| `CLOSED` | Players are released, tasks cancelled, the arena released. The match is forgotten. |

- States only change through the transitions above; anything else throws `IllegalTransitionException`.
- **One match per player.** `MatchService.join` refuses a player who is already in a match (`match.already_in_match`). Other refusals: `match.not_found`, `match.not_joinable`, `match.full`.
- `MatchService` is used from the main thread only.

## Arena lifecycle

```text
AVAILABLE → RESERVED → IN_USE → RESETTING → AVAILABLE
                                    │
                                    └─→ DISABLED → (enable) → RESETTING
```

- A match reserves an available instance when it is created and releases it when it closes.
- After a match the instance is reset from its template before being used again.
- A failed reset is retried with an increasing delay (`nextRetryDelay`: the base delay doubled after each consecutive failure, capped at 5 minutes).
- The instance is **disabled** when the failure is permanent (a configuration error or a corrupted template) or after `maxFailures` consecutive failures (5 by default). It stays out of use until staff fix it and enable it again.
- A successful reset clears the failure count.

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

## Arena setup

Each game describes what an arena needs in a `SetupSchema`, and the engine gives every game the same `/arena` command, holograms and checklist.

| Class | What it is |
|---|---|
| `SetupSchema` | The game's modes, the team rule of each mode (`TeamRule`: fixed teams, or a number of colours to choose) and the steps of each mode. Its default methods read the mode and display name from the arena name: `1v1_slip` is mode `1v1`, map `Slip`. |
| `SetupStep` | One thing to mark: id, label, kind, whether it repeats per team, minimum and maximum, and hologram lines (`{team}`, `{index}`). |
| `StepKind` | `LOCATION` (where the player stands, centred, with the direction snapped to 45°), `CENTER` (the block under the player), `BLOCK` (the block the player looks at), `HEIGHT` (the player's Y) and `REGION` (two corners). |
| `ArenaDraft` | The arena being set up: mode, display name, teams, points and heights. |
| `SetupChecklist` | What is done and what is missing. An arena is ready when its teams are complete and every required step has its minimum. |
| `ArenaFiles` | Reads and writes `arenas/<name>.yml` in expanded YAML, with `ready: true` once nothing is missing. |
| `SetupService` | Opens setup worlds, applies the commands, shows the holograms and the action bar, and saves. |
| `TeamColor` | The nine team colours: Azul `&9`, Amarelo `&e`, Ciano `&b`, Cinza `&8`, Vermelho `&c`, Rosa `&d`, Verde `&a`, Branco `&f`, Laranja `&6`, each with its wool and leather colour. |

`/arena criar <name> [mode]` opens an arena:

1. If `maps/<name>.mxw` exists, the map is loaded from it.
2. Otherwise, if the server folder has a world folder named `<name>`, it is converted to `.mxw` right away and loaded.
3. Otherwise an empty world is created, and the player arrives at `0 60 0` to build or paste the map.

The setup world is a `muxe_setup_<name>` world, edited in creative and flying. Several staff members may edit it together.

| Command | What it does |
|---|---|
| `/arena` or `/arena ajuda` | The checklist, with one clickable line per step and team: `✔` done, `✘` missing, `○` optional. `[Definir]`, `[+]` and `[-]` run the command, and the hover says how to stand. |
| `/arena definir <step> [team]` | Marks the step at the player's position. A single point is replaced, a list grows up to its maximum, a region takes two corners and starts over on the third. |
| `/arena remover <step> [team]` | Removes the point nearest to the player, or the whole step when it has a single point. |
| `/arena time <adicionar\|remover> <colour>` | Chooses the teams in modes whose teams are not fixed. Removing a team removes its points. |
| `/arena nome <name>` | Changes the display name of the map. |
| `/arena salvar` | Saves every chunk of the setup world to `maps/<name>.mxw` and the points to `arenas/<name>.yml`. |
| `/arena sair` | Leaves the setup and goes back to where the player was. The world closes when its last editor leaves; map changes not saved are lost. |
| `/arena listar` | Lists the arenas, ready or incomplete, with a button to edit each one. |

- Every point shows a hologram to the editors, such as `SPAWN` with the team name, or `FERRO #2`.
- The action bar shows the next missing item and how many more are left, or that everything is ready.
- Points are saved to the arena file after every change.
- Permission: `muxe.minigame.setup`.
- Messages come from `minigame-setup.yml` inside the engine, under `messages`. A game overrides any of them in its own `config.yml`.

## Rooms

Pieces a game uses to run its rooms:

| Class | What it does |
|---|---|
| `RoomCodes` | Room codes like the reference: 6 characters, digits and lowercase letters, starting with a digit (`2v7bss`), never repeated on the server. |
| `TeamAssigner` | Splits the players of a room into the teams when the match starts. Parties (`PartyGroups`) stay together, the largest first; everyone else goes to the team with the fewest players, in join order. A party larger than a team spills into the next. |
| `PartyGroups` | The hook for the party system. `PartyGroups.NONE` until parties exist. |
| `CountdownTitles` | The titles of the start countdown, by second, read from `countdown.titles`. |
| `MatchWorld` | The match world of a game: one `muxe_<game>` world with its cells, plus a task that unloads chunks nobody is watching every 5 seconds (64 at most), keeping what matches changed. |
| `ArenaCatalog` | The arenas a server can play: every `arenas/<name>.yml` marked ready, with its `maps/<name>.mxw`, that fits a cell. `reload(name)` picks up an arena saved in the setup. |

## Worlds and maps

Matches never use world folders. See [ADR 0009](../../decisions/0009-own-map-format-and-match-cells.md).

| Class | Package | What it does |
|---|---|---|
| `MapTemplate` | `world.map` | A map in memory: its chunks, each one compressed. `chunk(x, z)` decompresses one. |
| `MapFiles` | `world.map` | Reads and writes `.mxw` files. |
| `AnvilImporter` | `world.anvil` | Turns a world folder (`region/*.mca`) into a `MapTemplate`, with no server. Entities are left out. |
| `EditableMap` | `world.map` | A map being edited in the setup. It takes every saved chunk, and `snapshot()` gives the map to write. |
| `MatchCells` | `world.cell` | Splits the match world into cells and serves each match its map around the cell centre. Chunks a match changes are kept until `release`. |
| `Cell` | `world.cell` | A match's cell. Converts map coordinates to world coordinates and back. |
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
- Every scheduled task goes through the match's `MatchTaskScope`, so nothing keeps running after a match closes.
- An error in one match's logic cancels that match only.

All matches of a server share the same process and main thread, so fatal problems (out of memory, infinite loop, blocking call) affect every match on it. That is why each game runs in its own servers. See [Network](../network/README.md).

## Results

The end of a match will be saved in **one PostgreSQL transaction** that records the result under the match's unique id, grants critical rewards and marks the match as finished. Saving the same match id twice has no effect, and rewards are shown only after commit. This is part of the gameplay work that is not implemented yet.

## Metrics

`muxe_matches_active{game}`, `muxe_arenas_available{game}` and `muxe_arenas_disabled{game}`. See [Observability](../observability/README.md#minigames).

## Status

Implemented and tested: modes, match and arena lifecycles, the one-match-per-player rule, the arena pool, task scopes and metrics. Also implemented: the `.mxw` map format, importing world folders, the in-memory chunk loader and match cells (unit tests, plus a run on PandaSpigot 226). Not implemented yet: event routing per match, gameplay, results and rewards.
