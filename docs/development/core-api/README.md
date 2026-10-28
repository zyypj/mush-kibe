**English** | [Português](README.pt-BR.md)

# Core API

What Muxe-Core offers to the other plugins. Reusable helpers belong here, in the core, instead of being rewritten in each feature: when a feature needs something generic (titles, action bars, effects, menus, items, scoreboards), it is added to the core and documented on this page.

## Getting the core

```java
MuxeCore core = MuxeCore.get();
```

Plugins declare `depend: [Muxe-Core]` in `plugin.yml` and `compileOnly(projects.core.bukkit)` in Gradle.

| Method | What it gives |
|---|---|
| `identity()` | This server's id, group and kind. |
| `packets()`, `packetRegistry()`, `requestTimeout()` | The [packet bus](../../architecture/messaging/README.md). |
| `commands()`, `queries()` | The CQRS buses. Register handlers on enable. |
| `transactions()`, `migrations()` | PostgreSQL transactions and Flyway migrations of your module. |
| `store()` | `ExpiringStore`: short-lived values in Redis, always with an expiration. |
| `metrics()` | The metric registry. |
| `servers()` | The live server directory. |
| `network()` | `NetworkGateway`: send a player to a server or group. |
| `mainThread()`, `async()` | Executors for the server thread and for blocking work. |
| `clock()` | The clock to inject in your services. |
| `messages()` | Shared messages of Muxe-Core. |
| `permissions()` | Ranks, tags and permissions of online players (see [Ranks and permissions](#ranks-and-permissions)). |

## Text

Package `br.com.muxe.core.bukkit.text`.

### Colors

```java
String text = Colors.translate("&aOlá");
String plain = Colors.strip("&aOlá");
```

### Titles

```java
Title.builder()
    .title("&b&lMUXE")
    .subtitle("&eUse /logar <senha>")
    .timings(0, 100, 10)
    .build()
    .send(player);

Title.of("&aVitória!", "&7Você venceu a partida").send(player);
Title.clear(player);
```

- Colors with `&` are translated automatically.
- Timings are in ticks (defaults 10, 70 and 20). On a server that does not expose title timings, the title is still shown with the client's default timings.
- `Title.clear` hides the title and the subtitle at once. The 1.8 client keeps the subtitle on screen after a plain reset, so `clear` first zeroes the timings, then empties the subtitle and only then resets.

### Action bar

```java
ActionBar.send(player, "&eDigite o código em frente no chat!");
```

Spigot 1.8.8 has no action bar API, so this sends the 1.8 packet through `LegacyPackets` (reflection on the server internals, resolved once). If the server does not expose them, `send` returns `false` and logs one warning; nothing else breaks. `ActionBar.isSupported()` tells whether it works.

The 1.8 client draws the action bar from the plain text of the message and drops JSON colors, so the `&` colors are kept inside the text as `§` codes, which the client does draw.

### Message catalog

```java
MessageCatalog messages = MessageCatalog.from(getConfig().getConfigurationSection("messages"));
player.sendMessage(messages.get("wrong-password", "remaining", "3"));
messages.lines("challenge").forEach(player::sendMessage);
```

Placeholders are written `{name}` in the configuration. `merge` combines catalogs (the plugin's own messages win).

A message can be a YAML list. `lines(key)` returns one entry per line, with blank items (`""`) turned into a line the chat shows empty, and `get(key)` joins the lines with line breaks, which suits kick screens. The proxy's `ProxyMessages` accepts lists the same way.

```yaml
messages:
  challenge:
    - "&6&lDESAFIO:"
    - "&eDigite o &bcódigo &eem frente &ano chat&e!"
    - ""
    - "&7&o(É necessário ativar os hologramas para visualizar corretamente)"
```

### Centered and clickable chat lines

```java
ChatLines.send(player, messages.lines("shop-payment"), CenteredText.DEFAULT_CENTER, "https://muxe.com.br/loja");
```

- A line that starts with `<center>` is padded with spaces so its middle sits on the given pixel of the chat (`CenteredText.DEFAULT_CENTER` is 114, the middle of a line of 57 struck-through spaces). Widths follow the 1.8 default font; bold adds one pixel per character.
- A line that starts with `<link>` opens the URL when clicked. Both markers can be combined.
- `ChatLines.send(player, lines, center, url, hover)` also shows `hover` (a list of lines) when the mouse is over a `<link>` line. `ChatLines.linked(text, url, hover)` builds that clickable line, which is what tests check.

### Chat prompts

```java
ChatPrompts prompts = ChatPrompts.start(plugin, core.clock());
prompts.ask(player, Duration.ofSeconds(60), code -> apply(code), () -> cancelled(), () -> expired());
```

- The next chat message of the player is taken as the answer and never reaches the chat. Typing `cancelar` cancels; after the timeout the expired callback runs. Answers are delivered on the main thread.
- The waiting list is `PendingPrompts` in `core:common`, tested with a `MutableClock`.

## Effects

Package `br.com.muxe.core.bukkit.effect`.

### Particle text

```java
ParticleText text = ParticleText.standard(ParticleColor.GREEN);
Location anchor = Anchors.inFrontOfEyes(player.getEyeLocation(), 4, text.height());
text.show(player, anchor, "A7KX");
```

- Draws text with colored dust particles, visible only to `player`. Particles last about a second, so call `show` every few ticks while the text must stay.
- `PixelFont.standard()` covers `A-Z`, `0-9` and space, 5×7 pixels each. Digits are drawn without slashes or serifs, so `0` and `O` look the same. `layout(text)` returns the pixel positions, which is what tests use.
- `Anchors.inFrontOfEyes` places content in front of where the player is looking, centered on the eye line.
- `ParticleColor` has `GREEN`, `WHITE`, `AQUA`, `YELLOW`, or any RGB value.

### Pixel holograms

Package `br.com.muxe.core.bukkit.hologram`.

```java
PixelHolograms holograms = PixelHolograms.standard();
Location anchor = Anchors.inFrontOfEyes(player.getEyeLocation(), 7, holograms.height());
SpawnedHologram hologram = holograms.show(player, anchor, "4829");
hologram.resend();
hologram.remove();
```

- Draws text as a grid of name-tag holograms (`⬛`), one per pixel of `PixelFont`, visible only to `player`. The holograms are invisible armor stands sent by packet: they never exist on the server, other players never see them and nobody can interact with them.
- `HologramStyle.standard()` shades each glyph: the top row in white (`&f`), the body in light green (`&a`) and the bottom row in dark green (`&2`). Any symbol and colors can be used.
- The holograms stay until `remove()`. Call `resend()` every few seconds while they must stay, in case the client dropped them.
- `PixelHolograms.layout(anchor, text)` returns the position and name of every pixel, which is what tests use. `ShadedLayout` does the same in grid units, without positions.
- Holograms need the 1.8 server internals, reached by reflection in `PacketHologramTransport`. When they are missing, `isAvailable()` is `false` and `show` draws nothing.

### Text holograms

```java
TextHolograms holograms = TextHolograms.standard();
SpawnedHologram hologram = holograms.show(player, top, List.of("&b&lSKYWARS", "&e37 jogando"));
```

- Lines go down from `top`, 0.28 blocks apart. A blank line keeps its space and shows nothing. `height(lines)` tells how tall the text is.
- Same transport as the pixel holograms: invisible armor stands sent only to `player`.

### Firework burst

```java
FireworkEffect effect = FireworkBurst.effect(config.getConfigurationSection("parkour.finish-firework"));
FireworkBurst.explode(plugin, location, effect);
```

- `explode(plugin, location, effect)` spawns a firework with that effect at `location` and makes it burst right there 2 ticks later (`DETONATE_AFTER_TICKS`), instead of flying up. Everyone nearby sees it.
- `effect(section)` reads an effect from the config: `type` (a `FireworkEffect.Type`, `BALL_LARGE` by default), `colors` (at least one), `fade-colors`, `flicker` and `trail` (both `false` by default). A section without colors is an error.
- `color(hex)` reads a color written as `#RRGGBB` (the `#` is optional).
- The burst needs the 1.8 server internals, reached by reflection. When they are missing, a warning is logged once and the firework flies a little before exploding.
- The [parkour](../../features/parkour/README.md) uses it when a player finishes.

## Items

Package `br.com.muxe.core.bukkit.item`.

```java
ItemStack star = ItemBuilder.of(Material.NETHER_STAR)
    .name("&aSelecionar lobby")
    .lore("&7Clique para abrir")
    .addLoreIf(player.hasPermission("muxe.vip"), "&6Vantagem VIP")
    .glow()
    .build();

ItemStack head = ItemBuilder.skull().name("&aMeu perfil").skullOwner(player.getName()).build();
ItemStack pane = ItemBuilder.from(config.getConfigurationSection("menus.lobby-selector.current"))
    .replacing("number", "3", "online", "32", "max", "60")
    .build();
```

- `ItemBuilder` is immutable: every method returns a new builder, so a template can be shared and filled in many times.
- Covers material and data, amount, name and lore (with `&` colors), `addLore`, `addLoreIf`, enchantments, `glow()`, `unbreakable()`, leather `color`, skulls by owner (`skullOwner`) or by texture (`skullTexture`: the base64 value, the texture URL or just the hash after `textures.minecraft.net/texture/`), and `{placeholder}` replacement in name and lore with `replacing`.
- Item flags are hidden by default; `showFlags()` shows them.
- `SkullTextures.apply(Skull, Optional<String>)` (package `br.com.muxe.core.bukkit.nms`) puts a texture on a head block already placed in the world; an empty texture gives the default head. Pass the value from `SkullTexture.value(...)`, then call `skull.update(...)`. The [social media heads](../../features/social-heads/README.md) use it.
- `ItemBuilder.from(section)` reads the same fields from configuration:

```yaml
item:
  material: LEATHER_CHESTPLATE
  data: 0
  amount: 1
  name: "&cPeitoral"
  lore:
    - "&7Uma linha"
  glow: true
  unbreakable: true
  hide-flags: true
  color: "#FF5555"
  skull-owner: Notch
  skull-texture: "d545b0ddd82cc4fd2eac64e445bee91b4706408514f71594d18c4b90a5491d74"
  enchantments:
    DURABILITY: 3
```

- `build()` needs a running server. Everything else is plain data, so tests check builders directly.

### Block updates

`BlockUpdates.resend(Block)` (package `br.com.muxe.core.bukkit.nms`) sends a block again to the players who see it, for changes the client does not pick up by itself, such as a new texture on a head block. It does nothing when the server internals are missing.

## Menus

Package `br.com.muxe.core.bukkit.menu`. Muxe-Core registers the listener, so any plugin can open menus.

```java
Menu menu = Menu.builder("&8Perfil").rows(3)
    .border(ItemBuilder.of(Material.STAINED_GLASS_PANE, 15).name(" "))
    .button(1, 4, ItemBuilder.skull().name("&a" + player.getName()).skullOwner(player.getName()), context -> context.close())
    .button(22, ItemBuilder.of(Material.BARRIER).name("&cFechar"), ClickContext::close)
    .onClose(viewer -> viewer.sendMessage("Até mais!"))
    .build();
menu.open(player);
```

- Buttons go by slot or by row and column (`Slots.of(row, column)`). `border` frames the menu, `fill` covers the empty slots; buttons always win.
- Clicks receive a `ClickContext`: the player, the click type (`isLeftClick`, `isRightClick`, `isShiftClick`), the slot, and actions to `close()`, `open(otherMenu)`, `update(slot, button)` or `updateIcon(icon)` while the menu is open.
- Every click inside a menu is cancelled: players never take or drop items from it.
- Titles are translated and cut to the 32 characters 1.8 allows.

### Paginated menus

```java
PaginatedMenu lobbies = PaginatedMenu.builder("Selecionar lobby ({page}/{pages})")
    .items(buttons)
    .previous(ItemBuilder.of(Material.ARROW).name("&aPágina anterior"))
    .next(ItemBuilder.of(Material.ARROW).name("&aPróxima página"))
    .build();
lobbies.open(player, 1);
```

- Up to 21 items per page, 7 per row, each row centered (`MenuLayout.centered`): one item sits in the middle, two lean to the left of the center, as in the lobby selector.
- The menu keeps one empty row above and below the items. The arrows sit at the ends of the last row and only appear when there is a page to go to.
- `slots(list)` places items in fixed slots instead, `rows(n)` fixes the height, `extra(slot, button)` adds a button to every page and `fill(icon)` covers the empty slots.
- `page(n)` builds the menu without opening it, which is what tests use.

### Configured buttons

Package `br.com.muxe.core.bukkit.menu.button`. Buttons whose slot, item and action come from configuration.

```java
List<ConfiguredButton> buttons = ConfiguredButton.list(config.getConfigurationSection("menus.game-selector.buttons"));
ConfiguredButton.requireFit(buttons, 4);
ButtonActions actions = new ButtonActions(games, shopAccess);
Menu.Builder menu = Menu.builder("Selecionar jogo").rows(4);
for (ConfiguredButton button : buttons) {
    menu.button(button.slot(), button.item(), context -> actions.run(context, button));
}
```

```yaml
buttons:
  skywars:
    slot: 14
    game: skywars
    item:
      material: EYE_OF_ENDER
      name: "&b&lSKY WARS"
  nick:
    slot: 39
    shop: ultra-plus
    item:
      material: NAME_TAG
      name: "&aNick (/nick)"
```

- Each button has a `slot` (an inventory slot, 0 to 53), an `item` in the [`ItemBuilder` format](#items) and, optionally, an action.
- `ButtonAction` is `NONE`, `GAME` (`game: <server group>`), `SHOP` (`shop: <category id>`, empty for the default category) or `COMMAND` (`command: <command>`, with or without `/`: closes the menu and the player runs the command).
- `ButtonActions.run` closes the menu and connects the player through the [`GameConnector`](#game-connector) for `GAME`, and opens the shop through [`ShopAccess`](#shop-access) for `SHOP`. `players(action)` gives the players of a `GAME` button's group, for `{players}`.
- `requireFit(buttons, rows)` refuses a button outside the menu's rows and two buttons in the same slot, so a wrong configuration fails on startup.
- The game selector and the profile of the [Hotbar](../../features/hotbar/README.md) are built this way.

## Scoreboard

Package `br.com.muxe.core.bukkit.scoreboard`.

```java
Sidebar sidebar = Sidebar.create(player);
sidebar.title("&b&lMUXE");
sidebar.lines(List.of("", "&fJogadores: &b2211", "", "&amuxe.com.br"));
```

- One scoreboard per player, with up to 15 lines. Each line lives in a team, so updating a line never makes the sidebar flicker; only lines that changed are sent.
- Lines up to about 30 visible characters are split between the team prefix and suffix (16 characters each in 1.8), carrying the color over (`SidebarText.split`). Blank lines are fine.
- `scoreboard()` exposes the Bukkit scoreboard for other teams, such as the one that hides NPC names.
- Muxe-Core puts the rank teams (tab list prefix and name tag, see [Ranks and tags](../../features/ranks-and-tags/README.md#prefixes)) on every scoreboard a player uses, sidebars included, within 5 ticks. Their names start with digits, so they never clash with the `muxe-line-*` teams or the NPC team.

## Boss bar

Package `br.com.muxe.core.bukkit.bossbar`.

```java
BossBars bars = BossBars.start(plugin);
BossBarFrames frames = new BossBarFrames(List.of("&e&lMUXE", "&6&lMUXE"), 10);
bars.show(player, frames.at(tick));
```

- 1.8 has no boss bar API: the bar is an invisible wither sent only to the player, kept 32 blocks in front of their eyes so it stays in view. It follows the player every 2 ticks when they move or turn.
- `show` renames the bar when it is already shown. The bar is spawned again after a respawn or world change.
- `BossBarFrames` turns a list of texts and a period in ticks into the frame for any tick, for animated titles.

## NPCs

Package `br.com.muxe.core.bukkit.npc`.

```java
NpcService npcs = NpcService.start(plugin, npc -> List.of("&b&lSKYWARS", "&e37 jogando"));
npcs.onClick((player, npc) -> player.sendMessage("Clicou em " + npc.id()));
npcs.put(new Npc("skywars", location, Optional.of(skin)));

MojangSkins.standard().byName("Notch").thenAccept(found -> ...);
```

- NPCs are players that only exist in packets: they are sent to each player within 48 blocks and removed beyond 64. They stay in the tab list for 2 seconds so the client loads the skin, and their name tag is hidden with a team; the text above them is a text hologram.
- Clicks are read from the network: `PacketInterceptor` adds a Netty handler to each player's connection and reports `PacketPlayInUseEntity`, because the server ignores clicks on entities it does not know. Every interceptor gets its own handler name, so several services (NPCs, display stands) can read clicks at once. The click handler runs on the main thread.
- `MojangSkins` finds the signed skin of an original player name (`api.mojang.com` and `sessionserver.mojang.com`). Store the result instead of asking on every start.
- See [ADR 0008](../../decisions/0008-packet-npcs-and-click-interception.md).

## Display stands

Package `br.com.muxe.core.bukkit.display`.

```java
DisplayService displays = DisplayService.start(plugin);
displays.onClick((player, display) -> openShop(player));
displays.put(new Display("shop", anchor, List.of(
    new DisplayStand(0.19, -0.03, -0.81, 0f, true, false, Optional.of(ItemBuilder.skull().skullTexture(hash)), Optional.empty(),
        StandPose.ZERO, StandPose.ZERO, StandMotion.NONE),
    new DisplayStand(0, -0.1, 0, 115f, false, true, Optional.empty(), Optional.of(ItemBuilder.of(Material.INK_SACK, 5).glow()),
        StandPose.ZERO, new StandPose(0, 90, 90), new StandMotion(4.5, 0.25, 40, 180))
), List.of(new DisplayLabel("&b&lLOJA", 3.6))));
```

- Invisible armor stands that only exist in packets, like the NPCs: shown within 48 blocks, removed beyond 64. Each stand has an offset from the anchor, a yaw, small or full size, arms, a helmet and an item in the hand, the head and right arm poses (in degrees) and a motion.
- `StandMotion` spins the stand by `degreesPerTick` and bobs it up and down (`bobHeight` blocks over `bobPeriodTicks`, shifted by `phaseDegrees`). Moving stands are updated every tick.
- Labels are text holograms at a height above the anchor (the height of the text itself).
- `labelText((viewer, text) -> ...)` changes the label text for each viewer before it is sent, so one display can show something different to each player, such as the personal record of the [parkour](../../features/parkour/README.md). `refresh(player)` sends the labels again to that player after what they should see changed. `Display.labelPixels(UnaryOperator<String>)` applies the same change to the lines of one display.
- `LabelStack` turns a list of lines into labels. `LabelStack.downward(lines, top, spacing)` keeps the first line at `top` and puts each next one `spacing` lower, as placed holograms do. `LabelStack.upward(lines, bottom, gaps, spacing)` keeps the last line at `bottom` and goes up using `gaps` from the bottom, then `spacing`, as the shop cash registers do. Blank lines keep their space.
- Clicking any stand or label, with either button, runs the click handler once on the main thread. A right click on an armor stand only sends `INTERACT_AT`, so that is the action that counts. Offsets are in world axes: moving the anchor moves the whole display without turning it.

## Placements

Packages `br.com.muxe.core.common.placement` and `br.com.muxe.core.bukkit.placement`.

Muxe-Core itself places the NPCs, holograms, shop cash registers, social media heads and parkour points that staff create with `/npc`, `/holograma`, `/loja caixa`, `/redes` and `/parkour`, on every server, as described in [NPCs and holograms](../../features/npcs-and-holograms/README.md). Plugins do not need their own NPC or hologram storage: staff place them by command.

- `Placement` is sealed: `NpcPlacement`, `HologramPlacement`, `RegisterPlacement`, `SocialHeadPlacement` (see [Social media heads](../../features/social-heads/README.md)) and `ParkourPointPlacement` (see [Parkour](../../features/parkour/README.md)), each with a server group, an id and a `Position`. `Placements` holds everything of a group.
- `PlacementSync` loads the placements of this server's group on startup, saves and removes them through `SavePlacementCommand` and `RemovePlacementCommand`, publishes `placement.changed` to the group, and reloads with `FindPlacementsQuery` when that packet arrives. Listeners registered with `onChange` run on the main thread.
- `Reconciler<T>` compares the wanted list with what is shown, by id, and returns only what was added or changed and what was removed, so a reload never respawns what did not change.
- `PlacementImport` reads `plugins/Muxe-Core/import.yml` once on startup: NPCs, cash registers and parkour points.

## Build mode

Package `br.com.muxe.core.bukkit.buildmode`. Muxe-Core registers `/build <on|off>` (permission `muxe.build`) on every server.

```java
BuildMode build = MuxeCore.get().buildMode();
if (build.isBuilding(player)) { ... }
build.addHook(new BuildModeHook() {
    @Override
    public boolean allows(Player player) { return authenticated(player); }

    @Override
    public void left(Player player) { giveItemsBack(player); }
});
```

- **On:** clears the inventory, switches to Creative and repeats `build-action-bar` in the action bar every second.
- **Off:** clears the inventory and the action bar and returns to Adventure on the kinds in `build-mode.adventure-only`, or to the server's default game mode elsewhere. Hooks then give items back.
- The state lives in Redis (`muxe:build:<uuid>`) for `build-mode.duration` (6 hours), renewed on every server join. A player who turns it on keeps it when moving to another server; players without `muxe.build` lose it on their next join.
- On `adventure-only` kinds (`LOBBY` and `AUTH`), any game mode other than Adventure is cancelled, except Creative in build mode (`BuildRules.allowsGameMode`).
- Plugins only read `isBuilding` to decide what a player may do. A hook can refuse build mode (`allows`), as the auth server does before login, and react to `entered` and `left`.

## Hotbar

Package `br.com.muxe.core.bukkit.hotbar`. Muxe-Core gives the lobby hotbar by itself on the server kinds of `hotbar.server-kinds` (`LOBBY` by default), as described in [Hotbar](../../features/hotbar/README.md). Plugins do not create it.

- On join it clears the inventory and the armor, gives the five items, selects slot 1 and applies the player's visibility (`VisibilityService` in `br.com.muxe.core.bukkit.visibility`, backed by `ChangeVisibilityCommand` and `FindVisibilityQuery` in `br.com.muxe.core.common.visibility`).
- `HotbarBuildHook` gives the hotbar back when a player leaves build mode. A plugin only adds a hook for what else it needs, as the lobby does to give flight back.
- A game's lobby gets the same hotbar by running with a kind listed in `hotbar.server-kinds`; its lobby selector lists the servers of its own group.

## Game connector

Package `br.com.muxe.core.bukkit.network`.

```java
GameConnector games = new GameConnector(core.servers(), core.network(), core.mainThread(), core.messages());
int playing = games.players("skywars");
games.connect(player, "skywars", "Sky Wars");
```

- `players(group)` sums the online players of the group's live servers.
- `connect` answers `game-unavailable` when the group has no live server. Otherwise it shows `game-connecting` and asks the proxy to send the player to the least loaded server of the group, with `connect-failed` if that fails.
- Game NPCs and the game selector use it, so both show the same messages.

## Shop access

Package `br.com.muxe.core.bukkit.shop`.

```java
shopAccess.open(player, "ultra-plus");
```

- Opens the [shop](../../features/shop/README.md) on a category; an empty or unknown category opens `shop.default-category`. When the shop is off on this server kind, it answers `shop-unavailable`.
- Muxe-Core creates it on startup. Shop NPCs and `shop` buttons use it.

## Locations

Package `br.com.muxe.core.bukkit.location`.

```java
ConfigLocation spawn = new ConfigLocation(plugin, "locations.spawn");
spawn.set(player.getLocation());
Location target = spawn.location().orElseGet(world::getSpawnLocation);
```

- `StoredLocation`: world, coordinates, yaw and pitch, read from and written to configuration (`from`, `toMap`), resolved to a Bukkit `Location` only when the world is loaded, and `describe()` for staff messages.
- `ConfigLocation`: one `StoredLocation` saved under a path of a plugin's `config.yml`, saved to disk on every change. It implements `LocationSlot`, so services can be tested with an in-memory slot.
- `Anchors.lookingAt(content, viewerEyes, height)`: places content, such as `ParticleText` or `PixelHolograms`, so it faces a viewer standing elsewhere.

## Platform-free helpers

In `core:common`, usable on the proxy too:

| Helper | Use |
|---|---|
| `Result` and `Problem` | Expected business failures with stable codes. |
| `ExpiringStore` | Short-lived key-value data in Redis (`RedisExpiringStore`), in-memory for tests (`InMemoryExpiringStore` in the test fixtures). |
| `MetricRegistry`, `Tags` | Counters, timers and gauges. |
| `ConfigSource`, `DurationParser`, `SecretResolver` | Reading configuration, durations (`5s`) and secrets (`env:`, `file:`). |
| `SpamGuard`, `CooldownFormat` | Anti-spam for clicks: a number of uses inside a window starts a cooldown (`3` uses in `1s` start `3s`), and the remaining time is shown as `2,9`. |
| `PendingPrompts` | Players waiting to answer something in the chat, each with a deadline (used by `ChatPrompts`). |

## Ranks and permissions

Package `br.com.muxe.core.bukkit.permission`. Muxe-Core loads every player's ranks and makes `hasPermission` follow them on every Bukkit server, as described in [Ranks and tags](../../features/ranks-and-tags/README.md). Plugins only ask.

```java
PermissionService ranks = MuxeCore.get().permissions();
if (player.hasPermission("muxe.lobby.fly")) { ... }
RankDisplay display = ranks.display(player.getUniqueId());
String name = display.formatName(player.getName());
Optional<ResolvedPermissions> resolved = ranks.resolved(player.getUniqueId());
ranks.onChange(changed -> refresh(changed));
```

- `hasPermission` works as usual: the player's permissible is replaced by one that asks the resolved ranks first and falls back to Bukkit (op and `plugin.yml` defaults) when the rank says nothing.
- `display(id)` gives the tag the player shows: `prefix()` (`&a&lVIP `), `color()` (`&a`), `formatName(name)` (`&a&lVIP &atadeu`), `coloredName(name)` (`&atadeu`), `tagDisplay()` (`&aVIP`) and `chatFormat(template)`.
- `resolved(id)` and `user(id)` give the resolved permissions and the raw nodes of online players; `data()` gives the groups, tags and tracks; `sync()` changes them for the whole network.
- `onChange` runs on the main thread whenever an online player's permissions change (new rank, new tag, world change, expiry).
- Commands and menus depend on the `PlayerRanks` interface, which tests implement with a `PermissionCache` and a `PermissionSync` over the in-memory fixtures.

## Commands

Commands are annotated classes registered with `Commands`; see [Commands](../commands/README.md).

## Adding a helper

1. Put it in the module that fits: `core:bukkit` if it needs the Bukkit API, `core:common` otherwise.
2. Give it a package that names its area (`text`, `effect`, `item`, `menu`, …).
3. Keep the logic that can be tested without a server separate from the call into Bukkit, and test it.
4. Document it on this page, in English and Portuguese.
