**English** | [Português](README.pt-BR.md)

# Game engine

The part of the platform that runs matches inside each game server. Each server runs many matches of the same game at once, so the engine's main job is keeping them isolated and their lifecycles predictable.

Code: `minigames/engine/src/main/java/br/com/muxe/minigame/engine`.

## Concepts

| Concept | Class | What it is |
|---|---|---|
| Game mode | `GameMode` | The rules of one variant of a game, such as SkyWars solo: id, display name, minimum and maximum players, countdown. Loaded from the game's `config.yml` by `GameModeLoader`. |
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

Implemented and tested: modes, match and arena lifecycles, the one-match-per-player rule, the arena pool, task scopes and metrics. Not implemented yet: world copies and resets, event routing per match, gameplay, results and rewards.
