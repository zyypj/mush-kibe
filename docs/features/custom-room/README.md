**English** | [Português](README.pt-BR.md)

# Custom room

A private room an Ultra+ party leader creates for their party, with every rule of the game open to change: teams, generators, shop, upgrades, events, items, knockback and others. A 30-second countdown starts once a second player is in the room; the leader can pause it, start the match at once, restart the room and close it.

## User stories

- As an Ultra+ player, I want to create a room with my own rules, so that my friends and I play the way we like.
- As a party leader, I want the players I invite to come into my room, so that I never have to send them a code.
- As a room leader, I want to save my options and share them with a code, so that I can use them again or give them to someone else.

## How it works

**Creating a room (lobby).**

1. `Sala customizada (/salacustom)` in `Selecionar jogo` (slot 35) or `/salacustom`. A player without `muxe.customroom.create` (Ultra+) gets the Ultra+ shop instead. A party member who is not the leader gets `custom-room-not-leader`.
2. `Selecionar modo` lists the games of the reference (Bed Wars, Duels, Sky Wars, CTF, Party, Block Party, Murder, Build Battle, Quick Builders, Hide and Seek, Nascó Mandou, Spleef). Bed Wars opens `Selecionar modo (Bed Wars)` and Duels opens `Selecionar modo (Duels)`; the others do nothing. A mode works only when a game server publishes it (today the BedWars modes); Mega, `Modos temporários` and every Duels mode do nothing.
3. `Criando sala...`: the mode (`Clique para trocar de jogo.`), `Mapas`, `Opções`, `Criar sala`, `Voltar` and `Predefinições`.
4. `Criar sala` says `Criando sala...`, asks a game server of the game for a room (`custom_room.create_request`) and, once it answers, says `Enviando para <code>...` and sends the leader and the online party members there.

**Maps.** `Selecione mapas (n/m)` lists the ready arenas of the mode. A click selects only that map (or clears it when it was the only one), shift adds or removes it. With several maps, each restart draws one; with none, any arena of the mode. `Carregar mapa externo` does nothing. In the room the change is applied at the next restart.

**Options.** Every option menu is drawn from the schema the game server publishes, with the texts of the reference: values in `&a`, changed shop and upgrade values with `*` and a glowing icon, toggles green and glowing when on, red when off. A decimal or a name opens an anvil with a paper named after the current value; typing changes the result to `&a<option>: &f<value>` and clicking the result applies it and reopens the menu. An invalid value says `Valor inválido.`.

| Menu | What it changes |
|---|---|
| `Opções` | `Tamanho do time` (up to the players of the mode: 8 in Solo, 16 in Duplas...) and the links below. |
| `Times` | `Quantidade de times` (2 to the teams of the mode) and each `Time #N`: left click enables or disables it, right click renames it (anvil), Q restores the name. A renamed team shows `Nome original` and its new name and first letter on the scoreboard, and the first letter is also the prefix of its players in the name tag, the tab list and the chat. |
| `Geradores` | Forge speed, diamond and emerald generators on or off, their speed (0.25x steps) and how many items they drop. |
| `Itens da loja (n/m)` | Every shop item, each tier on its own. `Editando <item>...`: status, price (left: value in an anvil, right: currency iron → gold → emerald → diamond) and limit (right: none → per team → per player → in the inventory; left: amount). `Tudo grátis` makes everything free. |
| `Melhorias (1/1)` | Every team upgrade, plus `Proteção da Cama` (off by default, only in custom rooms). A disabled upgrade shows a barrier. `Editando <upgrade>...`: status and the price and currency of each level. `Tudo grátis`. Long level lines wrap at the last comma that fits in 40 characters, as in the reference (`Nível 2: Madeira, Lã, Pedra do Fim,` / `Grátis`). |
| `Eventos` | Diamante II, Esmeralda II, Diamante III, Esmeralda III, Sem Camas, Deathmatch and Fim do Jogo: the time after the previous event (left +30s, right -30s, shift ±1min) and Q to turn it off (not the game end). |
| `Itens` | `Tipo da Bola de fogo` (`Padrão` or `Fireball Fight`: 1.5x more lift and half the damage) and `Recarga do Estilingue`. |
| `Alterar knockback` | The fourteen knockback values of the mode's [knockback profile](../../development/core-api/README.md#knockback), applied at once to everyone in the room. |
| `Outros` | `Totem` (`Cama` or `Ovo do Dragão`: the egg stands where the bed was and is broken with a left click) and `Criativo` (everyone in creative). |

