**English** | [Português](README.pt-BR.md)

# Parkour

A timed parkour in the lobby: a start, numbered checkpoints and an end, marked on pressure plates of the map. Players get items to restart, go back to the last checkpoint or stop, and keep a personal record per map. Staff mark the points by command on any server. It lives in Muxe-Core (`core:common` packages `br.com.muxe.core.common.parkour` and `br.com.muxe.core.common.placement`, `core:bukkit` package `parkour`), so every lobby of every mode gets it, and its points are shared by the server group like [NPCs and holograms](../npcs-and-holograms/README.md).

## User stories

- As a player, I want to run the lobby parkour against the clock, with checkpoints, so that I can beat my own record.
- As a player, I want to see my personal record above the start, and know when I beat it.
- As a player, I want to restart, go back to my last checkpoint or stop with one click, without walking back.
- As a staff member, I want to mark the start, the checkpoints and the end by command, on every server of a group at once.
- As an operator, I want to change the items, holograms, firework and messages in the config, without touching code.

## How it works

### The course

- A course has one start, any number of checkpoints numbered from 1, and one end. Each point is a pressure plate built in the map; the core only stores where it is and watches players entering that block (a `PlayerMoveEvent` that changes block). The core never places or removes the plate itself.
- The course only works when it has both a start and an end. Checkpoints are optional; when there are several, they are reached in the order of their numbers (gaps in the numbering are allowed).
- Above every point there is a packet-only hologram, like the [display stands](../../development/core-api/README.md#display-stands), centred on the plate:

| Point | Lines (top to bottom) |
|---|---|
| Start | `&b&lPARKOUR`, `&aInício`, `&eRecorde pessoal: &7{record}` |
| Checkpoint | `&b&lPARKOUR`, `&eCheckpoint &a#{number}` |
| End | `&b&lPARKOUR`, `&cFim` |

- `{record}` is different for each player who looks at it: their best time on this server group, or `parkour.no-record` (`...`) when they have none. It is loaded on join and updated after each finish.

### A run

- Stepping on the start begins the run: `&b&lPARKOUR &aIniciado!`. The player gets the parkour items and loses flight (see below).
- Stepping on the start again during a run restarts the timer where the player stands: `&b&lPARKOUR &aTemporizador reiniciado!`.
- Stepping on the next checkpoint shows the time since the start: `&b&lPARKOUR &aVocê completou o &echeckpoint #2 &aem &71m 22,704s&a!`. Stepping on a later checkpoint before the previous one, or on one already reached, does nothing at all, with no message.
- The end only counts after every checkpoint. Stepping on it earlier does nothing.
- Finishing:
  - shows `&b&lPARKOUR &aVocê finalizou o &eparkour &aem &7{time}&a!`;
  - explodes a firework right at the end plate (green, cyan and red, large ball, by default);
  - saves the time. When it beats a previous record, the player also sees `&b&lPARKOUR &eVocê atingiu um novo &brecorde! &a{previous} &7-> &a{time}`. The first finish of a player becomes their record without that message;
  - ends the run, gives back the items and the flight the player had.
- Quitting the server ends the run. Runs are kept in memory only: switching lobbies starts over.

### Items

While running, the player has three items in the hotbar. The items that were in those slots (the [hotbar](../hotbar/README.md) cosmetics chest in slot 5) are saved and come back when the run ends.

| Slot | Item | Name | Action |
|---|---|---|---|
| 4 | Music disc (`RECORD_9`) | `&aReiniciar Parkour` | Teleports to the start and restarts the timer (`parkour-restarted`). |
| 5 | Gold pressure plate | `&aVoltar ao Checkpoint` | Teleports to the last checkpoint reached (`parkour-returned`). Before the first checkpoint, teleports to the start and restarts the timer (`parkour-restarted`). |
| 6 | Redstone | `&aParar Parkour` | Ends the run, gives back the items and flight (`parkour-stopped`). |

- Items are used with a right click, in the air or on a block, and only act in their own slot and with their own material. The click is cancelled, so the gold plate is never placed.
- Item clicks share the anti-spam of Muxe-Core (`spam.*`) with the hotbar, NPCs, cash registers and social media heads: three uses within 1 second block them for 3 seconds with `&cAguarde 2,9s para usar novamente.`
- Teleports put the player in the middle of the plate, facing the way the point was marked, and reset the fall distance.

### Flight

- Starting a run turns flight off. The core remembers whether the player was allowed to fly and whether they were flying, and gives both back when the run ends (finish, `Parar Parkour` or the server stopping).

### Time

Times are shown as minutes, seconds and milliseconds with a comma: `1m 22,704s`. Under a minute, only seconds: `8,050s`. From an hour on, hours too: `1h 0m 5,000s`.

### Records per map

- Records belong to the server group (`server.group`), like the points: `lobby-1` and `lobby-2` share the main lobby record, while the lobby of another mode has its own course and records.
- Only the best time of each player is kept.

### Shared by the server group

- Points are placements of the kind `PARKOUR` (`ParkourPointPlacement(group, point, position)`, with `ParkourPoint` `START`, `CHECKPOINT` or `END`): they are saved in PostgreSQL and sync through `placement.changed`, exactly as described in [NPCs and holograms](../npcs-and-holograms/README.md#shared-by-the-server-group). Their ids are `start`, `end` and `checkpoint-<n>`.
- Marking a point that already exists moves it. Removing it removes its hologram on every server of the group that is online.
- A point is shown only if its world is loaded on that server.

## Placing a course

1. Build the course in the map with a pressure plate on the start, on each checkpoint and on the end.
2. Stand on the start plate, facing the way players should face after a restart, and run `/parkour inicio`.
3. Stand on each checkpoint plate, in order, and run `/parkour checkpoint 1`, `/parkour checkpoint 2`, and so on.
4. Stand on the end plate and run `/parkour fim`.
5. Check with `/parkour listar`. The holograms show up on every server of the group.

The point is saved on the block where you stand, centred on it, with your yaw and a level pitch. The points can also be brought in through `import.yml` (see [Importing old placements](../npcs-and-holograms/README.md#importing-old-placements)):

```yaml
parkour:
  start:
    world: lobby
    x: 12.5
    y: 33.0
    z: 4.5
    yaw: 90.0
    pitch: 0.0
  checkpoints:
    1:
      world: lobby
      x: 20.5
      y: 36.0
      z: 4.5
      yaw: 90.0
      pitch: 0.0
  end:
    world: lobby
    x: 30.5
    y: 40.0
    z: 4.5
    yaw: 90.0
    pitch: 0.0
```

Checkpoints are keyed by their number. Broken entries are skipped with a warning.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/parkour inicio` | Marks the start on the block where you stand. `início` also works. | `muxe.parkour` |
| `/parkour checkpoint <n>` | Marks checkpoint `n` (1 or more) on the block where you stand. | `muxe.parkour` |
| `/parkour fim` | Marks the end on the block where you stand. | `muxe.parkour` |
| `/parkour remover <inicio\|fim\|n>` | Removes a point. `remover checkpoint <n>` also works. | `muxe.parkour` |
| `/parkour listar` | Lists the points of this server group: start, checkpoints in order, end. | `muxe.parkour` |

Without arguments, or with a wrong one, the command shows its usage (`messages.parkour-usage`). Only players can use it.

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.parkour` | Use `/parkour`. | op |

Running the parkour needs no permission.

## Configuration

`plugins/Muxe-Core/config.yml`:

```yaml
parkour:
  no-record: "..."
  items:
    restart:
      slot: 4
      item:
        material: RECORD_9
        name: "&aReiniciar Parkour"
        hide-flags: false
    checkpoint:
      slot: 5
      item:
        material: GOLD_PLATE
        name: "&aVoltar ao Checkpoint"
    stop:
      slot: 6
      item:
        material: REDSTONE
        name: "&aParar Parkour"
  holograms:
    start:
      - text: "&b&lPARKOUR"
        height: 2.075
      - text: "&aInício"
        height: 1.79375
      - text: "&eRecorde pessoal: &7{record}"
        height: 1.2625
    checkpoint:
      - text: "&b&lPARKOUR"
        height: 1.54375
      - text: "&eCheckpoint &a#{number}"
        height: 1.2625
    end:
      - text: "&b&lPARKOUR"
        height: 1.54375
      - text: "&cFim"
        height: 1.2625
  finish-firework:
    type: BALL_LARGE
    colors:
      - "#55FF55"
      - "#55FFFF"
      - "#FF5555"
    flicker: false
    trail: false
```

| Key | Default | Description |
|---|---|---|
| `parkour.no-record` | `...` | Shown as `{record}` to a player without a record. |
| `parkour.items.restart`, `.checkpoint`, `.stop` | Slots 4, 5 and 6 | `slot` (1 to 9) and `item` (as [configured items](../../development/core-api/README.md#items)). The three items need different slots. |
| `parkour.holograms.start`, `.checkpoint`, `.end` | See above | Lines of each hologram: `text` (with `&` colors) and `height` above the bottom of the plate block. `{number}` is the checkpoint number; `{record}` the viewer's record. |
| `parkour.finish-firework.type` | `BALL_LARGE` | Firework shape: `BALL`, `BALL_LARGE`, `STAR`, `BURST` or `CREEPER`. |
| `parkour.finish-firework.colors` | `#55FF55`, `#55FFFF`, `#FF5555` | Colors as `#RRGGBB`. At least one. |
| `parkour.finish-firework.fade-colors` | none | Colors it fades to, as `#RRGGBB`. |
| `parkour.finish-firework.flicker`, `.trail` | `false`, `false` | Firework flicker and trail. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam shared with the hotbar, NPCs, cash registers and social media heads. |

Without a `parkour` section, the parkour is off on that server: the core logs `The parkour is off: config.yml has no 'parkour' section`, shows no hologram and ignores the plates. `/parkour` still saves points, which the other servers of the group show. A missing item or hologram list, two items in the same slot, a hologram line without `text` or `height`, or a firework without colors is an error on startup.

### Messages

All in `messages` of `plugins/Muxe-Core/config.yml`:

| Key | Default |
|---|---|
| `parkour-started` | `&b&lPARKOUR &aIniciado!` |
| `parkour-restarted` | `&b&lPARKOUR &aTemporizador reiniciado!` |
| `parkour-checkpoint` | `&b&lPARKOUR &aVocê completou o &echeckpoint #{number} &aem &7{time}&a!` |
| `parkour-returned` | `&b&lPARKOUR &aVocê voltou para o checkpoint #{number}.` |
| `parkour-finished` | `&b&lPARKOUR &aVocê finalizou o &eparkour &aem &7{time}&a!` |
| `parkour-new-record` | `&b&lPARKOUR &eVocê atingiu um novo &brecorde! &a{previous} &7-> &a{time}` |
| `parkour-stopped` | `&b&lPARKOUR &cVocê saiu do parkour.` |
| `parkour-usage` | List: `&eUso do /parkour:` and one line per subcommand. |
| `parkour-invalid-point` | `&cUse inicio, fim ou o número do checkpoint (1 ou mais).` |
| `parkour-saved` | `&aPonto &f{point} &ado parkour marcado onde você está.` |
| `parkour-unknown` | `&cO parkour deste grupo não tem &f{point}&c.` |
| `parkour-removed` | `&aPonto &f{point} &aremovido do parkour.` |
| `parkour-list-empty` | `&7O parkour deste grupo não tem pontos. Use &f/parkour inicio&7.` |
| `parkour-list-header` | `&ePontos do parkour do grupo &f{group} &e({count}):` |
| `parkour-list-line` | `&8- &f{point} &8{location}` |
| `parkour-point-start`, `parkour-point-end`, `parkour-point-checkpoint` | `início`, `fim`, `checkpoint #{number}`: the `{point}` of the staff messages. |
| `placement-failed`, `spam-cooldown`, `players-only`, `no-permission` | Shared with other features. |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `placement.changed` | the server group | standard | A point of the group was marked or removed; every server of the group reloads its placements from PostgreSQL. |

Runs and records send no packet.

## Data

| Table | Content |
|---|---|
| `core.parkour_placements` | `server_group`, `id` (`start`, `end`, `checkpoint-<n>`), position (`world`, `x`, `y`, `z`, `yaw`, `pitch`). Primary key `(server_group, id)`. |
| `core.parkour_records` | `server_group`, `player_id`, `best_millis`, `achieved_at`. Primary key `(server_group, player_id)`. |

Created by the core migration `V7__create_parkour.sql`. Points are saved and removed through `SavePlacementCommand` and `RemovePlacementCommand` (`PlacementKind.PARKOUR`) and read with `FindPlacementsQuery`. Records go through `SubmitParkourTimeCommand(group, playerId, time)` (transaction `parkour.submit_time`), which keeps the time only when it beats the stored one and answers a `ParkourOutcome` (time, previous record, whether it is a new record), and `FindParkourRecordQuery(group, playerId)` (transaction `parkour.find_record`), both registered by `ParkourModule`. Runs in progress are memory only.

## Metrics

None of its own. `muxe_db_transaction_seconds{operation="parkour.submit_time"|"parkour.find_record"}` and `muxe_cqrs_handle_seconds` cover the record command and query; marking and removing points count in the placement metrics listed in [NPCs and holograms](../npcs-and-holograms/README.md#metrics).

## Integrations

- [NPCs and holograms](../npcs-and-holograms/README.md): same placement storage, group sync and `import.yml`.
- [Hotbar](../hotbar/README.md): slots 4 to 6 hold the parkour items during a run; the cosmetics chest comes back afterwards.
- [Lobby](../lobby/README.md): flight is off during a run and comes back afterwards.
- The [Core API](../../development/core-api/README.md) provides `DisplayService` for the holograms (with per-viewer text for `{record}`), `FireworkBurst` for the finish firework and `SpamGuard` for the anti-spam.

## Acceptance criteria

- [x] Stepping on the start starts a run; stepping on it again restarts the timer.
- [x] Checkpoints count only in order; a later or repeated checkpoint is ignored.
- [x] The end counts only after every checkpoint, and gives the time since the start.
- [x] Going back to the checkpoint teleports to the last one reached, or restarts at the start before the first.
- [x] A time is saved only when it beats the stored record, and the outcome tells the previous record.
- [x] Points can be marked, moved and removed, and points and records survive a round trip through PostgreSQL.
- [x] Each server group has its own records.
- [x] Times are formatted as `1m 22,704s`.
- [x] Points are marked on the block where the staff member stands, centred, and `import.yml` reads start, checkpoints and end.
- [x] The parkour items replace slots 4 to 6 and flight, and both come back when the run ends.
- [ ] Holograms, the personal record, the items, the firework and the messages work in game, and a point marked on one lobby appears on another lobby of the group (checked in game).

## Failures and recovery

- PostgreSQL unavailable when a staff command runs: staff see `&cNão foi possível salvar agora. Tente novamente em instantes.` and nothing changes.
- PostgreSQL unavailable when a player finishes: the player still sees the finish message and the firework, but the time is not saved and no new-record message is sent; a warning is logged.
- PostgreSQL unavailable on join: the record is not loaded and the start hologram shows `...` for that player until they finish a run.
- PostgreSQL unavailable on startup, or Redis unavailable: same as [NPCs and holograms](../npcs-and-holograms/README.md#failures-and-recovery).
- World not loaded on a server: that point is skipped on that server with a warning.
- Server stopping: every run ends and players get their items and flight back.
- Server internals missing (another server version): the firework flies a little before exploding instead of bursting on the plate.

## Known limitations

- The core does not place the pressure plates: building the course is a map job, and a point marked where there is no plate still works.
- Only one course per server group.
- There is no ranking: only each player's own best time is kept and shown.
- Runs are not kept across servers: switching lobbies ends the run.
- Points are found by block, so two points cannot share a block.
- Turning build mode on does not end a run; stop the run first.
