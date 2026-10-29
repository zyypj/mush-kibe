**English** | [Português](README.pt-BR.md)

# Account

What a player can see about an account: `/account` in the chat, `/accountmenu` in a menu, and the head of the profile menu. Lives in Muxe-Core (`core:bukkit` package `account`), on every Bukkit server. The data comes from [Accounts](../accounts/README.md) (`FindAccountDetailsQuery`: account, account number and location) and the ranks from [Ranks and tags](../ranks-and-tags/README.md).

## User stories

- As a player, I want to see my account: type, ranks with their dates, first and last login and where I log in from.
- As a staff member, I want to see the same about any player, online or not, and have the staff tools of the account at hand.

## How it works

### `/account [jogador]`

Without a name it shows your own account; with a name, another player's (needs `muxe.account.others`). Players who never joined get `account.not-found`. The console must give a name.

```text
Usuário: tadeu
Tipo: Premium
Discord: Em breve
Rank: VIP
  Adicionado em: 09/09/2022 14:09
  Atualizado em: 09/09/2022 14:09
Rank: Ultra
  Adicionado em: 21/10/2023 11:29
  Atualizado em: 21/10/2023 11:29
  Expira em: 20/11/2026 11:29 (23d 2h)
Primeiro login: 10/09/2021 16:29
Último login: 28/10/2026 13:07
Localização:
  País: Brasil
  Estado: Minas Gerais
  Cidade: Tupaciguara
  ASN: AS264207 Ponto Net Telecom
```

- Labels are `&a` and values `&f`; the indented lines are `&7`.
- `Tipo` is `Premium` or `Pirata`. Discord is not linked yet and shows `Em breve`.
- One `Rank` block per rank given to the player, from the lowest: the date the rank was first given and the date of its last change. Temporary ranks add `Expira em` with the time left. A player without ranks shows `Rank: Membro`, without dates.
- Dates use `menus.account.date-format` (`dd/MM/yyyy HH:mm`) in `menus.account.time-zone` (`America/Sao_Paulo`).
- The location is filled by the proxy at login. Unknown parts are left out; with nothing known it shows `Desconhecida`.

### `/accountmenu [jogador]`

Opens a 6-row menu titled `Conta: <name>`, with the same rule for other players.

- Slot 4: the player's head, named in the color of the tag they show (`&atadeu`), with the lore:

```text
Tipo: Premium
ID: (1680062)
ID de Perfil: Em breve
UUID: 595a25f1-eb36-4999-baa8-9c73efc8fdb7

Rank: VIP
Rank: Ultra
Primeiro login: 10/09/2021 16:29:25
Último login: 28/10/2026 13:07:27
Discord: Em breve
```

- Labels are `&7` and values `&f`; each `Rank` is the tag display (`&aVIP`). `ID` is the account number. The dates have seconds (`menus.account.menu-date-format`).
- Slot 49: a red bed, `&cFechar`, that closes the menu.
- Players with `muxe.account.staff` also see `Punições` (slot 20), `Histórico de punições` (21), `Estatísticas` (23) and `Migrar estatísticas` (24). Those systems do not exist yet: clicking answers `&cEm breve.`

### Profile head

The head of `Meu perfil` (see [Hotbar](../hotbar/README.md#profile)) shows the name with the displayed tag (`&c&lNATAL &ctadeu`), the first login, the credits and one line per rank: `&7Rank: &aVIP &7(Vitalício)` or the time left, such as `(29d 23h)`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/account` | Your account in the chat. | everyone |
| `/account <jogador>` | Another player's account. | `muxe.account.others` |
| `/accountmenu` | Your account menu. | everyone |
| `/accountmenu <jogador>` | Another player's account menu. | `muxe.account.others` |

Aliases: `/conta` and `/contamenu`. Names complete with the online players.

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.account.others` | `/account` and `/accountmenu` on other players. | op; seeded on `mod` and above by `V10` |
| `muxe.account.staff` | The staff buttons of `/accountmenu`. | op; seeded on `mod` and above by `V10` |

## Configuration

`plugins/Muxe-Core/config.yml`, under `menus.account`:

| Key | Default | Description |
|---|---|---|
| `title`, `rows` | `Conta: {player}`, `6` | Menu title and height. |
| `time-zone`, `date-format`, `menu-date-format` | `America/Sao_Paulo`, `dd/MM/yyyy HH:mm`, `dd/MM/yyyy HH:mm:ss` | Dates of `/account` and of the menu. |
| `head` | Slot `4`, `SKULL_ITEM:3` named `{colored-name}` | `slot` and `item` of the head. Placeholders: `{player}`, `{name}` (with the tag prefix), `{colored-name}`, `{type}`, `{number}`, `{profile-id}`, `{uuid}`, `{first-login}`, `{last-login}`, `{discord}`. |
| `rank-line` | `&7Rank: {rank}` | Lore line added after the head lore for each rank. |
| `head-footer` | `Primeiro login`, `Último login`, `Discord` | Lore lines after the ranks. Same placeholders. |
| `close` | Slot `49`, `BED` named `&cFechar` | The close button. |
| `staff-buttons.<id>` | `punishments`, `punishment-history`, `statistics`, `migrate-statistics` | Buttons only staff see. |

### Messages

In `messages.account` of `plugins/Muxe-Core/config.yml`:

| Key | Default |
|---|---|
| `usage` | `&cUso: &f/account <jogador>` |
| `not-found` | `&cO jogador &f{player} &cnunca entrou no servidor.` |
| `premium`, `cracked`, `soon` | `Premium`, `Pirata`, `Em breve` |
| `soon-click` | `&cEm breve.` |
| `user`, `type`, `discord` | `&aUsuário: &f{player}`, `&aTipo: &f{type}`, `&aDiscord: &f{discord}` |
| `rank` | `&aRank: &f{rank}` (`{rank-display}` gives the colored tag) |
| `rank-added`, `rank-updated`, `rank-expires` | `  &7Adicionado em: {date}`, `  &7Atualizado em: {date}`, `  &7Expira em: {date} ({time})` |
| `first-login`, `last-login` | `&aPrimeiro login: &f{date}`, `&aÚltimo login: &f{date}` |
| `location`, `country`, `region`, `city`, `asn`, `location-unknown` | `&aLocalização:`, `  &7País: {value}`, ..., `  &7Desconhecida` |

## Packets

None.

## Data

Read only: `core.accounts` (with `number`, `V9`), `core.account_locations` (`V9`) and the rank tables of [Ranks and tags](../ranks-and-tags/README.md#data), through `FindAccountDetailsQuery` and `FindPermissionUserQuery`.

## Metrics

None of its own.

## Integrations

- [Accounts](../accounts/README.md): account, number and location.
- [Ranks and tags](../ranks-and-tags/README.md): ranks, dates and the displayed tag.
- [Hotbar](../hotbar/README.md): the profile menu.

## Acceptance criteria

- [x] `/account` shows user, type, Discord, each rank with its dates, logins and location, with the reference colors.
- [x] Other players need `muxe.account.others`; unknown players and the console without a name get clear replies.
- [x] `/accountmenu` has the head in slot 4 with the reference lore and the bed in slot 49; staff see four more buttons.
- [x] The profile head shows the displayed tag and the ranks with `Vitalício` or the time left.
- [ ] The menu and the messages look like the reference in game (checked in game).

## Failures and recovery

- PostgreSQL unavailable: the command answers `rank-failed` and nothing opens.

## Known limitations

- Discord, the profile id, punishments and statistics do not exist yet.
- Players are looked up by their current name; a name released to someone else shows that account.
