**English** | [Português](README.pt-BR.md)

# SkyWars

SkyWars for the Muxe network. Lives in `minigames:skywars` (`Muxe-SkyWars`) and runs on servers of the `skywars` group.

## User stories

- As a player, I want to fight other players on floating islands until only my team survives.

## How it works

Today the plugin loads its game modes from configuration, creates the [game engine](../../architecture/game-engine/README.md) for SkyWars and registers its metrics. Gameplay (islands, chests, kits, kills, victory) is not implemented yet.

## Commands

None yet.

## Permissions

None yet.

## Configuration

`plugins/Muxe-SkyWars/config.yml`:

| Key | Default | Description |
|---|---|---|
| `modes.<id>.display-name` | `SkyWars Solo`, `SkyWars Duplas` | Name shown to players. |
| `modes.<id>.min-players` | `2` (solo), `4` (duo) | Players needed to start the countdown. |
| `modes.<id>.max-players` | `12` (solo), `24` (duo) | Players per match. |
| `modes.<id>.team-size` | `1` (solo), `2` (duo) | Players per team. |

## Packets

None yet.

## Data

None yet.

## Metrics

`muxe_matches_active{game="skywars"}`, `muxe_arenas_available{game="skywars"}`, `muxe_arenas_disabled{game="skywars"}`.

## Integrations

- [Lobby](../lobby/README.md): `/jogar skywars`.
- [Game engine](../../architecture/game-engine/README.md).

## Acceptance criteria

- [x] Modes are loaded from configuration (engine tests).
- [ ] Gameplay criteria will be written with the gameplay stories.

## Failures and recovery

If Muxe-Core is not enabled, the plugin disables itself.

## Known limitations

Gameplay is not implemented. Mode policies (reconnection, leaving before start, shutdown and crash compensation) will be documented here when they are.
