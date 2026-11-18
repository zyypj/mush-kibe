**English** | [Português](README.pt-BR.md)

# Preferences

The `Preferências` menu of `Meu perfil`, where each player chooses how the network treats them: chat, confirmations, invites and privacy. Saved per player and valid on every server.

## User stories

- As a player, I turn the general chat off and stop seeing other players' messages.
- As a player, I turn `Confirmar voltar para o lobby` on so a stray `/lobby` does not take me out of a match.
- As a player, I hide my statistics or stop receiving party invites.
- As an Ultra+ player, I choose the format in which I see every player's tag in the chat, the TAB and above their heads.

## How it works

- **Menu:** `Preferências`, 6 rows, opened by `Preferências` (slot 31) of `Meu perfil`, with `Voltar` at slot 49 (`Para Meu perfil`). Three tabs at slots 1 to 3: `Geral` (repeater), `Social` (cookie) and `Privacidade` (hopper). The open tab says `Selecionado.`, the others `Clique para selecionar.`; the row of panes under them (slots 9 to 17) is grey with a lime pane under the open tab.
- **Entries:** each preference shows its icon and, right under it, a dye with the same name and lore; clicking either changes it at once and saves it. A toggle shows `Valor: Ativado` (lime dye, green name) or `Valor: Desativado` (grey dye, grey name) and `Clique para desativar.`/`Clique para ativar.`. `Mensagens privadas` cycles `Público` → `Limitado` → `Amigos` → `Desativado`, listing all four with the selected one marked and `Clique para trocar para <next>.`
- **Geral:** `Bate-papo` (slot 29, on), `Confirmar voltar para o lobby` (31, off) and `Formato da tag` (33, Ultra+ only), which opens the `Formato da tag` menu.
- **Social:** `Mensagens privadas` (28, Público), `Convite para party` (30, on), `Convite para clan` (32, on) and `Pedidos de amizade` (34, on).
- **Privacidade:** `Visibilidade de estatísticas` (29, on), `Site: Mostrar Discord` (31, on) and `Site: Congelar estatísticas` (33, on).
- **Formato da tag:** 3 rows, `Voltar` at slot 21 (`Para Preferências`). Six dyes on slots 10 to 15: `Padrão` (white dye, for everyone), `Cor` (orange), `Colchetes (Maiúsculo)` (magenta), `Colchetes (Normal)` (light blue), `Padrão (Branco)` (yellow) and `Padrão (Cinza)` (lime), the last five `Exclusivo para ULTRA+`. Each one shows an example with the viewer's own tag (`§5§lULTRA§6+ §5tadeu`, `§5tadeu§6+`, `§5[ULTRA§6+§5] tadeu`, `§5[Ultra§6+§5] tadeu`, `§5§lULTRA§6+ §ftadeu`, `§5§lULTRA§6+ §7tadeu`) and `Selecionado.` (glowing), `Clique para selecionar.` or, for a player without Ultra+, `Bloqueado.`; clicking a locked one answers `messages.ultra-plus-only`.
- **What the format changes:** only what the player who chose it sees. Every player's tag is shown in that format in the chat, in the TAB and in the name tag above the head:
  - `Cor`: no prefix, the nick in the tag color, followed by the colored end of the tag name (the `+` of Ultra+).
  - `Colchetes (Maiúsculo)` and `Colchetes (Normal)`: `[TAG]` or `[Tag]` in the tag color, without bold.
  - `Padrão (Branco)` and `Padrão (Cinza)`: the usual prefix with a white or grey nick.
  - Tags without a prefix (`Membro`) stay the same; medals and emblems stay around the name.
  - The format only applies while the player is Ultra+; without it they see `Padrão`. A scoreboard shared by players with different formats shows `Padrão`, and team name tags of a match are not changed.
- **What each one does now:**
  - `Bate-papo` off: the player stops receiving the public chat of other players; their own messages, party, staff and system messages still arrive.
  - `Confirmar voltar para o lobby` on: the first `/lobby` answers `messages.lobby-confirm`; it only sends the player if they type it again within 5 s.
  - `Convite para party` off: invites to the player are refused and the inviter gets `messages.party-invites-off`.
  - `Visibilidade de estatísticas` off: other players' `/stats <player>` answers `messages.statistics-hidden`.
  - `Mensagens privadas`, `Convite para clan`, `Pedidos de amizade` and the two `Site` options are saved but have no effect yet, because private messages, clans, friends and the website do not exist.

