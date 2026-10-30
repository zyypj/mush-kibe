**English** | [Português](README.pt-BR.md)

# BedWars

BedWars for the Muxe network. Lives in `minigames:bedwars` (`Muxe-BedWars`) and runs on servers of the `bedwars` group.

## User stories

- As a player, I want to protect my team's bed and destroy the others', so that my team is the last one standing.

## How it works

The plugin loads its nine modes, creates the [game engine](../../architecture/game-engine/README.md) for BedWars and gives staff the [arena setup](../../architecture/game-engine/README.md#arena-setup). Matches, beds, generators, shop, upgrades and victory are being built.

### Modes

| Mode | Minimum | Maximum | Per team | Teams |
|---|---:|---:|---:|---|
| Solo | 6 | 8 | 1 | 8 of the 9 colours, chosen in the setup |
| Duplas | 8 | 16 | 2 | 8 of the 9 colours, chosen in the setup |
| Trios | 6 | 12 | 3 | Azul, Vermelho, Verde, Amarelo |
| Quartetos | 8 | 16 | 4 | Azul, Vermelho, Verde, Amarelo |
| 1v1 | 2 | 2 | 1 | Azul, Vermelho |
| 2v2 | 4 | 4 | 2 | Azul, Vermelho |
| 3v3 | 6 | 6 | 3 | Azul, Vermelho |
| 4v4 | 8 | 8 | 4 | Azul, Vermelho |
| 4s | 8 | 8 | 4 | Azul, Vermelho, on a map with two more islands for resources and NPCs |

### Arena setup

An arena is named `<mode>_<map>`, such as `1v1_slip` or `quartetos_air_nation`. `/arena criar 1v1_slip` converts a world folder named `1v1_slip` or opens an empty world.

| Step | Per team | Kind | Required |
|---|---|---|---|
| `espera`: waiting spawn | no | where the player stands | yes |
| `area-espera`: waiting area, removed when the match starts | no | two corners | yes |
| `espectador`: spectator spawn | no | where the player stands | yes |
| `void`: void height | no | the player's Y | yes |
| `limite`: highest block players may place | no | the player's Y | no |
| `diamante`, `esmeralda`: generators | no | the block under the player | no, up to 16 each |
| `spawn` | yes | where the player stands | yes |
| `cama`: bed | yes | the block the player looks at | yes |
| `loja`, `melhorias`: shop and upgrades NPCs | yes | where the player stands | yes |
| `ferro`, `ouro`: island generators | yes | the block under the player | at least 1, up to 4 |
| `loja-extra`, `melhorias-extra`, `ferro-extra`, `ouro-extra` | no | as above | 4s only, optional |

## Commands

| Command | Permission | Description |
|---|---|---|
| `/arena ...` | `muxe.minigame.setup` | Creates and sets up arenas. See [arena setup](../../architecture/game-engine/README.md#arena-setup). |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `muxe.minigame.setup` | op | Allows `/arena`. |

## Configuration

`plugins/Muxe-BedWars/config.yml`:

| Key | Default | Description |
|---|---|---|
| `modes.<id>.display-name` | `Solo`, `Duplas`, `1v1`... | Name shown to players. |
| `modes.<id>.min-players` | see the table above | Players needed to start the countdown. |
| `modes.<id>.max-players` | see the table above | Players per match. |
| `modes.<id>.team-size` | see the table above | Players per team. |
| `modes.<id>.fixed-teams` | none (Solo, Duplas) | Team colours of modes whose teams are fixed. Without it, the setup chooses `max-players / team-size` colours. |
| `modes.<id>.extra-islands` | `true` on 4s | Adds the extra shop, upgrades and generator steps to the setup. |
| `messages.setup-*` | the engine's | Overrides any setup message. |

## Packets

None yet.

## Data

| File | What it holds |
|---|---|
| `plugins/Muxe-BedWars/maps/<arena>.mxw` | The map, in the [Muxe map format](../../decisions/0009-own-map-format-and-match-cells.md). Copied to every BedWars server. |
| `plugins/Muxe-BedWars/arenas/<arena>.yml` | Mode, display name, teams, points and heights, with `ready: true` when nothing is missing. Copied with the map. |

## Metrics

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrations

- [Lobby](../lobby/README.md): `/jogar bedwars`.
- [Game engine](../../architecture/game-engine/README.md).

## Acceptance criteria

- [x] Modes are loaded from configuration (engine tests).
- [x] The nine modes have the limits and teams of the spec (`BedWarsSchemaTest`).
- [x] An arena can be set up in game, with a checklist of what is missing (engine tests).
- [ ] Gameplay criteria will be written with the gameplay stories.

## Failures and recovery

If Muxe-Core is not enabled, the plugin disables itself.

## Known limitations

Gameplay is not implemented. Mode policies (reconnection, leaving before start, shutdown and crash compensation) will be documented here when they are.
