**English** | [Português](README.pt-BR.md)

# Missions

Daily and weekly missions shared by every game mode, with a credit path that pays for completing every weekly mission. Muxe-Core keeps the catalog per game, the progress and the rewards; each game only says how much a player did of each goal during a match.

## User stories

- As a player, I want daily and weekly missions in each game, so that I have goals beyond winning.
- As a player, I want to know the moment I complete a mission, and see at the end of the match what moved.
- As a game developer, I want to add missions to a new mode by declaring its goals and reporting totals, without writing storage or menus.

## How it works

### Catalog

- Each game registers its catalog on start: `core.missions().register(game, catalog, rewards)`. The catalog is read from the game's `missions` config section by `MissionConfig.from(section, goals)`, where `goals` turns a goal name into a `MissionGoal(id, best)`.
- A goal either adds up (final kills, blocks broken) or keeps the best value reached (kills in a row, emeralds in one life). `WEEKLY_MISSIONS` is built in: it counts the weekly missions completed this week.
- Every player gets `daily-count` daily missions drawn from the daily pool, different per player and per day and stable during the day; `weekly-count` weekly missions drawn per week, the same for everybody; and every credit path mission.
- Days and weeks follow the calendar in São Paulo time; a week starts on Monday. Progress belongs to the day or week it was made in, so a new day or week starts from zero.

### During a match

- When the match starts, the game creates a `MissionTracker` and loads the missions of its players.
- The game reports the match totals of each player (`Map<goal, amount>`) every second and at the end. `MissionSession` turns them into progress: the difference since the last report for goals that add up, the value itself for the others.
- When a mission completes, the player sees, on the spot, `&b&lMISSÕES &aVocê completou ` followed by the mission name and category (`&eNinguém me para! &7(Diária)`). Hovering the name shows the name, a blank line and the description with the progress (`&7Elimine 2 adversários seguidos em uma partida: &f2/2`). The rewards follow, one per line: `&b+625 XP (Bed Wars)`, `&d+15 créditos`.
- Progress is saved when a mission completes and at the end of the match, with `RecordMissionProgressCommand(game, player, amounts)`. Saving pays the rewards of what completed in the same transaction: credits through the credit repository (reason `missions:<game>`) and the game reward (BedWars adds the XP to the player's statistics).
- At the end of the match, every player whose missions moved sees a summary: a border, then for each category with changes its header, a blank line, one line per mission (`" &eFim da linha: &73/35 &a(+3)"`, with `" &bCOMPLETO!"` when it completed) and a blank line, then the border again.

### Menu

- `MissionMenus.open(player, back)` opens `Missões do Bed Wars` with one row per category: the category icon (`Progresso: done/total` and the time left until the day or week ends) followed by its missions. A mission in progress has a yellow name, glows and ends with `&eEm progresso.`; a completed one has a green name (`&a`), does not glow and ends with `&aCompleta!`.
- `back` follows the server rule for menus: a menu opened from another menu shows `Voltar` (slot 48) back to it and has 6 rows; opened directly (NPC, item, command) it has none and 5 rows, as in the export.

## Commands

None.

## Permissions

None.

## Configuration

In each game's config, under `missions`:

| Key | Default | Description |
|---|---|---|
| `daily-count`, `weekly-count` | `3`, `5` | How many daily and weekly missions each player gets. |
| `daily.<id>`, `weekly.<id>`, `credit-path.<id>` | the exports | `name`, `goal`, `target`, `xp`, `credits` and `description` (lines with `{progress}`). |
| `announce.*` | the reference | `prefix`, `name` (`{name}`, `{category}`), `description`, `progress`, `xp`, `credits`. |
| `summary.*` | the reference | `border`, `headers.DAILY/WEEKLY/CREDIT_PATH`, `line` (`{name}`, `{progress}`, `{target}`, `{added}`) and `completed`. |
| `menu.*` | the exports | Title, rows (`rows` 5 without `Voltar`, `rows-with-back` 6 with it), category slots, items and labels, the name of a completed mission (`completed-name`, `&a{name}`), lore texts, the `back` button and the time units. |

## Packets

None.

## Data

| Table | Content |
|---|---|
| `core.mission_progress` | One row per player, game, mission and day or week start, with the progress and whether it completed. Migration `V15__create_mission_progress.sql`. |

`RecordMissionProgressCommand` (transaction `missions.record`) and `FindMissionsQuery(game, player)` (transaction `missions.find`), registered by `MissionModule`.

## Metrics

None besides the command and query metrics.

## Integrations

- [Credits](../credits/README.md): mission credits are added to the balance.
- [BedWars](../bedwars/README.md#missions): the first game with missions.

## Acceptance criteria

- [x] Totals become progress, goals that keep the best value use it, and missions complete on the spot (`MissionsTest`).
- [x] Completing every weekly mission advances the credit path (`MissionsTest`).
- [x] Saving pays credits and the game reward once, and a new day or week starts from zero (`MissionsTest`).
- [x] Progress survives a round trip through PostgreSQL, per game, mission and period (`PostgresMissionRepositoryIntegrationTest`).
- [x] The completion line, its hover and the match summary follow the reference (`BedWarsMissionsTest`).
- [ ] Missions complete and show in game (checked in game).

## Failures and recovery

- PostgreSQL unavailable when the match starts: the player's missions are not loaded and do not move in that match; a warning is logged.
- Saving fails: a warning is logged and the progress of that save is lost.

## Known limitations

- The missions of a player who leaves before the end are saved only when a mission completes or the match ends.
- Event missions (such as Halloween) are not in yet.
