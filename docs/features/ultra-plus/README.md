**English** | [Português](README.pt-BR.md)

# Ultra+

The perks of the Ultra+ rank: emblems next to the nick, the `Menu do Ultra+` with the monthly credit reward and the tag levels, the color of the `+`, flight in the waiting room of matches and a shorter wait between chat messages.

## User stories

- As an Ultra+ player, I pick an emblem in `Meu perfil` > `Emblemas` and see it after my nick in the chat and in the TAB.
- As an Ultra+ player, I claim the monthly credits once a month in `Meu perfil` > `Menu do Ultra+`.
- As an Ultra+ player, I unlock new tag colors and `+` colors as my months of Ultra+ add up.
- As an Ultra+ player, I fly while I wait for a match to start and wait less between chat messages.

## How it works

- **Months of Ultra+:** every time the `ultra+` rank is given with a time, its days are added to the player's total (30 days = 1 month); extending an active rank only adds the new days. A permanent Ultra+, or Ultra+ inherited from a permanent rank such as Partner, unlocks everything. The rank grants that existed before this feature were counted once by the migration.
- **Emblemas** (4 rows, `Voltar` at slot 30 to `Meu perfil`): `Nenhum` (barrier, slot 10) and ten emblems on sugar: `Coração ❤`, `Flor ✿`, `Yin Yang ☯`, `X ✖`, `Estrela ✦`, `Sorriso ツ` (slots 11 to 16), `Estrelado ✩`, `Seta ⬅`, `Correto ✔` and `Mais +` (slots 19 to 22). Each one shows an example of the nick with it (`§5§lULTRA§6+ §5tadeu❤`). The emblem goes right after the nick, in the nick's color (`Mais` is always gold), in the chat and in the TAB (the name tag suffix), whatever tag the player shows, while they are Ultra+.
- **Menu do Ultra+** (5 rows, `Voltar` at slot 39 to `Meu perfil`):
  - `Recompensa mensal` (minecart, slot 12) pays 450 credits once per calendar month in Brasília time, says `Reinicia em <time>.` until the next month and `Já resgatado.` or `Clique para resgatar.`.
  - `Cor do + (/cormais)` (slot 14) opens the `+` colors.
  - The tag levels on row 4: `Tag Nível I` (slot 28, included), `Tag Nível II` (30, 6 months, aqua), `Tag Nível III` (32, 12 months, gold) and `Tag Nível IV` (34, 24 months, red). Between them, yellow panes show the progress to the next level as a bar of ten dashes (green for each 10%) and `Meses necessários: <months>/<required> (<percent>%)`.
  - The chosen level colors the `ULTRA` and the nick of the Ultra+ tag (`§b§lULTRA§6+ §btadeu`). The tag name follows the same level and `+` colors in `/tag`, in the `/tag` messages and in the lobby scoreboard (`§bUltra§6+`).
- **Cor do +** (4 rows, `Voltar` at slot 30 to `Menu do Ultra+`): twelve colors of the `+` of the Ultra+ tag, from `Dourado` (included) to `Preto` (12 months), one more each month.
- A level, color or emblem shows `Selecionada.`/`Selecionado.` (and glows) when chosen, `Clique para selecionar.` when unlocked and `Bloqueado.` when the player is not Ultra+ or lacks the months. A choice that is no longer unlocked falls back to the included one.
- **Flight in the waiting room:** players with `muxe.pregame.fly` (Ultra+) can fly in the waiting room of any match; the flight ends when the match starts.
- **Chat interval:** members wait 3 s between chat messages, Ultra+ (`muxe.chat.fast`) 1 s, and staff (`muxe.chat.no-cooldown`) nothing. A message sent too early is not delivered and answers `messages.chat-cooldown`.
- **Tag format:** Ultra+ players choose in `Preferências` > `Formato da tag` how they see every player's tag (chat, TAB and name tag). See [Preferences](../preferences/README.md).

## Commands

| Command | Description | Permission |
|---|---|---|
| `/cormais` | Opens `Cor do +`. | None |
| `/emblemas` (`/emblema`) | Opens `Emblemas`. | None |

## Permissions

| Permission | Grants |
|---|---|
| `muxe.pregame.fly` | Flying in the waiting room of matches. Given to `ultra+` by migration V23. |
| `muxe.chat.fast` | The short chat interval. Given to `ultra+` by migration V23. |
| `muxe.chat.no-cooldown` | No chat interval. Given to `builder` (and so to every staff rank) by migration V23. |

## Configuration

Core `config.yml`, with English in `lang/en.yml`:

