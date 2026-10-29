**English** | [Português](README.pt-BR.md)

# Ranks and tags

Ranks (groups of permissions), tags (what is shown before a player's name), medals (a symbol shown before the tag in the chat) and the in-game commands to manage them. The model and storage live in `core:common` (package `br.com.muxe.core.common.permission`); every Bukkit server applies them through Muxe-Core (`core:bukkit` package `permission`), so `hasPermission`, the chat, the tab list and the name above the head follow the same ranks on the whole network.

## User stories

- As a player, I want my rank and my tag shown in the chat, in the tab list and above my head, so that everyone sees them.
- As a player, I want to list the tags I own, preview them and pick one with a click.
- As a player, I want to pick one of my medals, with a preview, to show it before my tag in the chat.
- As a staff member, I want to give and take medals, temporary or for good.
- As a staff member, I want to give and remove ranks, temporary or for good, promote and demote along a track, and see what a player has, without leaving the game.
- As a staff member, I want to set permissions on players and ranks with tab completion, and test what a player can do.
- As an admin, I want to open the web editor from the game.

## How it works

### Hierarchy

Ranks are groups. Each rank inherits the one below it, so a higher rank has every permission of the lower ones. Every player is `membro` without anything being stored. Admin has `*`.

| Rank | Weight | Tag | Inherits |
|---|---|---|---|
| `membro` | 0 | `&7Membro` (no prefix) | |
| `vip` | 10 | `&aVIP` | `membro` |
| `mvp` | 20 | `&9MVP` | `vip` |
| `pro` | 30 | `&6Pro` | `mvp` |
| `ultra` | 40 | `&dUltra` | `pro` |
| `partner` | 110 | `&bPartner` | `ultra` |
| `partner+` | 120 | `&1Partner+` | `partner` |
| `mod` | 130 | `&5Mod` | `partner+` |
| `mod+` | 140 | `&5Mod+` | `mod` |
| `admin` | 150 | `&cAdmin` | `mod+`, plus `*` |

Tracks list ranks in order for `/cargo promover` and `/cargo rebaixar`: `doacao` (`membro`, `vip`, `mvp`, `pro`, `ultra`) and `staff` (`mod`, `mod+`, `admin`).

A rank is a node `group.<id>` on the player. Nodes have a value (`true` or `false`), optional contexts (`server=<server group>`, `world=<world>`), an optional expiry and the dates they were added and last updated. A node with contexts only applies where every context matches.

### Tags

A tag has a name, a color, a weight and a prefix. Its display is the color and the name (`&aVIP`); its chat prefix is the color, bold, the name in capitals and a space (`&a&lVIP `). `membro` has an empty prefix, so a member's name is just gray (`&7tadeu`).

- **Rank tags** come with their rank: holding `vip` (or anything above it) gives the `vip` tag.
- **Decorative tags** give no permission; they are only something to show: `2021` to `2026` (`&b`), `beta` (`&1`), `enderlore` (`&5`), `carnaval` (`&6`), `natal` (`&c`), `ferias` (`&a`) and `ferias-2` (`&2`). A player owns one through the node `muxe.tag.<id>`, given with `/tag dar`.

The tag a player shows is, in order: the tag they picked with `/tag` (if they still own it), the tag of their highest rank, or their highest tag.

### Permissions on Bukkit servers

- `AsyncPlayerPreLoginEvent` loads the player's nodes from PostgreSQL (up to 5 seconds) before they join. If it fails, they join as `membro` and are updated when the load finishes.
- On login, the player's permissible (`CraftHumanEntity#perm`) is replaced, by reflection, with one that asks the resolved ranks first. When a rank says nothing about a permission, Bukkit decides as before (op and the defaults in `plugin.yml`), so operators keep working. If the field cannot be replaced, the permissions are copied into a `PermissionAttachment` instead (wildcards then only work for permissions written in full).
- Permissions are resolved with the contexts `server=<server.group>` and `world=<world name>`, and again when the player changes world, when their nodes or any rank or tag change anywhere on the network (`permission.changed`), and when a temporary node expires (checked every second).
- On quit, the original permissible is put back and the player is forgotten. Disabling the core puts back the permissible of everyone online.

### Prefixes

- **Chat:** `messages.chat-format`, by default `{medal}{prefix}{color}{player}: &f{message}`: `&a&lVIP &atadeu: &fOlá!`, or `&7tadeu: &fOlá!` for a member. With a medal chosen, its symbol and a space come first: `&e☕ &5&lENDERLORE &5tadeu: &fOlá!`.
- **Tab list and name above the head:** one scoreboard team per rank and tag. They show only the tag, never the medal, because 1.8 allows 16 characters of prefix. The team prefix is the chat prefix followed by the tag color (`§a§lVIP §a`), cut to the 16 characters 1.8 allows without breaking a color code. Teams are named by rank weight, then tag weight (`849849admin` comes before `989989vip`), so the 1.8 tab list sorts players by hierarchy.
- The teams are put on every scoreboard players use (the main one and each player's sidebar) within 5 ticks, and updated as soon as a tag or rank changes.
- The lobby scoreboard line `Tag:` shows the tag display (`&aVIP`), and the profile, `/account` and `/accountmenu` cite ranks the same way (see [Account](../account/README.md)).

### `/tag`

- `/tag` lists the tags the player owns in one chat line, from the lowest weight to the highest:

```text
Suas tags: Membro, VIP, MVP, Pro, Ultra, Enderlore, Carnaval, Natal
```

- `Suas tags: ` is `&a`, each tag is in its color and the commas are `&f`. Each tag runs `/tag <id>` when clicked and, on hover, shows a preview of the chat with that tag (and the medal the player shows, if any):

```text
Prévia:
VIP tadeu: Olá!

Clique para selecionar!
```

- `/tag <id>` selects a tag the player owns: `&aVocê selecionou a tag &5Enderlore &acom sucesso.` The choice is saved in PostgreSQL (`SelectTagCommand`) and the chat, tab list, name tag and scoreboard change at once on every server.
- A tag the player does not own is refused (`tag-locked`), an unknown one too (`tag-unknown`). If a selected tag is lost later, the player shows the tag of their rank again.

### Medals

A medal is a symbol in a color (`&e☕`) with a name (`Café`) and a weight. A player owns a medal through the permission `muxe.medal.<id>`, given to a rank or to the player; Admin owns every medal through `*`. Nobody shows a medal until they pick one: there is no default medal.

| Id | Medal | Weight | Owned by |
|---|---|---|---|
| `toxico` | `&a☣` Tóxico | 10 | VIP and above |
| `yin-yang` | `&f☯` Yin Yang | 20 | VIP and above |
| `musico` | `&5♫` Músico | 30 | Ultra and above |
| `flash` | `&6⚡` Flash | 40 | Ultra and above |
| `paz-e-amor` | `&c✌` Paz e Amor | 50 | Ultra and above |
| `amor` | `&4❤` Amor | 60 | Ultra and above |
| `cafe` | `&e☕` Café | 70 | Ultra and above |
| `carinha` | `&6ツ` Carinha | 80 | VIP and above |
| `halloween` | `&4✝` Halloween | 90 | given individually |
| `natal` | `&b✽` Natal | 100 | given individually |
| `cacador-de-bruxas` | `&2∅` Caçador de Bruxas | 110 | given individually |
| `casa-de-gengibre` | `&6۩` Casa de Gengibre | 120 | given individually |
| `alerta` | `&e⚠` Alerta | 130 | given individually |
| `atomo` | `&b⚛` Átomo | 140 | given individually |
| `anel` | `&6℧` Anel | 150 | given individually |
| `boneco-de-neve` | `&f☃` Boneco de Neve | 160 | given individually |
| `carnaval` | `&6✴` Carnaval | 170 | given individually |
| `feiticeiro-aprendiz` | `&a⊰⊱` Feiticeiro Aprendiz | 180 | given individually |
| `biscoito-de-gengibre` | `&6۞` Biscoito de Gengibre | 190 | given individually |
| `liga-prata` | `&f✶` Liga Prata (Bed Wars Champions) | 200 | given individually |
| `fogo` | `&c火` Fogo | 210 | given individually |
| `agua` | `&b水` Água | 220 | given individually |
| `ar` | `&f気` Ar | 230 | given individually |
| `estrela` | `&e✯` Estrela | 240 | given individually |
| `sortudo` | `&2☘` Sortudo | 250 | given individually |
| `carta` | `&f回` Carta | 260 | given individually |
| `modo-aviao` | `&b✈` Modo Avião | 270 | given individually |
| `celular` | `&a✆` Celular | 280 | given individually |
| `flor` | `&d✿` Flor | 290 | given individually |
| `briga-de-espadas` | `&7⚔` Briga de Espadas | 300 | given individually |
| `imperial` | `&6⚜` Imperial | 310 | given individually |

- VIP, MVP and Pro own 3 medals (`carinha`, `toxico`, `yin-yang`); Ultra and every rank above own 8. Seasonal and shop medals belong to no rank: staff give them one by one with `/medalha dar`. Buying a shop pack that lists medals does not give them yet.

### `/medalha`

- `/medalha` (or `/medalhas`, `/medal`, or the `Medalhas` button of the profile menu) lists, in one chat line, `Nenhuma` and then every medal the player owns, from the lowest weight to the highest:

```text
Medalhas disponíveis: Nenhuma, ☣, ☯, ♫, ⚡, ✌, ❤, ☕, ツ
```

- `Medalhas disponíveis: ` is `&a`, `Nenhuma` is `&7`, each medal is its symbol in its color and the commas are `&f`. Clicking `Nenhuma` runs `/medalha nenhuma`; clicking a medal runs `/medalha <id>`.
- On hover, a medal shows its name in its color and a preview of the chat with the tag the player shows now:

```text
Café

Prévia:
☕ ENDERLORE tadeu: Olá!

Clique para selecionar!
```

- `Nenhuma` shows the same hover with `Nenhuma` as the first line and the preview without a medal.
- `/medalha <id>` selects a medal the player owns: `&aVocê selecionou a medalha &e☕ &eCafé &acom sucesso.` The choice is saved in PostgreSQL (`SelectMedalCommand`) and the chat changes at once on every server. `/medalha nenhuma` removes it: `&aVocê removeu a sua medalha.`
- A medal the player does not own is refused (`medal-locked`), an unknown one too (`medal-unknown`).
- If the player loses the chosen medal later (a rank ends, staff take it), it simply stops showing; the choice stays saved and comes back if they get the medal again.

### Temporary ranks and tags

- `[tempo]` is written as amounts and units, together: `30d`, `12h`, `1d12h`, `2w` (`s`, `m`, `h`, `d`, `w`). `permanente` (or nothing) means for good; it shows as `Vitalício`.
- Giving a temporary rank or tag the player already has for a time adds the time to what is left (`30d` with 10 days left ends in 40 days). Giving it to a player who has it for good is refused.
- When the time ends, the rank stops counting on the next check (within a second) on every server, without anyone running a command. The node stays stored, expired.

### Management

- Players are found by their name, online or not, through their [account](../accounts/README.md).
- Every change goes through `PermissionSync`: it is saved in PostgreSQL, logged in `perm_actions` with the name of whoever ran the command (`CONSOLE` from the console) and announced to the whole network with `permission.changed`.
- Replies are sent on the main thread after the database answers; if PostgreSQL fails, the sender gets `rank-failed`.
- Everything completes with tab: subcommands, online players, ranks, tags (only owned tags for `/tag <id>`, only decorative tags for `/tag dar`), medals (`nenhuma` and the owned medals for `/medalha <id>`, every medal for `/medalha dar` and `remover`), tracks, times (`permanente`, `1h`, `12h`, `1d`, `7d`, `15d`, `30d`, `60d`, `90d`, `365d`), permissions already in use (on ranks, from `plugin.yml` files, `group.*`, `muxe.tag.*` and `muxe.medal.*`) and options (`true`, `false`, times, `server=<group>` of live servers, `world=<world>` of this server).
- `/permissao ... definir` takes up to four options after the permission, in any order: `true`/`false` (default `true`), a time and contexts (`server=bedwars world=arena`). `remover` takes the contexts of the node to remove.
- `/permissao jogador testar` checks the permission with the contexts of the server and world where the player is when they are online here, and with no context otherwise.
- `/editor` opens a session of the web editor (`EditorSessions`): a token valid for 30 minutes, stored hashed in Redis. Players get a clickable link to `<permissions.editor-url>/#<token>` with a hover; the console gets the plain link.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/tag` | Lists your tags, clickable, with a preview. | everyone |
| `/tag <tag>` | Selects one of your tags. | everyone |
| `/tag dar <jogador> <tag> [tempo]` | Gives a decorative tag. | `muxe.tags.manage` |
| `/tag remover <jogador> <tag>` | Takes a decorative tag. | `muxe.tags.manage` |
| `/medalha` | Lists your medals, clickable, with a preview. | everyone |
| `/medalha <medalha>` | Selects one of your medals. | everyone |
| `/medalha nenhuma` | Removes your medal. | everyone |
| `/medalha dar <jogador> <medalha> [tempo]` | Gives a medal (the node `muxe.medal.<id>`). | `muxe.medals.manage` |
| `/medalha remover <jogador> <medalha>` | Takes a medal given with `/medalha dar`. | `muxe.medals.manage` |
| `/cargo ver <jogador>` | Main rank, tag shown and every rank held with its time left. | `muxe.ranks.manage` |
| `/cargo dar <jogador> <cargo> [tempo]` | Gives a rank, for good or for a time. | `muxe.ranks.manage` |
| `/cargo remover <jogador> <cargo>` | Takes a rank. | `muxe.ranks.manage` |
| `/cargo promover <jogador> <trilha>` | Replaces the player's rank on the track with the next one. | `muxe.ranks.manage` |
| `/cargo rebaixar <jogador> <trilha>` | Replaces it with the previous one (or none, below the first). | `muxe.ranks.manage` |
| `/cargo listar` | Ranks from the highest, with id, weight and parents, and the tracks. | `muxe.ranks.manage` |
| `/cargo info <cargo>` | Weight, tag, chat prefix, parents and permissions of a rank. | `muxe.ranks.manage` |
| `/permissao jogador ver <jogador>` | The player's own nodes. | `muxe.permissions.manage` |
| `/permissao jogador definir <jogador> <permissão> [opções]` | Sets a node on the player. | `muxe.permissions.manage` |
| `/permissao jogador remover <jogador> <permissão> [contexto]` | Removes a node from the player. | `muxe.permissions.manage` |
| `/permissao jogador testar <jogador> <permissão>` | Tells whether the player has the permission. | `muxe.permissions.manage` |
| `/permissao cargo ver <cargo>` | Same as `/cargo info`. | `muxe.permissions.manage` |
| `/permissao cargo definir <cargo> <permissão> [opções]` | Sets a node on the rank. | `muxe.permissions.manage` |
| `/permissao cargo remover <cargo> <permissão> [contexto]` | Removes a node from the rank. | `muxe.permissions.manage` |
| `/editor` | Opens a 30-minute session of the web editor. | `muxe.permissions.editor` |

Aliases: `/tags`, `/medalhas` and `/medal`, `/cargos` and `/rank`, `/permissoes` and `/perm`, `/editorpermissoes`; `promote` and `demote` for `promover` and `rebaixar`. `membro` cannot be given or taken: everyone has it. A rank change that would break the rules (a rank inheriting itself, an unknown parent) is refused with `perm-group-refused`.

Examples:

```text
/cargo dar tadeu vip
/cargo dar tadeu ultra 30d
/cargo promover tadeu doacao
/tag dar tadeu natal 15d
/medalha dar tadeu natal 30d
/permissao jogador definir tadeu muxe.lobby.fly server=lobby 7d
/permissao cargo definir vip muxe.kit.vip false world=arena
/permissao jogador testar tadeu muxe.build
```

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.tags.manage` | `/tag dar` and `/tag remover`. | op |
| `muxe.medals.manage` | `/medalha dar` and `/medalha remover`. | op |
| `muxe.ranks.manage` | `/cargo`. | op |
| `muxe.permissions.manage` | `/permissao`. | op |
| `muxe.permissions.editor` | `/editor`. | op |
| `muxe.tag.<id>` | Owning the tag `<id>`. Rank tags come with the rank; decorative tags are given with `/tag dar`. | |
| `muxe.medal.<id>` | Owning the medal `<id>`. Seeded on `vip` and `ultra` (see [Medals](#medals)); other medals are given with `/medalha dar`. | |
| `group.<id>` | Holding the rank `<id>`. | |
| `*` | Everything. Seeded on `admin`, so Admin has every permission above. | |

## Configuration

`plugins/Muxe-Core/config.yml`:

```yaml
permissions:
  editor-url: "http://151.244.40.191:1031"
```

| Key | Default | Description |
|---|---|---|
| `permissions.editor-url` | `http://151.244.40.191:1031` | Address of the web editor. The link is `<editor-url>/#<token>`. |
| `messages.chat-format` | `{medal}{prefix}{color}{player}: &f{message}` | Chat format. `{medal}` is the chosen medal and a space (empty without one), `{prefix}` the tag's chat prefix, `{color}` its color. A `config.yml` written before medals has no `{medal}`: add it to show medals in the chat. |
| `menus.profile.buttons.medals.command` | `medalha` | Command run by the `Medalhas` button of the profile menu. |

Ranks, tags and tracks are data, not configuration: change them with the commands, the editor or in PostgreSQL.

### Messages

All in `messages` of `plugins/Muxe-Core/config.yml`:

| Key | Default |
|---|---|
| `no-permission` | `&cVocê não possui permissão para usar este comando.` |
| `tag-list-header`, `tag-list-separator` | `&aSuas tags: `, `&f, ` |
| `tag-hover` | List: `&fPrévia:`, `{preview}`, `""`, `&eClique para selecionar!` |
| `tag-preview-message` | `Olá!` (the message in the preview) |
| `tag-none` | `&cVocê ainda não possui nenhuma tag.` |
| `tag-selected` | `&aVocê selecionou a tag {tag} &acom sucesso.` |
| `tag-unknown`, `tag-locked` | `&cA tag &f{tag} &cnão existe.`, `&cVocê não possui a tag {tag}&c.` |
| `tag-not-decorative` | `&cA tag {tag} &cvem de um cargo. Use &f/cargo dar&c.` |
| `tag-given`, `tag-removed` | `&aTag {tag} &adada para &f{player} &7({time})&a.`, `&aTag {tag} &aremovida de &f{player}&a.` |
| `tag-already-held`, `tag-not-held` | `&f{player} &cjá possui a tag {tag} &cpara sempre.`, `&f{player} &cnão possui a tag {tag}&c.` |
| `tag-usage`, `tag-usage-staff` | Lists: `&cUso: &f/tag [tag]` for players, every subcommand for staff. |
| `medal-list-header`, `medal-list-separator`, `medal-list-none` | `&aMedalhas disponíveis: `, `&f, `, `&7Nenhuma` |
| `medal-hover` | List: `{name}`, `""`, `&fPrévia:`, `{preview}`, `""`, `&eClique para selecionar!` (`{name}` is the name in the medal color) |
| `medal-none-hover` | List: `&7Nenhuma`, `""`, `&fPrévia:`, `{preview}`, `""`, `&eClique para selecionar!` |
| `medal-preview-message` | `Olá!` |
| `medal-selected`, `medal-cleared` | `&aVocê selecionou a medalha {medal} &acom sucesso.`, `&aVocê removeu a sua medalha.` (`{medal}` is the symbol and the name, `&e☕ &eCafé`) |
| `medal-unknown`, `medal-locked` | `&cA medalha &f{medal} &cnão existe.`, `&cVocê não possui a medalha {medal}&c.` |
| `medal-given`, `medal-removed` | `&aMedalha {medal} &adada para &f{player} &7({time})&a.`, `&aMedalha {medal} &aremovida de &f{player}&a.` |
| `medal-already-held`, `medal-not-held` | `&f{player} &cjá possui a medalha {medal} &cpara sempre.`, `&f{player} &cnão possui a medalha {medal}&c.` |
| `medal-usage`, `medal-usage-staff` | Lists: `&cUso: &f/medalha [medalha]` for players, every subcommand for staff. |
| `rank-usage`, `perm-usage` | Lists with every subcommand and how to write times and options. |
| `rank-failed` | `&cNão foi possível concluir agora. Tente novamente em instantes.` |
| `rank-permanent`, `rank-remaining` | `Vitalício`, `{time}` |
| `rank-invalid-time` | `&cTempo inválido: &f{input}&c. Use &f30d&c, &f12h&c, &f1d12h &cou &fpermanente&c.` |
| `rank-player-unknown` | `&cO jogador &f{player} &cnunca entrou no servidor.` |
| `rank-unknown`, `rank-default`, `rank-track-unknown` | Unknown rank, `membro` given or taken, unknown track. |
| `rank-given`, `rank-already-held`, `rank-removed`, `rank-not-held` | `&aCargo {rank} &adado para &f{player} &7({time})&a.` and the like. |
| `rank-promoted`, `rank-demoted`, `rank-track-top`, `rank-track-bottom` | `&f{player} &afoi promovido para {rank}&a.` and the like. |
| `rank-view-*`, `rank-list-*`, `rank-info-*`, `rank-nothing` | Lines of `/cargo ver`, `/cargo listar` and `/cargo info`. |
| `node-line`, `node-true`, `node-false`, `node-contexts`, `node-time`, `node-none` | How a node is written: `&8• &f{key} &8= {value}{contexts}{time}`. |
| `perm-invalid-node`, `perm-invalid-option` | A permission or option that cannot be read. |
| `perm-user-set`, `perm-user-removed`, `perm-user-missing`, `perm-user-header` | `/permissao jogador` replies. |
| `perm-test`, `perm-test-true`, `perm-test-false`, `perm-test-undefined`, `perm-test-online`, `perm-test-offline` | `/permissao jogador testar` replies. |
| `perm-group-set`, `perm-group-removed`, `perm-group-missing`, `perm-group-refused` | `/permissao cargo` replies. |
| `editor-opened`, `editor-hover` | Lists: the clickable link (the `<link>` line) and its hover. `{minutes}`, `{url}`. |
| `editor-console` | `&aEditor de permissões: &f{url} &7(expira em {minutes} minutos)` |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `permission.changed` | every server | standard | A player's nodes, tag or medal changed (the player's id), or ranks, tags, tracks or medals changed (no id). Servers reload that player, if loaded, or everything. |

## Data

| Table | Content |
|---|---|
| `core.perm_tags` | `id`, `name`, `color`, `weight`, `prefix` (null for the default bold prefix), `decorative`. |
| `core.perm_groups` | `id`, `display_name`, `weight`, `tag_id`. |
| `core.perm_group_nodes` | Nodes of each rank: `group_id`, `permission`, `value`, `contexts` (`server=lobby;world=arena`, sorted), `expires_at`, `added_at`, `updated_at`. |
| `core.perm_user_nodes` | Nodes of each player: `player_id` and the same columns. Indexed by `permission`. |
| `core.perm_tracks` | `id`, `groups` (array in order). |
| `core.player_tags` | The tag each player selected. |
| `core.perm_medals` | `id`, `name`, `symbol`, `color`, `weight`. |
| `core.player_medals` | The medal each player selected (`player_id`, `medal_id`, `updated_at`). |
| `core.perm_actions` | Every change: `at`, `actor`, `description`. |

Created by the core migration `V8__create_permissions.sql`, which also seeds the ranks, tags and tracks above. `V10__seed_staff_permissions.sql` gives `mod` (and, by inheritance, every rank above) `muxe.account.others` and `muxe.account.staff`. `V11__create_medals.sql` creates the medal tables, seeds the 31 medals and gives `vip` and `ultra` their medals. `V12__grant_lobby_fly_to_vip.sql` gives `vip` (and every rank above) `muxe.lobby.fly`, the lobby's `/fly`. Read with `LoadPermissionDataQuery` and `FindPermissionUserQuery`; changed with `ApplyPermissionChangesCommand`, `AddUserNodeCommand`, `RemoveUserNodeCommand`, `SelectTagCommand` and `SelectMedalCommand`.

## Metrics

None of its own. Commands and queries count in the CQRS metrics, and `permission.changed` in the packet metrics.

## Integrations

- [Accounts](../accounts/README.md): players are found by name, online or not.
- [Account](../account/README.md): `/account`, `/accountmenu` and the profile head show the ranks.
- [Hotbar](../hotbar/README.md): the profile head shows the tag and ranks, and the `Medalhas` button runs `/medalha`.
- [Lobby](../lobby/README.md): the scoreboard `Tag:` line.
- The [Core API](../../development/core-api/README.md#ranks-and-permissions) gives other plugins `MuxeCore.permissions()`.

## Acceptance criteria

- [x] Ranks inherit the ones below; admin has everything; contexts, negation, wildcards and expiry are respected.
- [x] Permissions follow the server group and the world, and a temporary rank stops counting when it ends.
- [x] The chat shows `&a&lVIP &atadeu: &fOlá!`, and `&7tadeu: &fOlá!` for members.
- [x] Team prefixes fit in 16 characters and team names sort by rank, then tag.
- [x] `/tag` lists the owned tags from the lowest, each clickable with the preview; `/tag <id>` selects only owned tags and saves the choice.
- [x] Staff give and remove decorative tags and ranks, for good or for a time that adds up, promote and demote along tracks, and every change is logged with their name.
- [x] Player and rank permissions are set, listed, tested and removed, with options in any order, and rank changes that break the rules are refused.
- [x] `/editor` creates a 30-minute session and sends its link.
- [x] `/medalha` lists `Nenhuma` and the owned medals from the lowest, each clickable with the preview; `/medalha <id>` selects only owned medals and `/medalha nenhuma` removes the medal.
- [x] The chat shows the chosen medal before the tag (`&e☕ &5&lENDERLORE &5tadeu: &fOlá!`); a lost medal stops showing and the choice is kept.
- [x] Staff give and remove medals, for good or for a time that adds up.
- [x] The `Medalhas` button of the profile menu closes the menu and sends the medal list.
- [x] Everything completes with tab and refuses players without permission.
- [ ] Permissions, chat, tab list and name tags change in game on every server right after a command (checked in game).

## Failures and recovery

- PostgreSQL unavailable at login: the player joins as `membro` and gets their ranks when the load succeeds; a warning is logged.
- PostgreSQL unavailable during a command: the sender gets `rank-failed` and nothing changes.
- Redis unavailable: changes are saved, but other servers only see them when the player joins them again; `/editor` fails with `rank-failed`.
- The permissible field cannot be replaced (another server build): a warning is logged and permissions go through an attachment; wildcards then only cover permissions written in full on a rank or player.

## Known limitations

- Name tags and tab prefixes follow the 1.8 limit of 16 characters; a longer prefix is cut. For that reason they never show the medal, only the chat does.
- Buying a shop pack that lists medals does not give them yet; staff give them with `/medalha dar`.
- `/medalha remover` only takes a medal given to the player; a medal that comes from a rank stays while the rank does.
- `/permissao jogador ver` lists the player's own nodes, not the ones inherited from ranks; use `/cargo info` for those.
- `/tag <id>` suggests the tags of online players only, from the cache.
