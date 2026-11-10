**English** | [Português](README.pt-BR.md)

# Language

Each player reads the network in their own language: Portuguese (the default) or English. The choice is saved per account and follows the player to every server and the proxy.

## User stories

- As a player who does not speak Portuguese, I want to switch the network to English, so that I understand the messages, menus and scoreboards.
- As a player, I want my choice to be remembered, so that I do not have to pick it again on every server or login.
- As a developer, I want one way to write a translatable text, so that new features are translated without extra plumbing.

## How it works

- Every new player starts in Portuguese. `/idioma <português|inglês>` (also `/language`, `/lang`, `/linguagem`) switches the language. The command accepts names and tags with or without accents: `português`, `portugues`, `portuguese`, `pt`, `pt-BR`, `inglês`, `ingles`, `english`, `en`. Without an argument it opens the language menu:
  - The menu has 3 rows, with one book per language: `Português` in slot 10 and `English` in slot 11.
  - Each book is written in its own language, so a player who picked the wrong one can still find theirs. Only the title follows the reader: `Selecionar idioma` or `Select language`.
  - The current language glows. Clicking a book closes the menu and switches to that language; clicking the current one answers `language-already`.
  - An unknown language typed after the command shows the usage and the current language.
- The choice is saved in `core.player_languages`. Each Bukkit server loads it before login, together with the permissions, and keeps it in memory while the player is online, so rendering a message never touches the database. The proxy loads it at login and is told about changes with `language.changed`.
- After a switch, what the player already sees is refreshed in the new language: hotbar items, the sidebar and the other per-viewer texts. An open menu is closed.
- A text that has no English translation falls back to Portuguese, key by key, so a missing translation never shows an empty or broken line.
- Texts seen only by staff (arena setup, rank and permission editing, placement and config commands) stay in Portuguese.
- The MOTD has no player behind it and stays in Portuguese. Kicks sent before the player's account is known (before login) are Portuguese too.

### Where the texts live

- Portuguese stays where it always was: the plugin's `config.yml`.
- English lives in `lang/en.yml` next to it. It has the same paths as `config.yml`, and holds only text leaves: messages, item names, lore, titles, scoreboard lines and menu titles. Materials, slots, numbers and sounds are never repeated there.
- The English file is created in the plugin folder on first start. Keys missing from the server copy come from the jar, as with `config.yml`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/idioma [língua]` | Changes the language of the player; without an argument it opens the language menu. Aliases `/language`, `/lang`, `/linguagem`. | None. |

## Permissions

None.

## Configuration

| Key | Default | Description |
|---|---|---|
| `messages.language-usage` | `&eUso: /idioma <português\|inglês>...` | Usage and current language. |
| `messages.language-changed` | `&aIdioma alterado para &f{language}&a.` | Sent in the new language after a switch. |
| `messages.language-already` | `&cVocê já está usando o idioma &f{language}&c.` | The player picked the language they already use. |
| `messages.language-failed` | `&cNão foi possível alterar o idioma agora...` | The choice could not be saved. |
| `messages.language-name-pt-br`, `messages.language-name-en` | `Português`, `Inglês` | Names of the languages shown in the messages above. |
| `language-menu.title`, `language-menu.rows` | `Selecionar idioma`, `3` | Title and height of the language menu. |
| `language-menu.languages.<tag>.slot`, `.item` | `pt-br` in slot 10, `en` in slot 11, both books | Slot and [item](../../development/core-api/README.md#items) of each language. The tag is `pt-br` or `en`; a slot outside the menu or an unknown tag fails on startup. |
| `lang/en.yml` | the English texts | English counterpart of every readable text of the plugin's `config.yml`, at the same paths. |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `language.changed` | proxy | standard, max age 30s | Tells the proxy that a player switched language, so its own texts follow. |

## Data

`core.player_languages` (migration `V20`):

| Column | Type | Description |
|---|---|---|
| `player_id` | `UUID` | Primary key. |
| `language` | `VARCHAR(8)` | The language tag, `pt-BR` or `en`. |
| `updated_at` | `TIMESTAMPTZ` | When the player last switched. |

A player without a row speaks Portuguese.

## Metrics

None. The command and query handlers are measured by the CQRS metrics like every other handler.

## Integrations

- Every Bukkit plugin: `MessageCatalog` and `LocalizedConfig` render each text in the reader's language. See [Core API](../../development/core-api/README.md#languages).
- Proxy: kicks after login, chat replies and the tab list follow the player's language.
- [Party](../party/README.md): notices are sent as keys and rendered on the receiving server in each member's language.

## Acceptance criteria

- [ ] A new player sees every message in Portuguese.
- [ ] `/idioma inglês` saves the choice and answers in English.
- [ ] After switching, the hotbar items and the sidebar are in the new language without relogging.
- [ ] After moving to another server, the player still reads English.
- [ ] A match announcement reaches each player in their own language.
- [ ] A text missing from `lang/en.yml` shows in Portuguese.
- [ ] `/idioma` with an unknown language shows the usage.
- [x] `/idioma` opens the menu with the current language glowing; the title follows the reader and each book keeps its own language.
- [ ] Clicking a book in the menu switches the language (checked in game).

## Failures and recovery

- **The language cannot be loaded before login (database slow or down):** the player is let in and reads Portuguese. The next server switch loads it again.
- **The choice cannot be saved:** the player sees `language-failed`, and nothing changes.
- **The proxy misses `language.changed`:** it loads the language again at the next login.

## Known limitations

- Holograms and NPC lines written by staff in the database are shown as written, in one language.
- The MOTD and pre-login kicks are Portuguese only.