| Key | Default | Description |
|---|---|---|
| `menus.ultra-plus.title`, `rows`, `back` | `Menu do Ultra+`, `5`, slot 39 | The Ultra+ menu. |
| `menus.ultra-plus.prefix` | `{color}&lULTRA{plus}+ ` | Prefix of the Ultra+ tag; `{color}` is the level color and `{plus}` the `+` color. |
| `menus.ultra-plus.tag-name` | `Ultra{plus}+` | Name of the Ultra+ tag in the level color, shown in `/tag`, in its messages and in the lobby scoreboard; `{plus}` is the `+` color. |
| `menus.ultra-plus.example` | `{prefix}{color}{player}{emblem}` | The example line of the menus. |
| `menus.ultra-plus.reward.slot`, `credits`, `time-zone`, `item` | `12`, `450`, `America/Sao_Paulo` | The monthly reward; `{credits}` in the lore. |
| `menus.ultra-plus.plus-color` | slot 14 | The button that opens `Cor do +`. |
| `menus.ultra-plus.levels.<n>` | levels 1 to 4 | `slot`, `color` (a color code without `&`), `months` and `item`. |
| `menus.ultra-plus.progress` | yellow pane, `dashes: 10` | The panes between levels. |
| `menus.ultra-plus.texts.*` | the reference | `included`, `required`, `progress`, `example`, `selected`, `emblem-selected`, `select`, `locked`, `exclusive`, `resets`, `claimed`, `claim`. |
| `menus.plus-colors.title`, `rows`, `back`, `colors.<id>` | `Cor do +`, `4`, slot 30, twelve colors | Each color has `slot`, `color`, `months` and `item`. |
| `menus.emblems.title`, `rows`, `back`, `none`, `emblems.<id>` | `Emblemas`, `4`, slot 30, ten emblems | Each emblem has `slot`, `symbol`, an optional `color` and `item`. |
| `chat-interval.seconds`, `fast-seconds` | `3`, `1` | Seconds between chat messages for members and for `muxe.chat.fast`. |
| `messages.ultra-plus-only`, `ultra-plus-locked`, `ultra-plus-reward-claimed`, `ultra-plus-reward-already`, `ultra-plus-reward-failed`, `chat-cooldown` | see the file | Answers of the menus and of the chat. |

## Packets

None.

## Data

`core.ultra_plus_players` (migration V23): `player_id`, `granted_days`, `emblem`, `plus_color`, `tag_level`, `reward_month` (`YYYY-MM` of the last claim) and `updated_at`. `ChangeUltraPlusStyleCommand` saves the choices, `ClaimUltraPlusRewardCommand` records the month and adds the credits in the same transaction (reason `ultra-plus-monthly-reward`), `FindUltraPlusQuery` reads a player. The permission handlers call `UltraPlusGrants` to add the days of each Ultra+ grant.

## Metrics

None.

## Integrations

- [Hotbar](../hotbar/README.md): `Emblemas` and `Menu do Ultra+` of `Meu perfil`.
- [Ranks and tags](../ranks-and-tags/README.md): the Ultra+ tag is restyled and the emblem added through `PermissionService.style`.
- [Credits](../credits/README.md): the monthly reward.
- [Minigame API](../../architecture/minigame-api/README.md): `Stage.waitingFlight` for the waiting room.

## Acceptance criteria

- [x] The three menus match the reference exports, with the progress panes and the claimed reward (`UltraPlusPagesTest`).
- [x] The tag level, the `+` color and the emblem change the chat, the prefix, the tag name and the name tag, and locked choices fall back (`UltraPlusLookTest`).
- [x] `/tag` lists and selects the tag with the colors the player chose (`TagCommandTest`).
- [x] The days of each grant add up and extensions only count the new days (`UltraPlusGrantTest`).
- [x] The reward pays once per month (`ClaimUltraPlusRewardHandlerTest`) and is saved with the choices (`PostgresUltraPlusRepositoryIntegrationTest`).
- [x] Members wait 3 s and Ultra+ 1 s between chat messages (`ChatIntervalTest`).
- [x] Joining a match lets the player fly in the waiting room (`MatchRuntimeTest`).
- [ ] Checked in game.

## Failures and recovery

If the Ultra+ profile cannot be loaded, the player shows the included look until the next join and the error is logged. A reward that cannot be claimed answers `messages.ultra-plus-reward-failed` and pays nothing.

## Known limitations

- `Efeito de entrada` (entry cosmetics in the lobby) is not done yet.
- The emblem and the restyled tag do not show on the team name tags inside a match.
- Months count only grants made through the permission commands or the editor.