**Presets.** `Predefinições` lists the presets of the player for the game (PostgreSQL, unlimited). Left click loads one (mode, maps and options), right click shows its code in the chat, Q deletes it. `Salvar novo` asks the name in an anvil; `Usar código` asks a code and loads that preset.

**In the room (game server).**

- The room code shows on the scoreboard as `<code> (C)`. It never appears to matchmaking and only takes players with a seat reserved for it.
- Waiting hotbar: `Selecionar time` (slot 4), the game menu (slot 5), `Menu da sala (/menu)` (slot 8, leader only) and `Voltar ao Lobby` (slot 9).
- `Selecionar time`: each enabled team with `Jogadores: n/size`, and `Espectador`. Players keep their choice until the start; those who chose nobody are spread over the emptiest teams; spectators watch the match.
- `Sua sala` (`/menu` or the item, leader only): the mode (switching restarts a waiting room), `Mapas`, `Opções`, `Iniciar sala` (starts the match at once, the leader may start alone), `Pausar contador` (stops the countdown until `Iniciar sala`; players who arrive afterwards do not start it again), `Reiniciar sala`, `Fechar sala` and `Predefinições`. During the match: `Pular evento` (runs the next event at once), `Reiniciar sala` and `Fechar sala`; the leader also finds `Menu da sala (/menu)` (the `hotbar.room-menu` item) in the first slot of the last row of the BedWars `Compra rápida`, and clicking it opens `Sua sala`.
- A custom match counts nothing: no stats, XP, missions or winstreak.
- The countdown starts at 30 seconds when the second player arrives and stops if the room drops back to one player.
- When the match ends, the room goes back to waiting with the same code and options (`A sala foi reiniciada.`), and the countdown starts again.
- **Proteção da Cama** builds a pyramid around the team's bed (or dragon egg) when bought: level I puts wood next to the bed, team wool around it and team-coloured glass outside; level II uses end stone instead of the glass; level III adds an outer glass layer over the end stone. Each layer is one block further (counting along the axes) from the bed, from the bed's height up. Only air and blocks of the previous level are replaced, and every block can be broken like a placed one. When it is on, it sits in slot 19 of `Melhorias do Time`, which grows from 3 to 4 rows.
- **Items only for custom rooms**: Obsidian works (off by default). Troca Congelante, Ponte de Gelo, Para-quedas, Rebobinar, Spider, Loja Portátil, Baú do Fim and Ressurreição are listed with the reference's text, price and currency, start off and answer "em breve" when bought; they are kept in `pendencias.txt`.

**Party.** A player who joins the leader's party (invited or through `/party abrir`) is pulled into the room; one who leaves the party is sent to the lobby. The room closes (`A sala foi fechada.`, everyone to the lobby) when the leader leaves the room or the server, or stops leading the party.

### Languages

Every menu, option, value and room message follows the reader's [language](../language/README.md).

