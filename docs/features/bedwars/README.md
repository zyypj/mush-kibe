**English** | [Português](README.pt-BR.md)

# BedWars

BedWars for the Muxe network. Lives in `minigames:bedwars` (`Muxe-BedWars`) and runs on servers of the `bedwars` group.

## User stories

- As a player, I want to protect my team's bed and destroy the others', so that my team is the last one standing.

## How it works

The plugin loads its nine modes, gives staff the [arena setup](../../architecture/game-engine/README.md#arena-setup) and runs the matches of the server, many at once, each in its own [cell](../../decisions/0009-own-map-format-and-match-cells.md) of the match world. The match lifecycle, joining and leaving, deaths, respawns, spectators, the scoreboard, titles, chat and matchmaking come from the [minigame API](../../architecture/minigame-api/README.md); BedWars adds the beds, forges, shop, upgrades, traps and its other rules on top (`BedWarsGame`, `BedWarsMatch`, `BedWarsHandlers`).

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

Arenas are set up on the BedWars lobby, not on the match servers, and saved in PostgreSQL: every BedWars server loads them on start and reloads one when the lobby saves it (`minigame.arena_saved`). To add a map, upload its world folder to the root of a BedWars lobby server and run `/arena criar <mode>_<map>` there. Editing puts the staff member in build mode (so the lobby lets them fly in creative and break and place blocks) and `/arena sair` takes them out.

An arena is named `<mode>_<map>`, such as `1v1_slip` or `quartetos_air_nation`. `/arena criar 1v1_slip` converts a world folder named `1v1_slip` or opens an empty world. The folder name must match the arena name exactly.

Mark each team's `spawn` first: it also marks the nearest bed within 20 blocks that no other team has. After that, the other team steps (bed, shop, upgrades, iron, gold) are one line each in the checklist and go to the team whose spawn is nearest.

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
| Waiting | Players wait in the waiting area above the map, in survival like the match, without damage or hunger and without breaking or placing blocks. The hotbar has the emerald `&aMenu do Bed Wars` (slot 5, opens the menu) and `&aVoltar ao Lobby` (slot 9). The scoreboard follows annex 2, and annex 3 once the countdown runs. A room nobody is in and nobody is coming to (no pending reservation) closes after `timings.empty-room-seconds` (60) and frees its cell. |
| Countdown | The [start countdown](../../architecture/game-engine/README.md#start-countdown) of the engine, with the titles `&a30 segundos`, `&610 segundos` and `&c5` to `&c1`. Each title plays a button click (`sounds.countdown`) and says `O jogo inicia em <n> segundos!` in the chat (`segundo` on the last one). |
| Playing | Teams are drawn, players spawn at their island with full leather armour in the team colour and a wooden sword. The shop and upgrades NPCs appear with their holograms, and the generators start. A team that got no players loses its bed (removed when found within 2 blocks of the marked point), but its island keeps the generators and the shop and upgrades NPCs, which anyone can use. Right after, the waiting area between Pos 1 and Pos 2 disappears at once, chunk by chunk (`RegionEraser`). |
| Ending | When one team is left, every player gets the result (annex 11), centred: winner, match time, top final kills and top beds broken, with names in the colour of their tag. The winning team sees the `VITÓRIA` title and everyone else in the room, eliminated players included, sees `DERROTA` (`end-titles.*`): the word appears white, a gold (red for the defeat) sweep goes over it letter by letter every 2 ticks, then it blinks between the colour and white every 7 ticks for about 6 seconds; the victory plays the dragon growl. A draw shows no title. After 30 seconds everyone goes to `lobby-group` and the room closes. |

**During the match:**

- **Teams:** the scoreboard follows annex 5 (2 or 4 teams) or annex 12 (8 teams). A team shows `&a✔` while it has a bed and someone connected, `&e✔` when everyone left but can still return, the number of players alive once the bed is gone, and `&c✘` when eliminated. Teams nobody joined are out from the start.
- **Joining and leaving the room:** the server's own join and quit messages are hidden. Entering the waiting room tells the room `tadeu entrou na sala (3/8).` (`messages.room-joined`), and leaving it before the match starts sends `tadeu saiu da sala (2/8).` (`messages.room-left`, with the players left), with the name in the colour of the player's tag.
- **Level:** from the waiting room on, the XP bar shows the BedWars level as the XP level and the progress to the next level as the bar, counting the XP earned in the match. It is refreshed every second, and the end-of-match XP is added when the match ends.
- **Level up:** reaching a new level, in the match or in the lobby, sends `&b&lPARABÉNS &7Você subiu para o nível [904⚝]&7!` with the badge of the new level (`messages.level-up`), through the engine's [level up](../../architecture/game-engine/README.md#level-up).
- **Name tags and tab list:** the team letter in bold and colour (`A` in blue), through `MuxeCore.nameTags()`. The scoreboard and the chat show the letter without bold. The tab list shows the health of players alive as a yellow number to the right of the name.
- **Health below the name:** players alive show their health under the name (`20 ❤`, the heart from `messages.health-below-name`), through the engine's [health below the name](../../architecture/game-engine/README.md#health-below-the-name). Spectators and players waiting to respawn show nothing.
- **Chat:** only the room sees it, with the team before the usual format: `[A] medal tag name`. In solo modes everyone in the room reads it. In team modes, while the match is played, the chat reaches only the player's team; `/g <mensagem>` talks to the whole room with `&6[G]` before it (`messages.global-prefix`). Every message starts with the sender's level badge (`[904⚝]`), also in the BedWars lobby; a player who turned off `Mostrar nível no chat` shows `[0✫]` to everyone. The chat runs off the main thread, so it reads a snapshot of each room (who hears whom and the team prefix) rebuilt on the main thread every second and whenever someone joins or leaves.
- **Beds:** breaking an enemy bed sends `CAMA DESTRUÍDA` (in the breaker's [kill message set](#kill-messages)) to everyone and `+100 XP` to the breaker. A team member who was away is eliminated, and the final kill goes to whoever broke the bed. Players cannot break their own bed.
- **Deaths:**
  - The kill goes to the last enemy hit within 15 seconds. A final death with no hit goes to whoever broke the bed.
  - The killer gets the victim's iron, gold, diamonds and emeralds, with `+28 Ferros`-style lines and `+12 XP` (kill) or `+60 XP` (final kill).
  - Falling below the arena's void height kills at once (`morreu no void`). The kill, death and final kill texts come from the [kill message sets](#kill-messages), `Padrão` unless the player picked another.
  - The player is not teleported on death: they are thrown up where they died (1.5 blocks per tick) and float there as a spectator; a player who fell into the void goes back up to the height of their team spawn. With a bed the title `VOCÊ MORREU` stays on screen with `Renascendo em <n> segundos.` under it, only the number changing each second, the same line goes to the chat, the death plays `ZOMBIE_UNFECT` at pitch 2 (`sounds.died`), and the player respawns after 5 seconds (`Você renasceu!` in the chat and as the subtitle), protected for 3 seconds, with the armour bought and the pickaxe and axe one tier lower. Without a bed the player is out (`KILL FINAL!`), and the team's last player out eliminates the team (`TIME ELIMINADO`).
- **Custom room:** a [custom room](../custom-room/README.md) applies its options at the start of each match: forge and generator speeds and drops, diamond and emerald generators off, shop item status, price, currency and purchase limits (`purchase-limit`), upgrade status and prices in any currency, event times and skipping, the fireball type, the slingshot cooldown, the dragon egg totem (broken with a left click) and creative mode. Renamed teams appear in the bed and chest messages. Nothing of a custom match counts for stats, XP or missions.
- **Knockback:** hits between players use the `bedwars` profile (the values of the reference room menu), applied when the player joins the room.
- **Blocks:** only blocks placed during the match can be broken, plus the map's grass and bushes (tall grass, double plants and dead bushes), which drop nothing; trying to break another block says `Você não pode quebrar este bloco.`. Nothing can be placed near spawns (5 blocks), NPCs (1) and generators (1) (`timings.spawn-protection-radius`, `npc-protection-radius`, `generator-protection-radius`), nor above the arena's build limit (`Você não pode colocar blocos aqui.`). Explosions only break placed blocks, never glass, and the blocks they break may drop. TNT lights by itself, waits 60 ticks (3 s) and explodes with strength 3. The fireball is thrown with a right click from the player's eyes, starts still and speeds up by 0.1 block per tick along the look (vanilla drag, up to about 1.9 blocks per tick), and explodes with strength 2. The thrower's own TNT or fireball never hurts them: it plays the hurt animation and launches them away from the blast, the fireball by 3.125 along the line from the blast to their feet, the TNT by 1.875 along the ground, both with 0.7 up (1.0 when already in the air) (`ExplosionKnockback`). Other players take the vanilla damage and push.
- **Slingshot:** right click with the `Estilingue` (magma cream, bought in the shop for 7 gold) spends one and launches the player forward: 1.5625 times the look on x and z, 1.0 up. It shows the slime block break effect and plays `mob.slime.small` four times, 4 ticks apart, at pitches 1.25, 1.5, 1.75 and 2.0. Then it waits 7 seconds, shown in the action bar as `ESTILINGUE: 7,0s` in red, the underlined part of the name shrinking as the time runs out; using it meanwhile does nothing. Fall damage is not cancelled (`Slingshot`).
- **Spectators:** dead and eliminated players are not in the spectator game mode: they stay in adventure, flying and invisible, hidden from the players still in the game (spectators see each other), and arrows and fireballs pass through them. Players waiting to respawn get nothing; eliminated players get `Teleportador` (compass, slot 0, opens a menu with the head of every player alive; a click teleports there), `Teleportar: Jogador aleatório` (iron sword, slot 3), `Teleportar: Jogador em combate` (diamond sword, slot 4, a player alive who hit or was hit in the last 5 seconds), `Jogar novamente` (paper, slot 7) and `Voltar ao Lobby` (bed, slot 8) (`hotbar.spectator`, `hotbar.teleporter`). With nobody to go to they say `Nenhum jogador para teleportar.` or `Nenhum jogador em combate agora.`. Spectators cannot drop, move or use items, open chests or shops, break or place blocks.
- **Match end:** the winners get `Jogar novamente` and `Voltar ao Lobby` in slots 7 and 8, over whatever was there; the rest of the inventory stays. Spectators keep or get the spectator items. `Jogar novamente` opens the same mode menu as the NPC of the BedWars lobby, for the mode just played; the room list comes from every match server, and a room on the same server is joined directly. `Voltar ao Lobby` sends the player to the lobby.
- **Generators:** island iron drops every 0.9 s, as many ingots as players per team (up to 4), gold every 7 s, each with a limit of items lying within 3 blocks. Diamond and emerald show their tier and countdown in a hologram and get faster at 6, 12, 18 and 24 minutes. Iron and gold picked up are copied to teammates within 2 blocks, who hear the pickup sound.
- **AFK:** a player who does not move, click or chat for 45 seconds is AFK: they cannot pick up generator items and get nothing from a teammate's pickup, until they act again.
- **Iron floor:** when the match starts, each island iron generator looks for a slab floor under it (from one block above to two below, with nothing solid on top). A 3x3 floor drops the iron in a plus (front, sides, centre, back); a 2x3 floor, whose generator point is the front centre, cycles through its front row and middle so the back corners stay empty. The front is the side that faces the team spawn; a 2x3 floor facing another way is also accepted. Each ingot lands on the next cell, so 4 ingots in one drop land on 4 cells. Without a 2x3 or 3x3 floor the iron drops on the point and a warning is logged. Gold always drops on the point.
- **Drops:** items fall 1.3 blocks above the point, without speed and with no pickup delay. With `forges.stack-items: false` each cell's items are marked so they do not merge with another cell's. Picking one up collects every item of the same kind within half a block at once, with the marker removed.
- **Diamond and emerald:** a diamond or emerald block spins above each generator (6° per tick, a full turn every 3 s, bobbing 0.08 block), with the three hologram lines above it, 0.3 apart.
- **Events:** after Emerald III, every bed is destroyed (30 min), the sudden death is announced (40 min; the dragons are not in yet) and the match ends (42 min), in a draw if more than one team is left.
- **Start:** when the countdown ends every player is placed on the island, hears `NOTE_PLING` at pitch 2 (`sounds.match-start`) and the chat gets `O jogo iniciou!` and the `AVISO:` that alliances with other players are forbidden and punished (`messages.match-started`).
- **Combat:** players of different teams can always hit each other; the match world turns PvP on by itself, whatever `pvp` says in `server.properties`. Teammates cannot hurt each other and spectators, eliminated players and anyone outside the match cannot hit anyone.
- **Spawn protection:** every player is protected for `timings.respawn-protection-seconds` (3 by default) when the match starts and after each respawn. A protected player takes no damage. The protection ends early as soon as the player hits an enemy, even one who is protected too.
- **Chests:** a chest within the island radius (17 blocks) of a team spawn belongs to the closest team. Only that team opens it while the team is in the match; once the team is eliminated anyone can (`Este baú é do Time ...`). Chests away from the islands are open to everyone. Spectators open no chest.
- **Hit to store:** the island chests and the ender chests show `&eBata para` / `&eguardar!` (`messages.chest-hologram`) to whoever may use them: every ender chest to every player alive, an island chest to its team (or to everyone once that team is out). Hitting one of them with an item stores the whole stack (what fits) and says `Você guardou: <item> (<n>x) em seu baú.`; an ender chest stores in the player's own `Baú do Fim`. A chest the player cannot open says `Este baú é do Time ...` and keeps the item. Pickaxes, axes, shears and the wooden sword cannot be stored (`Você não pode guardar este item.`, `messages.chest-kept`) nor dropped, and the only sword in the inventory cannot be stored either (`Você não pode guardar só um deste item.`, `messages.chest-last-one`). The same rules apply when moving items into an island chest or the `Baú do Fim` by clicking, shift-clicking, using the number keys or dragging. The `Guardar itens clicando no baú` preference turns the holograms or the whole feature off.
- **TNT timer:** a lit TNT shows the seconds left above it, updated every tick (`2,95s`, two decimals with a comma), dark green, green, yellow and red as the fuse burns (from 90%, 60% and 30% left; `tnt-timer.*`), to players within 24 blocks who kept `Mostrar temporizador da TNT` on.
- **Item names:** every item the match hands out has its Portuguese name (`Barra de Ferro`, `Bola de Fogo`, `Estilingue`, `Espada de Madeira`...), from `item-names` or, for shop items, the shop name.
- **Ender chest:** every ender chest opens the player's own `Baú do Fim`, the same one from any ender chest of the map, kept between deaths. When the player is eliminated, its items drop on the team's first island iron generator (or the spawn when there is none).
- **Disconnecting:** a player who leaves with the bed alive can come back within 3 minutes and respawns 5 seconds later (`reconectou`). `/l` takes the player to the BedWars lobby and warns `Você desconectou de uma partida...` first. `/retornar`, on any server, sends the player back to that match: the match server keeps `match:return:<player>` in `MuxeCore.store()` while the player can return and deletes it when they come back, lose the bed, run out of time or the room closes.
- **Invisibility:** the invisibility potion shows no particles. While it lasts, enemies and spectators see no armour on the player (the real armour stays on, so hits are reduced as usual), no name and only the footsteps the player leaves on the ground, every 0.8 blocks walked. Teammates see the player half transparent, with the armour. Leaving without a bed eliminates the player, crediting the last hit or the bed breaker.

### Favourite team colour

Each player can pick a favourite team colour in `Cosméticos do Bed Wars` (button `Cor preferida`, slot 35, which opens `Cor preferida (1/1)`). The menu has `Nenhuma` (barrier, slot 10) and one wool per team colour, in this order: `Vermelho`, `Azul`, `Verde`, `Amarelo`, `Ciano`, `Branco` (slots 11 to 16), `Laranja`, `Rosa` and `Cinza` (slots 19 to 21). Every option says `Clique para selecionar.`; the chosen one (`Nenhuma` while nothing is chosen) says `Selecionada!` and glows. Clicking an option saves it, says `Você selecionou <nome>.` (`messages.cosmetic-selected`) and refreshes the menu; clicking the one already chosen does nothing. `Voltar` (slot 48) goes back to the cosmetics menu. The button is open to everyone for now, although its lore still says `Exclusivo para ULTRA`.

The choice is saved in PostgreSQL (`bedwars.favourite_team_color`, one row per player; choosing `Nenhuma` deletes the row) and loaded into memory when the player joins a BedWars server, so the room never waits for the database when a match starts.

When the match starts, `TeamAssigner` puts players on their favourite team when it can:

1. Parties first, as always, and a party is never split. If most of the party's members prefer the same colour (the first colour wins a tie) and that team of the arena is still empty, the whole party takes it; otherwise it goes to the emptiest team. A party larger than a team starts on its preferred team and spills into the others.
2. Then the players without a party who have a favourite colour, colour by colour: those whose colour is a team of the arena with seats left take them. When more players want a colour than it has seats (a team of 1 in solo, 2 in doubles, 4 in quads), a random draw picks who gets the seats and the others go to step 3. In team modes, players without a party who prefer the same colour therefore play together.
3. Everyone else goes to the team with the fewest players, in join order, as before.

A colour the arena does not have, or a player whose preference could not be loaded, is treated as no preference. With nobody having a preference, the teams are exactly the ones of the join order.

### Shop

The shop NPC opens the menus of the exports: Compra Rápida (the exported layout) and the seven categories, with the tab of the open category marked by a green pane.

- An item costs iron, gold or emeralds, taken first from the inventory, then from the player's `Baú do Fim` and then from the team's island chests, so 5 gold in hand and 7 in the chest buy a 12 gold armour. Upgrades cost diamonds from the same places plus the inventories of the teammates alive; no other resource is shared. The name is red when not even all of that is enough, with `Você não tem <recurso> o suficiente.`
- Trying to buy what the player already has and cannot buy again (armour already worn, the shears, a tool at its last tier) says `Você já tem este item.` (`messages.already-have`). Blocks, swords and the other items can be bought again.
- Each purchase says `Você comprou <item>.` and plays `NOTE_PLING` (`sounds.bought`); buying an upgrade plays `LEVEL_UP` at pitch 2 (`sounds.upgrade`); storing the held item by punching the team chest plays `CHICKEN_EGG_POP` at pitch 1.5 (`sounds.deposited`); breaking a bed plays `ENDERDRAGON_GROWL` to the breaker (`sounds.bed-destroyed`) and `WITHER_DEATH` to every player of the team that lost it (`sounds.bed-lost`); a kill plays `NOTE_PLING` at pitch 2 to the killer (`sounds.kill`, `sounds.final-kill` for a final kill) and `ZOMBIE_UNFECT` at pitch 2 to the eliminated player (`sounds.eliminated`); a team out plays `EXPLODE` to the whole room (`sounds.team-eliminated`); the end of the match, won or lost, plays `ENDERDRAGON_GROWL` and `FIREWORK_LAUNCH` to the whole room (`sounds.match-end`, `sounds.match-end-firework`); a purchase without enough resources plays `VILLAGER_NO` (`sounds.cannot-afford`).
- **Quick buy:** each player has their own, saved in PostgreSQL (`bedwars.quick_buy`) and loaded when they join a BedWars server, so a change made in one match is there in the next. A player who never changed it gets `shop.quick-buy`. It has 21 positions; an empty one shows `&cEspaço vazio!`.
  - Shift-click on an item already in the quick buy removes it, from the Compra Rápida page or from its category.
  - Shift-click on any other item opens `Adicionando à Compra Rápida...`: the item on top and the 21 positions below, each with `Clique para substituir!` or `Espaço vazio! Clique para adicionar!`. Clicking a position puts the item there (and takes it out of where it was) and opens Compra Rápida again.
  - Items the shop no longer sells show as empty positions.
- Wool comes in the team colour. A sword replaces the wooden sword. Armour gives boots and leggings and cannot be bought below what the player owns. The pickaxe and axe go up one tier per purchase. The pickaxe has five tiers: wood (Efficiency I, 10 iron), stone (Efficiency I, 10 iron), iron (Efficiency II, 10 iron), gold (Efficiency III, 3 gold) and diamond (Efficiency III, 6 gold). Shears are permanent. What the player already has shows `Já adquirido.`.
- **Hotbar editor:** each player can say where bought items go in the hotbar, saved in PostgreSQL (`bedwars.hotbar_layout`) and loaded when they join a BedWars server. It is opened by `Editor de hotbar` in the Menu do Bed Wars (slot 31) and by the `&aEditor de hotbar` button of the Compra Rápida page of the shop (slot 53, `shop.hotbar-editor`); `Voltar` returns to where it was opened from. A player who never edited it has an empty layout and nothing changes: bought items are added to the first free slot, as before.
  - `Editando hotbar...` (3 rows) shows the 9 hotbar slots at 9 to 17 as `&aSlot #N`. A slot with entries shows the icon of one of them and lists them all under `Selecionado:` or `Selecionados:`, the one shown in green; with more than one entry, the icon and the green line move to the next entry every second (`shop.hotbar-menus.frame-ticks`, 20). An empty slot is a red pane. Left click adds, right click removes.
  - `Adicionado no slot #N...` (6 rows) has the tabs `Categorias` and `Itens` at slots 0 and 1, the open one marked by a green pane under it. Categories lists every shop category except `armadura`; Itens lists every non-armour item with its shop look, 21 per page (`Página N` arrows at 48 and 50, the previous-page arrow replacing `Voltar`). Clicking one adds it to the end of the slot, saves and returns to the editor. An entry is in one slot only: adding it to another slot moves it. A slot holds at most 14 entries; a full slot answers `messages.hotbar-slot-full`.
  - `Removendo do slot #N...` (4 rows) lists the entries of the slot from slot 10. Clicking one removes it and saves; the menu stays open while entries remain and returns to the editor when the last one is gone.
  - **Placement rule:** when a purchase delivers an item, the slots are checked from #1 to #9; the first slot that lists the item or a category it belongs to and is empty, or holds a stack of the same item with room for the whole purchase, receives it. If there is none, the item is added as before. Armour is not affected. The rule is `HotbarPlacement` in `br.com.muxe.minigame.bedwars.hotbar.domain`; `HotbarDelivery` applies it to the inventory. The "inventory is full" check of a purchase is unchanged.
  - Entries the shop no longer sells are dropped when the layout is loaded.
- Not working yet: the special items Golem de Ferro, Leite Mágico, Torre Compacta, Ovo das Pontes, and Rastreador, which say they are coming soon and charge nothing.

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

- **Menu do Bed Wars** (emerald): in the waiting room on BedWars servers, and in hotbar slot 3 on the BedWars lobby. Five rows, as in the reference export: `Missões` (slot 11), `Cosméticos` (pufferfish, slot 13), `Lista de Níveis` (slot 15, the level badges in its lore), `Compra rápida` (29), `Editor de hotbar` (31) and `Preferências` (33). `Editor de hotbar` opens the hotbar editor described in the Shop section.
- **Preferências** (slot 33) opens `Preferências do Bed Wars` (4 rows): `Mostrar nível no chat` (slot 11), `Mostrar temporizador da TNT` (13) and `Guardar itens clicando no baú` (15: `Ativado`, `Sem hologramas`, `Desativado`), each with a dye below showing its value (lime on, grey off, light blue without holograms). Clicking either switches the value; `Voltar` goes back to the Bed Wars menu. They are saved in `bedwars.preferences` and loaded on every BedWars server.
- **Compra rápida** (slot 29 of the Bed Wars menu) opens `Editando compra rápida...` with the player's 21 positions. Left click swaps the item, right click removes it, `Voltar` goes back to the Bed Wars menu and `Importar compra rápida` opens an anvil with `Insira o usuário`: the nick typed and taken from the result slot opens `Importando compra rápida...` with that player's quick buy (the default one if they never changed it, every position empty if there is no such player), `Cancelar` (back to the editor) and `Confirmar`, which copies it into the player's own. Swapping opens `Selecionando item... (1/2)`, every item the shop sells in category order, 21 per page; clicking one puts it in that position. Pickaxe and axe show as `Picareta` and `Machado` in diamond, and `editor-name`, `editor-material` and `editor-description` of a shop item change how it looks here. Changes are saved like those made in a match.
- **Cosméticos do Bed Wars**: the **Destruição de Cama** category shows `owned/total (percent%)` and the selected cosmetic. **Cor preferida** (slot 35) opens the favourite team colour menu described in Favourite team colour. **Tipos de madeira** and **Mensagens de kill** (slots 29 and 31) open the lists described in Wood types and Kill messages. The other categories and the **Loja** button answer "coming soon".
- **Destruição de Cama (page/pages)**: 28 entries per page. Page 1 starts with **Nenhuma** and **Aleatório** (the default). The **Aleatório** icon cycles every 10 ticks through the icons of the cosmetics it can draw (the owned ones with an animation), keeping its name and lore; with none it keeps its own icon. Owned cosmetics come first in alphabetical order, then all the others in alphabetical order (`CosmeticOrder`, the rule for every cosmetics menu of the network).
  - Owned: left click selects, right click previews. The selected one glows and shows `Já selecionado.`
  - For sale: left click buys with credits (price by stars: 750, 900, 1.300, 1.750, 2.000), right click previews. When the player lacks credits, the lore shows `Faltam <n> créditos!`.
  - Unavailable (event cosmetics the player does not own, or cosmetics without an animation yet): right click previews when there is an animation.
  - `Incluso no ULTRA` cosmetics are owned by everyone with `muxe.cosmetic.ultra`.
  - Players with `muxe.cosmetic.all` (ops by default) own every cosmetic. The ones without an animation still show `Indisponível`.
  - In the preview, hitting the bed takes it out of the preview world for real (so the viewer can walk where it was without getting stuck) until the last player who broke it goes back; meanwhile everyone else in the preview still sees it as a fake block through a `BlockMask`, and can break it too.
- 31 cosmetics are listed and all of them have an animation (Ajudante de Papai Noel, Avalanche, Coelhos, Passarinhos, Friaca, Fogos de Artifício, Colheita de Abóboras, Apavorante, Deserto, Tinta, Bola de Canhão, Enderman, Sal na Ferida, Espiral de Coelhos, Six Seven, Ghor, Raio, Tesouro Encontrado, Garota Mágica, Quarto Abandonado, Tornado, Tubarão, Vulcânica, Carro, Anoitecer, Assombrado, Balões, Chuva de Bolos, Encantado, Explosão da Vitória, Kraken). In Espiral de Coelhos the bed and a jack o'lantern rise faster and faster while three rabbits circle them two blocks away, leaving clouds and dropping easter egg heads that fall to the ground. In Six Seven a bold `6` and `7` float above the bouncing bed and seesaw faster and faster, each beat with a note, until they meet a `67`, blow up in a white firework and rain fourteen `6` and `7` on the island. In Ghor a purple rift opens five blocks past the bed head, facing it, and a giant eye made of a 3×3×3 cube of heads comes out of it, drooling lava, staring at the bed, then glaring at the breaker and turning back hungry to the bed with a growl (the nine front heads swap skins for each face). Four tentacles of eighteen heads shoot out one after the other and grip the corners of the bed, shake it loose and drag it faster and faster into the eye, which eats it with a crunch, chews, burps, blinks, and backs into the rift while still staring at the breaker; the rift then shrinks shut with a fizz, about six seconds after the bed broke. In Raio three lightning bolts strike the bed head (on the break and about one and one and a half seconds later), each with an explosion sound, while a huge explosion and a ring of flames bursts out of the bed. In Tesouro Encontrado the bed sinks into the ground as the floor around it turns to sandstone and a red wool X marks the spot; a diamond shovel whistles down from the sky, spinning, and digs in with an explosion that throws ten diamonds and ten gold ingots, each chiming and sparkling where it lands, and the shovel is pulled out with a puff three seconds after the break. In Garota Mágica a pink heart of sparkles is drawn over the bed from its tip, facing the breaker; a pink firework rises from it and bursts, and eight star heads fall from the sky one after another with falling notes, spinning and leaving rainbow trails, land on the bed and fizz out. In Quarto Abandonado sixteen bats burst out of the bed in a cloud of smoke and flutter up and around the island, squeaking, until they all vanish together under three seconds later. In Tornado a cloud-and-dust funnel grows out of the bed while its two halves circle each other and rise in the wind; after about two seconds the bed shoots into the sky with a whoosh as the funnel unwinds, and some thirty blocks up it bursts into red wool and a green firework. In Tubarão a blue firework pops and four sharks leap out of the bed towards the breaker in a fan, splashing, dive back into the floor and swim apart with only their fins out, wiggling and splashing, until they vanish in puffs of cloud about three seconds after the break. In Vulcânica an explosion blows the bed four and a half blocks up on top of a jet of flames and lava; it bobs on the eruption with bubbling lava sounds until it cools with a hiss and disappears about four seconds after the break. In Carro the bed rises out of the floor with a lantern at the head and four spinning wheel heads that puff smoke, then speeds away from the head and slows down about 28 blocks later, under a second after the bed broke. In Anoitecer the sky flashes to night for a moment and comes back to day when the moon scatters, while a yellow crescent moon shines three blocks above the bed and an owl swoops in from the side, perches on the bed and flies off with it up and to the right, shedding feathers, until it vanishes in a cloud. The moon then scatters in sparks. In Assombrado 36 bones burst out of the bed, and every one that lands turns the floor block under it into quartz, white wool or diorite. Three ghosts rise from the bed one after the other, wander around it leaving puffs of cloud and fade away with a fizz. In Balões 25 balloons tied to a knot next to the bed carry it and its lantern up, faster and faster, swaying gently, until they are out of sight. In Chuva de Bolos six cakes are thrown out of the bed in pairs, sixty degrees apart, leaving a white and red trail, and land around it among sugar and cake items. Sparks then fan out across the floor. In Encantado a giant sunflower falls from twelve blocks up onto the bed, shedding petals. The bed turns into leaves, foot first, then bursts into a green firework, petals and 24 flowers. In Explosão da Vitória the bed shatters like gold. Seventeen sunflowers fly out in a circle, each pop higher than the last, while ten trophies are thrown all around, spinning once before they land on the floor. In Kraken (five stars, for sale; the kraken head is only the menu icon) the broken bed floats on a sea that swells around it, its two halves bobbing in turn among splashes, haze and swimming sounds. About a second after the break four tentacles of purple, magenta and pink wool burst out of the floor around the bed one after the other, each with a crack and a loud splash, and sway, dripping, bent towards their half of the bed. They then curl over and slap down on it, the kraken roars spraying ink, and they squeeze it three times with a crunch, pushing it a little deeper each time, until a last roar and a burst of water drag the bed and then the tentacles under, leaving two rings of ripples, about six seconds after the break.
- Animations are drawn only with packets (armor stands, falling blocks, items, fireworks, particles, sounds and fake block changes), so the real world never changes. They are our own reproductions of the recorded behaviour, with original pixel sprites and community heads.

**In a match** the breaker's selection plays at the broken bed for everyone in the room. `Aleatório` picks one of the player's owned animations; `Nenhuma` plays nothing.

**Preview** (BedWars lobby only; anywhere else the player gets `Você só pode visualizar cosméticos no lobby.`):

1. The player is saved (place, inventory, armor, game mode, scoreboard) and teleported to the preview world, with an empty inventory except for the bed `&aVoltar ao Lobby` in slot 9 (`preview.back-item`). Right-clicking it ends the preview at once.
2. Other previewers are hidden both ways. The scoreboard shows `Visualizando:` and the cosmetic name, and the action bar repeats `&eQuebre a cama!`.
3. Left-clicking the bed hides it for that player only and plays the animation for that player only.
4. `preview.return-delay` after the animation ends (2 s), or after `preview.timeout` without breaking the bed (2 min), the player goes back to where they were, with their lobby items, and the cosmetics menu reopens on the page they were on. Leaving the server during the preview opens nothing.

### Setting up the previews in game

Staff place the five previews from inside the BedWars lobby with `/bwconfig previa <tipo> <ação> [valor]` (permission `muxe.admin.config`), without editing `config.yml`. The world folders must already be in the server root.

| `<tipo>` | Section | World folder |
|---|---|---|
| `cama` | `preview` | `previa_quebra_de_cama` |
| `vitoria` | `victory-preview` | `previa_efeito_de_vitoria` |
| `kill-final` | `final-kill-preview` | `previa_kill_final` |
| `aura` | `aura-preview` | `previa_aura` |
| `rastro` | `projectile-trail-preview` | `previa_aura` |

| Command | Writes |
|---|---|
| `previa <tipo> spawn [--centro]` | `<section>.world` with the world you are in and `<section>.spawn` (`x`, `y`, `z`, `yaw`, `pitch`) with where you stand: coordinates in hundredths, angles in tenths; `--centro` puts `x` and `z` in the middle of the block. |
| `previa cama cama` | `preview.bed.foot` and `preview.bed.head` from the bed block you are looking at (up to 6 blocks away). Either half can be the one you look at: the other is found from the block data. If it is not a bed, nothing changes and the answer says to look at one. |
| `previa kill-final distancia <n>` | `final-kill-preview.target.distance`, how many blocks ahead the target stands (above 0, up to 64; `4,5` is accepted). |
| `previa <tipo> tempo <duração>` | `<section>.timeout`, such as `60s` or `2m` (at least `1s`). |

Every shortcut saves the file and applies the change live: each preview reads its settings through a `LiveValue` that the change swaps, and loads the new world when it differs. A player who is already in a preview keeps the settings and the world they started with and the next one gets the new ones; a bed that a viewer broke is put back in the world it was taken from. If the new world folder does not exist the answer says so and the preview keeps the previous settings until the folder is there and `/bwconfig recarregar` is used. On the match servers, where the previews do not exist, the shortcuts only save and the answer says the change applies after a restart. The generic subcommands reach the other keys of the same sections (`/bwconfig definir victory-preview.countdown-ticks 15`, `/bwconfig ver aura-preview`).

```text
/bwconfig previa kill-final spawn
/bwconfig previa cama cama
/bwconfig previa kill-final distancia 5
/bwconfig previa aura tempo 90s
```

### Final kill effects

`Kill Final` (slot 12 of `Cosméticos do Bed Wars`) lists the 43 final kill effects of the exports in `Kill Final ({page}/{pages})`, two pages, under the category `bedwars-kill-final`, with the same rules as the other categories. Every one of them has an animation. Every scene effect (victory and final kill) starts on the ground below where it happened (`Ground.surface`, up to 32 blocks down), so it plays the same on any map and at any height; a final kill effect also knows the hit direction (from the killer to the victim), the victim's skin and the wool colour of the victim's team (`Scene.dye`, from `TeamColor.woolData`; none in the preview, whose target has no team).

- **In the match:** when a player makes a final kill (not into the void), their selected final kill effect (or a random one they own) plays where the victim died, seen by the whole room.
- **Presenteado:** a big present rises 5 blocks from the victim for 26 ticks with red dust and sparks and a pling and firework blast rising in pitch every 2 ticks; then it explodes, a red and white burst firework goes off and eight presents fly out at once in eight directions at different heights, disappearing 3 seconds later.
- **Presentes:** a red, green and white star firework goes off on the victim and eight presents pop out one every 2 ticks in eight directions, spinning about 8 blocks up and landing about 3 blocks away. They disappear about 4 seconds later.
- **Passarinhos:** four birds circle the victim at different heights, one and a half turns at 1 block with bat sounds, then widen to 4 blocks while rising 2 more and burst into wool dust (red, light blue, yellow, light blue).
- **Brain Freeze:** the victim's head, inside an ice block, flies back from the hit (away from the killer) and up for 28 ticks, shedding ice and with a pling rising in pitch, then shatters with an explosion and breaking glass.
- **Afogar:** a squid drops into the ground where the victim died, with a splash, a burst of dripping water and lapis dust, while a 7-row blue and white wave rises across the killer's view, two rows a tick, and sways for 2 seconds as water sounds lower in pitch, ending with a deep splash.
- **Anoitecer:** the victim's body stays standing while an owl glides in from the killer's left, slowing down, grabs it with a screech and a puff, and flies it up and away to the right faster and faster, leaving a trail of feathers and wing beats, until both vanish in a cloud with a pop.
- **Apavorante:** a big ghost and three small ghosts rise out of the ground; the big one spirals up three blocks rocking its head while the small ones circle around it leaving pastel trails, with chimes and a ghast scream, and they burst in a pastel firework that scatters glowstone dust and sugar and leaves a sinking pastel cloud.
- **Arco-íris:** two waves of fourteen rainbow wool pop out all around the victim with a level-up sound, while sparkles cycling through every hue fill the spot for about 2.5 seconds; the wool stays on the ground for 5 seconds.
- **Biscoitos e Leite:** an orange star firework pops and fifteen milk buckets and cookies fly out one per tick in a spiral, each with a higher note, and crumble into milk-white or cookie-orange dust half a second after they land, with a crunch and a burst.
- **Caçador de Recompensas:** a green firework pops at the victim with an experience chime, and six reward heads (two kinds, alternating) hop out in six even directions about two blocks high, land a block away and stay on the ground for almost 3 seconds.
- **Colheita de Abóboras:** a jack o'lantern drawn in coloured dust appears above the victim facing the killer, grows and starts shaking and flickering with flames to a rising pling, then bursts: its pixels scatter and fade, twelve pumpkin seeds fly out in a ring, and ten pumpkins, jack o'lanterns and pumpkin pies are tossed up one by one with a rising plop, each landing with a puff of pumpkin dust; everything disappears about 4 seconds later.
- **Colmeia:** a honey-coloured firework pops and a beehive appears over the victim facing the killer; ten bees leave it one after another, bobbing and wandering around with a buzz, and each pops in a cloud after 2 seconds, then the hive vanishes in a cloud too.
- **Decapitado:** the victim's head rises 5 blocks in a second and a half, spinning and bleeding (redstone particles) with a pling rising in pitch, and then explodes.
- **Decolando:** the victim's body, in their skin, takes off like a rocket: it shoots away from the killer and up (6 blocks back and 2.5 up every tick) trailing firework sparks, until a firework blast is heard.
- **Deserto:** the ground under the victim turns to quicksand and a figure with their head and arms raised sinks into it, swaying, until it is swallowed with a burp. Meanwhile seven cacti jump out of the sand and puff away, and a smiling sun with sunglasses and turning rays shines 4 blocks up, dropping glitter to the ground.
- **Dragões Chineses:** a red and gold firework pops over the victim with a high-pitched dragon growl, and five dragon heads fly out of them in five directions, climbing gently and leaving a trail of red dust that first puffs straight up. After 2 seconds they stop and vanish in sparks of lava.
- **Eletrizante:** a body with the victim's skin stays where they died with a hidden skeleton inside. For 31 ticks they swap every tick with a buzzing hurt sound, as if electrocuted, while five silent lightning bolts strike above the head and crits burst every 5 ticks. It ends on the skeleton, which dies with an explosion; everything disappears 3 seconds after the kill.
- **Embrulhado:** a green and yellow firework goes off and the victim's body sinks 2 blocks into an open present at their feet. 14 ticks later the present closes with an armor sound, and it pops away about 3 seconds after the kill.
- **Encantado:** a light coral firework goes off and twelve leaves burst up, each puffing into a small cloud when it lands. Nine mushrooms then pop out of the ground around the victim one every 2 ticks, with rising pops; each hops and spins, then vanishes in a puff about a second later.
- **Esquelético:** the victim's head sinks from head height to the ground, turning once in 28 ticks and dropping a bone or bone meal every 2 ticks with a skeleton cry rising in pitch. Then it explodes and 20 bones fly out in every direction.
- **Esqueleto Amaldiçoado:** with a blaze cry, a black firework and a lightning bolt, a black circle 2 blocks around the victim pulses for over 2 seconds. Sixteen bones are thrown around it in a spinning circle and four skeleton skulls in four directions, another skull falls from above, and a skeleton death cry closes the effect.
- **Explosão de Abóbora:** an orange firework pops above the victim and throws out five pumpkin pies and three pumpkins, which stay on the ground for five seconds.
- **Explosão de Melancia:** a green and red firework bursts above the victim with an explosion and a plop, and eight melons and melon slices fly out in a ring and stay for five seconds.
- **Explosão de TNT:** a red firework pops and three lit TNT are thrown up from the victim, five ticks apart, each exploding about a second later.
- **Fantasmas:** a puff of smoke leaves the victim and three ghosts appear one after the other with a bat screech, wander around the victim for over two seconds leaving a trail of clouds, then vanish with a fizz.
- **Festa Disco:** a disco ball spins above the victim for 4 seconds while a bass-and-harp beat plays, notes circle the spot and ten music discs are thrown out; it ends with two fireworks and an explosion.
- **Fogos de Artifício:** a green firework bursts just above where the victim died.
- **Galático:** a firework bursts at the victim, then twelve nether stars fountain out in a circle with a rising chime, finishing with a level-up sound.
- **Garota Mágica:** a rainbow grows from the victim and arches over to the killer's left while ten stars shoot up with golden sparkling trails, spinning, and land around the spot.
- **Incendiar:** with thunder and an explosion, flames burst out, a skeleton sinks into the ground rattling ever lower, and a pixel flame burns where the victim stood until a fireball sound puts it out.
- **Jardim Florido:** a pink firework bursts a block above the victim, then thirteen pink tulips fly out evenly all around and lie on the ground for about 5 seconds.
- **Lula Explosiva:** a squid shoots up from the victim, faster and faster, splashing water with a rising pling, and about 9 blocks up it explodes in smoke and throws out twelve ink sacs, which stay for about three seconds.
- **Moedas:** sixty gold coins (sunflowers) spiral up three blocks around the victim, two per tick with gold dust and an orb chime, each thrown outward. They vanish 5 seconds after the kill.
- **Passe de Mágica:** a black top hat appears where the victim died and, with a cloud puff, a white rabbit jumps out of it. The rabbit hops away from the killer, about five blocks per hop, every 13 ticks, and the hat and rabbit vanish after about 3 seconds.
- **Pinguins na Neve:** with a snow step sound, fish and penguin heads pop out of the victim, while sixty snowballs shoot up from a widening spiral of snow puffs, each with a plop and a couple of clucks. Everything clears away after about 5 seconds.
- **Portal do Fim:** with an enderman scream, a purple witch ring spreads around the victim, the ground shatters like an eye of ender every 7 ticks with a portal swirl, and twenty eyes of ender appear floating around the victim with portal sounds. An enderman grumbles, and the eyes vanish after about 6 seconds.
- **Quebrado:** the victim shatters into stained glass of their team's colour (blue when there is no team, as in the preview): shards burst from the feet to the head, densest at the waist, with the sound of breaking glass.
- **Raio:** a lightning bolt strikes the block where the victim died, with thunder and an explosion, and a ring of 50 flames bursts outwards from the victim's waist.
- **Redstone:** the victim crumbles into redstone block shards from the feet to the head, with five stone-breaking sounds.
- **Six Seven:** a dancer with the victim's head appears where the victim died, facing the killer, weighing a **6** and a **7** in its hands and swapping the raised arm every 6 ticks to a beat. In the last three beats it fizzes, flashes red and shrinks twice, then explodes (tick 35). Ten 6s and 7s then fountain out one per tick and land around it, disappearing about 4 seconds after the kill.
- **Tinta:** paint splashes on the victim (wool dust and a growing ring of paint) and four paint blobs (green, yellow, blue, red) are thrown up one every 2 ticks, each with a squirt of paint and a trail. As each comes down, a star firework of its colour goes off on the victim and a stick pops out, while a paint star grows and sways above the victim, ending in a twinkling four-colour star firework. The floor is not painted; the sticks disappear about 3 seconds after the kill.
- **Toque de Midas:** a gold block drops from 6 blocks onto the victim and smashes in a burst of gold dust, then 15 sunflowers pop out one per tick with a rising pitch and stay on the ground until 5 seconds after the kill.
- **Vanilla:** the normal death: a copy of the victim, wearing their skin and facing the killer, appears with no health, so the client tips it over and turns it red; it is knocked about 2 blocks away from the killer, landing 6 ticks later, the client puffs it away after 20 ticks and it is removed at tick 29. No extra sound or particle.
- **Encantado:** the gnomes come to celebrate and bring the forest along, around the spot 7 blocks in front of the winner (where they look when the match ends). Every 2 ticks for 6 seconds a red or brown mushroom pops out of the top block of a random column within 17 blocks of that spot, with a puff of dirt and a pop. Ten giant mushrooms, one every 8 ticks, grow within 17 blocks too: a stem 4 to 6 blocks high, one block 2 ticks after the first and then one every 3 ticks with a rustle that rises in pitch and green sparkles, then the cap the same way, a flat brown 7 by 7 cap without corners on top or a red one made of three rings around the top of the stem and a 3 by 3 roof, ending with a chime and a spark; blocks already there are kept. Fifteen gnomes (fake players with a signed gnome skin), one every 3 ticks, appear in a cloud with a plop within 10 blocks of the spot and walk straight at 0.225 blocks per tick, turning a random way whenever a block or an edge is in front of them. Everything grows on the top block of its column, mushrooms included, and nothing grows over the void. The blocks are packets only and come back at the end, and the gnomes leave at the end.
- **Preview:** right click opens `final-kill-preview.world` (`previa_kill_final`, in the server root of the BedWars lobby) with a wooden sword in hand (`target.weapon`) and a player 4 blocks ahead (`final-kill-preview.target.distance`), wearing the skin of another online player (the viewer's own when alone) and a grey nick drawn from `target.names`, with `&eAcerte o jogador!` in the action bar. Hitting it removes it with the hurt sound and plays the effect there; then the player goes back and the list opens again.

### Victory effects

`Efeito de Vitória` (slot 16 of `Cosméticos do Bed Wars`) lists the 22 victory effects of the export in `Efeito de Vitória ({page}/{pages})`, with the same layout, order, buying and selection as the bed destruction list. They are saved under the category `bedwars-vitoria`, so ownership and selection are separate from the bed ones; credits are shared. All of them have an animation (`Tinta`, `Feliz Natal`, `Como treinar seu... porco?`, `Altas aventuras`, `Colheita de Abóboras`, `Apavorante`, `Contagem de Flash`, `Apocalipse Zumbi`, `Aventureiro`, `Necromante`, `Deserto`, `Bruxa`, `Anoitecer`, `Congelado`, `Dragão`, `Festival Dourado`, `Garota Mágica`, `Inferno`, `O End`, `Tsunami`, `Wither`, `Encantado`); only one effect plays per match, drawn among the winners online, and every effect lasts 10 seconds; what falls from the sky (paint buckets, pumpkins, candy, presents, gold) falls all over the map, inside the box around every island point (spawns, beds, shops, generators) plus 12 blocks, twice as much on maps bigger than 32 by 32 blocks, and what misses the islands falls into the void.

- **In the match:** when the match ends with a winner, every player of the winning team plays their selected victory effect (or a random one they own) at their position, seen by the whole room.
- **Tinta:** 22 paint buckets fall one every 8 ticks from 12 blocks up onto the ground all over the map (never within 2.5 blocks of the winner), with red, green, blue and yellow sparkles. When one lands it splashes, scatters drops and four wool blobs (lime, yellow, blue, red) hop out in four directions with a pop that rises in pitch, each flashing a pink star of particles and a short beam of its colour. Then four star fireworks (green, yellow, red, blue) and a coloured one higher up go off on that spot, each tossing a stick and painting the floor around it with wool of its colour (a 3 by 3 patch on the top block of each column, plus a splat 2 blocks away); later fireworks paint over earlier ones. The painted blocks are packets only and come back when the effect ends, after 12 seconds. A bucket stops on the first block it hits on the way down (a tree or a roof counts); a bucket that misses the island falls straight into the void for 2 seconds and does nothing else.
- **Feliz Natal:** every living player gets their own flying hyena (a skull on an invisible seat), starting where they stand and facing where they look. Each player steers their own: holding W flies it where they look (up to 1 block per tick, easing in and out); without W it only bobs up and down. Nobody can get off. Presents rain over the map, 6 every 5 ticks, landing on the floor and vanishing a second later, and star fireworks (red, green, white) go off on a ring of 15 blocks.
- **Como treinar seu... porco?:** the same flight on a saddled pig, one for every living player, followed by a train of six pigs (one baby) that replay its path 5 to 35 ticks behind, with a cloud trail, hearts and pig sounds.
- **Altas aventuras:** every living player sits under their own 25 skull balloons tied with leads to their side and slowly rises; each player's W moves their own balloons forward at 0.25 blocks per tick. Thirty loose balloons around pop one by one.
- **Colheita de Abóboras:** spinning pumpkins with a flame trail fall over the map every 10 ticks and burst on the floor into seeds, pies, small pumpkins and sometimes an orange firework.
- **Apavorante:** night falls (from midday to midnight in 30 ticks, easing in and out); two ghasts circle above and candy heads fall over the map with coloured trails, bursting into cake, sugar and cookies or, sometimes, fusing like a creeper and releasing ghost heads.
- **Contagem de Flash:** surprised emoji faces made of particles pop up in front of the winner, facing them wherever they are, three at a time, while white camera-flash fireworks go off around.
- **Apocalipse Zumbi:** night falls the same way; zombies (some babies) climb out of the ground around the winner and walk toward them, vanishing in smoke or blowing up like creepers, with lightning and a witch swirl.
- **Aventureiro:** every living player gets a `Grappler` lead in the first hotbar slot: left click throws a hook to the first block looked at (up to 80 blocks), right click pulls the player toward it. It is taken back at the end.
- **Necromante:** wither skulls circle the winner inside a purple ring and ten risen dead (fake players with grey names) climb out of the ground, walk to the winner and sink again, with lightning.
- **Deserto:** the whole map turns into desert from the winner outwards (the map area, or at least 144 by 144 blocks around the winner, as in the preview), in 162 ticks however big it is (at least 128 columns a tick): stone and dirt become layered sandstone or clay, the top becomes sand or red sand, plants and leaves vanish, grass becomes dead bushes and some cacti grow. Every living player gets an oasis (a pond, a gold fountain, palms and sugar cane), built 10 blocks in front of them, level with their floor, over the island and the void alike; when something stands above the floor there (a building, a tree, another player's oasis) it turns to the sides or goes farther, up to 48 blocks, and with no free room anywhere it stays straight ahead. Every living player rides their own flying carpet that only their W moves, where they look, while gold blocks, ingots, nuggets and golden apples rain over the map. Everything is packets only and comes back at the end.
- **Bruxa:** every living player rides their own broom (a bristle head and five lying handle heads on invisible stands, with a sitting black kitten behind the rider) and gets the `Varinha Mágica` stick in the fifth hotbar slot. Four witches (fake players, each with one of four signed witch skins in purple, black, olive and green dresses (`WitchSkins`), a stick and an upside-down hopper as a hat, each on the same broom with a kitten) appear in a puff of cloud around each player, 3 to 4 blocks ahead and 4 and 7 blocks to each side. Holding W flies the broom where the player looks at up to 1 block per tick, easing in and out by 30% a tick, climbing or diving with the look; the broom turns a fifth of the way toward the look each tick and always bobs half a block up and down (about every 31 ticks), so without W it bobs in place. Every witch flies the same way and bobs one tick ahead of the previous one. A witch's place is 3 or 4 blocks ahead and 4 or 7 blocks to the side of the way its player's broom is flying (not where the player looks, so turning in place moves nobody); while the witch is more than 4 blocks from its place it flies toward it, turning as the broom does, and otherwise it only bobs, facing the same way. So the witches set off a few ticks after their player, trail behind at full speed and fall further back in turns. A click with the wand (at most once every 13 ticks) fires a guardian beam from behind the broom to the first block looked at (up to 80 blocks, or 80 blocks into the air); 9 ticks later it bursts in a huge explosion and repaints the wool, stained clay, stained glass, glass panes and carpets within 5.5 blocks in one colour drawn among orange, magenta, light blue, pink, cyan, purple, brown, green and red, anywhere on the map. A dragon growls at the start and fireworks launch every 10 ticks. The painted blocks are packets only and come back at the end, and the wand is taken back.
- **Dragão:** every living player rides their own ender dragon, which appears 5 blocks above where they stand (the rider sits on its back, 6 blocks above it) and flies all the time at 1 block per tick where its rider looks: it turns at once with the look, holding A or D sends it sideways (a quarter turn to that side), and it climbs or dives with the look only when the rider looks more than 23 degrees up or down. It digs a tunnel through everything it touches (7 blocks wide, from its height to 8 blocks above): about one block in 18 crumbles with the block break particles and sound, and one in 6 is thrown out 2 ticks later (2 blocks per tick away from the dragon and 1 up) and vanishes when it lands. The dragon growls now and then. The dug blocks are packets only and come back at the end.
- **Inferno:** the whole map turns into the Nether from the winner outwards (the map area, or at least 129 by 129 blocks around the winner, as in the preview), 256 columns a tick in order of distance, every column from the bottom up to height 127: every full block becomes netherrack, except inside random lava pockets (balls of radius 3 to 9 blocks, about 15% of the ground) where it becomes still lava; glass, panes, stairs, slabs, fences, walls, chests and other solid blocks that are not full cubes become soul sand; liquids, plants and other loose blocks vanish; barriers stay. Night falls in the first second, from noon to midnight, slowing down at the end. Flames burst every tick somewhere within 15 blocks of the winner. Fifteen blazes appear on a ring 10 blocks around the winner, up to 19 blocks high, and slowly sink to the ground; every 5 ticks one of them shoots a big fireball at a random living player, bending towards where the winner looked at the start. A fireball that hits a player explodes and launches them 1.5 blocks a tick along its path and 1 up; one that hits the ground blows a 3-block crater and throws its blocks as netherrack that vanishes 2 seconds later. Everything is packets only and comes back at the end.
- **O End:** the whole map turns into the End from the winner outwards (the map area, or at least 129 by 129 blocks around the winner, as in the preview), 256 columns a tick in order of distance, every column from the bottom up to height 127: every full block becomes end stone, except inside random patches (balls of radius 3 to 9 blocks, about 10% of the ground) whose core becomes birch planks and whose outer block and a half becomes sandstone; everything that is not a full cube (glass, panes, stairs, slabs, fences, chests, plants, webs, liquids) vanishes; barriers stay. Five round obsidian pillars, 9 blocks wide, from the bottom up to height 127, stand only around the middle of the map, 33 blocks from it (at 33,0; 10,32; -27,19; -27,-19 and 10,-32 from it), wherever the winner is; in a match the middle is the average position of the emerald generators (the centre of the map area when the map has none), in the preview it is `victory-preview.middle`; each rises whole in the tick the wave reaches its centre. Two endermen appear on the ground 18 to 25 blocks from the winner; after 2.35 seconds the first one teleports 1.5 to 3.5 blocks beside a random living player and stares at them, groans a moment later and from 5.75 seconds walks straight on past them until it meets a wall or a drop. Everything is packets only and comes back at the end.
- **Tsunami:** a dragon growls, every living player is allowed to fly until the effect ends (whoever could not fly before stops flying then), and rain fades in by a hundredth every tick. From the far side of the map along the axis (north, south, east or west) closest to where the winner faces, still water sweeps back over the whole map, rising every other tick and filling every block of air, tall grass, double plants and flowers from y 0 up to 2 blocks above the highest block of the map (torches, vines, carpets and every other block stay): the first row floods whole on the first tick, then the bottom edge runs 4 rows a tick and the full-height crest 2 rows a tick, with the water ramping between them, so a 147-block map is under water in about 3.7 seconds. The water is sent as whole chunk sections, like a chunk reload, instead of block by block. Squids and guardians (about one every 125 square blocks, up to 240, 40% of them guardians, from the winner's height up to the water's surface) appear in the water as it reaches them. Squids swim in strokes, and guardians drift and lock their beam on the nearest squid within 16 blocks for 4 seconds. Splashes rising in pitch play every 7 ticks and fireworks launch every 10 ticks. The water is packets only and comes back at the end.
- **Wither:** every living player rides their own wither, sitting 3.375 blocks above it. It always flies half a block a tick where the player looks, staying level while the look is within 5° of the horizon. Every 20 ticks it fires a wither skull that leaps 10 blocks along the look and then slows by 5% a tick. On the first block, or after 8 ticks, the skull bursts in a huge explosion, and the blocks within 20 blocks of it turn into clay and sand, nearest first, about 45 a tick. Fireworks launch every 10 ticks and the wither makes its idle sound at 4 and 8 seconds. The blighted blocks are packets only and come back at the end.
- **Anoitecer:** night falls with a rumble of thunder and the winner mounts an owl that appears in a puff of smoke and climbs into the sky in a straight line, holding reins, leaving a trail of feathers and beating its wings, while a flock of owls gathers around the rider, one every 4 ticks from the first tick until there are 12, each appearing in a puff up to 12.5 blocks to the side of the owl and 0.75 to 6.75 blocks above it, gliding on its own for 50 to 120 ticks and vanishing in a puff, replaced by a new one around where the owl is then; every living player rides their own owl.
- **Congelado:** the map freezes from the winner outwards in diamond-shaped rings, about 250 columns a tick so a whole map takes about 5 seconds; full blocks turn into snow, with angular patches of packed ice where about one seed in seven of a random three-dimensional Manhattan cell pattern falls, glass, panes, stairs, slabs, chests and water turn into clear ice, and carpets, buttons, heads and plants vanish. Up to four snowmen wake up on the first frozen ground around the winner and stroll around, crunching on snow and clicking on ice. The frozen blocks are packets only and come back at the end.
- **Festival Dourado:** every living player rides their own rainbow sheep (its wool cycles red, orange, yellow, lime, light blue, blue and purple every 4 ticks, with dust of the same colour around its feet) one block under an invisible seat; holding W flies it where they look, like the pig, up to 1 block per tick. The sheep digs a tunnel through everything it crosses: every block 5 wide and 7 tall, from one block under the sheep, vanishes with a pop and a puff of snow. Every 5 ticks 30 sunflowers, gold blocks and diamonds (shared between the players) are tossed up 10 to 34 blocks in front of each player, higher the more they look up, and vanish 84 to 100 ticks later. Fireworks launch every 10 ticks. The dug blocks are packets only and come back at the end.
- **Garota Mágica:** a dragon growls and every living player gets the `Foguete` firework in the fifth hotbar slot; a tick later they sit on a big star head that waits in the air and turns a fifth of the way toward their look each tick. A click with the rocket (at most once every 5 ticks, either button) throws the star where the player looks, 1.5 blocks a tick forward and 0.8 up, with a launch sound and sparks; it then glides and falls like a body and stops on the first block below or at the height where it started, until the next rocket. Every 8 ticks a chime rings and a spinning star head falls over the map from 3.7 to 9.5 blocks above the winner's star, drawing a six-colour rainbow ribbon with sparkles; when it lands, sparks burst with a pop and a twinkle and it fizzles out 4 ticks later, and a star that misses the islands falls into the void. Fireworks launch every 10 ticks, and the rocket is taken back at the end.
- **Preview:** right click opens the world `victory-preview.world` (`previa_efeito_de_vitoria`, which must be in the server root of the BedWars lobby). The player stands at `victory-preview.spawn` and sees `3...`, `2...`, `1...` one second apart with the countdown click, then the `VITÓRIA` title and the effect. When it ends (or on the `Voltar ao Lobby` bed in slot 8, or after `timeout`) the player goes back where they were and the effect list opens again; a `VITÓRIA` title still sweeping is stopped, so it does not cover the countdown of the next preview.

### Aura

`Aura` (slot 33 of `Cosméticos do Bed Wars`, an emerald) lists the auras in `Aura ({page}/{pages})`, with the same layout, order (owned first, then the rest, each group alphabetical), selection and `Voltar` rule as the other lists: `Nenhuma` (barrier, slot 10), `Aleatório` (name tag, slot 11, whose icon cycles through the auras the player owns), the auras from slot 12, `Voltar` (slot 48, `Para Cosméticos do Bed Wars`) and `Selecionado: <name>` (slot 49). They are saved under the category `bedwars-aura`, so ownership and selection are separate from the other categories. Both auras are tournament prizes (`availability: EVENT`, tag `&6Premiação de torneio`, five stars): a player who does not own one sees `Indisponível.` and can still preview it with the right button. They are not for sale and `/cosmeticos dar` only grants bed destruction cosmetics, so for now a player owns them through `muxe.cosmetic.all`.

| Aura | Icon | Particle |
|---|---|---|
| `Liga Mestre` (`liga-mestre`) | `REDSTONE` | `FLAME` |
| `Liga Diamante` (`liga-diamante`) | `DIAMOND` | `SPELL_WITCH` |

- **Rule:** while a player is playing (room in `PLAYING`, player alive, not a spectator) the aura is always on, and the only thing that hides it is the invisibility potion. There is no aura in the waiting lobby, for dead or eliminated players (they spectate), or after the match ends. It is shown to everyone in the room, the owner included, and is cheap main-thread work: one particle per owner per tick.
- **Pattern** (`AuraPattern`, `AuraTrails`): every tick, per player with an aura, one particle (count 0, offsets 0, speed 1, long distance). If the player moved horizontally since the previous tick, it appears at the feet, at `(x, y + 0.1, z)`. Otherwise it is a point of a helix around the player, for a counter `k` that grows by 1 every tick, moving or not (starting at 0 when the aura starts showing): angle `18° · k`, offset `x = sin(angle)`, `z = -cos(angle)` (radius 1) and `y = 0.1 + ((k + 15) mod 60) / 30`. So it climbs 1/30 of a block per tick from 0.1 to about 2.07 over three turns (20 ticks per turn) and starts over every 60 ticks. Only horizontal movement counts: jumping or falling in place keeps the helix.
- **Random:** `Aleatório` draws one owned aura per player and per match, the first tick the player is playing, and keeps it until they leave the match (`AuraCosmetics`, `AuraAssignments`); the next match draws again. `Nenhuma` shows nothing, and a chosen aura the player does not own is never used.
- **Preview:** right click opens the world `aura-preview.world` (`previa_aura`, which must be in the server root of the BedWars lobby; without it the preview answers `preview-only-lobby`). The player stands at `aura-preview.spawn` (10.5 19 21.5, yaw 180) with the `Voltar ao Lobby` bed in slot 8 and sees that aura on themselves, only on themselves, while they walk around. There is no countdown or title. It ends on the bed, on `aura-preview.timeout` (`60s`) or when the player quits, and the player goes back where they were and the aura list opens again. It reuses the scene preview (`ScenePreview`) with an `AuraAnimation` per aura.

### Projectile trails

`Rastros de Projétil` (slot 27 of `Cosméticos do Bed Wars`, an arrow showing `{owned}/{total} ({percent}%)` and `Selecionado: {selected}`) lists the 23 trails in `Rastros de Projétil ({page}/{pages})`, with the same layout, order (owned first, then the rest, each group alphabetical), selection and `Voltar` rule as the aura list: `Nenhuma` (barrier, slot 10), `Aleatório` (name tag, slot 11), the trails from slot 12, `Voltar` (slot 48), `Selecionado: <name>` (slot 49) and `Loja` (slot 53). They are saved under the category `bedwars-rastro`. Prices by stars: 300, 350, 500, 700 and 800 credits. `Crítico` is `DEFAULT`, so every player owns it. `Ultra` is an ULTRA trail that shows its own tag, `&7Exclusivo para &d&lULTRA`, instead of `Incluso no ULTRA`: in every category, a VIP, ULTRA or ULTRA+ cosmetic with its own `tag` shows that tag instead of the rank one.

A trail is animated only when `ProjectileTrails` (package `cosmetic.trail`) has a `ProjectileTrail` with its id. Until then it shows `Indisponível.`, is not sold, `Aleatório` never draws it, nobody gets it in a match and its preview answers `cosmetic-no-preview`; the rest of the category works with an empty registry.

| Trail | Icon | Stars | Availability | Tag |
|---|---|---|---|---|
| `Espiral da Fortuna` (`espiral-da-fortuna`) | `DOUBLE_PLANT` | 5 | `EVENT` | `&6Premiação de torneio` |
| `Crítico` (`critico`) | `DIAMOND_SWORD` | 1 | `DEFAULT` |  |
| `Chamas` (`chamas`) | `BLAZE_POWDER` | 5 | `ULTRA` |  |
| `Ultra` (`ultra`) | `INK_SACK:9` | 5 | `ULTRA` | `&7Exclusivo para &d&lULTRA` |
| `Natalino` (`natalino`) | `SKULL_ITEM:3` | 5 | `EVENT` | `&cNatal 2025` |
| `Ossos` (`ossos`) | `BONE` | 5 | `EVENT` | `&6Halloween 2025` |
| `Espectro Verde` (`espectro-verde`) | `EMERALD` | 5 | `ULTRA_PLUS` |  |
| `Corações` (`coracoes`) | `APPLE` | 5 | `ULTRA_PLUS` |  |
| `Magia` (`magia`) | `CAULDRON_ITEM` | 5 | `ULTRA_PLUS` |  |
| `Sorte Azul` (`sorte-azul`) | `INK_SACK:4` | 4 | `SALE` |  |
| `Faísca Cósmica` (`faisca-cosmica`) | `EYE_OF_ENDER` | 1 | `SALE` |  |
| `Lava` (`lava`) | `LAVA_BUCKET` | 1 | `SALE` |  |
| `Chroma` (`chroma`) | `POTION:0` | 5 | `EVENT` | `&6Premiação mensal` |
| `Fumaça` (`fumaca`) | `COAL` | 3 | `SALE` |  |
| `Vapor` (`vapor`) | `WOOL:0` | 3 | `SALE` |  |
| `Água` (`agua`) | `WATER_BUCKET` | 4 | `SALE` |  |
| `Aldeão Furioso` (`aldeao-furioso`) | `SKULL_ITEM:3` | 4 | `SALE` |  |
| `Fogos de Artifício` (`fogos-de-artificio`) | `FIREWORK` | 3 | `SALE` |  |
| `Notas Musicais` (`notas-musicais`) | `NOTE_BLOCK` | 5 | `EVENT` | `&6Premiação de torneio` |
| `Sangrento` (`sangrento`) | `REDSTONE` | 5 | `SALE` |  |
| `Crítico Encantado` (`critico-encantado`) | `BOOK` | 2 | `SALE` |  |
| `Mega-Loucura` (`mega-loucura`) | `SPONGE` | 4 | `SALE` |  |
| `Arco-íris` (`arco-iris`) | `SKULL_ITEM:3` | 4 | `SALE` |  |

- **Rule:** when a living player of a match (room in `PLAYING`, player alive, not a spectator) launches a projectile whose shooter is them (a bow arrow, snowball, egg, ender pearl, splash potion, fishing hook, the fireball of the shop's fire charge or any other projectile), their trail follows it. The kind the trail gets (`ProjectileKinds`) is `ARROW`, `SNOWBALL`, `EGG`, `ENDER_PEARL`, `POTION` (splash potions), `FISHING_HOOK`, `FIREBALL` (fireballs and small fireballs) or `OTHER` (anything else, such as an experience bottle or a wither skull).
- **Random:** `Aleatório` draws one owned trail per player and per match, on their first launch, and keeps it for the whole match (`TrailCosmetics`, `TrailAssignments`); the next match draws again. `Nenhuma` shows nothing, and a chosen trail the player does not own is never used.
- **Flight** (`TrailFlight`): the first server tick after the launch is age 0, at the projectile's current position (where it was spawned). Every tick while the projectile exists and is not on the ground, the trail gets `tick` with its position, velocity, age and kind. When it is gone (hit, exploded, removed, reeled in) or on the ground (an arrow stuck in a block, a hook lying still), `land` is called once with the last position and age, the stage stays open `linger()` more ticks so what the trail spawned can finish, and then it closes and removes every actor the trail created. A flight that lasts 1200 ticks lands anyway; an arrow that bounces off keeps flying.
- **Viewers:** the online players of the room. Each flight has its own core `PacketStage`. The task (`TrailTask`, every tick) builds the viewer list once per room per tick, and only while some trail is flying. When the room leaves `PLAYING`, or the shooter quits or leaves the room, their flights close at once without `land`. A trail that throws is logged and closed without stopping the others.
- **Preview:** right click opens the world `projectile-trail-preview.world` (`previa_aura`, the aura map) at the aura spawn (10.5 19 21.5, yaw 180), with the same scoreboard (`Visualizando:` and the name). There is no countdown and no scripted scene: the viewer gets the kit of the recording and throws things themselves: snowball ×16 (slot 0), splash potion of healing (`POTION:16421`) ×16 (slot 1), ender pearl ×16 (slot 2), a glowing unbreakable bow (slot 3), an unbreakable fishing rod (slot 4), one arrow (slot 9, the first of the main inventory) and the `Voltar ao Lobby` bed (slot 8). Right clicking with a kit item uses it (the clicked block is not used); everything else stays locked as in the other previews: blocks, drops, pickups, inventory clicks, hunger and damage. `kit.refill-ticks` (2) ticks after every launch each kit slot that is short or empty is filled back, so the items are infinite. As in the recordings, the ender pearl teleports the viewer (its fall damage is cancelled), the potion splashes but applies nothing (`PotionSplashEvent` cancelled, the splash still shows) and an arrow that hits something is removed. Every projectile the viewer launches gets the previewed trail, seen only by them (`TrailPreview`). It ends on the bed, on `projectile-trail-preview.timeout` (`60s`) or when the player quits; projectiles still flying are removed, and the player goes back where they were with the list open again. It is the kit mode of the scene preview: any `ScenePreview` section with a `kit` (`PreviewKit`) gives the kit instead of playing a scene, and a `KitPlay` hook hears every launch.

Effects of each trail:

- **Espiral da Fortuna:** golden dust spins in a ring half a block around the flight path, a full turn every 9 ticks, and drops a sunflower every tick that pops up and vanishes 5 ticks later.
- **Crítico:** a critical hit spark follows the projectile every tick.
- **Chamas:** a flame follows the projectile every tick.
- **Ultra:** thick pink dust follows the projectile every tick.
- **Natalino:** dust follows the projectile, alternating red, green and white every tick.
- **Ossos:** every other tick a bone or a bone meal pops out under the projectile and vanishes 5 ticks later.
- **Espectro Verde:** a green villager sparkle follows the projectile every tick.
- **Corações:** a heart follows the projectile every tick.
- **Magia:** a purple witch spell sparkle follows the projectile every tick.
- **Sorte Azul:** every tick a lapis lazuli pops out under the projectile and vanishes 5 ticks later.
- **Faísca Cósmica:** five portal particles swirl around the projectile every tick.
- **Lava:** a lava drop pops from the projectile every tick.
- **Chroma:** a few spell particles in random colours follow the projectile every tick.
- **Fumaça:** a puff of large smoke follows the projectile every tick.
- **Vapor:** a small white cloud follows the projectile every tick.
- **Água:** a water splash follows the projectile every tick.
- **Aldeão Furioso:** an angry villager cloud follows the projectile every tick.
- **Fogos de Artifício:** a firework spark drifts down behind the projectile every tick.
- **Notas Musicais:** every other tick a music note pops up within a block of the projectile, each later note more scattered and in a changing colour.
- **Sangrento:** a red dust particle follows the projectile every tick.
- **Crítico Encantado:** an enchanted critical hit spark follows the projectile every tick.
- **Mega-Loucura:** every tick a particle drawn at random from 28 kinds (explosions, sparks, splashes, crits, smoke, spells, drips, villagers, notes, portal, flame, lava, cloud, dust, snow, slime and hearts) follows the projectile.
- **Arco-íris:** thick dust follows the projectile, one rainbow colour a tick: red, orange, yellow, green, cyan, blue and magenta.

### Kill messages

`Mensagens de kill` (slot 31 of `Cosméticos do Bed Wars`, paper) lists the chat message sets in `Mensagens de kill ({page}/{pages})`, saved under the category `bedwars-mensagem-kill`. The slots follow the recording and the order is the catalog order, not owned first: `Padrão` (paper, slot 10), `Aleatório` (name tag, slot 11) and `Estatístico` (book, slot 12), then `Voltar` (slot 48, `Para Cosméticos do Bed Wars`) and `Selecionado: <name>` (slot 49). There is no `Nenhuma`: a player with nothing selected uses `Padrão`, which everyone owns and nobody buys (`availability: DEFAULT`). The lists have no right click: the lore of each set has `Prévia:` with sample lines and `Botão esquerdo: Selecionar`.

| Set | Icon | Availability |
|---|---|---|
| `Padrão` (`padrao`) | `PAPER` | everyone |
| `Estatístico` (`estatistico`) | `BOOK` | tournament prize (`availability: EVENT`, `&6Premiação de torneio`, shown as `Indisponível.` to anyone who does not own it) |

The selected set replaces the chat messages of the match (`KillAnnouncer`, called by `BedWarsMatch`):

| Event | Whose set | Message kinds |
|---|---|---|
| Kill | The killer's | `kill`, `final-kill`, `void-kill`, `void-final-kill` |
| Death without a killer (also a player eliminated for being away or when every bed is destroyed) | The victim's | `death`, `final-death`, `void-death`, `void-final-death` |
| Bed destroyed | The breaker's | `bed`, after the `kill-messages.bed-prefix` (`CAMA DESTRUÍDA »`) every set shares |

A player who leaves with the bed already destroyed counts as a final death (or a final kill for whoever hit him). The final kills of the players a bed break eliminates are final kills of the breaker, with the breaker's set.

- **Templates** live in `kill-messages.sets.<id>.messages`, keyed by kind, with `{victim}`, `{killer}`, `{player}`, `{team-color}`, `{team}` and `{count}` (the counter, `17.089`, with the thousands separated by dots). `{br}` marks where the `Prévia:` breaks a long line in two (the game removes it). A set only writes the kinds it changes: a missing `void-*` kind uses the normal kind (`Estatístico` has no void variants). The default set (`kill-messages.default`, `padrao`) must have all of them and no `{count}`. Every message starts with `kill-messages.line-color` (`&7`), the same way in the game and in the preview.
- **`Padrão`** keeps the texts the game already had, with the void kill (`morreu no void para <killer>`) as the only new one, since a void death with a killer used to read as a normal kill. Names keep the team colour.
- **`Estatístico`** counts with the player's lifetime BedWars totals, all modes together, including the event itself: `Fulano foi a 17.089ª kill de tadeu.`, `Fulano foi a 19.215ª kill final de tadeu.`, `tadeu morreu pela 12.538ª vez.`, `tadeu teve sua 2.537ª morte final.`, `A Cama do Vermelho foi a 11.938ª cama destruída de tadeu.` The number is in `&b` and ends with `ª`. A kill adds one to the victim's death counter as well. The totals (`kills`, `finalKills`, `deaths`, `finalDeaths`, `bedsBroken` of `bedwars.player_stats`, general) are loaded when the player enters the room, together with the level (`FindPlayerStatsQuery`), and `LifetimeTally` adds what happens during the match. If they are not loaded yet, that message is told with `Padrão`.
- **Random:** `Aleatório` draws one owned set per player and per match, the first time a message needs it, and keeps it until the match is closed (`KillMessages`, `MessageAssignments`). A selected set the player does not own is never used: `Padrão` is.
- **`Prévia:`** is built from the templates above with the viewer's name in the colour of his tag (the `§8tadeu` of the recording), a victim `Fulano` in red and the team `Vermelho` (`kill-messages.preview`, `MessagePreview`). Padrão shows `kill`, `void-kill`, `death`, `void-death` and `bed`; Estatístico shows `kill`, `final-kill`, `death`, `final-death` and `bed` with the sample counters of `kill-messages.preview.counts`. A test checks that every preview line joined is exactly what the game sends.
- `/cosmeticos dar` only grants bed destruction cosmetics, so for now a player owns `Estatístico` through `muxe.cosmetic.all`.

### Wood types

`Tipos de madeira` (slot 29 of `Cosméticos do Bed Wars`, planks) opens `Tipos de madeira ({page}/{pages})`, saved under the category `bedwars-madeira`. It has neither `Nenhuma` nor `Aleatório`, and the order is the catalog order. The item `madeira` of the shop (16 planks for 4 gold) is delivered with the selected wood instead of oak; its amount, price, name (`Madeira`) and place in the shop and the quick buy do not change, and it is placed in the hotbar like any other purchase (`HotbarDelivery`, which only looks at the item id). Placing and breaking behave as for any block placed in the match.

| Slot | Name | Item | Included in |
|---|---|---|---|
| 10 | `Madeira de carvalho` (default) | `WOOD:0` | everyone |
| 11 | `Madeira de pinheiro` | `WOOD:1` | `VIP` |
| 12 | `Madeira de eucalipto` | `WOOD:2` | `VIP` |
| 13 | `Madeira da selva` | `WOOD:3` | `ULTRA` |
| 14 | `Madeira de acácia` | `WOOD:4` | `ULTRA` |
| 15 | `Madeira de carvalho escuro` | `WOOD:5` | `ULTRA+` |
| 16 | `Tronco de carvalho` | `LOG:0` | `ULTRA+` |
| 19 | `Tronco de pinheiro` | `LOG:1` | nobody yet |
| 20 | `Tronco de eucalipto` | `LOG:2` | tournament prize |
| 21 | `Tronco de árvore da selva` | `LOG:3` | nobody yet |
| 22 | `Tronco de acácia` | `LOG_2:0` | nobody yet |
| 23 | `Tronco de carvalho escuro` | `LOG_2:1` | nobody yet |

- **Lore:** `Incluso no VIP` / `ULTRA` / `ULTRA+` for the rank woods (`lore.vip-tag`, `lore.ultra-tag`, `lore.ultra-plus-tag`), `Premiação de torneio` for the prize, then `Botão esquerdo: Selecionar`, `Já selecionado.` or `Indisponível.`. Owned names are green and locked ones red. The selected wood glows and `Selecionado: <name>` (slot 49) shows the wood itself, with `&7` as lore.
- **Ranks:** `Incluso no X` means every player of that rank or above owns it (`CosmeticAccess`): `muxe.cosmetic.vip` for `VIP`, `muxe.cosmetic.ultra` for `ULTRA` and `muxe.cosmetic.ultraplus` for `ULTRA+` (each tier covers the lower ones), and `muxe.cosmetic.all` owns everything. The permission comes from the groups, so give `muxe.cosmetic.vip` to the `vip` group (the ranks above inherit it) and `muxe.cosmetic.ultraplus` to whoever has Ultra+, which is not a group of its own yet. `muxe.cosmetic.ultra` is already given to `ultra`. The woods marked `nobody yet` or `tournament prize` are owned only through `muxe.cosmetic.all` or a grant (`core.owned_cosmetics`).
- **Fallback:** the wood the shop delivers is the selected one if the player still owns it, and oak otherwise (a rank that expired, a wood never owned, nothing selected, or cosmetics not loaded). The selection is read when the player enters the server, so a change made in the lobby applies in the next match.

### BedWars lobby

The BedWars lobby (`bedwars-lobby` group) runs Muxe-Lobby plus this plugin in lobby mode. The main game selector, game NPCs and `/jogar bedwars` lead there.

- **Mode NPCs**: SOLO, DUPLAS, TRIOS and QUARTETOS open their mode menu; 1V1/2V2/3V3/4V4/4S opens the duels menu; MEGA and PILARES open nothing yet. `{players} jogando` counts the players in the rooms of their modes on every BedWars server. The two top lines of PILARES change between white and pink every 0.5 s.
- **Mode menu** (`Bed Wars Solo`...): `Bed Wars (Solo)` shows the players of the mode and the top 3 of the daily winstreak, and sends the player to a match; `Selecionar mapa` opens the map menu for players with `muxe.bedwars.map-select` (VIPs) and answers `map-vip-only` to the others; `Treino: Ponte` answers `coming-soon`. The duel modes also have `Voltar`, back to the duels menu.
- **Level**: the XP bar shows the BedWars level as the XP level and the progress to the next level as the bar, loaded on join and refreshed every 30 seconds.
- **Map menu** (`Bed Wars 1v1 (1/1)`): the maps of the mode found on the BedWars servers, favourites first, each group in alphabetical order. Left click asks for a room on that map; right click adds or removes the favourite. `Mapa favorito aleatório` joins a random favourite, `Pesquisar` filters by name typed in the chat (accents ignored), and the arrows change page.
- **Matchmaking**: every BedWars server publishes its rooms and ready arenas every 2 s (`bedwars.rooms`). Joining picks the fullest room of the mode (and map) that still has space on any server; if there is none, it asks the emptiest server that has an arena of that mode (and map) to create one. The lobby sends `bedwars.join_request` to that server, which reserves a seat in the room for 30 s and replies with its code. Pending reservations count as taken seats, both when choosing a room and in what the server publishes, so a seat is never promised twice; expired ones are dropped every second and whether it created the room; the player sees `&aEnviando para <code>...` and is moved there. When the lobby asks for a new room, the player first sees `&aSolicitando nova sala...`, and `&aSala criada!` after the room line when the server did create it. When they arrive, they enter the reserved room, or another room of the mode if it filled up meanwhile.
- **Tops**: four holograms (final kills, wins, current winstreak and level) with the top 100 in 10 pages. Each player sees their own page: a click (left or right) on the upper part (names and above) turns the page, and on the lower part (`Clique para trocar!` and below) changes the period (Semanal, Mensal, Total; Total first on the level top). On Total the level top shows the level badge; on Mensal and Semanal it ranks and shows the XP earned in that period (`120.806 XP`, `lobby.tops.xp-value`). Every click plays `CLICK` (`lobby.tops.click-sound`). Empty positions show `Ninguém` with `0` (`0 XP` on the Mensal and Semanal level top). Names are coloured with the highest tag the player has. The tops reload every 60 s, and a failed load is logged as a warning.
- **Mode selector**: the `SELECIONAR MODO` hologram from the export lists five modes (Geral, Solo, Duplas, Trios, Quartetos), the chosen one in green; a click opens `Selecionar modo`, and the choice applies to every top with modes (the level top has none). When the chosen mode is further down the menu, the list moves so it stays visible.
- **Scoreboard** (annex 1): `&b&lBED WARS` with the level, the XP bar, the wins and current winstreak of the two modes the player played most (1v1 and Solo while they have no games), `Jogadores` (players on the BedWars servers and lobbies) and the site. It replaces the lobby scoreboard through `core.sidebars()`.
- **Statistics NPC**: shows each player as themselves (their own skin), with their level, XP bar, wins, kills, final kills, beds and winstreak above it. Clicking it opens `Suas estatísticas (Bed Wars)`, with Geral (level and XP included) and every mode.
- **Missions NPC**: `MISSÕES` / `Clique para ver!` at 24.5 81 8.5 opens `Missões do Bed Wars` without `Voltar`; the `Missões` button of `Menu do Bed Wars` opens it with `Voltar`. See [Missions](#missions).
- **Hologram spacing**: the NPC lines follow the exported heights. Mode and missions NPCs use `lobby.npc-labels` (bottom line 2.25625 above the feet, then 0.28125, 0.46875 and 0.28125); the statistics NPC uses `lobby.stats-npc.labels`, with the larger gaps of the reference before `Vitórias`, `Winstreak` and `SUAS ESTATÍSTICAS`. The tops use `lobby.tops.spacing` (0.28125 under the header, 0.375 before the first name and before `Clique para trocar!`, 0.28125 between names, 0.34375 before the periods) and the selector list `lobby.tops.selector.list-spacing` (0.28125).
- **Menu rule**: a menu opened from another menu has `Voltar`; opened directly (NPC, item, command) it has none. `Cosméticos do Bed Wars` shows `Voltar` only when opened from `Menu do Bed Wars`, not from `/cosmeticos`.

### Missions

BedWars is the first game with [missions](../missions/README.md). It registers its catalog, its goals and its reward (the mission XP goes to the player's BedWars statistics, so it counts in the level and the tops); Muxe-Core does the rest.

| Goal | Counts | Missions |
|---|---|---|
| `KILL_STREAK` | best | Kills in a row in a match, back to 0 on death. Ninguém me para! (2). |
| `INVISIBLE_BED` | sum | Beds broken while invisible. Quem foi? (1). |
| `END_STONE_DEFENSE` | sum | 1 when the player places `end-stone-blocks` (10) End Stone within `end-stone-radius` (3) blocks of their own bed in a match. Proteção do Fim (1). |
| `CLEAN_WIN` | sum | Wins without placing TNT or throwing a Fireball. Rei das pontes (1). |
| `BEDBUG_KILLS` | sum | Kills of an enemy a Percevejo of the killer's team hit in the last `bedbug-assist-seconds` (10). Um aliado inesperado (3). |
| `EMERALDS_IN_A_LIFE` | best | Emeralds picked up from generators without dying. Colecionador de Esmeraldas (8). |
| `FIREBALL_DAMAGE` | sum | Damage dealt to other players with Fireballs. Sou eu bola de fogo (1.000). |
| `FINAL_KILLS` | sum | Final kills. Fim da linha (35). |
| `EXPLOSIVE_BLOCKS` | sum | Placed blocks destroyed by the player's TNT or Fireball. Olha a explosão (1.000). |
| `FAST_WIN` | sum | Wins within `fast-win-seconds` (210). Ansiedade mais de 8000 (1). |
| `CLAY_BOUGHT` | sum | Clay blocks bought. Artesão (50). |

Daily missions give 625 XP; weekly ones 2.500 XP and 15 credits; Complecionista (complete every weekly mission) gives 15 credits. Each player gets 3 of the 6 daily missions and the week's 5 weekly missions. The Halloween missions of the reference are not included.

The Percevejo can be bought now: thrown, its snowball spawns a silverfish of the thrower's team where it lands. For 15 seconds it chases the closest enemy within 16 blocks, never breaks blocks, drops nothing, and its kills count for the thrower.

### Statistics

- Every match end saves, for each player who played, the games, beds broken and lost, kills, deaths, assists, final kills, final deaths, final assists, wins, losses, winstreak and XP, per mode and in `geral`, for the day, week (Monday to Sunday), month and total, in the São Paulo time zone.
- An assist goes to every other enemy who hit the victim in the last 15 seconds. Kill, final kill and bed XP come from `xp-rewards`; at the end each player also gets the per minute, per teammate and win XP.
- The winstreak grows with each win and goes back to 0 on a loss; a draw keeps it. The best winstreak never goes down.
- The level comes from the total XP and starts at 1 (0 XP is level 1): leaving each level costs 5000 up to level 9, 10000 from 10 to 19, and 5000 more every 10 levels, so level 10 needs 45000 XP. The badge follows the level list (`&7[1✫]`, `&a[10✶]`... `&4[100❤]`... rainbow from 1000).

## Commands

| Command | Permission | Description |
|---|---|---|
| `/arena ...` | `muxe.minigame.setup` | Creates and sets up arenas, on the BedWars lobby only. See [arena setup](../../architecture/game-engine/README.md#arena-setup). |
| `/bedwars entrar <modo>` (`/bw`) | none | Moves the player to a room of that mode, while waiting. |
| `/g <mensagem>` | none | Talks to everyone in the room, with `&6[G]` before the message. In team matches the plain chat reaches only the team. |
| `/bedwars iniciar` | `muxe.bedwars.admin` | Starts the countdown at 10 seconds without the minimum players, for tests, showing the `10 segundos` title right away (a countdown already running at more than 10 seconds is shortened). A match started this way with a single team keeps going until that team is eliminated; with two or more teams it ends as usual, when one team is left. |
| `/bedwars finalizar` | `muxe.bedwars.admin` | Ends the match being played right away, for tests, as a real match ends: the team left standing wins (the only player of a forced match too), with the usual result, tops, mission summary, XP and stats; with several teams standing it is a draw. |
| `/cosmeticos` (`/cosmeticosbw`) | none | Opens Cosméticos do Bed Wars. |
| `/cosmeticos dar <jogador> <cosmetico>` | `muxe.cosmetics.admin` | Gives a bed destruction cosmetic, for event and award cosmetics. |
| `/bwconfig ver\|definir\|adicionar\|remover\|local\|recarregar ...` | `muxe.admin.config` | Views and changes any key of this plugin's `config.yml` in game, applying it live where a component supports it. See the [configuration editor](../../development/core-api/README.md#configuration-editor). |
| `/bwconfig previa <tipo> <ação> [valor]` | `muxe.admin.config` | Sets up the cosmetic previews from the world, live. See [Setting up the previews in game](#setting-up-the-previews-in-game). |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `muxe.minigame.setup` | op | Allows `/arena`. |
| `muxe.bedwars.admin` | op | Allows `/bedwars iniciar` and `/bedwars finalizar`. |
| `muxe.cosmetics.admin` | op | Allows `/cosmeticos dar`. |
| `muxe.admin.config` | op | Allows `/bwconfig`, and also `/lobbyconfig` and `/coreconfig`. Declared by Muxe-Core. |
| `muxe.bedwars.map-select` | op | Choosing the map in the mode menus (VIPs). Give it to the VIP groups. |
| `muxe.cosmetic.all` | op | Owns every BedWars cosmetic. |
| `muxe.cosmetic.vip` | false | Owns the `Incluso no VIP` cosmetics (the pine and eucalyptus woods). Give it to the `vip` group; the groups above inherit it. |
| `muxe.cosmetic.ultra` | false | Owns the `Incluso no ULTRA` cosmetics, and the VIP ones. The `ultra` group has it (migration V14). |
| `muxe.cosmetic.ultraplus` | false | Owns the `Incluso no ULTRA+` cosmetics, and the ULTRA and VIP ones. Ultra+ is not a group yet, so give it to those players directly. |

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
| `lobby-group` | `bedwars-lobby` | Where players go when a match ends or they leave the waiting room, and the group that receives the room lists. |
| `default-mode` | `solo` | The mode of the room a player joins on arrival. |
| `knockback.default` | `bedwars` | [Knockback profile](../../development/core-api/README.md#knockback) of the matches: vertical 0.6, horizontal 0.75, boosts 0.1 and 0.5, vertical limit 0.4, frictions 2.0, attacker multiplier 0.6, simulated Y, sprint boost ignored, normal click limiter. |
| `timings.*` | 5 s respawn, 180 s rejoin, 30 s restart, 3 s protection, island radius 17, protection radii 5/1/1, 60 s empty room | Match timings and protected areas; `empty-room-seconds` closes rooms left empty. |
| `countdown.titles` | `30`, `10`, `5`...`1` | Titles of the start countdown. |
| `preferences.*` | 4 rows, slots 11/13/15 | Texts, slots and dyes of the preferences menu. |
| `favourite-color.*` | 6 rows, `Nenhuma` at slot 10, colours at 11 to 16 and 19 to 21 | Title, option lore (`choose`, `chosen`), slot and name of each team colour (keyed by its id: `vermelho`, `azul`...) and the back button of the favourite colour menu. |
| `cosmetics.menus.categories.team-color` | slot 35, grey wool | The `Cor preferida` button of the cosmetics menu. Without it the slot stays a "coming soon" item. |
| `tnt-timer.*` | `{color}{seconds}s`, 2 decimals, 24 blocks, every tick | TNT timer format, height, view distance, update rate, colour thresholds and colours. |
| `item-names.*` | `IRON_INGOT: Barra de Ferro`... | Portuguese name of each item handed out in a match. |
| `sounds.*` | `CLICK`, `NOTE_PLING` (pitch 2), `ZOMBIE_UNFECT` (pitch 2), `NOTE_PLING`, `LEVEL_UP` (pitch 2), `VILLAGER_NO`, `CHEST_OPEN`, `CHICKEN_EGG_POP` (pitch 1.5), `ENDERDRAGON_GROWL`, `WITHER_DEATH`, `NOTE_PLING` (pitch 2) twice, `ZOMBIE_UNFECT` (pitch 2), `EXPLODE`, `ENDERDRAGON_GROWL`, `FIREWORK_LAUNCH` | `countdown`, `match-start`, `died`, `bought`, `upgrade`, `cannot-afford`, `ender-chest`, `deposited`, `bed-destroyed`, `bed-lost`, `kill`, `final-kill`, `eliminated`, `team-eliminated`, `match-end` and `match-end-firework`, each with `sound`, `volume` and `pitch`. A sound left empty plays nothing. |
| `forges.*` | the spec values | Island iron, gold and emerald, and the three diamond and emerald tiers with their start second. `stack-items: false` keeps items of different cells apart; `split-radius` (2) is how far teammates share iron and gold. |
| `events.*` | 1800, 2400, 2520 | Seconds of the beds destroyed, sudden death and game end events, and their names on the scoreboard. |
| `xp-rewards.*` | 10, 5, 250, 100, 12, 60 | XP per minute, per teammate, win, bed, kill and final kill. |
| `hotbar.waiting.*` | emerald and bed | Waiting room items. |
| `scoreboard.*` | annexes 2, 3, 5 and 12 | Scoreboard lines; `{teams}` and `{event}` become the team lines and the next event. |
| `holograms.*` | annexes 7 and 8 | NPC and generator holograms. |
| `shop.*`, `upgrades.*` | the exports | Shop categories, items, prices and texts; upgrades, tiers and prices. |
| `shop.hotbar-menus.*` | the exports | Titles, rows, slots, texts and refresh period of the three hotbar editor menus, and the names of the places `Voltar` goes to. |
| `cosmetics.menus.bedwars.hotbar` | slot 31 | The `Editor de hotbar` button of the Menu do Bed Wars. |
| `messages.match-started` | `O jogo iniciou!` and the alliance `AVISO:` | Sent when the match starts. |
| `messages.chest-kept`, `messages.chest-last-one` | `&cVocê não pode guardar este item.`, `&cVocê não pode guardar só um deste item.` | Said when a tool or the only sword would go into a chest. |
| `messages.already-have` | `&cVocê já tem este item.` | Said when buying something the player already has. |
| `messages.hotbar-slot-full` | `&cEste slot da hotbar já tem o máximo de entradas.` | Said when a slot already has 14 entries. |
| `messages.*` | annexes 4, 6, 9, 10, 11, 13, 14, 15 | Every match message. `setup-*` overrides the engine's setup messages. `cosmetic-*` and `preview-*` are the cosmetic messages. |
| `cosmetics.category` | `bedwars-cama` | Category under which ownership and selection are saved. |
| `cosmetics.prices.<stars>` | 750, 900, 1300, 1750, 2000 | Price in credits by stars. |
| `cosmetics.bed-destruction.<id>` | the 30 cosmetics | `name`, `icon` (item), `stars`, `tag`, `availability` (`SALE`, `ULTRA`, `EVENT`) and `description`. The id must match an animation id to have an animation. |
| `cosmetics.menus.*` | the exports | Titles, slots, items and lore lines of the three menus. |
| `cosmetics.victory-effect.*` | `bedwars-vitoria`; 300, 400, 500, 700, 800; the 22 effects | Category, prices by stars and victory effects, with the same fields as the bed ones (`stars: 0` hides the stars). Optional: the catalog comes from the [minigame API](../../architecture/minigame-api/README.md#cosmetics) (`cosmetics.yml`), and this section only overrides it (prices, fields, `hidden: true`, extra cosmetics). The category is always generated. |
| `cosmetics.final-kill.*` | `bedwars-kill-final`; 750, 900, 1300, 1750, 2000; the 43 effects | Category, prices by stars and final kill effects. Optional: the catalog comes from the [minigame API](../../architecture/minigame-api/README.md#cosmetics) (`cosmetics.yml`), and this section only overrides it (prices, fields, `hidden: true`, extra cosmetics). The category is always generated. |
| `final-kill-preview.*` | `previa_kill_final`, -3.5 24 2.5 facing east, target 4 blocks ahead, `&eAcerte o jogador!` | Like `victory-preview`, with `target.distance` and `target.action-bar` instead of a countdown. |
| `end-titles.victory`, `end-titles.defeat` | `VITÓRIA` `&6` `&e` `&f`; `DERROTA` `&c` `&c` `&f` | Word, colour after the sweep, colour of the sweeping letter, starting colour and an optional `sound`. |
| `victory-preview.*` | `previa_efeito_de_vitoria`, -19.5 27 68.5, middle 0.5 0.5, `3...` `2...` `1...` every 20 ticks, `60s`, `1s` | World, spawn, middle of the map (`middle.x`, `middle.z`, used by effects built around it, like the O End pillars; without it the middle is the viewer), countdown titles and ticks between them, timeout, wait after the effect, back item and scoreboard of the victory preview. |
| `cosmetics.aura.*` | `bedwars-aura`; 750, 900, 1300, 1750, 2000; `liga-mestre` and `liga-diamante` | Category, prices by stars and the two auras, with the same fields as the bed ones. The id must match an aura of `Auras` to have an effect. Optional: the catalog comes from the [minigame API](../../architecture/minigame-api/README.md#cosmetics) (`cosmetics.yml`), and this section only overrides it (prices, fields, `hidden: true`, extra cosmetics). The category is always generated. |
| `cosmetics.menus.categories.aura`, `cosmetics.menus.aura.*` | slot 33, emerald; `Aura ({page}/{pages})`, `Aleatório` as a name tag | The category button and the list overrides of the aura category (the rest follows the bed destruction list). |
| `kill-messages.*` | `default: padrao`, `line-color: &7`, `bed-prefix`, `preview`, the sets `padrao` and `estatistico` | The chat message sets: the default set, the colour that starts every message, the prefix of the bed message, the sample of the `Prévia:` (`title`, `victim`, `team-color`, `team`, `counts` by kind) and `sets.<id>` with its `preview` kinds and its `messages` by kind (`kill`, `final-kill`, `void-kill`, `void-final-kill`, `death`, `final-death`, `void-death`, `void-final-death`, `bed`). `{br}` breaks the preview line. They replace the old `messages.kill`, `final-kill`, `death`, `void-*` and `bed-destroyed`. Optional: the catalog comes from the [minigame API](../../architecture/minigame-api/README.md#cosmetics) (`cosmetics.yml`), and this section only overrides it (prices, fields, `hidden: true`, extra cosmetics). The category is always generated. |
| `cosmetics.kill-message.*` | `bedwars-mensagem-kill`; `default: padrao`; `padrao` and `estatistico` | Category, default cosmetic and the sets, with the same fields as the bed ones (`stars: 0`, no prices). The id must match a set of `kill-messages.sets`. Optional: the catalog comes from the [minigame API](../../architecture/minigame-api/README.md#cosmetics) (`cosmetics.yml`), and this section only overrides it (prices, fields, `hidden: true`, extra cosmetics). The category is always generated. |
| `cosmetics.wood-type.*` | `bedwars-madeira`; `default: carvalho`; the 12 woods | Category, default cosmetic and woods. `icon` (`WOOD` or `LOG`, `LOG_2` and `data`) is the item the shop delivers; `availability` is `DEFAULT`, `VIP`, `ULTRA`, `ULTRA_PLUS` or `EVENT`. |
| `cosmetics.menus.categories.kill-message`, `cosmetics.menus.categories.wood-type` | slot 31 paper; slot 29 planks | The category buttons. |
| `cosmetics.menus.kill-message.*`, `cosmetics.menus.wood-type.*` | titles, `entries`, `lore`, `selected` | The list overrides. `entries` has `none` and `random` (show those entries), `random-after` (cosmetics before `Aleatório`), `owned-first` (owned first, as the other lists, or the catalog order), `previews` (right click) and `selected-icon` (the selected item shows the cosmetic). `lore.header` starts every cosmetic lore, `lore.vip-tag`, `lore.ultra-tag` and `lore.ultra-plus-tag` are the rank lines. Lore keys left out are inherited from the bed destruction list. |
| `cosmetics.projectile-trail.*` | `bedwars-rastro`; 300, 350, 500, 700, 800; the 23 trails | Category, prices by stars and the trails, with the same fields as the bed ones. The id must match a trail of `ProjectileTrails` to be animated. Optional: the catalog comes from the [minigame API](../../architecture/minigame-api/README.md#cosmetics) (`cosmetics.yml`), and this section only overrides it (prices, fields, `hidden: true`, extra cosmetics). The category is always generated. |
| `cosmetics.menus.categories.projectile-trail`, `cosmetics.menus.projectile-trail.*` | slot 27, arrow; `Rastros de Projétil ({page}/{pages})`, `Aleatório` as a name tag | The category button and the list overrides of the trail category (the rest follows the bed destruction list). |
| `projectile-trail-preview.*` | `previa_aura`, 10.5 19 21.5 facing yaw 180, `60s`, `1s` | World, spawn, timeout, back item and scoreboard of the trail preview, like `aura-preview`. |
| `projectile-trail-preview.kit.items.<slot>`, `projectile-trail-preview.kit.refill-ticks` | the six kit items of the recording; `2` | The items given instead of a scripted scene (same fields as any item; the slot counts from 0, 9 and up is the main inventory) and the ticks after a launch before short stacks are filled back (at least 1). Any scene preview section with `kit` works this way. |
| `aura-preview.*` | `previa_aura`, 10.5 19 21.5 facing yaw 180, `60s`, `1s` | World, spawn, timeout, back item and scoreboard of the aura preview. It has no countdown. |
| `preview.world` | `previa_quebra_de_cama` | Folder of the preview world in the server root. Without it the preview is off. |
| `preview.spawn`, `preview.bed` | 6.5 11 21.5; foot 6 11 17, head 6 16 | Where the player stands and the bed they break. |
| `preview.timeout`, `preview.return-delay` | `120s`, `2s` | Time to break the bed and wait after the animation. |
| `preview.back-item` | slot 8 (the 9th), `BED`, `&aVoltar ao Lobby` | The item that ends the preview. The slot counts from 0. |
| `preview.action-bar`, `preview.scoreboard` | `&eQuebre a cama!`, MUXE / Visualizando | Preview texts; `{name}` is the cosmetic. |
| `messages.preview-config-*` | the answers of `/bwconfig previa` | Texts of the preview shortcuts; the generic answers are `messages.config.*` in Muxe-Core. |
| `lobby.menu-item` | slot 3, emerald | Menu do Bed Wars item on the BedWars lobby. |
| `levels.badges`, `levels.rainbow` | the level list | Badge of each level range; from `rainbow.from` (1000) the digits take the rainbow colours. |
| `lobby.npcs.<id>` | the exports | `action` (a mode id, `duelos` or `nenhuma`), `location`, signed `skin`, `players` (modes counted in `{players}`) and `lines`; `{blink}` takes the next colour of `lobby.blink-colors` every 0.5 s. |
| `lobby.stats-npc` | 22.5 81 -1.5 | Position, lines, XP bar size and `labels` (line heights) of the statistics NPC. |
| `lobby.npc-labels` | the exports | `bottom`, `gaps` and `spacing` of the mode and missions NPC lines. |
| `lobby.tops.spacing.*` | the exports | `title`, `list`, `entries`, `click` and `periods` gaps of the tops. |
| `missions.*` | the exports | The [missions](../missions/README.md#configuration) catalog and texts, plus `fast-win-seconds` (210), `end-stone-radius` (3), `end-stone-blocks` (10) and `bedbug-assist-seconds` (10). |
| `lobby.tops.boards.<id>` | the four tops | `category` (`FINAL_KILLS`, `WINS`, `WINSTREAK`, `LEVEL`), `title`, `location` of the top stand, `periods` in display order and `modes`. |
| `lobby.tops.*` | the exports | Header, entry, empty (`Ninguém` - `0`), click and period texts, `label-above-stand` (2.38125, where the 1.8 client draws the name of a full-size stand), `refresh-seconds` (60), `click-sound` (`CLICK`, empty for none) and `xp-value` (`{xp} XP`, the level top outside Total). |
| `lobby.tops.selector.*` | the export | `location` of the `SELECIONAR MODO` stand, `list-offset` and `footer-offset` (0.375 and 1.90625 below it), `list-spacing` (0.28125), `visible-modes` (5) and the header, footer, selected, other and `general-name` texts. |
| `lobby.scoreboard.*` | annex 1 | `title`, `lines` (`{level}`, `{bar}`, `{players}`; `{modes}` becomes the mode lines), `mode-lines` (`{mode}` and the counters), `default-modes` (1v1, solo), `shown-modes` (2) and `bar-segments` (10). |
| `lobby.menus.*` | the exports | Mode, duels, maps, top mode and statistics menus. |

## Packets

| Type | From → to | Delivery | Purpose |
|---|---|---|---|
| `bedwars.rooms` | BedWars servers → `lobby-group` | volatile, 10 s | Rooms (code, mode, arena, open, players, max) and ready arenas of the server, every 2 s. |
| `bedwars.join_request` | BedWars lobby → one BedWars server | request, 5 s | Reserves a room of a mode, optionally of an arena or a given room. |
| `bedwars.join_reply` | BedWars server → lobby | reply | Whether a room was reserved, its code, and whether it was created for this request. |
| `minigame.arena_saved` | BedWars lobby → `bedwars` group | standard | An arena was saved; the BedWars servers reload it from PostgreSQL. |

## Data

| File | What it holds |
|---|---|
| `core.arenas` (game `bedwars`) | One row per arena: mode, `ready`, the definition (display name, teams, points and heights, in YAML) and the map in the [Muxe map format](../../decisions/0009-own-map-format-and-match-cells.md). Migration `V16__create_arenas.sql`. Shared by every BedWars server. |
| `core.owned_cosmetics`, `core.selected_cosmetics` | Owned and selected cosmetics, category `bedwars-cama` (and `bedwars-vitoria`, `bedwars-kill-final`, `bedwars-aura`, `bedwars-rastro`, `bedwars-mensagem-kill` and `bedwars-madeira`). See [Credits and cosmetics](../credits/README.md). |
| `<server>/previa_quebra_de_cama/` | The preview world. Deployed to the BedWars lobby only, never committed. |
| `<server>/previa_aura/` | The aura preview world. Deployed to the BedWars lobby only, never committed. |
| `bedwars.player_stats` | One row per player, mode (and `geral`), period (`diario`, `semanal`, `mensal`, `total`) and period start, with every counter and the XP. Migration `V1__create_player_stats_and_favourite_maps.sql`. |
| `bedwars.recorded_matches` | The id of every match already saved, claimed in the same transaction as its stats, so a match saved twice counts once. Migration `V2__create_recorded_matches.sql`. |
| `bedwars.favourite_maps` | Favourite arenas of each player. |
| `bedwars.preferences` | The BedWars preferences of each player (`show_level`, `tnt_timer`, `chest_deposit`). Migration `V4__create_preferences.sql`. |
| `bedwars.quick_buy` | The quick buy of each player: the 21 item ids in order (`''` for an empty position). Migration `V3__create_quick_buy.sql`. |
| `bedwars.favourite_team_color` | The favourite team colour of each player (`team_color`, the colour id). A player without a favourite has no row. Migration `V6__create_favourite_team_color.sql`. |
| `bedwars.hotbar_layout` | The hotbar layout of each player: 9 texts, one per slot, each a comma-separated list of entries (`c:<category id>` or `i:<item id>`, `''` for an empty slot). Migration `V5__create_hotbar_layout.sql`. |

Commands and queries in `br.com.muxe.minigame.bedwars.stats`: `RecordMatchCommand`, `ToggleFavouriteMapCommand`, `FindPlayerStatsQuery`, `FindTopQuery`, `FindFavouriteMapsQuery`.

## Metrics

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrations

- [Lobby](../lobby/README.md): `/jogar bedwars`.
- [Credits and cosmetics](../credits/README.md): prices, purchases and ownership.
- [Game engine](../../architecture/game-engine/README.md).
- [Minigame API](../../architecture/minigame-api/README.md).
- [Party](../party/README.md): only the party leader picks a mode, a map or "play again"; the party goes to a room with a seat for everyone and stays on one team. When a party has to be split across teams, `AVISO Esta partida não conta estatísticas.` is said and its players are left out of `RecordMatchCommand` and the XP of the match.

## Acceptance criteria

- [x] Modes are loaded from configuration (engine tests).
- [x] The nine modes have the limits and teams of the spec (`BedWarsSchemaTest`).
- [x] An arena can be set up in game, with a checklist of what is missing (engine tests).
- [x] Scoreboards, team indicators and the forge, timeline, kill credit, level, XP and ranking rules follow the spec (`RoomTest`, `GameRulesTest`, `GeneratorTimerTest`, and the minigame API tests `MatchTest`, `MatchRuntimeTest`, `KillCreditTest`, `TimelineTest`, `RankingTest`).
- [x] Shop and upgrade names and lore follow the exports (`ShopCatalogTest`, `UpgradeCatalogTest`).
- [x] The hotbar editor menus, the one-slot-per-entry layout, the storage and the placement of bought items follow the rules above (`HotbarLayoutTest`, `HotbarPlacementTest`, `HotbarEditorTest`, `HotbarHandlersTest`, `PostgresHotbarRepositoryIntegrationTest`).
- [x] The favourite colour menu, its storage and the team assignment by favourite colour follow the rules above (`FavouriteColorMenuTest`, `FavouriteColorHandlersTest`, `PostgresFavouriteColorRepositoryIntegrationTest`, `TeamAssignerTest`, `RoomTest`).
- [x] The 13 animations end without leftovers and follow the recorded timings (`BedAnimationsTest`).
- [x] Cosmetic states, prices, sorting and lore follow the exports (`CosmeticCatalogTest`).
- [x] The kill message sets, their recorded previews, whose set is used, the lifetime counters, the fallback and the random draw follow the rules above (`MessageSetsTest`, `KillAnnouncerTest`, `KillMessagesTest`, `LifetimeTallyTest`, `OrdinalTest`, `KillMessageCosmeticTest`).
- [x] The wood types menu, the rank inclusion and the wood the shop delivers follow the rules above (`WoodTypeCosmeticTest`, `WoodTypesTest`, `WoodDeliveryTest`, `CosmeticAccessTest`, `CosmeticListSettingsTest`).
- [x] Deaths, assists, beds lost, XP and match outcomes are counted (`RoomStatsTest`); periods, counters, tops and favourites are saved (`StatsHandlersTest`, `StatCountersTest`, `StatsPeriodTest`, `PostgresBedWarsStatsRepositoryIntegrationTest`).
- [x] Matchmaking picks the fullest open room or asks the emptiest server for one (`MatchmakerTest`).
- [x] Lobby NPCs, menus, tops, the mode selector, level badges, the statistics NPC and the lobby scoreboard follow the exports and annex 1 (`LobbyViewTest`, `LobbyFormatTest`).
- [ ] A full match checked in game.
- [ ] The hotbar editor and the placement of purchases checked in game.
- [ ] Lobby NPCs, menus, matchmaking, map choice and tops checked in game.
- [x] The preview shortcuts write the right keys, find both halves of a bed from either one, refuse bad values and apply live while running sessions keep their settings (`PreviewEditsTest`, `BedBlocksTest`, `BedWarsConfigCommandTest`).
- [x] The trail catalog and menu, the tag of `Ultra`, whose trail is used, the flight lifecycle, the projectile kinds and the preview kit follow the rules above (`ProjectileTrailCosmeticTest`, `CosmeticIconsTagTest`, `TrailCosmeticsTest`, `TrailFlightTest`, `TrailFlightsTest`, `ProjectileKindsTest`).
- [ ] Every animation and the preview checked in game.
- [ ] The projectile trails and their preview kit checked in game.
- [ ] `/bwconfig previa` checked in game on the lobby.
- [ ] The kill messages in a real match and the wood types in the shop checked in game.

## Failures and recovery

If Muxe-Core is not enabled, the plugin disables itself.

## Known limitations

- MEGA and PILARES are not modes yet, so their NPCs open nothing.
- The bow of the trail preview kit glows instead of carrying Infinity I (enchantments cannot be read from the configuration without a server in the tests); the arrow it shoots is given back by the refill instead.
- A player who reaches a BedWars server without a reservation still joins a room of `default-mode`.
- The lobby only knows the rooms of servers that published in the last 10 seconds; the room list of a server that stops is forgotten after that.
- The other special shop items, the dragons and the victory and defeat titles are not in yet.
- A restart or crash ends every match on the server without compensation.
- `Skin dos Vendedores`, `Rastros de Projétil` and the cosmetic shop are not in yet. The favourite colour is read when the player joins the server, so a choice made on another server is seen only after the player joins again.
- A purchase updates the menu at once, but another server sees it only when the player joins it again.
- The `Estatístico` counters are the totals read when the player enters the room plus what happens in that match; they are not refreshed during it. The selected kill message set and wood are read when the player joins the server, like the other cosmetics.
