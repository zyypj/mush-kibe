**English** | [Português](README.pt-BR.md)

# BedWars

BedWars for the Muxe network. Lives in `minigames:bedwars` (`Muxe-BedWars`) and runs on servers of the `bedwars` group.

## User stories

- As a player, I want to protect my team's bed and destroy the others', so that my team is the last one standing.

## How it works

The plugin loads its nine modes, gives staff the [arena setup](../../architecture/game-engine/README.md#arena-setup) and runs the matches of the server, many at once, each in its own [cell](../../decisions/0009-own-map-format-and-match-cells.md) of the match world.

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
| `area-espera`: waiting area, removed when the match starts | no | Pos 1 and Pos 2 | yes |
| `espectador`: spectator spawn | no | where the player stands | yes |
| `void`: void height | no | the player's Y | yes |
| `limite`: highest block players may place | no | the player's Y | no |
| `diamante`, `esmeralda`: generators | no | the block under the player | no, up to 16 each |
| `spawn` | yes | where the player stands | yes |
| `cama`: bed | yes | the block the player looks at | yes |
| `loja`, `melhorias`: shop and upgrades NPCs | yes | where the player stands | yes |
| `ferro`, `ouro`: island generators | yes | the block under the player | at least 1, up to 4 |
| `loja-extra`, `melhorias-extra`, `ferro-extra`, `ouro-extra` | no | as above | 4s only, optional |

### Matches

A player who reaches a BedWars server goes back to the match they left if they still can, and otherwise to a room of `default-mode` (or the one picked with `/bedwars entrar <modo>`): the fullest room waiting for players, or a new room on a random ready arena of that mode.

| Phase | What happens |
|---|---|
| Waiting | Players wait in the waiting area above the map, in adventure, without damage or hunger. The hotbar has the emerald `&aMenu do Bed Wars` (slot 5, does nothing yet) and `&aVoltar ao Lobby` (slot 9). The scoreboard follows annex 2, and annex 3 once the countdown runs. |
| Countdown | The [start countdown](../../architecture/game-engine/README.md#start-countdown) of the engine, with the titles `&a30 segundos`, `&610 segundos` and `&c5` to `&c1`. |
| Playing | Teams are drawn, players spawn at their island with full leather armour in the team colour and a wooden sword. The shop and upgrades NPCs appear with their holograms, and the generators start. Right after, the waiting area between Pos 1 and Pos 2 disappears at once, chunk by chunk (`RegionEraser`). |
| Ending | When one team is left, every player gets the result (annex 11), centred: winner, match time, top final kills and top beds broken, with names in the colour of their tag. After 30 seconds everyone goes to `lobby-group` and the room closes. |

**During the match:**

- **Teams:** the scoreboard follows annex 5 (2 or 4 teams) or annex 12 (8 teams). A team shows `&a✔` while it has a bed and someone connected, `&e✔` when everyone left but can still return, the number of players alive once the bed is gone, and `&c✘` when eliminated. Teams nobody joined are out from the start.
- **Name tags and tab list:** the team letter and colour (`A` in blue), through `MuxeCore.nameTags()`, plus health in the tab list.
- **Chat:** only the room sees it, with the team before the usual format: `[A] medal tag name`.
- **Beds:** breaking an enemy bed sends `CAMA DESTRUÍDA` to everyone and `+100 XP` to the breaker. A team member who was away is eliminated, and the final kill goes to whoever broke the bed. Players cannot break their own bed.
- **Deaths:**
  - The kill goes to the last enemy hit within 15 seconds. A final death with no hit goes to whoever broke the bed.
  - The killer gets the victim's iron, gold, diamonds and emeralds, with `+28 Ferros`-style lines and `+12 XP` (kill) or `+60 XP` (final kill).
  - Falling below the arena's void height kills at once (`morreu no void`).
  - With a bed the player watches as a spectator and respawns after 5 seconds, protected for 4 seconds, with the armour bought and the pickaxe and axe one tier lower. Without a bed the player is out (`KILL FINAL!`), and the team's last player out eliminates the team (`TIME ELIMINADO`).
- **Blocks:** only blocks placed during the match can be broken. Nothing can be placed near spawns (5 blocks), NPCs (1) and generators (1), nor above the arena's build limit (`Você não pode colocar blocos aqui.`). Explosions only break placed blocks, never glass. TNT lights by itself and the fireball is thrown with a right click.
- **Generators:** island iron drops every 0.9 s, as many ingots as players per team (up to 4), gold every 7 s, each with a limit of items lying within 3 blocks. Diamond and emerald show their tier and countdown in a hologram and get faster at 6, 12, 18 and 24 minutes. Iron and gold picked up are copied to teammates within 2 blocks.
- **Events:** after Emerald III, every bed is destroyed (30 min), the sudden death is announced (40 min; the dragons are not in yet) and the match ends (42 min), in a draw if more than one team is left.
- **Disconnecting:** a player who leaves with the bed alive can come back within 3 minutes and respawns 5 seconds later (`reconectou`). `/l` warns `Você desconectou de uma partida...` first. Leaving without a bed eliminates the player, crediting the last hit or the bed breaker.

### Shop

The shop NPC opens the menus of the exports: Compra Rápida (the exported layout) and the seven categories, with the tab of the open category marked by a green pane.

- An item costs iron, gold or emeralds from the inventory. Its name is red when unaffordable, with `Você não tem <recurso> o suficiente.`
- Each purchase says `Você comprou <item>.`
- Wool comes in the team colour. A sword replaces the wooden sword. Armour gives boots and leggings and cannot be bought below what the player owns. The pickaxe and axe go up one tier per purchase. Shears are permanent.
- Not working yet: shift-click (editing the quick buy), the hotbar editor, and the special items Percevejo, Golem de Ferro, Leite Mágico, Torre Compacta, Ovo das Pontes, Rastreador and Estilingue, which say they are coming soon and charge nothing.

### Team upgrades

The upgrades NPC opens `Melhorias do Time` with the seven exported upgrades, paid with diamonds:

| Upgrade | Effect |
|---|---|
| Afiação nas Espadas I–II | Sharpness on every sword and axe of the team. |
| Reforço na Armadura I–IV | Protection on every armour piece. |
| Sem tempo Irmão I–II | Permanent Haste. |
| Forja I–IV | Island iron and gold 1.5×, 2×, an island emerald generator, then 3× and double emeralds. |
| Regeneração na Ilha | Regeneration for the team within 17 blocks of its spawn. |
| É uma Cilada! | The next enemy within 17 blocks of a team with a bed gets Blindness and Slowness for 8 s. Used once. |
| Fadiga I–II | The next enemy gets Mining Fatigue for 7 or 10 s. Used once. |

The whole team sees `<player> adquiriu a melhoria <upgrade>.`

### Bed destruction cosmetics

Players pick an animation that plays when they break an enemy bed. The menus follow the recorded exports:

- **Menu do Bed Wars** (emerald): in the waiting room on BedWars servers, and in hotbar slot 3 on the BedWars lobby. Only **Cosméticos** works for now.
- **Cosméticos do Bed Wars**: the **Destruição de Cama** category shows `owned/total (percent%)` and the selected cosmetic. The other categories, **Cor preferida** and the **Loja** button answer "coming soon".
- **Destruição de Cama (page/pages)**: 28 entries per page. Page 1 starts with **Nenhuma** and **Aleatório** (the default). Owned cosmetics come first, then the ones for sale, then the unavailable ones, each group in configuration order.
  - Owned: left click selects, right click previews. The selected one glows and shows `Já selecionado.`
  - For sale: left click buys with credits (price by stars: 750, 900, 1.300, 1.750, 2.000), right click previews. When the player lacks credits, the lore shows `Faltam <n> créditos!`.
  - Unavailable (event cosmetics the player does not own, or cosmetics without an animation yet): right click previews when there is an animation.
  - `Incluso no ULTRA` cosmetics are owned by everyone with `muxe.cosmetic.ultra`.
- 30 cosmetics are listed; 13 have an animation (Ajudante de Papai Noel, Avalanche, Coelhos, Passarinhos, Friaca, Fogos de Artifício, Colheita de Abóboras, Apavorante, Deserto, Tinta, Bola de Canhão, Enderman, Sal na Ferida). The other 17 show `Indisponível.` until their animation exists.
- Animations are drawn only with packets (armor stands, falling blocks, items, fireworks, particles, sounds and fake block changes), so the real world never changes. They are our own reproductions of the recorded behaviour, with original pixel sprites and community heads.

**In a match** the breaker's selection plays at the broken bed for everyone in the room. `Aleatório` picks one of the player's owned animations; `Nenhuma` plays nothing.

**Preview** (BedWars lobby only; anywhere else the player gets `Você só pode visualizar cosméticos no lobby.`):

1. The player is saved (place, inventory, armor, game mode, scoreboard) and teleported to the preview world, with an empty inventory.
2. Other previewers are hidden both ways. The scoreboard shows `Visualizando:` and the cosmetic name, and the action bar repeats `&eQuebre a cama!`.
3. Left-clicking the bed hides it for that player only and plays the animation for that player only.
4. `preview.return-delay` after the animation ends (2 s), or after `preview.timeout` without breaking the bed (2 min), the player goes back to where they were, with their lobby items.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/arena ...` | `muxe.minigame.setup` | Creates and sets up arenas. See [arena setup](../../architecture/game-engine/README.md#arena-setup). |
| `/bedwars entrar <modo>` (`/bw`) | none | Moves the player to a room of that mode, while waiting. |
| `/bedwars iniciar` | `muxe.bedwars.admin` | Starts the countdown at 10 seconds without the minimum players, for tests. |
| `/cosmeticos` (`/cosmeticosbw`) | none | Opens Cosméticos do Bed Wars. |
| `/cosmeticos dar <jogador> <cosmetico>` | `muxe.cosmetics.admin` | Gives a bed destruction cosmetic, for event and award cosmetics. |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `muxe.minigame.setup` | op | Allows `/arena`. |
| `muxe.bedwars.admin` | op | Allows `/bedwars iniciar`. |
| `muxe.cosmetics.admin` | op | Allows `/cosmeticos dar`. |
| `muxe.cosmetic.ultra` | false | Owns the `Incluso no ULTRA` cosmetics. The `ultra` group has it (migration V14). |

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
| `lobby-group` | `lobby` | Where players go when a match ends or they leave the waiting room. |
| `default-mode` | `solo` | The mode of the room a player joins on arrival. |
| `timings.*` | 5 s respawn, 180 s rejoin, 30 s restart, 4 s protection, island radius 17, protection radii 5/1/1 | Match timings and protected areas. |
| `countdown.titles` | `30`, `10`, `5`...`1` | Titles of the start countdown. |
| `forges.*` | the spec values | Island iron, gold and emerald, and the three diamond and emerald tiers with their start second. |
| `events.*` | 1800, 2400, 2520 | Seconds of the beds destroyed, sudden death and game end events, and their names on the scoreboard. |
| `xp-rewards.*` | 10, 5, 250, 100, 12, 60 | XP per minute, per teammate, win, bed, kill and final kill. |
| `hotbar.waiting.*` | emerald and bed | Waiting room items. |
| `scoreboard.*` | annexes 2, 3, 5 and 12 | Scoreboard lines; `{teams}` and `{event}` become the team lines and the next event. |
| `holograms.*` | annexes 7 and 8 | NPC and generator holograms. |
| `shop.*`, `upgrades.*` | the exports | Shop categories, items, prices and texts; upgrades, tiers and prices. |
| `messages.*` | annexes 4, 6, 9, 10, 11, 13, 14, 15 | Every match message. `setup-*` overrides the engine's setup messages. `cosmetic-*` and `preview-*` are the cosmetic messages. |
| `cosmetics.category` | `bedwars-cama` | Category under which ownership and selection are saved. |
| `cosmetics.prices.<stars>` | 750, 900, 1300, 1750, 2000 | Price in credits by stars. |
| `cosmetics.bed-destruction.<id>` | the 30 cosmetics | `name`, `icon` (item), `stars`, `tag`, `availability` (`SALE`, `ULTRA`, `EVENT`) and `description`. The id must match an animation id to have an animation. |
| `cosmetics.menus.*` | the exports | Titles, slots, items and lore lines of the three menus. |
| `preview.world` | `previa_quebra_de_cama` | Folder of the preview world in the server root. Without it the preview is off. |
| `preview.spawn`, `preview.bed` | 6.5 11 21.5; foot 6 11 17, head 6 16 | Where the player stands and the bed they break. |
| `preview.timeout`, `preview.return-delay` | `120s`, `2s` | Time to break the bed and wait after the animation. |
| `preview.action-bar`, `preview.scoreboard` | `&eQuebre a cama!`, MUXE / Visualizando | Preview texts; `{name}` is the cosmetic. |
| `lobby.menu-item` | slot 3, emerald | Menu do Bed Wars item on the BedWars lobby. |

## Packets

None yet.

## Data

| File | What it holds |
|---|---|
| `plugins/Muxe-BedWars/maps/<arena>.mxw` | The map, in the [Muxe map format](../../decisions/0009-own-map-format-and-match-cells.md). Copied to every BedWars server. |
| `plugins/Muxe-BedWars/arenas/<arena>.yml` | Mode, display name, teams, points and heights, with `ready: true` when nothing is missing. Copied with the map. |
| `core.owned_cosmetics`, `core.selected_cosmetics` | Owned and selected cosmetics, category `bedwars-cama`. See [Credits and cosmetics](../credits/README.md). |
| `<server>/previa_quebra_de_cama/` | The preview world. Deployed to the BedWars lobby only, never committed. |

## Metrics

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrations

- [Lobby](../lobby/README.md): `/jogar bedwars`.
- [Credits and cosmetics](../credits/README.md): prices, purchases and ownership.
- [Game engine](../../architecture/game-engine/README.md).

## Acceptance criteria

- [x] Modes are loaded from configuration (engine tests).
- [x] The nine modes have the limits and teams of the spec (`BedWarsSchemaTest`).
- [x] An arena can be set up in game, with a checklist of what is missing (engine tests).
- [x] Scoreboards, team indicators and the forge, timeline, kill credit, level, XP and ranking rules follow the spec (`RoomTest`, `GameRulesTest`, `GeneratorTimerTest`).
- [x] Shop and upgrade names and lore follow the exports (`ShopCatalogTest`, `UpgradeCatalogTest`).
- [x] The 13 animations end without leftovers and follow the recorded timings (`BedAnimationsTest`).
- [x] Cosmetic states, prices, sorting and lore follow the exports (`CosmeticCatalogTest`).
- [ ] A full match checked in game.
- [ ] Every animation and the preview checked in game.

## Failures and recovery

If Muxe-Core is not enabled, the plugin disables itself.

## Known limitations

- Rooms only exist on the server a player reaches; the BedWars lobby, mode NPCs, map choice and `/retornar` from the lobby are not in yet, so a player picks a mode with `/bedwars entrar`.
- Statistics, levels and XP are shown but not saved.
- The special shop items, the quick buy editor, the dragons, the spinning blocks above the generators, and the victory and defeat titles are not in yet.
- A restart or crash ends every match on the server without compensation.
- 17 bed destruction cosmetics have no animation yet. The other cosmetic categories, Cor preferida and the cosmetic shop are not in yet.
- A purchase updates the menu at once, but another server sees it only when the player joins it again.
