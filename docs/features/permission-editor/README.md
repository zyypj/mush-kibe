**English** | [Português](README.pt-BR.md)

# Permission editor

A website served by the proxy where staff edit ranks (groups), their hierarchy and permissions, tags, medals, tracks and the permissions of each player, then save everything at once. It lives in the `core:proxy` package `br.com.muxe.core.proxy.editor` and works on the permission model of `core:common` (`br.com.muxe.core.common.permission`).

## User stories

- As an administrator, I want to open a private link from the game and edit ranks, tags and permissions in the browser, so that I do not type long commands.
- As an administrator, I want to create a rank that inherits another one or starts as a copy of another one, so that I build the hierarchy quickly.
- As an administrator, I want to give a player a temporary rank or a decorative tag, so that rewards expire by themselves.
- As an administrator, I want to create medals, choose which ranks grant them and give a player a medal or change the medal they chose, so that cosmetic rewards are managed in one place.
- As an administrator, I want to review every pending change before saving and see who changed what, so that mistakes are easy to spot.

## How it works

### Opening the editor

1. An administrator runs `/editor` on a game server (Bukkit side). The command opens an editor session (`EditorSessions.open`) and sends the link `<editor.public-url>/#<token>`.
2. The token travels in the URL **fragment**, so the browser never sends it to the server in the request line, logs or `Referer`. The page reads it, keeps it only in memory and `sessionStorage` of that tab, and removes it from the address bar.
3. Every API call sends `Authorization: Bearer <token>`. The session lasts 30 minutes (`EditorSessions.DEFAULT_DURATION`); the top bar counts down and the page closes itself when it ends.
4. On every call the proxy also loads the session owner's permissions and checks that they still resolve `muxe.permissions.editor`. A player who lost it is refused and the session is closed. A link opened from a server console (owner id `00000000-0000-0000-0000-000000000000`, `EditorSessions.CONSOLE`) is always allowed, since only the console can create it.

### What can be edited

