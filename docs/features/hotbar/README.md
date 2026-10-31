**English** | [Português](README.pt-BR.md)

# Hotbar

The items every lobby gives to players, and the menus they open: game selector, profile, cosmetics, player visibility and lobby selector. Lives in Muxe-Core (`core:bukkit`), so every lobby of the network gets the same hotbar without configuring it: the main lobby today and, later, the lobby of each game.

## User stories

- As a player, I want the same items in every lobby, so I always know where to find the game selector, my profile and the lobby selector.
- As a player, I want to choose a game from a menu that shows how many people are playing each one.
- As a player, I want to see my profile, with my first login and my credits.
- As a player, I want to hide other players when the lobby is crowded, and keep that choice in every lobby.
- As a player, I want to switch to another lobby of the same group.
- As an operator, I want to add a game to the selector by editing the configuration only.

## How it works

### Hotbar

On a server whose kind is in `hotbar.server-kinds` (only `LOBBY` by default; auth and game servers get no hotbar), Muxe-Core clears the inventory and the armor on join, gives the hotbar, selects slot 1 and applies the player's saved visibility.

| Slot | Item | Name | Action |
|---|---|---|---|
| 1 | Compass | `&aSelecionar jogo` | Opens the game selector |
| 2 | Player's head | `&aMeu perfil` | Opens the profile |
| 5 | Chest | `&aCosméticos` | None yet |
| 8 | Dye | `&fJogadores: …` | Changes the player visibility, see below |
| 9 | Nether star, glowing | `&aSelecionar lobby` | Opens the lobby selector |