## Commands

None. The menu is opened from `Meu perfil`.

## Permissions

None.

## Configuration

Core `config.yml`, with English in `lang/en.yml`:

| Key | Default | Description |
|---|---|---|
| `menus.preferences.title`, `rows`, `first-pane` | `Preferências`, `6`, `9` | Title, height and first slot of the pane row. |
| `menus.preferences.tab-selected`, `tab-unselected` | `Selecionado.`, `Clique para selecionar.` | Lore of the tabs. |
| `menus.preferences.name-on`, `name-off`, `value`, `enabled`, `disabled`, `click-enable`, `click-disable` | the reference | Name colour and lore of a toggle. |
| `menus.preferences.option-selected`, `option-other`, `click-cycle`, `options.<public\|limited\|friends\|"off">` | the reference | Lines of `Mensagens privadas`, and the name and dye of each option (quote `"off"`). |
| `menus.preferences.back` | slot 49 | `Voltar`. |
| `menus.preferences.tabs.<tab>` | `geral`, `social`, `privacidade` | `slot`, `item` and `entries.<id>` with `slot`, `preference` (or none for an entry that answers "em breve"), `opens` (a menu the entry opens: `tag-format`), `below.material` and `item`. |
| `menus.tag-format.title`, `rows`, `example` | `Formato da tag`, `3`, `{prefix}{color}{player}` | Title, height and example line of the `Formato da tag` menu. |
| `menus.tag-format.texts` | the reference | `example`, `exclusive`, `selected`, `select` and `locked` lines. |
| `menus.tag-format.formats.<default\|color\|brackets-upper\|brackets\|default-white\|default-gray>` | slots 10 to 15 | `slot`, `exclusive` (only Ultra+) and `item` of each format. |
| `menus.tag-format.back` | slot 21 | `Voltar`. |
| `messages.preferences-soon`, `lobby-confirm`, `statistics-hidden`, `party-invites-off` | see the file | Answers of the menu, `/lobby`, `/stats` and party invites. |

## Packets

None.

## Data

`core.player_preferences` (migration V22): `player_id`, `preference` (`chat`, `confirm-lobby`, `private-messages`, `party-invites`, `clan-invites`, `friend-requests`, `stats-visible`, `site-discord`, `site-freeze-stats`, `tag-format`), `value` and `updated_at`. Only changed preferences have a row; the rest use the default. `ChangePreferenceCommand` saves one, `FindPreferencesQuery` reads all of a player.

## Metrics

None.

## Integrations

- [Hotbar](../hotbar/README.md): `Preferências` of `Meu perfil`.
- [Statistics](../statistics/README.md): `Visibilidade de estatísticas`.
- [Party](../party/README.md): `Convite para party`, checked by the proxy.
- `MuxeCore.preferences()` gives other plugins the loaded preferences of online players (`of`) and of anyone (`load`).

## Acceptance criteria

- [x] The three tabs show the reference entries, lore and dyes (`PreferencesPagesTest`).
- [x] Defaults, toggles and the private messages cycle (`PreferencesTest`), saved per preference (`PostgresPreferenceRepositoryIntegrationTest`).
- [x] Chat off hides other players' chat (`PreferenceServiceTest`); `/lobby` asks twice within 5 s (`PreferencesPagesTest`); party invites off are refused (`PartyCoordinatorTest`).
- [x] `Preferências` opens with a way back to the profile (`ProfileMenuTest`).
- [x] `Formato da tag` shows the six formats with the viewer's example and locks five of them without Ultra+ (`TagFormatPagesTest`); each format renders the chat line, the example and the name tag, and a viewer with another format gets their own chat line (`TagFormatLookTest`); the format is saved and parsed (`PreferencesTest`).
- [ ] Checked in game.

## Failures and recovery

If the preferences cannot be loaded or saved, the defaults are used and the error is logged; a change is applied on the server at once even if saving fails.

## Known limitations

- Private messages, clan invites, friend requests and the website options do nothing yet.
- The tag format does not change party chat, which the proxy renders.
- Only `/lobby` asks for confirmation; other ways back to the lobby (such as items) do not.