- **Ranks (groups):** list ordered by weight; create (empty, as a copy of an existing rank's permissions, or inheriting an existing rank); change the display name, weight and tag; duplicate; delete (the default rank `membro` cannot be deleted; deleting a rank also removes it from the ranks that inherit it and from tracks). Ids are fixed once created.
- **Inheritance:** parents are the `group.<id>` permissions of the rank. The editor adds and removes them, shows the indirect chain and does not offer ranks that would create a cycle.
- **Permissions** of ranks and players: a table with search and filters (true, false, temporary, with context); add one, add many at once (one per line: `-key` for false, `key=value` contexts and a duration like `30d`), toggle true/false, edit and remove. Each permission can have contexts (`server`, `world` or any `key=value`) and an expiry (a duration such as `12h`, `30d`, `1mo`, or a date).
- **Tags:** create, edit and delete; id, name, color chosen among the 16 `&` codes, weight, optional chat prefix (by default color + bold + NAME), decorative flag. Live previews of the chat line (`&a&lVIP &atadeu: Olá!`), of `/tag` (`&aVIP`) and of the name tag.
- **Medalhas:** listed by weight (lowest first, as in `/medalha`); create, edit and delete; id, name, symbol (1 to 4 characters, such as `☕` or `⊰⊱`), color with the same 16-color picker as tags, weight. Live previews of the chat line (`<medal> <tag> tadeu: Olá!`, with a tag chosen for the preview, `: Olá!` in white) and of the `/medalha` entry. The medal page lists the ranks that grant it (`muxe.medal.<id>` on the rank, with contexts and expiry), the ranks that get it by inheritance or by a wildcard (`*`, `muxe.medal.*`), and adds or removes the permission on a rank. Deleting a medal also removes it from the ranks that grant it and clears it as the chosen medal of the players open in the editor (the database clears it for everyone else).
- **Tracks:** ordered lists of ranks used to promote and demote; reorder, add and remove ranks.
- **Players:** search by name or UUID; see the account, give ranks (permanent or temporary, optionally with contexts), promote or demote along each track, give or take decorative tags (`muxe.tag.<id>`, optionally temporary), choose the selected tag, give or take medals (`muxe.medal.<id>`, optionally temporary; medals granted by a rank show which one), choose the player's medal (or **Nenhuma**; the page warns when the player cannot use it) and edit the remaining permissions.
- **History:** the last 300 saved changes with author, filterable.

### Saving

- Nothing is written while editing. A floating panel lists every pending change (new, edited, deleted), each with **Desfazer** and, for medals, ranks and players, what changed (medal fields, `+ medalha X` / `− medalha X`, `medalha escolhida: A → B`); **Salvar** sends them all in one request.
- The proxy turns the request into `PermissionChange`s and applies them atomically with `PermissionSync.apply(actor, changes)`: the rules run on the result (default rank present, known ranks and tags, known chosen medal, no inheritance cycle), the changes are written in one transaction with one action-log row each, `permission.changed` is published to the network and every server reloads.
- If a rule fails, nothing is saved and the page shows the problem; the pending changes stay so they can be fixed.
- The proxy log gets one line per saved batch with the actor and a summary of the changes.

## API

All endpoints are under `/api`, answer JSON with `Cache-Control: no-store` and need `Authorization: Bearer <token>`. Errors are `{"error":{"code":"...","message":"..."}}`.

| Method and path | Answer |
|---|---|
| `GET /api/session` | `actor`, `actorId`, `createdAt`, `expiresAt`, `now` (epoch milliseconds). |
| `GET /api/data` | `defaultGroup`, `groups` (by weight, with `nodes`), `tags` (with `display` and `chatPrefix`), `tracks`, `medals` (by weight, lowest first, with `display`, `chatPrefix` and `permission`). |
| `GET /api/users/search?name=` | The player found by name (ignoring case) or UUID: account fields, `nodes`, `selectedTag` and `selectedMedal`. `404 editor.user-not-found`. |
| `GET /api/users/{uuid}` | The same, by id. |
| `GET /api/groups/{id}/members` | `total` and up to 200 `members` (`id`, `username`) with that rank directly. |
| `GET /api/actions?limit=` | The latest saved changes (`at`, `actor`, `description`), 1 to 500, 100 by default. |
| `POST /api/changes` | Body `{"changes":[...]}` with up to 500 changes of the types `put-group`, `delete-group`, `put-tag`, `delete-tag`, `put-track`, `delete-track`, `put-medal`, `delete-medal`, `put-user`. Answers `saved` and the new `data`, or `400` with the rule's problem code. |
| `POST /api/logout` | Closes the session. |

A node is `{"key","value","contexts":{...},"expiresAt"}` with an optional `addedAt`/`updatedAt` (milliseconds; future values become now). A group is `{"id","displayName","weight","tag","nodes"}`, a tag `{"id","name","color","weight","prefix","decorative"}`, a medal `{"id","name","symbol","color","weight"}` (`delete-medal` takes `id`), a track `{"id","groups"}` and a user `{"id","nodes","selectedTag","selectedMedal"}`. A `put-user` replaces the whole player: `selectedTag` or `selectedMedal` missing or `null` clears it, so the site always sends both back as loaded. An invalid medal (id, empty name, symbol outside 1 to 4 characters, color that is not one `&` code) is `400 editor.invalid-request`; a chosen medal that does not exist is `400 permission.unknown-medal`.

Status codes: `400 editor.invalid-request` and the `permission.*` problem codes, `401 editor.unauthenticated` / `editor.session-expired`, `403 editor.forbidden` / `editor.bad-origin`, `404`, `405`, `413 editor.too-large`, `415`, `429 editor.too-many-requests`, `503 editor.not-ready` / `editor.unavailable`.

## Security

- **Access:** only a valid, unexpired session token whose owner still has `muxe.permissions.editor`. Tokens are 32 random bytes; Redis keeps only their SHA-256, so the lookup does not depend on the token's characters. There are no cookies, so cross-site requests cannot ride on a login.
- **Rate limits per address:** `editor.requests-per-minute` requests to the site and API; after `editor.failed-logins-per-ten-minutes` refused tokens the address is blocked for 10 minutes.
- **Requests:** bodies over `editor.max-body-bytes` are refused (`413`); `POST` needs `Content-Type: application/json` and, when the browser sends `Origin`, it must be the editor's own address. Every field is validated again on the server: ids, permission keys, contexts, colors, lengths, numbers and limits (500 changes, 5,000 permissions per rank or player, 8 contexts per permission).
- **Headers:** `Content-Security-Policy` with `default-src 'self'` and no inline scripts or styles (the only outside sources are the Montserrat font from `fonts.googleapis.com` and `fonts.gstatic.com`), `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, cross-origin isolation headers, no CORS headers. Only the known site files are served.
- **Rendering:** the page builds every element with DOM APIs and `textContent`; nothing from the server is inserted as HTML.
- **Threads:** the HTTP server runs on its own virtual threads, never on Velocity's event threads, and stops with the proxy.
- **HTTPS:** the editor speaks plain HTTP on port 1031. Until the network has a domain, the token and changes cross the internet without encryption; once there is a domain, put a reverse proxy with TLS (for example Caddy or nginx) in front of it, set `editor.bind` to `127.0.0.1` and `editor.public-url` to the `https://` address. The rate limit uses the connection address, so behind a reverse proxy it applies to the proxy as a whole until forwarded addresses are supported.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/editor` | Opens an editor session and sends the link (game servers). | `muxe.permissions.editor` |

## Permissions

| Permission | Grants |
|---|---|
| `muxe.permissions.editor` | Open and use the editor. The `admin` rank has it through `*`. |

## Configuration

Proxy `plugins/muxe-proxy/config.yml`:

| Key | Default | Description |
|---|---|---|
| `editor.enabled` | `true` | Starts the editor's HTTP server. |
| `editor.bind` | `0.0.0.0` | Address to listen on. Use `127.0.0.1` behind a reverse proxy. |
| `editor.port` | `1031` | Port to listen on (allocated to the proxy in the panel). |
| `editor.public-url` | `http://151.244.40.191:1031` | Address players open; the start of the `/editor` link and the accepted `Origin`. |
| `editor.requests-per-minute` | `240` | Requests per address per minute. |
| `editor.failed-logins-per-ten-minutes` | `20` | Refused tokens per address before it is blocked for 10 minutes. |
| `editor.max-body-bytes` | `1048576` | Largest request body. |

If the port cannot be opened, the proxy logs it and keeps running without the editor.

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `permission.changed` | broadcast | standard | Sent after every save so every server reloads ranks, tags and the affected players. |

## Data

Uses the permission tables and the action log of `core:common`. Editor sessions live in Redis under `muxe:permissions:editor:<sha256 of the token>` for 30 minutes.

## Metrics

None.

## Integrations

- `core:common` permissions: `PermissionSync`, `PermissionResolver`, `PermissionRules`, `EditorSessions`, `FindAccountDetailsQuery`, `FindPermissionUserQuery`, `FindUsersWithNodeQuery`, `RecentPermissionActionsQuery`.
- The `/editor` command on the game servers creates the sessions.

## Acceptance criteria

- [ ] Opening the link from `/editor` shows the ranks; opening the site without a token or with an old link shows how to get a new one.
- [ ] A rank created as a copy has the same permissions as the original; a rank created by inheritance has only `group.<original>`.
- [ ] Adding a parent that would create a cycle is not offered, and a crafted request that does it is refused with `permission.inheritance-cycle`.
- [ ] Giving a player a rank for 30 days saves a `group.<id>` permission that expires in 30 days.
- [ ] Changing a tag's color updates the chat and `/tag` previews immediately.
- [ ] Changing a medal's symbol or color updates the chat and `/medalha` previews immediately; granting it to a rank from the medal page adds `muxe.medal.<id>` to that rank.
- [ ] Changing only a player's ranks keeps the medal they chose.
- [ ] After **Salvar**, the change shows in the history with the author, and players on other servers get it without relogging.
- [ ] A player who loses `muxe.permissions.editor` is refused on the next request.

## Failures and recovery

- **Database down while saving:** the transaction is rolled back, the page shows the error and keeps the pending changes.
- **Redis down:** sessions cannot be read; the page shows the session as invalid. Saved data is not affected.
- **Groups not loaded yet:** the API answers `503 editor.not-ready` instead of refusing the session.
- **Session expired while editing:** the page shows "Sessão expirada"; unsaved changes are lost, so long edits should be saved in parts.
- **Two editors at once:** the last save wins for each rank, tag, track or player it sends.

## Known limitations

- No HTTPS until there is a domain and a reverse proxy (see Security).
- A rank's id cannot be renamed; create a copy and delete the old one.
- Members are listed only for direct members, up to 200.
- The tag and medal previews approximate the game's font and colors; the `/medalha` entry preview is `symbol name` in the medal's color.
- Whether a player owns a medal is estimated in the page from their direct ranks, inheritance and wildcards; the server's resolver is the reference.
