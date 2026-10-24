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

### Action bar

```java
ActionBar.send(player, "&eDigite o código em frente no chat!");
```

Spigot 1.8.8 has no action bar API, so this sends the 1.8 packet through `LegacyPackets` (reflection on the server internals, resolved once). If the server does not expose them, `send` returns `false` and logs one warning; nothing else breaks. `ActionBar.isSupported()` tells whether it works.

### Message catalog

```java
MessageCatalog messages = MessageCatalog.from(getConfig().getConfigurationSection("messages"));
player.sendMessage(messages.get("wrong-password", "remaining", "3"));
```

Placeholders are written `{name}` in the configuration. `merge` combines catalogs (the plugin's own messages win).

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

## Adding a helper

1. Put it in the module that fits: `core:bukkit` if it needs the Bukkit API, `core:common` otherwise.
2. Give it a package that names its area (`text`, `effect`, `item`, `menu`, …).
3. Keep the logic that can be tested without a server separate from the call into Bukkit, and test it.
4. Document it on this page, in English and Portuguese.
