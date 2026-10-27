**English** | [Português](README.pt-BR.md)

# Social media heads

A player head placed in the world that shows, one at a time, the social networks of Muxe, with a label just above it. Staff place it by command on any server. It lives in Muxe-Core (`core:common` package `br.com.muxe.core.common.placement`, `core:bukkit` package `social`) and is shared by the server group like [NPCs and holograms](../npcs-and-holograms/README.md).

## User stories

- As a player, I want to see the network's social media in the lobby and get the link with one click, so that I can follow it.
- As a staff member, I want to place, move and remove the head by command, on every server of a group at once.
- As an operator, I want to change the networks, labels, textures and links in the config, without touching code.

## How it works

### The head

- `/redes criar <id>` places a player head on the floor of the block where you stand, turned to where you look (16 directions, like a head placed by hand). The head replaces whatever was in that block.
- A label sits `social-heads.label-height` (0.5125) blocks above the bottom of the head block, centered on it.
- Every `social-heads.interval` (1 minute) the head switches to the next network of `social-heads.networks`, in the order of the config, and starts over after the last one. The texture of the head and the label change together.
- The network shown comes from the clock: `floor(epochMillis / interval) % networks`. Every server of every group computes the same index, so all heads of the network show the same network at the same time, with no message between servers. Each server checks the clock once per second.
- A network without `texture` shows the default player head.
- The head block is a real block of the world (a `SKULL`), resent to nearby players when it changes. It cannot be broken. The label is a packet-only hologram, like the [display stands](../../development/core-api/README.md#display-stands).

### Clicking

- A left or right click on the head, or on its label, sends the `message` of the network shown at that moment, line by line, with `&` colors. With the default config, Instagram sends:

```
&d&lINSTAGRAM:
&fhttps://www.instagram.com/muxe/
```

- Clicks share the anti-spam of Muxe-Core (`spam.*`) with NPCs, cash registers, the hotbar and the parkour items: three clicks within 1 second block them for 3 seconds with `&cAguarde 2,9s para usar novamente.`
- Clicking the head never places or breaks anything: the click is cancelled.

### Shared by the server group

- Heads are placements of the kind `SOCIAL_HEAD` (`SocialHeadPlacement(group, id, position)`): they belong to the server group (`server.group`), are saved in PostgreSQL and sync through `placement.changed`, exactly as described in [NPCs and holograms](../npcs-and-holograms/README.md#shared-by-the-server-group). Ids follow the same rules (lowercase letters, digits, `-` and `_`, up to 32 characters).
- `/redes mover <id>` takes the head out of its old block (the block becomes air) and places it where you stand, turned to where you look. `/redes remover <id>` turns the head block into air and removes the label on every server of the group that is online.
- A head is shown only if its world is loaded on that server.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/redes criar <id>` | Places a head where you stand, facing where you look. | `muxe.social` |
| `/redes mover <id>` | Moves a head to where you stand, facing where you look. | `muxe.social` |
| `/redes remover <id>` | Removes a head. | `muxe.social` |
| `/redes listar` | Lists the heads of this server group. | `muxe.social` |

Without arguments, or with a wrong one, the command shows its usage (`messages.social-usage`).

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.social` | Use `/redes`. | op |

Clicking a head needs no permission.

## Configuration

`plugins/Muxe-Core/config.yml`:

```yaml
social-heads:
  interval: 1m
  label-height: 0.5125
  networks:
    instagram:
      label: "&d@muxe"
      message:
        - "&d&lINSTAGRAM:"
        - "&fhttps://www.instagram.com/muxe/"
    youtube:
      label: "&c@muxe"
      texture: "ffb6ce188b84bdf96985a356bf3f485416f33d3b6dfce3a3fc144e77fe8dbb25"
      message:
        - "&c&lYOUTUBE:"
        - "&fhttps://www.youtube.com/@muxe"
```

| Key | Default | Description |
|---|---|---|
| `social-heads.interval` | `1m` | How long each network is shown. Must be positive. |
| `social-heads.label-height` | `0.5125` | Height of the label above the bottom of the head block. |
| `social-heads.networks.<id>` | `instagram`, `twitch`, `tiktok`, `twitter`, `youtube` | The networks, shown in this order. At least one is needed. |
| `social-heads.networks.<id>.label` | `&d@muxe`, `&5@muxe`, `&b@muxe`, `&b@muxe`, `&c@muxe` | Text of the label above the head. |
| `social-heads.networks.<id>.texture` | Twitch, TikTok and YouTube only | Head texture: the hash after `textures.minecraft.net/texture/`, the texture URL or the base64 value, as `skull-texture` on [items](../../development/core-api/README.md#items). Without it, the default player head. |
| `social-heads.networks.<id>.message` | Name of the network and its link | Chat lines sent on click. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam shared with NPCs, cash registers, the hotbar and the parkour items. |

Without a `social-heads` section, heads are off on that server: the core logs `The social heads are off: config.yml has no 'social-heads' section`, shows no head and ignores clicks. `/redes` still saves placements, which the other servers of the group show. An empty `networks` or an `interval` that is not positive is an error on startup.

### Messages

All in `messages` of `plugins/Muxe-Core/config.yml`:

| Key | Default |
|---|---|
| `social-usage` | List: `&eUso do /redes:` and one line per subcommand. |
| `social-invalid-id` | `&cO id precisa ter só letras minúsculas, números, - e _.` |
| `social-exists` | `&cJá existe uma cabeça de redes sociais com o id &f{id}&c.` |
| `social-unknown` | `&cNão existe cabeça de redes sociais com o id &f{id}&c.` |
| `social-created` | `&aCabeça de redes sociais &f{id} &acolocada.` |
| `social-moved` | `&aCabeça de redes sociais &f{id} &amovida para onde você está.` |
| `social-removed` | `&aCabeça de redes sociais &f{id} &aremovida.` |
| `social-list-empty` | `&7Nenhuma cabeça de redes sociais. Use &f/redes criar&7.` |
| `social-list-header` | `&eCabeças de redes sociais do grupo &f{group} &e({count}):` |
| `social-list-line` | `&8- &f{id} &8{location}` |
| `placement-failed`, `spam-cooldown` | Shared with NPCs and holograms. |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `placement.changed` | the server group | standard | A head of the group was created, moved or removed; every server of the group reloads its placements from PostgreSQL. |

The network switch sends no packet: every server reads it from the clock.

## Data

| Table | Content |
|---|---|
| `core.social_head_placements` | `server_group`, `id`, position (`world`, `x`, `y`, `z`, `yaw`, `pitch`). Primary key `(server_group, id)`. |

Created by the core migration `V6__create_social_head_placements.sql`. Saved and removed through `SavePlacementCommand` and `RemovePlacementCommand` (`PlacementKind.SOCIAL_HEAD`), read with `FindPlacementsQuery`. The networks are configuration, not data.

## Metrics

None of its own. Saving, removing and loading heads count in the placement metrics listed in [NPCs and holograms](../npcs-and-holograms/README.md#metrics).

## Integrations

- [NPCs and holograms](../npcs-and-holograms/README.md): same storage, ids and group sync.
- The [Core API](../../development/core-api/README.md) provides `DisplayService` for the label, `SkullTextures.apply(Skull, Optional<String>)` for the head texture and `BlockUpdates.resend(Block)` to show the new texture to nearby players.

## Acceptance criteria

- [x] The default config has five networks and changes every minute.
- [x] The network shown follows the clock, so every server shows the same one at the same time.
- [x] Each network has its label, optional texture and message.
- [x] An interval that is not positive, or no network, is refused.
- [x] The head looks where the staff member who placed it was looking.
- [x] A head survives a round trip through PostgreSQL, can be moved and removed, and other groups stay apart.
- [x] A head placed on one lobby appears on another lobby of the group, and removing it on either removes it from both.
- [ ] The head, its label and its texture change together in game, clicks on the head and the label send the message, and the head cannot be broken (checked in game).

## Failures and recovery

- PostgreSQL unavailable when a command runs: staff see `&cNão foi possível salvar agora. Tente novamente em instantes.` and nothing changes.
- PostgreSQL unavailable on startup, or Redis unavailable: same as [NPCs and holograms](../npcs-and-holograms/README.md#failures-and-recovery).
- World not loaded on a server: that head is skipped on that server with a warning.
- Server internals missing (another server version): the head shows the default texture and may only change for players after they reload the chunk.

## Known limitations

- The head is a real block of the server's world. If a head is removed or moved while a server of the group is offline, that server keeps a plain head block where it was (no label, no click, cannot be told apart from a decoration) until someone removes it by hand. The same happens when the `social-heads` section is taken out of a server's config.
- Removing or moving a head leaves air: the block that was there before the head is not restored.
- The network shown is the same for every head; one head cannot show a single network.
- Servers whose clocks disagree may show different networks for a moment around each switch.

## Pending

- The handles and links in the default config are placeholders (`muxe`): replace them with the real accounts of the network.
- Instagram and Twitter have no texture yet and show the default player head until one is set.
