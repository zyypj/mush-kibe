**English** | [Português](README.pt-BR.md)

# Lobby

The main lobby, where players arrive after logging in and choose a game. Lives in `servers:lobby` (`Muxe-Lobby`).

## User stories

- As a player, I want to arrive at a clean, safe spawn, so that I can choose what to play.
- As a player, I want `/jogar <mode>` to send me to a game.
- As a builder, I want to edit the lobby with permission, while regular players cannot break it.

## How it works

- On join: game mode Adventure, inventory and armor cleared, full health and hunger, experience reset, teleported to the configured spawn (or the main world's spawn when none is set).
- Join and quit messages are hidden.
- Players cannot take damage or lose hunger. Rain is cancelled. Falling below `protection.void-level` teleports back to spawn.
- Breaking, placing and dropping items require `muxe.lobby.build`.
- `/jogar <mode>` checks that the mode is in `play.games` and asks the proxy to send the player to the least loaded server of that group.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/jogar <mode>` (`/play`) | Sends the player to a game group. | None |
| `/setspawn` | Saves the current position as the lobby spawn. | `muxe.lobby.setspawn` |

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.lobby.build` | Break, place and drop items in the lobby. | op |
| `muxe.lobby.setspawn` | Use `/setspawn`. | op |

## Configuration

`plugins/Muxe-Lobby/config.yml`:

| Key | Default | Description |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Groups allowed in `/jogar`. |
| `protection.void-level` | `0` | Y below which players are sent back to spawn. |
| `spawn.*` | not set | Written by `/setspawn`: world, x, y, z, yaw, pitch. |
| `messages.*` | Portuguese texts | Messages of this plugin; shared messages come from Muxe-Core. |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.connect` | proxy | standard | Sent by `/jogar`. |

## Data

None.

## Metrics

The Spigot server metrics of Muxe-Core.

## Integrations

- [Network routing](../network-routing/README.md) moves the player.

## Acceptance criteria

- [x] The spawn survives being saved to and read from configuration.
- [ ] Players arrive at the spawn in Adventure mode with an empty inventory (checked in game).
- [ ] `/jogar skywars` sends the player to a SkyWars server (checked in game).
- [ ] Players without permission cannot break or place blocks (checked in game).

## Failures and recovery

- Proxy or Redis unavailable: `/jogar` shows `play-failed` and the player stays in the lobby.
- Configured spawn world missing: the main world's spawn is used.

## Known limitations

- No game selector item, scoreboard or NPCs yet.
