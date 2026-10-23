**English** | [Português](README.pt-BR.md)

# BedWars

BedWars for the Muxe network. Lives in `minigames:bedwars` (`Muxe-BedWars`) and runs on servers of the `bedwars` group.

## User stories

- As a player, I want to protect my team's bed and destroy the others', so that my team is the last one standing.

## How it works

Today the plugin loads its game modes from configuration, creates the [game engine](../../architecture/game-engine/README.md) for BedWars and registers its metrics. Gameplay (beds, generators, shop, teams, victory) is not implemented yet.

## Commands

None yet.

## Permissions

None yet.

## Configuration

`plugins/Muxe-BedWars/config.yml`:

| Key | Default | Description |
|---|---|---|
| `modes.<id>.display-name` | `BedWars Solo`, `BedWars Quartetos` | Name shown to players. |
| `modes.<id>.min-players` | `4` (solo), `8` (quartets) | Players needed to start the countdown. |
| `modes.<id>.max-players` | `8` (solo), `16` (quartets) | Players per match. |
| `modes.<id>.countdown` | `20s`, `30s` | Countdown before the match starts. |

## Packets

None yet.

## Data

None yet.

## Metrics

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrations

- [Lobby](../lobby/README.md): `/jogar bedwars`.
- [Game engine](../../architecture/game-engine/README.md).

## Acceptance criteria

- [x] Modes are loaded from configuration (engine tests).
- [ ] Gameplay criteria will be written with the gameplay stories.

## Failures and recovery

If Muxe-Core is not enabled, the plugin disables itself.

## Known limitations

Gameplay is not implemented. Mode policies (reconnection, leaving before start, shutdown and crash compensation) will be documented here when they are.
