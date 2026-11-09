**English** | [Português](README.pt-BR.md)

# Party

Players group up in a party that lives on the whole network: they invite each other, talk in the party chat, queue together for games and are kept on the same team. The proxy owns the parties; every Bukkit server keeps a copy and runs the commands.

## User stories

- As a player, I want to invite my friends to a party from any server, so that we play together.
- As a player, I want a party chat, and to make it my default channel, so that only my party reads me.
- As a party leader, I want to queue once and have the whole party follow me to a room with enough seats.
- As a party leader, I want to pull my party to where I am, a lobby or my match.
- As a player with Ultra+ or above, I want to open my party so that anyone can join it.
- As a player, I want to stay on my party's team, and to know when a match will not count stats because the party had to be split.

## How it works

### The party

- `/party <jogadores>` invites one or more players (names separated by spaces). The first invite creates the party with the sender as its leader. Only the leader invites.
- The party (`[PARTY]` in light purple) hears `<nick> recebeu um convite para a party.`; the invited player gets two lines, `<nick> convidou você para uma party` and `CLIQUE AQUI para aceitar o convite.`, where the second line runs `/party aceitar <leader>` when clicked.
- An invite lasts 60 seconds (`party.invite-duration`). When it expires, the leader and the invited player are told.
- There is no member limit.
- Every nick in party messages has the colour of the player's tag, as reported by the server the player is on.
- A party ends when the leader uses `/party acabar`, when its last member leaves, and also when it is left with only the leader, closed, without pending invites.

### Leaving and being offline

- `/party sair` leaves. When the leader leaves, the oldest member online becomes the leader (`<nick> é agora líder da party.`).
- A member who leaves the network stays in the party, shown in red in `/party membros`, for 3 minutes (`party.offline-grace`). The online members hear `<nick> desconectou-se e tem 3 minutos para voltar.` (in red, the minutes taken from `party.offline-grace`) and, when the member comes back in time, `<nick> reconectou.` (in yellow). After the 3 minutes the member is removed (`<nick> foi removido da party por ficar offline.`).
- When the leader leaves the network, the oldest online member becomes the leader at once. If nobody is online, the leadership waits; the 3-minute rule removes everyone in the end.
- `/party removeroffline` removes every offline member now.

### Open parties

- `/party abrir` opens the party, or closes it when it is open. Only players with `muxe.party.open` (the Ultra+ rank and every rank above it) may use it. Opening without a party creates one.
- While the party is open, anyone joins with `/party entrar <leader nick>`, which completes the leaders of open parties.

### Chat

- `/pc <mensagem>` sends to the online members, on every server: `[PARTY]`, then the normal chat line (medal, tag, nick and message), as in the reference.
- `/canal <canal>` changes the default channel: `default` (or `padrao`/`padrão`), `party` or `clan`. In the `party` channel, every chat message goes to the party chat. When the player is not in a party any more, the chat falls back to the normal channel. `clan` answers `Você não está em um clan.` until clans exist. The choice is kept for 7 days in `MuxeCore.store()` (`chat:channel:<uuid>`).

### Matchmaking

- Only the leader searches for a room. A member who clicks a mode, a map or "play again" gets `Apenas o líder da party consegue procurar salas!`.
- When the leader joins, the online members go too, wherever they are (a member in another match leaves it and can come back with `/retornar` or `/reconectar`). The room must have a free seat for each of them: rooms with fewer free seats are skipped and a new room is created when none fits. A mode that cannot hold the whole party (for example 9 players in solo, 8 seats) is refused with `Sua party tem jogadores demais para este modo.`
- The game server reserves a seat for every member in the same room (`<game>.join_request` version 2 carries the companions).
- Changing lobby (`/jogar`, the game selector, `/lobby`) does not move the party; `/party teleportar` does.

### Teleport

`/party teleportar` (or `/party warp`) pulls the online members to the server the leader is on:

- **Leader in a lobby:** the members are sent to that lobby instance. Members in a match leave it and can return with `/retornar`/`/reconectar`.
- **Leader in a match that has not started:** each member gets a seat in that room while it has room; when it is full, the member stays where they are.
- **Leader in a started match:** the members join it as spectators. They see the match, its chat and the spectator hotbar, but they never play nor count in the result.

### Teams