- The fixed texts of the room menus live in `custom-room.texts` in Muxe-Core's `config.yml` and `lang/en.yml`.
- The schema a game ships to the lobbies may carry a `lang.en` section, at the same paths as the schema. `SchemaReader` lays it over the Portuguese for English readers: page titles, option names, descriptions, value labels and currency names.
- The knockback page reads its texts from `custom-room.knockback` in the game's config.
- Team names typed by the leader are shown as typed.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/salacustom` (`/salacustomizada`, `/criarsala`) | Opens `Selecionar modo` to create a custom room. Lobby servers only. | `muxe.customroom.create` |
| `/menu` (`/sala`) | Opens `Sua sala`. Only the leader of a custom room. | None |

## Permissions

| Permission | Grants |
|---|---|
| `muxe.customroom.create` | Creating custom rooms. Given to `ultra+` by migration V19. |

## Configuration

Muxe-Core `config.yml`, `custom-room`:

| Key | Default | Description |
|---|---|---|
| `selector.*` | the reference | `Selecionar modo`: title, rows, back slot and title, lore and the games (`menu` names their mode menu). |
| `mode-menus.<id>.*` | Bed Wars, Duels | Title, rows, back slot, `game` and the modes (keyed by mode id). |
| `room-menu.*` | the reference | `Criando sala...` and `Sua sala`: slots and items (`skull-texture` for the heads), `playing-slot` of restart and close, `skip-event`. |
| `maps-menu.*`, `presets-menu.*`, `teams-menu.*` | the reference | Slots, items and texts of the maps, presets and team menus. |
| `hotbar.team`, `hotbar.room-menu` | slots 3 and 7 | Waiting items of a custom room. |
| `texts.*` | see `RoomTexts` | Any line of the option menus (`value`, `click-change`, `limit-team`...). |
| `lobby-group` | `lobby` | Group that receives the catalogs. |
| `code-suffix` | ` (C)` | After the room code on the scoreboard. |

Messages (Muxe-Core `messages`): `custom-room-creating`, `custom-room-sending`, `custom-room-unavailable`, `custom-room-not-leader`, `custom-room-invalid-value`, `custom-room-only-leader`, `custom-room-not-in-room`, `custom-room-closed`, `custom-room-restarted`, `custom-room-paused`, `custom-room-team-full`, `custom-room-team-chosen`, `custom-room-spectator-chosen`, `custom-room-event-skipped`, `custom-room-mode-changed`, `custom-room-preset-*`.

BedWars `config.yml`: `shop.categories.<id>.custom-items` and `upgrades.custom-list` hold the items and upgrades that only custom rooms list, and `enabled: false` on an item or upgrade makes it start off. `custom-room`: the fixed pages (`Opções`, `Times`, `Geradores`, `Eventos`, `Itens`, `Outros`), the titles of the generated pages and the diamond currency. The shop pages come from `shop`, the upgrades from `upgrades`, the event times from `forges`/`events` and the knockback page from the mode's profile. BedWars adds the message `purchase-limit`.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `custom_room.catalog` | groups `lobby` and the game's `lobby-group` (BedWars: `bedwars-lobby`) | volatile, 30 s | Every 10 s from each game server: the game, its option schema (YAML in chunks), its modes and ready arenas. |
| `custom_room.create_request` | one game server | 5 s | Leader, game, mode, maps, options and online party members. |
| `custom_room.create_reply` | the lobby | 5 s | Whether the room was created and its code. |

## Data

`core.custom_room_presets` (V19): `id`, `code` (unique, 8 characters), `owner`, `game`, `name`, `mode`, `maps` (comma separated), `options` (`key=value` lines), `created_at`.

## Metrics

None.

## Integrations

- [Party](../party/README.md): the leader, the members pulled in, the room closing when leadership is lost.
- [Minigame API](../../architecture/minigame-api/README.md#custom-rooms): the room on the game server.
- [BedWars](../bedwars/README.md): applies every option.
- [Knockback](../../development/core-api/README.md#knockback): the knockback page and the profile of the room.

## Acceptance criteria

- [ ] An Ultra+ player clicks `Sala customizada`, picks Bed Wars Solo and `Criar sala`; the chat says `Criando sala...` then `Enviando para <code>...` and they arrive in a waiting room with `<code> (C)` on the scoreboard.
- [ ] A player without Ultra+ gets the Ultra+ shop.
- [ ] A second player in the room starts a 30-second countdown; `Pausar contador` stops it and `Iniciar sala` starts the match at once.
- [ ] A party member invited after the room was created is sent into it.
- [ ] The leader leaving closes the room and sends everyone to the lobby.
- [ ] Changing `Vertical` to 0.8 in `Alterar knockback` changes the knockback of everyone in the room.
- [ ] A match ending brings the room back to waiting.
- [ ] Nothing of a custom match reaches the stats.

## Failures and recovery

- No game server publishes the game: the mode does nothing, or `Criar sala` says `custom-room-unavailable`.
- The game server does not answer in 5 s: `custom-room-unavailable`; nothing was created.
- The leader never arrives: the empty room closes after `timings.empty-room-seconds`.
- A game server restarts: its rooms are gone; players go back to a lobby as on any restart.
- Saving a preset fails: `custom-room-preset-failed`.

## Known limitations

- Only BedWars modes can be created; the other games and modes of the menus appear and do nothing.
- `Carregar mapa externo` does nothing.
- Troca Congelante, Ponte de Gelo, Para-quedas, Rebobinar, Spider, Loja Portátil, Baú do Fim, Ressurreição, Torre Compacta, Ovo das Pontes and Rastreador answer "em breve". The reference's default limits of these items are not set.
- The click limiter of the knockback skips pushes that come too soon after the last one; its tick counts are a guess (`knockback.click-limiter-ticks`).
