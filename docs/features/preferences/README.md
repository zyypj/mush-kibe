**English** | [Português](README.pt-BR.md)

# Preferences

The `Preferências` menu of `Meu perfil`, where each player chooses how the network treats them: chat, confirmations, invites and privacy. Saved per player and valid on every server.

## User stories

- As a player, I turn the general chat off and stop seeing other players' messages.
- As a player, I turn `Confirmar voltar para o lobby` on so a stray `/lobby` does not take me out of a match.
- As a player, I hide my statistics or stop receiving party invites.

## How it works

- **Menu:** `Preferências`, 6 rows, opened by `Preferências` (slot 31) of `Meu perfil`, with `Voltar` at slot 49 (`Para Meu perfil`). Three tabs at slots 1 to 3: `Geral` (repeater), `Social` (cookie) and `Privacidade` (hopper). The open tab says `Selecionado.`, the others `Clique para selecionar.`; the row of panes under them (slots 9 to 17) is grey with a lime pane under the open tab.
- **Entries:** each preference shows its icon and, right under it, a dye with the same name and lore; clicking either changes it at once and saves it. A toggle shows `Valor: Ativado` (lime dye, green name) or `Valor: Desativado` (grey dye, grey name) and `Clique para desativar.`/`Clique para ativar.`. `Mensagens privadas` cycles `Público` → `Limitado` → `Amigos` → `Desativado`, listing all four with the selected one marked and `Clique para trocar para <next>.`
- **Geral:** `Bate-papo` (slot 29, on), `Confirmar voltar para o lobby` (31, off) and `Formato da tag` (33, Ultra+ only, answers `messages.preferences-soon` for now).
- **Social:** `Mensagens privadas` (28, Público), `Convite para party` (30, on), `Convite para clan` (32, on) and `Pedidos de amizade` (34, on).
- **Privacidade:** `Visibilidade de estatísticas` (29, on), `Site: Mostrar Discord` (31, on) and `Site: Congelar estatísticas` (33, on).
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
| `menus.preferences.tabs.<tab>` | `geral`, `social`, `privacidade` | `slot`, `item` and `entries.<id>` with `slot`, `preference` (or none for an entry that answers "em breve"), `below.material` and `item`. |
| `messages.preferences-soon`, `lobby-confirm`, `statistics-hidden`, `party-invites-off` | see the file | Answers of the menu, `/lobby`, `/stats` and party invites. |

## Packets

None.

## Data

`core.player_preferences` (migration V22): `player_id`, `preference` (`chat`, `confirm-lobby`, `private-messages`, `party-invites`, `clan-invites`, `friend-requests`, `stats-visible`, `site-discord`, `site-freeze-stats`), `value` and `updated_at`. Only changed preferences have a row; the rest use the default. `ChangePreferenceCommand` saves one, `FindPreferencesQuery` reads all of a player.

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
- [ ] Checked in game.

## Failures and recovery

If the preferences cannot be loaded or saved, the defaults are used and the error is logged; a change is applied on the server at once even if saving fails.

## Known limitations

- Private messages, clan invites, friend requests, the website options and the tag format do nothing yet.
- Only `/lobby` asks for confirmation; other ways back to the lobby (such as items) do not.
