**English** | [Português](README.pt-BR.md)

# Statistics

A menu with a player's statistics in every game, opened from `Meu perfil` or with `/stats`, for any player on any server.

## User stories

- As a player, I open `Ver estatísticas` in my profile and see my statistics, then go back to the profile.
- As a player, I type `/stats Steve` and see Steve's statistics, even when Steve is offline or on another server.

## How it works

- **Games menu:** `Suas estatísticas` (or `Estatísticas de <name>` for another player), 4 rows, with the ten games of the reference: HG (slot 10), Duels (11), PvP (12), Sky Wars (13), Bed Wars (14), Quick Builders (15), CTF (16), Murder (19), Build Battle (20) and Hide and Seek (21). The games Muxe does not have yet show their lines with zeros; the HG mini mode is called Minimuxe. Duels and Sky Wars say `Clique para ver mais.` and answer `messages.statistics-soon` when clicked. Opened from `Meu perfil`, it has `Voltar` at slot 31 (`Para Meu perfil`); opened by `/stats` it has none.
- **Bed Wars page:** clicking Bed Wars opens `Suas estatísticas (Bed Wars)` (or `Estatísticas de <name> (Bed Wars)`), 6 rows: `Geral` at slot 4 with the level badge, XP and every counter, and Solo, Duplas, Trios, Quartetos, 1v1, 2v2, 3v3, 4v4 and Mega (beds, slots 10 to 40) with the counters of that mode. Numbers use a dot every three digits. `Voltar` at slot 49 goes back to the games menu (`Para <title of the games menu>`).
- **Data:** the counters are the lifetime ones (`period = total`) of `bedwars.player_stats`, the table the BedWars plugin writes. The core reads it (`FindBedWarsTotalsQuery`), so the menu works on servers without the BedWars plugin, such as the main lobby. A player with no row shows zeros.
- **Level:** the level comes from the total XP with the BedWars formula, now in the core (`BedWarsLevels`), and the badge from `menus.statistics.bedwars.level-badges` (`LevelBadges`, also in the core and used by the BedWars plugin).

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/stats [player]` | `/estatisticas` | Opens the statistics of the player, or your own. A name nobody has used answers `messages.statistics-not-found`; a player who turned `Visibilidade de estatísticas` off in the [preferences](../preferences/README.md) answers `messages.statistics-hidden`. |

## Permissions

None.

## Configuration

Core `config.yml`, with English in `lang/en.yml`:

| Key | Default | Description |
|---|---|---|
| `menus.statistics.title`, `title-other`, `rows` | `Suas estatísticas`, `Estatísticas de {name}`, `4` | Games menu titles and height. |
| `menus.statistics.back` | slot 31, arrow | `Voltar` when opened from the profile. |
| `menus.statistics.games.<id>` | the ten games | `slot`, `item` and `opens` (`nothing`, `soon` or `bedwars`). |
| `menus.statistics.bedwars.title`, `title-other`, `rows`, `back` | `Suas estatísticas (Bed Wars)`, `Estatísticas de {name} (Bed Wars)`, `6`, slot 49 | Bed Wars page; `{parent}` in the back lore is the games menu title. |
| `menus.statistics.bedwars.general`, `lore`, `modes.<mode>` | the reference | `Geral`, the mode lines and each mode slot and bed; placeholders `{level}`, `{xp}`, `{games}`, `{beds-broken}`, `{beds-lost}`, `{kills}`, `{deaths}`, `{assists}`, `{final-kills}`, `{final-deaths}`, `{final-assists}`, `{wins}`, `{losses}`, `{winstreak}`, `{best-winstreak}`. |
| `menus.statistics.bedwars.level-badges` | the BedWars badges | Badge per level and the rainbow from level 1000; keep it equal to `levels` of the BedWars `config.yml`. |
| `messages.statistics-soon`, `messages.statistics-not-found` | `&cEm breve.`, `&cO jogador &f{player} &cnunca entrou no servidor.` | Answers of the menu and of `/stats`. |

## Packets

None.

## Data

Reads `bedwars.player_stats` (owned by the BedWars plugin, see [BedWars](../bedwars/README.md)); writes nothing.

## Metrics

None.

## Integrations

- [Hotbar](../hotbar/README.md): `Ver estatísticas` of `Meu perfil`.
- [BedWars](../bedwars/README.md): the counters, the level formula and the badges.
- `MuxeCore.statistics()` opens the menus from another plugin (`openHub`, `openBedWars`).

## Acceptance criteria

- [x] The games menu lists the ten games in the reference slots, with `Voltar` only when opened from the profile (`StatsPagesTest`).
- [x] The Bed Wars page shows the lifetime counters with dotted thousands and the level badge (`StatsPagesTest`).
- [x] The lifetime counters are read per mode and the shorter periods are ignored (`PostgresBedWarsTotalsRepositoryIntegrationTest`).
- [x] `Ver estatísticas` opens the statistics with a way back to the profile (`ProfileMenuTest`).
- [ ] Checked in game.

## Failures and recovery

If the counters cannot be read, the Bed Wars page opens with zeros and the error is logged.

## Known limitations

- Only Bed Wars has real statistics; the other games show zeros until they exist.
- The badges are configured twice, in the core and in the BedWars plugin.