- At the start, the members of a party in the room are placed together on one team (`TeamAssigner` groups them before anyone else).
- When a party is larger than a team, or the mode has teams of one, part of it ends on an enemy team. The match then says `AVISO Esta partida não conta estatísticas.` (`AVISO` dark red and bold) to everyone, and the match does not record stats for the players of that party. BedWars leaves them out of the saved results and the XP of the match.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/party` | Shows the usage list. | None |
| `/party <jogadores>` | Invites players. | None (leader) |
| `/party aceitar <jogador>` | Accepts an invite. | None |
| `/party sair` | Leaves the party. | None |
| `/party acabar` | Ends the party. | None (leader) |
| `/party expulsar <jogador>` | Removes a member. | None (leader) |
| `/party transferir <jogador>` | Gives the leadership to an online member. | None (leader) |
| `/party membros` | Lists the members: online in green, offline in red. | None |
| `/party teleportar` (`warp`) | Pulls the members to the leader's server. | None (leader) |
| `/party entrar <nome>` | Joins an open party by its leader's nick. | None |
| `/party removeroffline` | Removes the offline members. | None (leader) |
| `/party abrir` | Opens or closes the party. | `muxe.party.open` |
| `/pc <mensagem>` | Talks in the party chat. | None |
| `/canal <canal>` | Changes the default chat channel. | None |

Tab completes the subcommands, the network players (every name of `/party <jogadores>`), the party members (`expulsar`, `transferir`), the leaders of open parties (`entrar`) and the channels. The names come from the proxy every 3 seconds (`network.players`). The commands do not exist on auth servers.

## Permissions

| Permission | Grants |
|---|---|
| `muxe.party.open` | `/party abrir`. Seeded on the `ultra+` rank, inherited by `partner` and every rank above. |

## Configuration

Proxy `config.yml`:

| Key | Default | Description |
|---|---|---|
| `party.invite-duration` | `60s` | How long an invite lasts. |
| `party.offline-grace` | `3m` | How long an offline member stays in the party. |
| `party.gather-timeout` | `3s` | How long the proxy waits for the leader's server to answer a teleport. |
| `party.players-period` | `3s` | How often the proxy sends the online names for tab completion. |

Muxe-Core `config.yml`, under `messages`: `party-usage` (the usage list), `party-invite-sent`, `party-invite` (two lines, the last one clickable), `party-disbanded`, `party-joined`, `party-kicked`, `party-new-leader`, `party-members`, `party-members-online`, `party-members-offline`, `party-members-separator`, `party-chat`, `party-chat-usage`, `party-left`, `party-you-left`, `party-opened`, `party-closed`, `party-offline-removed`, `party-offline-kicked`, `party-member-disconnected` (with `{minutes}`), `party-member-reconnected`, `party-summoned`, `party-invite-expired-sender`, `party-invite-expired`, the refusals (`party-player-not-found`, `party-not-in-party`, `party-already-in-party`, `party-not-leader`, `party-target-in-party`, `party-already-member`, `party-already-invited`, `party-no-invite`, `party-not-member`, `party-member-offline`, `party-invite-self`, `party-kick-self`, `party-already-leader`, `party-not-open`, `party-open-no-permission`, `party-no-offline`), the matchmaking texts (`party-leader-only`, `party-too-big`, `party-split`) and the channel texts (`channel-usage`, `channel-changed`, `channel-name-default`, `channel-name-party`, `channel-no-clan`).

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `party.action` | proxy | standard, max age 10 s | A player ran a party command; carries the action, its arguments and, for `abrir`, whether the player may open. |
| `party.notice` | server | standard, max age 10 s | Messages for the players of that server: a message key with placeholders and an optional click command, or a ready line (party chat). |
| `party.updated` | broadcast | standard, max age 30 s | The members, leader and open flag of a party that changed. |
| `party.removed` | broadcast | standard, max age 30 s | A party ended. |
| `party.sync_request` / `party.sync_response` | proxy / requester | standard, max age 5 s | A starting server loads every party. |
| `party.gather_request` / `party.gather_response` | leader's server / proxy | standard, max age 5 s | `/party teleportar`: the leader's server says who may come (and reserves their seats); the proxy connects them. |
| `player.display` | proxy | standard, max age 30 s | The colour of a player's nick (their tag), sent on join and on rank changes. |
| `network.players` | broadcast | volatile, 10 s | The names of the authenticated players, for tab completion. |

## Data

- Parties live in the proxy's memory (`PartyBook` in `core:common`); servers keep a copy (`PartyDirectory`). Nothing is stored in PostgreSQL.
- `chat:channel:<uuid>` in `MuxeCore.store()`: the chosen chat channel, 7 days.
- Migration `V18__create_ultra_plus.sql` creates the `ultra+` rank and its permission.

## Metrics

None besides the packet metrics of the packets above.

## Integrations

- [Minigame API](../../architecture/minigame-api/README.md): leader-only queueing, rooms with seats for the whole party, team grouping, spectators and the stats warning.
- [BedWars](../bedwars/README.md): leaves the players of a split party out of the stats.
- [Ranks and tags](../ranks-and-tags/README.md): the nick colours, the chat line and the Ultra+ rank.
- [Network routing](../network-routing/README.md): the proxy moves the members; `/retornar` and `/reconectar` bring them back to a match.

## Acceptance criteria

- [x] Invites reach players on other servers, with the clickable accept line, and expire after 60 seconds.
- [x] Only the leader invites, kicks, transfers, teleports and removes offline members.
- [x] Disconnects and returns are announced, and offline members are removed after 3 minutes; the leadership passes on when the leader leaves.
- [x] `/party abrir` needs Ultra+ and `/party entrar` joins only open parties.
- [x] `/pc` and the `party` channel reach every online member; without a party the chat stays normal.
- [x] Only the leader queues, and the party only goes to a room with a seat for everyone.
- [x] A party stays on one team; when it is split, the match warns and its players get no stats.
- [x] `/party teleportar` reserves seats in a waiting room and makes the members spectators of a started match.
- [ ] The messages match the reference screenshots (checked in game).
- [ ] The party follows the leader between real servers (checked in game).

## Failures and recovery

- **Proxy restart:** every player is disconnected and the parties are lost.
- **A server starts:** it asks the proxy for every party; until the answer arrives it sees nobody in a party.
- **A lost packet:** a notice or an update can be lost (Redis Pub/Sub delivers at most once); the next change of that party sends the whole party again.
- **The leader's server does not answer a teleport:** nobody is moved; the leader may try again.
- **A member cannot be moved** (the leader's server is full or went down): the member stays where they are.

## Known limitations

- With more than one proxy, each proxy would have its own parties.
- Clans do not exist yet: `/canal clan` only answers that the player has no clan, and the party chat line has no clan tag.
- Changing lobby does not move the party; only `/party teleportar` and joining a match do.
