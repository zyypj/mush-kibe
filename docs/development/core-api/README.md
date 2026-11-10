**English** | [Português](README.pt-BR.md)

# Core API

What Muxe-Core offers to the other plugins. Reusable helpers belong here, in the core, instead of being rewritten in each feature: when a feature needs something generic (titles, action bars, effects, menus, items, scoreboards), it is added to the core and documented on this page.

## Getting the core

```java
MuxeCore core = MuxeCore.get();
```

Plugins declare `depend: [Muxe-Core]` in `plugin.yml` and `compileOnly(projects.core.bukkit)` in Gradle.

Muxe-Core loads every `br.com.muxe.core` class when the server loads it (`ClassPreloader`, in `onLoad`). The 1.8 plugin class loader is not safe when another plugin's thread and a core thread load the same class at the same moment (it fails with `LinkageError: duplicate class definition`), and loading everything up front removes that race.

| Method | What it gives |
|---|---|
| `identity()` | This server's id, group and kind. |
| `packets()`, `packetRegistry()`, `requestTimeout()` | The [packet bus](../../architecture/messaging/README.md). |
| `commands()`, `queries()` | The CQRS buses. Register handlers on enable. |
| `transactions()`, `migrations()` | PostgreSQL transactions and Flyway migrations of your module. Write repositories with the [typed schema and statements](../../architecture/persistence/README.md#typed-schema-and-statements) (`Table`, `Column`, `Sql`). |
| `store()` | `ExpiringStore`: short-lived values in Redis, always with an expiration. |
| `metrics()` | The metric registry. |
| `servers()` | The live server directory. |
| `network()` | `NetworkGateway`: send a player to a server or group. |
| `mainThread()`, `async()` | Executors for the server thread and for blocking work. `async()` is not limited: send database work as a command or query, whose bus is (see [Backpressure](../../architecture/observability/README.md#backpressure)). |
| `clock()` | The clock to inject in your services. |
| `messages()` | Shared messages of Muxe-Core. |
| `permissions()` | Ranks, tags and permissions of online players (see [Ranks and permissions](#ranks-and-permissions)). |
| `parties()` | The network parties as this server sees them (see [Parties](#parties)). |

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
- Every server clears the title when a player joins (`LeftoverTitleCleaner`), before any plugin sends its own. Velocity only resets the title on a server switch, which would leave the previous server's subtitle on screen.

### Action bar

```java
ActionBar.send(player, "&eDigite o código em frente no chat!");
```

Spigot 1.8.8 has no action bar API, so this sends the 1.8 packet through `LegacyPackets` (reflection on the server internals, resolved once). If the server does not expose them, `send` returns `false` and logs one warning; nothing else breaks. `ActionBar.isSupported()` tells whether it works.

The 1.8 client draws the action bar from the plain text of the message and drops JSON colors, so the `&` colors are kept inside the text as `§` codes, which the client does draw.

### Plugin configuration

```java
FileConfiguration config = PluginConfig.load(plugin);
```

`load` saves the default `config.yml` when there is none, then writes into the server's file every key of the jar's `config.yml` that it lacks, without changing the values already there, and reads it again. A plugin updated with new sections or messages keeps working on a server whose file was created by an older version. BedWars loads its configuration this way.

### Configuration editor

Staff view and change the `config.yml` of a plugin in game, without editing files. The pieces live in `br.com.muxe.core.bukkit.config.edit`, and the command in `br.com.muxe.core.bukkit.command.config`.

```java
ConfigReloads reloads = new ConfigReloads()
    .register("scoreboard", "scoreboard", config -> template.reload(() -> ScoreboardTemplate.from(config)));
ConfigEditor editor = new ConfigEditor(new PluginConfigStore(plugin, core.async()), reloads);
ConfigPathSuggestions.register(commands, editor);
commands.register(new LobbyConfigCommand(editor, new ConfigReplies(messages)));
```

- **Store.** `PluginConfigStore` works on the plugin's live `FileConfiguration` (`plugin.getConfig()`), so what other code of the plugin saves never overwrites a staff change. A change is written to the file on the core's async executor with `LatestWriter`: the text is built on the main thread and when several changes queue up only the latest content is written. The rest of the file is kept as Bukkit's YAML save writes it (comments are not kept, as with `saveConfig`).
- **Paths.** Words of letters, digits, `_` and `-` joined by dots (`ConfigPaths`). Any path with `password`, `senha`, `secret`, `token` or `credential` in it is protected: it is never shown, completed or changed. A section is never replaced by a value, and a value is never turned into a section.
- **Values.** `ConfigValues` infers the type from the text typed: `true`/`false`, whole numbers, decimals and text. When the key already has a value its type wins: a number key refuses text, a decimal key accepts `27` and stores `27.0`, a text key keeps `123` as text. Durations such as `60s` are text. `"123"` in quotes forces text. A list is set with comma separated values (`a, b, c`, or `[]` to empty it) and its items follow the type of the first item; `adicionar` and `remover` change one item, commas included.
- **Locations.** `LocationValues.of(world, x, y, z, yaw, pitch, centered)` rounds the coordinates to hundredths and the angles to tenths; with `centered` the horizontal coordinates go to the middle of the block (`.5`). It is written as `x`, `y`, `z`, `yaw`, `pitch` under the path, and `world` too when the section already has a `world` key, does not exist yet, or `--mundo` is used.
- **Unknown paths.** The editor only changes keys that exist in the file or in the jar's defaults; `--novo` at the end of the command creates a new one.
- **Live apply.** A component registers a `Reloadable` for a path prefix with `ConfigReloads.register(name, prefix, callback)`. After a change, every component whose prefix covers the changed path (or lies below it) is called once with the plugin configuration; the staff sees the names that were applied, the ones that failed with the reason, or that the change only applies after a restart. Settings that components read through a `LiveValue<T>` (package `br.com.muxe.core.common.config.live`) are replaced by `reload(loader)`, which keeps the old value when the loader throws; whoever already took a snapshot keeps using it, so a running session is never changed in the middle. The editor remembers the changed paths no component covers and `recarregar` lists them.

`ConfigCommand` is the abstract command every plugin extends with its own name and `@Permission(ConfigCommand.PERMISSION)`: `/coreconfig` (Muxe-Core), `/lobbyconfig` (lobby) and `/bwconfig` (BedWars). The permission is `muxe.admin.config` (default op), declared by Muxe-Core.

| Subcommand | What it does |
|---|---|
| `ver [caminho]` | Shows a value and its type, the items of a list or the keys of a section (those with `.` at the end are sections). Without a path it lists the top level. |
| `definir <caminho> <valor...> [--novo]` | Sets a value, saves and applies it live when a component covers it. |
| `adicionar <caminho> <valor...>` | Adds an item to a list. |
| `remover <caminho> <valor...>` | Removes an item from a list. |
| `local <caminho> [--novo] [--centro] [--mundo]` | Stores the location of the sender (players only) under the path. |
| `recarregar` | Reads the file again, applies every live component and lists the changes that wait for a restart. |

Tab completes the subcommands and the paths one level at a time.

```text
/lobbyconfig ver scoreboard
/lobbyconfig definir scoreboard.title &b&lMUXE
/lobbyconfig adicionar scoreboard.lines &7Novo
/lobbyconfig local spawn --novo
/bwconfig definir timings.respawn-seconds 7
/coreconfig definir join-greeting.delay 1s
```

The texts are `messages.config.*` in Muxe-Core's `config.yml`, which every plugin inherits.

### Message catalog

```java
MessageCatalog messages = MuxeCore.get().messages().merge(MessageCatalog.from(LocalizedConfig.load(this), "messages"));
player.sendMessage(messages.get(player, "wrong-password", "remaining", "3"));
messages.lines(player, "challenge").forEach(player::sendMessage);
```

Pass the player who reads the text as the first argument (`CommandSender`, `UUID` or `Language`), so the message comes out in their language. `get(key)` without a reader is Portuguese and is meant for the console and logs.

Placeholders are written `{name}` in the configuration. `merge` combines catalogs (the plugin's own messages win). Keys missing from the file on the server come from the defaults of the jar, so a message added in a new version works on a server whose `config.yml` was created by an older one.

A message can be a YAML list. `lines(key)` returns one entry per line, with blank items (`""`) turned into a line the chat shows empty, and `get(key)` joins the lines with line breaks, which suits kick screens. The proxy's `ProxyMessages` accepts lists the same way.

```yaml
messages:
  challenge:
    - "&6&lDESAFIO:"
    - "&eDigite o &bcódigo &eem frente &ano chat&e!"
    - ""
    - "&7&o(É necessário ativar os hologramas para visualizar corretamente)"
```

### Languages

Every player has a language, Portuguese (the default) or English. See [Language](../../features/language/README.md).

- `MuxeCore.get().languages()` (`PlayerLanguages`) gives a player's language with `of(player)`, changes it with `change(player, language)`, and calls `onChange(player -> ...)` on the main thread after a switch. Use that callback to re-render what the player already sees.
- `LocalizedConfig.load(plugin)` reads the plugin's `config.yml` and `lang/en.yml`. `in(language)` is the configuration root for one language, with English laid over Portuguese key by key. `map(root -> Settings.from(...))` builds a `Localized<Settings>`, one per language. Read it with `settings.in(languages.of(player))`. `MuxeCore.get().texts()` is the core's own.
- A text sent to many players is never rendered once. Render it per reader, or pass a `Text` (`messages.text(key, ...)`) and call `text.in(language)` for each reader.
- A value that is itself translated, such as a team or mode name, is a `Text` placeholder: `messages.get(player, "team-eliminated", "team", Text.of("Vermelho", "Red"))`.
- `LocalizedConfig.bundled(MyPlugin.class, "config.yml").untranslated(Language.ENGLISH, "")` lists the readable Portuguese texts without an English entry. Every plugin has a test that keeps it empty.

```java
LocalizedConfig texts = LocalizedConfig.load(this);
Localized<ShopSettings> shop = texts.map(root -> ShopSettings.from(root.getConfigurationSection("shop")));
Text announcement = messages.text("bed-destroyed", "team", team.name());
for (Player player : players) {
    player.sendMessage(announcement.in(messages.language(player)));
}
```

### Centered and clickable chat lines

```java
ChatLines.send(player, messages.lines("shop-payment"), CenteredText.DEFAULT_CENTER, "https://muxe.com.br/loja");
```

- A line that starts with `<center>` is padded with spaces so its middle sits on the given pixel of the chat (`CenteredText.DEFAULT_CENTER` is 114, the middle of a line of 57 struck-through spaces). Widths come from `TextWidth` in `core:common` (see [Platform-free helpers](#platform-free-helpers)).
- A line that starts with `<link>` opens the URL when clicked. Both markers can be combined.
- `ChatLines.send(player, lines, center, url, hover)` also shows `hover` (a list of lines) when the mouse is over a `<link>` line. `ChatLines.linked(text, url, hover)` builds that clickable line, which is what tests check.

### Command buttons in chat

```java
ClickableText.send(player,
    ChatButton.text("&c✘ &fCama do &9Azul"),
    ChatButton.run(" &e[Definir]", "/arena definir cama azul", "&7Clique para definir."),
    ChatButton.suggest(" &b[Nome]", "/arena nome "));
```

- `ChatButton.run` runs the command on click, `ChatButton.suggest` puts it in the chat box, and `ChatButton.text` is plain text. Each takes optional hover lines.
- `ClickableText.line(buttons)` builds the components, which is what tests check.

### Chat prompts

```java
ChatPrompts prompts = ChatPrompts.start(plugin, core.clock());
prompts.ask(player, Duration.ofSeconds(60), code -> apply(code), () -> cancelled(), () -> expired());
```

- The next chat message of the player is taken as the answer and never reaches the chat. Typing `cancelar` cancels; after the timeout the expired callback runs. Answers are delivered on the main thread.
- The waiting list is `PendingPrompts` in `core:common`, tested with a `MutableClock`.

### Anvil prompts

```java
AnvilPrompts anvils = AnvilPrompts.start(plugin);
anvils.ask(player, "Insira o usuário", name -> lookUp(name));
```

- Opens a working anvil (1.8 `ContainerAnvil`, no block needed) with a paper named after the placeholder. The player types in the rename field and clicks the result slot; the text without colours reaches the callback on the main thread, and the anvil closes.
- Clicks elsewhere are cancelled, and closing the anvil drops nothing. An empty text or the placeholder itself is ignored (`AnvilText.typed`).

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

- Lines go down from `top`, 0.28 blocks apart (`TextHolograms.standard(spacing)` for another spacing). `show(player, top, lines, gaps)` and `layout(top, lines, gaps)` give each pair of lines its own gap, from the top; lines past the list use the spacing, and `depth(gaps, line)` tells how far a line is below `top`. A blank line keeps its space and shows nothing. `height(lines)` tells how tall the text is.
- Same transport as the pixel holograms: invisible armor stands sent only to `player`.
- The height of a line is where its text starts: the top of the text sits 0.09375 blocks above it. The 1.8 client draws the name of a small armor stand as if the stand were full size (1.975 blocks), so `PacketHologramTransport` spawns each stand `LABEL_HEIGHT` (1.39375) blocks below the line.
- `hologram.rename(lines)` changes the text of a hologram that keeps the same number of lines, without respawning it; it returns `false` when the lines do not match and the hologram must be shown again.
- A click on a hologram reaches a `PacketInterceptor` with the id of an armor stand (`hologram.owns(id)`), but the stand sits well below its text. To know which part of the text the player aimed at, `LineAim.heightAt(player.getEyeLocation(), x, z)` gives the height where the player's sight passes closest to the hologram column.

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

### Sound cues

```java
Optional<SoundCue> cue = SoundCue.from(config.getConfigurationSection("parkour.sounds.start"));
cue.ifPresent(sound -> sound.play(player));
```

- `from(section)` reads `sound` (a 1.8 `Sound` name), `volume` and `pitch` (1.0 by default). A missing section or an empty `sound` gives nothing; an unknown sound is an error. Keys missing from the server's file come from the defaults of the jar, so a sound added in a new version plays on a server whose `config.yml` was created by an older one.
- `play(player)` plays it to that player only, at their position. The [parkour](../../features/parkour/README.md) uses it.

## Missions

Packages `br.com.muxe.core.common.mission` and `br.com.muxe.core.bukkit.mission`. See [Missions](../../features/missions/README.md).

```java
MissionConfig missions = MissionConfig.from(config.getConfigurationSection("missions"), MyGoal::find);
core.missions().register("mygame", missions.catalog(), (player, completed, at) -> giveGameXp(player, completed));
MissionMenus menus = new MissionMenus("mygame", missions, core.queries(), core.mainThread(), core.clock(), MissionModule.ZONE, logger);

MissionTracker tracker = new MissionTracker(core, "mygame", missions, "room " + code, logger);
tracker.begin(players);
tracker.check(id -> Optional.of(Map.of("FINAL_KILLS", finals(id))));
tracker.finish(id -> Optional.of(totals(id)));
```

- A game declares its goals as `MissionGoal(id, best)` and reports, for each player, the match totals by goal id. The tracker announces completions on the spot, saves, and sends the summary at the end.
- The core pays the credits; the game reward hook runs in the same transaction.

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

### Block masks

`BlockMask.start(plugin)` (package `br.com.muxe.core.bukkit.nms`) returns a mask that shows one player a fake block in place of a real one, and keeps it fake even when the server sends the real block again: the 1.8 server resends a block when a dig is aborted or a click is cancelled, without any Bukkit event.

```java
Optional<BlockMask> mask = BlockMask.start(plugin);
mask.ifPresent(blocks -> blocks.hide(player, location, Material.AIR, (byte) 0));
mask.ifPresent(blocks -> blocks.reveal(player, location));
```

- An outgoing Netty handler replaces every `PacketPlayOutBlockChange` of a masked position, for that player only, with a new packet carrying the fake block. The original packet is never changed, since the same instance can go to other players.
- `reveal` removes the mask and sends the real block. Quitting removes every mask of the player.
- Multi-block and chunk packets are not masked.
- The BedWars cosmetic preview uses it to keep the bed hidden while the animation plays.

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
- `MenuButton.animated(frames, period, click)` is a button whose icon cycles through `frames`, one every `period` ticks, while the menu is open (the core's `MenuAnimator` ticks every open menu); with a single frame it is a plain button.
- Every click inside a menu is cancelled: players never take or drop items from it.
- Titles are translated and cut to the 32 characters 1.8 allows.
- Opening a menu with the same title and rows as the menu the player has open redraws the open one in place (`Menu.sameLayout`), so a menu that reopens itself after a click does not close and does not send the cursor back to the middle. The new menu's buttons and `onClose` take over.

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

- One scoreboard per player, with up to 15 lines. Each line lives in a team, so updating a line never makes the sidebar flicker; only lines that changed are sent, and scores are only rewritten when a line changes its entry or the number of lines changes.
- Lines up to about 30 visible characters are split between the team prefix and suffix (16 characters each in 1.8), carrying the color over (`SidebarText.split`). Longer lines also use the line's entry name in the middle (up to 40 characters, starting with a hidden color code that keeps lines unique), so a line can reach about 70 characters (`SidebarText.layout`); only those lines change their entry when they change. Blank lines are fine.
- `core.sidebars()` (`SidebarOverrides`) lets a plugin replace the sidebar another plugin draws: `provide(player -> Optional.of(new SidebarContent(title, lines)))`. The lobby scoreboard asks it on every refresh; the BedWars lobby uses it for annex 1. `clear()` gives the sidebar back.
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
- `NpcService.start(plugin, npc -> layout, lines)` picks a layout per NPC (the BedWars lobby gives its statistics NPC its own gaps). `NpcService.start(plugin, layout, lines)` places the lines with an `NpcLabelLayout(bottom, gaps, spacing)`: the last line `bottom` blocks above the feet and the others going up through `gaps`, then `spacing`, with `LabelStack.upward`. `NpcLabelLayout.from(section)` reads it from config; without one, `NpcLabelLayout.STANDARD` keeps the last line 2.05 blocks up and the others 0.28 apart.
- `new Npc(id, location, skin, Optional.of(item))` puts an item in the NPC's hand, sent to each viewer as equipment right after the NPC appears. `PlacedNpcs.hand(HeldItem)` turns a stored material and data into that item.
- `MojangSkins` finds the signed skin of an original player name (`api.mojang.com` and `sessionserver.mojang.com`). Store the result instead of asking on every start.
- `npcs.visibleTo((viewer, npc) -> ...)` hides an NPC from the viewers the rule refuses, as if they were out of range. With it an NPC can exist for one player only, such as the BedWars statistics NPC that wears the skin of whoever looks at it.
- When only the text of the lines changes and they keep their places, the NPC's hologram renames its armor stands with a metadata packet instead of being removed and spawned again, so blinking text does not flicker.
- `PlayerSkins.of(player)` (package `br.com.muxe.core.bukkit.nms`) reads the signed skin of an online player from their profile, to dress an NPC as that player.
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
- `PlacementImport` reads `plugins/Muxe-Core/import.yml` on startup and on `/npc importar`: NPCs (with their own lines and item in hand), cash registers and parkour points. `PlacementImport.withSkins` fills the skin of NPCs that have a `skin-name` and no texture, and `run` returns an `Outcome` (file found, entries imported).

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
- On `adventure-only` kinds (`LOBBY` and `AUTH`), any game mode other than Adventure is cancelled, except Creative in build mode (`BuildRules.allowsGameMode`). A plugin that needs another mode for a moment calls `releaseGameMode(player)` before changing it and `lockGameMode(player)` when it gives the player back, as the BedWars cosmetic preview does with Survival (a 1.8 client in Adventure cannot hit blocks). Quitting locks the player again.
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

A game is entered through its lobby: the group `<game>-lobby` (such as `bedwars-lobby`) when one of its servers is alive, otherwise the game group itself. `ServerDirectory.entryGroup(game)` gives that group and `gamePlayers(game)` counts both.

- `players(group)` sums the online players of the game's live servers and of its lobby.
- `connect` answers `game-unavailable` when neither the lobby nor the game has a live server. Otherwise it asks the proxy, with no chat message, to send the player to the least loaded server of the entry group, with `connect-failed` if that fails.
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

## Worlds

- Every world of every Spigot server stays at midday without rain: `ClearMidday` (package `br.com.muxe.core.bukkit.world`) turns `doDaylightCycle` off, sets the time to 6000 and clears the weather of every world loaded, including the ones a plugin creates later, such as match worlds, and cancels every change to rain or thunder. Plugins do not need to do anything.

## Credits

Package `br.com.muxe.core.bukkit.credit`. Feature behaviour is in [Credits and cosmetics](../../features/credits/README.md).

- `CreditBalancesListener.start(plugin, core, Duration.ofSeconds(30))` returns a `CreditBalances` that loads every online player's balance on join, forgets it on quit and reloads all of them on the given period. `of(playerId)` reads the cached value (0 until loaded) without blocking; `refresh(playerId)` reloads one now.
- `CreditFormat.format(1750)` gives `1.750`.
- Every cosmetics menu lists the owned cosmetics first, then the others, each group in alphabetical order ignoring accents and case: sort with `CosmeticOrder.ownedFirst(wallet::owns, Cosmetic::name)` (core:common).
- Changing credits or cosmetics goes through the core commands: `ChangeCreditsCommand`, `BuyCosmeticCommand`, `GrantCosmeticCommand`, `SelectCosmeticCommand`; reads through `FindCreditsQuery` and `FindCosmeticsQuery`.

## Bed animations

Packages `br.com.muxe.core.bukkit.cosmetic.animation` (pure) and `br.com.muxe.core.bukkit.cosmetic.stage` (NMS).

- A `BedAnimation` writes a script: given the `BedFrame` (foot, head, forward, right), the breaker position, a `Stage` and a `Timeline`, it schedules what happens on each tick.
- `Stage` spawns packet-only actors (armor stands, mobs, falling blocks, items, heads dropped as items with a skin texture through `skullItem`, fireworks, snowballs), sends particles and sounds and fakes block changes. `Actor` moves, equips, poses, rides and removes them; `target(other)` makes a guardian fire its beam at another actor (other entities ignore it).
- `Timeline.at(tick, action)` and `repeat(from, until, period, action)` schedule steps; `lasts(ticks)` keeps the animation alive longer.
- `AnimationPlayer.play(animation, world, bed, breaker, viewers, onFinish)` runs it one step per tick for the viewers only. When the timeline ends it calls `onFinish` and the effect stays as it is: actors, fake blocks, seats and given items remain until no viewer is online in that world any more (in BedWars, until the players leave the match; in a preview, until the viewer is teleported back). Only then the stage closes, and fake blocks and chunks are resent only to viewers still in that world, so nobody ever sees the map going back to normal. `cancel()` freezes the effect where it is and takes the riders off their seats (so the viewer can be teleported), and the stage closes the same way.
- `Fireballs.accelerate(fireball, direction, acceleration)` (in `nms`) stops a launched fireball and sets its acceleration to the unit direction times `acceleration`, with no random spread, so it speeds up like a vanilla one (BedWars uses 0.1).
- A `SceneAnimation` is the same without a bed: its script gets a `Scene` (origin, horizontal facing and an optional skin texture, such as a victim's), and `AnimationPlayer.play(animation, world, scene, viewers, onFinish)` runs it. BedWars victory and final kill effects use it.
- Interactive scenes: a `Scene` also carries `riders` (player ids, the main one first). `Stage.mount(seat, yaw, riders)` seats them on real invisible marker armor stands (one each, lined up behind the first; leaving is cancelled, the seats cannot be hurt) and returns a `Mount` the script moves every tick (`move`), reading the first rider's look (`lookYaw`, `lookPitch`) and whether they hold W (`pushing`). `give(player, slot, material, name, onClick)` puts an item in the hotbar whose left and right clicks call back (it cannot be dropped), `push(player, velocity)` launches a player, `letFly(player)` lets a player fly until the stage closes (whoever could not fly before stops flying then and takes no fall damage), `eyes`/`look` read where they are and look, `hook(position, owner)` shows a fishing hook whose line the client draws to the owner, `tnt(position, velocity)` throws a primed TNT, `fireball(position, heading)` shows a big fireball that the client bends along `heading` (the script moves it and removes it on impact), `breakEffect(x, y, z, material, data)` shows the block break particles and sound, `rain(strength)` fades rain in for the viewers (0 to 1) and puts the world's weather back when the stage closes, `leash(actor, player)` ties a stage actor to a real player with a lead, `Mount.strafing()` reads the first rider's A/D input (positive to the left), `silentLightning(position)` strikes a bolt without the thunder sound, a `Scene` can carry the victim's signed skin (`body`, set with `withBody`), and `player(position, yaw, skin)` does the same with a given `SignedSkin` (when its signature is missing, it borrows the signature of an online player wearing the same texture) (the texture value and Mojang's signature, which the 1.8 client requires to show a skin), and `player(position, yaw)` shows a fake player with a random 8-character name whose own name tag is hidden (scripts draw their own label) and the skin of a random online player who is not watching the effect, or a default skin when there is nobody else (out of the tab list after 2 seconds), `lightning(position)` strikes a harmless bolt and `time(dayTime)` sets the time of day for the viewers. For big terrain changes, `type(x, y, z)` reads the real block (air in chunks that are not loaded) and `blocks(changes)` sends many fake blocks at once, one multi-block packet per chunk, `sections(changes)` does the same for huge fills by keeping a fake copy of each touched chunk (built from the real one, light included, empty sections with full sky light) and sending the touched sections whole, one chunk packet per chunk, so the client does not relight block by block, and `highest(x, z)` reads the y of the highest non-air block of a column (-1 when it is empty or not loaded); those chunks are resent whole at the end. When the stage closes the seats are removed and the riders put on the ground below, given items are taken back if they are still in their slot, fake blocks restored for whoever is still in the world and the time reset.
- A `Scene` also has an `area` (`Area`, a box in x and z): the map the effect may use, around the origin when the caller gives none (`Scene.REACH`, 16 blocks). Its middle (`middleX`, `middleZ`) is the centre of the box unless the caller sets one with `area.middleAt(x, z)`, as BedWars does with the emerald generators. `area.pick(random, y)` draws a point inside it and `area.copies()` says how many times to repeat what falls from the sky (1, or 2 on areas bigger than 32 by 32).
- A `Scene` can also carry a `dye` (`OptionalInt`, a wool colour from 0 to 15, set with `withDye`), such as the colour of the victim's team, for effects that tint blocks or particles; it is empty when the caller knows none.
- `Ground.surface(solid, x, y, z)` finds the top of the floor below a point (from 2 blocks above to 32 below, roofs ignored), so effects start on the ground wherever they happen; `Stage::solid` or a world lookup can be the `solid` test. `Ground.hit(solid, x, top, z, depth)` is for something falling: the top of the first solid block from `top` down to `depth` blocks below, or empty over the void.
- `BedFrame.ofBlock(x, y, z, data)` builds the frame from either half of a 1.8 bed.
- A caller can also drive a `PacketStage` itself, without a timeline: `new PacketStage(world, viewers, MountGuard.of(plugin))`, call the `Stage` methods whenever it wants, and `close()` it to remove every actor and fake block it created. BedWars projectile trails open one per flight and close it after the trail's linger.
- Animations are tested without a server with a recording `Stage`, as `BedAnimationsTest` does in BedWars.

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
| `TextWidth` | Pixel width of a text in the 1.8 default font (`TextWidth.of`), reading `&` and `§` codes; bold adds one pixel per character. `TextWidth.center(text, area)` pads with spaces so the text sits in the middle of an area, such as the server list (`TextWidth.SERVER_LIST_WIDTH`, 271). |

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

- **A plugin can replace a player's name tag**, which also sets the prefix in the tab list. `MuxeCore.get().nameTags().override(player -> Optional.of(tag))` answers a `NameTag(team, prefix)` for the players it wants and `Optional.empty()` for the others, which keep their rank tag. Call `nameTags().refresh(player)` after the answer changes, and `clearOverride()` when the plugin stops. BedWars uses it for the team letter, such as `§9§lA§9 `. The name tag teams let teammates see each other half transparent when invisible.
- `ArmorVisibility.hide(viewer, target)` (package `br.com.muxe.core.bukkit.nms`) sends `viewer` an empty armour for `target` without touching the real armour, and `show(viewer, target)` sends the real one back. BedWars uses it for invisible players.
- `MatchReturns` (package `br.com.muxe.core.common.match`) keeps, in an `ExpiringStore`, the server a player can return to: `remember(player, serverId, ttl)`, `serverOf(player)` and `forget(player)`. `/retornar` reads it on every server.

## Parties

Package `br.com.muxe.core.bukkit.party`. Muxe-Core runs the [party](../../features/party/README.md) on every server except auth servers: `/party`, `/pc`, `/canal`, the party chat channel and the copy of every party sent by the proxy. Games only read it.

```java
Parties parties = MuxeCore.get().parties();
Optional<PartySnapshot> party = parties.of(player.getUniqueId());
if (!parties.mayQueue(player.getUniqueId())) { ... }
List<UUID> companions = parties.companions(leader);
List<List<UUID>> groups = parties.groupsOf(playersInTheRoom);
parties.onGather((leader, members) -> acceptedMembers);
```

- `of(id)` gives the party of a player (`PartySnapshot`: id, leader, open flag and members with their online state); `mayQueue(id)` is `false` only for a member who is not the leader; `companions(leader)` lists the online members besides the leader.
- `groupsOf(players)` gives the parties among those players, which is what `PartyGroups` and `TeamAssigner` take.
- `onGather(handler)` answers `/party teleportar` on this server: given the leader and the members, it returns who may come, on the main thread. The default accepts everyone; the [minigame API](../../architecture/minigame-api/README.md#parties) reserves seats or spectator seats.
- `NetworkPlayers` keeps the names of the players online on the network (`network.players`, every 3 seconds, plus this server's own); the `network-players` suggestion provider uses it.
- The rules live in `core:common` (package `br.com.muxe.core.common.party.domain`): `PartyBook` (the parties of the proxy, tested with a `MutableClock`) and `PartyDirectory` (the copy on each server). `ChatChannels` (package `br.com.muxe.core.common.chat`) keeps the chosen chat channel in an `ExpiringStore`.

## Knockback

Package `br.com.muxe.core.bukkit.combat.knockback`, rules in `br.com.muxe.core.common.combat.knockback`. Each game mode has its own knockback profile; a player without a profile keeps the server's own knockback.

```java
KnockbackService knockback = MuxeCore.get().knockback();
KnockbackProfile bedwars = knockback.named("bedwars").orElseThrow();
knockback.assign(player.getUniqueId(), bedwars);
knockback.clear(player.getUniqueId());
```

- **Profiles.** `KnockbackProfile` holds the values of the room menu: `vertical`, `horizontal`, `vertical-boost`, `horizontal-boost`, `vertical-limit`, `vertical-friction`, `horizontal-friction`, `attacker-multiplier`, `restore-velocity`, `simulate-y`, `keep-sprint`, `ignore-sprint`, `use-rotation` and `click-limiter` (`off`, `low`, `normal`, `high`). `KnockbackSetting` reads and writes each one by key, which is what the custom room and the presets use. `KnockbackProfile.VANILLA` is the 1.8 knockback.
- **Named profiles.** `knockback.profiles.<name>` in Muxe-Core's `config.yml` (every key optional over the vanilla values): `vanilla`, `bedwars`, `skywars` and one `duels-<mode>` for each Duels mode of the reference. `knockback.click-limiter-ticks` gives the ticks of each limiter level (`off` 0, `low` 2, `normal` 4, `high` 6). A game picks profiles in its own `config.yml` (see the [minigame API](../../architecture/minigame-api/README.md#configuration)).
- **How a hit is pushed.** On a melee hit between two players, `EntityDamageByEntityEvent` (monitor) reads the victim and the attacker straight from the server objects and computes the push into the victim's `KnockbackState`. The server fires `PlayerVelocityEvent` for that same hit right after; the listener puts the computed push in it, so the client gets one velocity packet with the new value. Nothing is allocated per hit: one `KnockbackHit`, one `Motion` and one `Vector` per player are reused.
- **The rules** (`KnockbackMath`, `KnockbackState`, tested without a server): the previous motion is divided by the frictions, the base push points away from the attacker (or along the attacker's look with `use-rotation`), the base lift is capped by `vertical-limit`, and each knockback level (enchantment, plus sprint unless `ignore-sprint`) adds the boosts along the attacker's look. A victim still recovering from the last hit only takes the boost. Without `restore-velocity` the previous motion is the last push the player received, slowed by the ticks since then; with it, the server's own motion. `simulate-y` predicts the victim's vertical speed from the last push, the ticks since then and the ping. `attacker-multiplier` slows the attacker's own tracked motion after a hit, `keep-sprint` keeps them sprinting, and the click limiter skips a push that comes fewer ticks after the last one than its level allows.

## Custom rooms

Packages `br.com.muxe.core.bukkit.customroom` and `br.com.muxe.core.common.customroom`. See [Custom room](../../features/custom-room/README.md).

```java
CustomRoomKit kit = MuxeCore.get().customRooms();
kit.options().open(player, target, schema.root());
kit.modes().open(player, choice);
kit.presets().open(player, presetTarget);
```

- **Options** (`RoomOptions`, `core:common`): only the values that differ from the defaults, as `key=value`; they travel in packets and are stored in presets.
- **Schema** (`RoomSchema`, `OptionPage`, `OptionEntry`): the option pages of a game, written in YAML by the game server and read with `SchemaReader`. Entry types: `link`, `stepper` (`integer`, `multiplier` with a comma, `seconds`; bounds can be `mode-team-size`, `mode-players` or `mode-teams`), `toggle`, `decimal`, `choice`, `timer`, `team`, `shop-item`, `upgrade` and `free`.
- **Rules without a server**: `RoomEditor` reads every value and turns a click (`LEFT`, `RIGHT`, the shift ones, `DROP`) into new options, a page to open or an anvil question, and checks the answers; `RoomLore` builds the name, lore and icon of each entry and of the shop and upgrade editors with the texts of `RoomTexts`.
- **Menus**: `OptionMenus` draws any page of a schema for an `OptionsTarget` (the lobby draft or a live room); `ModeSelector`, `MapSelector` and `PresetMenu` are the mode, map and preset menus.
- **`AnvilEdits`** (`prompt`): an anvil with a paper named after the current value whose result is renamed while the player types (`&a<label>: &f<value>`); the answer is what was typed, even when unchanged.
- **Presets**: `SavePresetCommand`, `DeletePresetCommand`, `ListPresetsQuery` and `FindPresetQuery` over `core.custom_room_presets`.
- **Lobby**: `CustomRoomLobby` and `/salacustom` run on lobby servers; `CatalogDirectory` keeps the catalogs the game servers publish.

## Commands

Commands are annotated classes registered with `Commands`; see [Commands](../commands/README.md).

## Adding a helper

1. Put it in the module that fits: `core:bukkit` if it needs the Bukkit API, `core:common` otherwise.
2. Give it a package that names its area (`text`, `effect`, `item`, `menu`, …).
3. Keep the logic that can be tested without a server separate from the call into Bukkit, and test it.
4. Document it on this page, in English and Portuguese.