- Items are used with a right click, in the air or on a block. An item only acts in its own slot and with its own material.
- Right clicks share the anti-spam of Muxe-Core (`spam.*`) with NPCs, cash registers, social media heads and the parkour items: three uses within 1 second block them for 3 seconds with `&cAguarde 2,9s para usar novamente.`
- Hotbar items do nothing while the player is in [build mode](../../development/core-api/README.md#build-mode). When a staff member leaves build mode, the hotbar comes back, with the current visibility dye.
- While a player runs the [parkour](../parkour/README.md), slots 4 to 6 hold the parkour items (`Reiniciar Parkour`, `Voltar ao Checkpoint`, `Parar Parkour`) instead: the cosmetics chest of slot 5 is put away and comes back when the run ends.
- Keeping the items in place (no moving, dropping or taking them) is the job of the server's protection; on the main lobby, the [Lobby](../lobby/README.md#protection) does it.
- If `config.yml` has no `hotbar` section, Muxe-Core logs `The hotbar is off: config.yml has no 'hotbar' section` and gives no hotbar.

### Game selector

- Title `Selecionar jogo`, 4 rows. Opened by the compass.
- Each button can send the player to a game or open the shop:
  - `game: <group>`: clicking closes the menu and sends the player to the least loaded live server of that group, with no chat message. When the group has no live server, the player sees `&cNenhuma sala de &f<name> &cdisponível no momento.` instead. `<name>` is the button name without colors.
  - `shop: <category>`: opens the [shop](../shop/README.md) on that category. An empty or unknown category opens the shop on `shop.default-category` (Ranks).
- `{players}` in a button's name or lore is the sum of the players online on the live servers of its group, read when the menu opens.
- By default it lists 14 games (Bed Wars, HG, Duels, PvP, Sky Wars, The Bridge, CTF, Party, Murder, Quick Builders, Build Battle, Hide and Seek, Treino: Ponte and Nascó Mandou) and a `Sala customizada` button, at the bottom right, that opens the shop on the Ultra+ category.

### Profile

- Title `Meu perfil`, 6 rows. Opened by the head.
- The head at the top (slot 13) shows the player's skin, the name with the tag they display and the lore, with one `Rank` line per rank given to the player (lowest first, `Vitalício` or the time left):

```text
NATAL tadeu

Primeiro login: 10/09/2021 16:29
Créditos: 0
Rank: VIP (Vitalício)
Rank: Ultra (29d 23h)
```

- The name is `&c&lNATAL &ctadeu`: the prefix and color of the displayed tag, as in the chat. The rank lines are `&7Rank: &aVIP &7(Vitalício)`. See [Ranks and tags](../ranks-and-tags/README.md).

- The first login comes from the player's [account](../accounts/README.md) and is formatted with `first-login.format` in `first-login.time-zone`. When the account cannot be read, `first-login.unknown` (`-`) is shown.
- Credits show `default-credits` until a wallet exists.
- Buttons: `Amigos`, `Ver estatísticas`, `Preferências`, `Medalhas`, `Sua skin`, `Nick (/nick)` and `Menu do Ultra+`. `Nick` opens the shop on the Ultra+ category; `Medalhas` closes the menu and runs `/medalha`, which sends the [medal list](../ranks-and-tags/README.md#medalha) in the chat; the others do nothing yet, because their menus do not exist.

### Lobby selector

- Title `Selecionar lobby (1/1)`. Opened by the nether star.
- Every live server of the current server's group (or of `menus.lobby-selector.group`, when set) appears in number order, centered on the middle row: one stained glass pane each, lime for the lobby the player is in, gray for the others.
- Each pane is named `&aLobby Principal #<number>` with the lore `&fJogadores &a<online>/<max>`.
- Clicking another lobby sends the player there through the proxy, without a "connecting" message. Clicking the current lobby shows `&cVocê já está conectado neste lobby.` and a full one `&cEste lobby está cheio.`
- More than 21 lobbies spread over pages with arrows.

### Player visibility

The dye cycles through three options. The choice is saved in PostgreSQL, so it follows the player to every lobby:

| Option | Item | Message |
|---|---|---|
| Show everyone | Lime dye (`data: 10`), `&fJogadores: &aAtivados` | `&aVocê agora está vendo todos os jogadores.` |
| Only friends | Light blue dye (`data: 12`), `&fJogadores: &bApenas amigos` | `&bVocê agora está vendo apenas seus amigos.` |
| Hide everyone | Gray dye (`data: 8`), `&fJogadores: &7Desativados` | `&cVocê escondeu todos os jogadores.` |

- On join the player gets the "show everyone" dye at once; the saved choice is loaded in the background and then applied, with its dye.
- The choice also applies to players who join later.
- There is no friend system yet, so "Only friends" hides everyone. Friends will come from `FriendDirectory`.

## Commands

None. `/lobby`, which sends the player to the least loaded lobby, is described in [Network routing](../network-routing/README.md).

## Permissions

None.

## Configuration

`plugins/Muxe-Core/config.yml`. Items use the [`ItemBuilder` format](../../development/core-api/README.md#items).

### Hotbar

| Key | Default | Description |
|---|---|---|
| `hotbar.server-kinds` | `LOBBY` | Server kinds that give the hotbar. |
| `hotbar.<item>.slot`, `hotbar.<item>.item` | See above | Slot (1 to 9) and item of `game-selector`, `profile`, `cosmetics` and `lobby-selector`. The profile item gets the player's skin. |
| `hotbar.visibility.slot` | `8` | Slot of the visibility dye. |
| `hotbar.visibility.all`, `friends`, `none` | See above | One item per visibility option. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam shared by hotbar, NPC, cash register, social media head and parkour item clicks. |

Two hotbar items in the same slot stop the core from starting.

```yaml
hotbar:
  server-kinds:
    - LOBBY
  game-selector:
    slot: 1
    item:
      material: COMPASS
      name: "&aSelecionar jogo"
  visibility:
    slot: 8
    all:
      material: INK_SACK
      data: 10
      name: "&fJogadores: &aAtivados"
```

### Menus

Slots in `menus` are inventory slots, from 0 (top left) to 53, like in the shop. A button outside the menu's rows, or two buttons in the same slot, stop the core from starting.

| Key | Default | Description |
|---|---|---|
| `menus.game-selector.title`, `rows` | `Selecionar jogo`, `4` | Game selector title and height. |
| `menus.game-selector.buttons.<id>` | 14 games and `custom-room` | `slot`, `item` and, optionally, `game` (a server group) or `shop` (a shop category id; empty for the default one). Placeholder: `{players}`. |
| `menus.profile.title`, `rows` | `Meu perfil`, `6` | Profile title and height. |
| `menus.profile.head` | Slot `13`, the player's head named `{name}` | `slot` and `item` of the head. Placeholders: `{player}` (plain name), `{name}` (name with the tag prefix and color), `{first-login}`, `{credits}`. |
| `menus.profile.ranks.line`, `ranks.permanent` | `&7Rank: {rank} &7({time})`, `Vitalício` | Lore line added for each rank (`{rank}` is the rank tag, such as `&aVIP`; `{time}` is the time left or `ranks.permanent`). |
| `menus.profile.buttons.<id>` | The 7 profile buttons | Same keys as the game selector buttons, plus `command` (a command the player runs after the menu closes; `medalha` on `medals`). |
| `menus.profile.first-login.format`, `time-zone`, `unknown` | `dd/MM/yyyy HH:mm`, `America/Sao_Paulo`, `-` | How the first login is written, and what is shown when it is unknown. |
| `menus.profile.default-credits` | `0` | Credits shown until a wallet exists. |
| `menus.lobby-selector.group` | not set (the server's group) | Group listed in the lobby selector. |
| `menus.lobby-selector.title`, `current`, `other`, `previous-page`, `next-page` | See above | Selector title and items. Placeholders: `{page}`, `{pages}`, `{number}`, `{online}`, `{max}`. |

```yaml
menus:
  game-selector:
    title: "Selecionar jogo"
    rows: 4
    buttons:
      skywars:
        slot: 14
        game: skywars
        item:
          material: EYE_OF_ENDER
          name: "&b&lSKY WARS"
          lore:
            - "&e{players} jogando agora!"
            - " "
            - "&aClique para entrar!"
      custom-room:
        slot: 35
        shop: ultra-plus
        item:
          material: BREWING_STAND_ITEM
          name: "&aSala customizada (/salacustom)"
```

### Adding a game to the selector

1. Start the game's servers with `server.group` set to the game group (such as `skywars`), so they show up in the server directory.
2. Add a button under `menus.game-selector.buttons` with a free `slot`, `game: <group>` and its `item`, using `{players}` in the lore if you want the player count.
3. Grow `rows` if the slot is beyond the last row, and restart the lobbies.

### Messages

| Key | Text |
|---|---|
| `messages.game-unavailable` | `&cNenhuma sala de &f{name} &cdisponível no momento.` (also used by game NPCs) |
| `messages.connect-failed` | `&cNão foi possível enviar você agora. Tente novamente em instantes.` |
| `messages.visibility-all`, `visibility-friends`, `visibility-none` | See the visibility table. |
| `messages.lobby-current`, `lobby-full` | See the lobby selector. |
| `messages.lobby-failed` | `&cNão foi possível trocar de lobby agora. Tente novamente.` |
| `messages.shop-unavailable` | `&cA loja não está disponível neste servidor.` |
| `messages.spam-cooldown` | `&cAguarde {seconds}s para usar novamente.` |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `player.connect` | proxy | standard | Sent by the game selector and the lobby selector to move the player. |

## Data

| Table | Content |
|---|---|
| `core.player_visibility` | `player_id` (primary key), `visibility` (`ALL`, `FRIENDS`, `NONE`), `updated_at`. Written when a player changes the visibility, read on join. Created by the core migration `V5__create_player_visibility.sql`, which copies the rows of the old `lobby.player_settings`. |

Commands and queries: `ChangeVisibilityCommand`, `FindVisibilityQuery` (players without a row see everyone), registered by `VisibilityModule`.

The profile reads the account with `FindAccountQuery`.

## Metrics

`muxe_db_transaction_seconds{operation="visibility.change"|"visibility.find"}` and `muxe_cqrs_handle_seconds` for the command and the query above.

## Integrations

- The [Core API](../../development/core-api/README.md) provides the menus, items, `ConfiguredButton`, `GameConnector` and `ShopAccess`.
- [Network routing](../network-routing/README.md) moves the player to the chosen game or lobby.
- The [Shop](../shop/README.md) opens from the `shop` buttons.
- [Accounts](../accounts/README.md) give the first login shown in the profile.
- [Ranks and tags](../ranks-and-tags/README.md) give the name, tag and ranks of the profile head.
- The [Lobby](../lobby/README.md) keeps the items in place and gives flight back after build mode.
- The [Parkour](../parkour/README.md) borrows slots 4 to 6 during a run.

## Acceptance criteria

- [x] The hotbar is given only on the server kinds in `hotbar.server-kinds`, in slots 1, 2, 5, 8 and 9, with the reference names.
- [x] The visibility dye cycles through the three options, and the choice is saved in PostgreSQL and read back.
- [x] The game selector shows the configured buttons, with `{players}` as the sum of the group's players, sends the player to the group and opens the shop on `shop` buttons.
- [x] The profile shows the player's head, displayed tag, first login, credits and ranks.
- [x] The lobby selector lists the lobbies of the current group in number order, centered, lime for the current one and gray for the others.
- [x] Three quick uses start a 3-second cooldown with the remaining time as `2,9`.
- [x] The rows of `lobby.player_settings` are copied to `core.player_visibility`.
- [ ] Hotbar, menus and visibility work in game (checked in game).

## Failures and recovery

- PostgreSQL unavailable: the visibility starts as "show everyone" and changes are applied but not saved, with a warning in the log. The profile shows `-` as the first login.
- Proxy or Redis unavailable: the game selector shows `connect-failed`, the lobby selector `lobby-failed`, and the player stays in the lobby.
- No live server in a game group: `game-unavailable`, and the player stays where they are.
- Shop off on this server kind: `shop` buttons answer `shop-unavailable`.
- Invalid configuration (two items in the same slot, a button outside the menu, a missing section): the core does not start, with the reason in the log.

## Known limitations

- The cosmetics item does nothing yet.
- The profile buttons `Amigos`, `Ver estatísticas`, `Preferências`, `Sua skin` and `Menu do Ultra+` do nothing yet.
- There is no friend system, so "Only friends" hides everyone.
- There are no credits yet: the profile shows `default-credits`.
- `{players}` in the game selector is read when the menu opens and does not refresh while it is open.
