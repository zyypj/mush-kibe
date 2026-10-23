**English** | [Português](README.pt-BR.md)

# Development

How the project is organized, how dependencies are declared, how to build it and how to run a local network.

## Requirements

- **JDK 25.** If it is not installed, Gradle downloads it automatically the first time it builds.
- **Docker**, for the integration tests and the local PostgreSQL and Redis. Without Docker the integration tests are skipped, never faked.
- Nothing else. The Gradle wrapper (`gradlew`) downloads the right Gradle version by itself.

## Building

```bash
./gradlew build
```

On Windows, use `gradlew.bat build`.

This compiles every module, runs the unit and integration tests, checks the dependency rules and assembles one folder per server:

```text
build/servers/
├── proxy/plugins/    muxe-core-proxy.jar
├── auth/plugins/     muxe-core-bukkit.jar  muxe-auth.jar
├── lobby/plugins/    muxe-core-bukkit.jar  muxe-lobby.jar
├── skywars/plugins/  muxe-core-bukkit.jar  muxe-skywars.jar
└── bedwars/plugins/  muxe-core-bukkit.jar  muxe-bedwars.jar
```

Each folder holds exactly the jars of that server: copy its contents into the server's `plugins` folder. Jar names carry no version, so a new build replaces the old file. The version is inside each `plugin.yml` or `velocity-plugin.json`.

| Command | What it does |
|---|---|
| `./gradlew build` | Everything: compile, test, check rules and assemble every server. |
| `./gradlew assembleServers` | Assembles every server folder, without running tests. |
| `./gradlew assembleLobbyServer` | Assembles a single server. There is one task per server: `assembleProxyServer`, `assembleAuthServer`, `assembleSkywarsServer`, `assembleBedwarsServer`. |
| `./gradlew :servers:auth:test` | Runs the tests of a single module. |

## Modules

```text
muxe/
├── build-logic/          build conventions shared by every module
├── core/
│   ├── common/           platform-free core: packets, persistence, metrics, CQRS, accounts
│   ├── bukkit/           Muxe-Core, the base plugin of every Spigot server
│   └── proxy/            the Velocity plugin
├── servers/
│   ├── auth/             Muxe-Auth, the authentication server
│   └── lobby/            Muxe-Lobby, the main lobby
└── minigames/
    ├── engine/           matches, arenas and modes, shared by every game
    ├── skywars/          Muxe-SkyWars
    └── bedwars/          Muxe-BedWars
```

| Module | Kind | What it holds | Where it ends up |
|---|---|---|---|
| `core:common` | Library | Everything that does not depend on Bukkit or Velocity. | Inside `muxe-core-bukkit.jar` and `muxe-core-proxy.jar` |
| `core:bukkit` | Spigot plugin | Boots the core on a Spigot server and exposes `MuxeCore` to the other plugins. | `muxe-core-bukkit.jar` |
| `core:proxy` | Velocity plugin | Boots the core on Velocity, routes players and guards authentication. | `muxe-core-proxy.jar` |
| `servers:auth` | Spigot plugin | Registration and login. | `muxe-auth.jar` |
| `servers:lobby` | Spigot plugin | The main lobby. | `muxe-lobby.jar` |
| `minigames:engine` | Library that uses the Spigot API | The [game engine](../architecture/game-engine/README.md). | Inside each game's jar |
| `minigames:skywars`, `minigames:bedwars` | Spigot plugin | One game each. | `muxe-skywars.jar`, `muxe-bedwars.jar` |

Packages follow the module: `br.com.muxe.core.common`, `br.com.muxe.core.bukkit`, `br.com.muxe.core.proxy`, `br.com.muxe.auth`, `br.com.muxe.lobby`, `br.com.muxe.minigame.engine`, `br.com.muxe.minigame.skywars`...

## Servers

Which jars go to each server is declared in one place, the root [`build.gradle.kts`](../../build.gradle.kts):

```kotlin
network {
    server("proxy", projects.core.proxy)
    server("auth", projects.core.bukkit, projects.servers.auth)
    server("lobby", projects.core.bukkit, projects.servers.lobby)
}
```

Adding a server to the build is one line there. Its `assemble<Name>Server` task is created automatically.

## Dependency rules

Each module may only depend on the groups below. The build enforces these rules: a forbidden dependency fails the build with a message saying what is allowed.

| Module | May depend on |
|---|---|
| `core:common` | nothing else |
| `core:bukkit`, `core:proxy` | `core:common` |
| `servers:*` | `core:common`, `core:bukkit` |
| `minigames:engine` | `core:common`, `core:bukkit` |
| `minigames:*` | `core:common`, `core:bukkit`, `minigames:engine` |

What these rules guarantee:

- **Nothing depends on a game, a server plugin or the proxy.** They are the end of the chain.
- **Games and server plugins never depend on each other.** Removing or changing one never breaks another.
- **`core:common` knows nothing about Bukkit or Velocity.** It is tested without any server.
- **The core is never bundled twice.** Server and game plugins use `core:bukkit` with `compileOnly`, since Muxe-Core is already on the server.
- **The game engine is bundled inside each game.** Two games never run on the same server, so their copies never meet.

The rules live in [`build-logic/src/main/kotlin/ModuleRules.kt`](../../build-logic/src/main/kotlin/ModuleRules.kt).

## Declaring dependencies

- **Versions live only in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).** Modules reference them as `libs.<name>`.
- **Modules are referenced with type-safe accessors**, such as `projects.core.bukkit` and `projects.minigames.engine`.
- **Pick the configuration by where the classes come from at runtime:**

| Configuration | When to use | Bundled into the plugin jar |
|---|---|---|
| `api` | A library exposes the dependency's types in its own public API. | Yes |
| `implementation` | The dependency is used internally. | Yes |
| `compileOnly` | The server already provides it at runtime: the Spigot API, the Velocity API or Muxe-Core. | No |

- **Third-party libraries are relocated** to `br.com.muxe.libs`, so they never clash with another plugin. Relocation is applied to every plugin jar, including those that only reference Muxe-Core, so their references point to the same relocated classes. The list lives in `Relocations` in [`ServerPlugins.kt`](../../build-logic/src/main/kotlin/ServerPlugins.kt).
- **Service files are merged** (`META-INF/services`), which Flyway and the PostgreSQL driver need after relocation.
- **Velocity already provides Gson and SLF4J**, so the proxy jar does not bundle them.
- **Test fixtures** of `core:common` (`DirectTransactions`, `MutableClock`, `InMemoryAccountRepository`, `RecordingTransport`, `TestNetwork`) are shared with `testImplementation(testFixtures(projects.core.common))`.

## Build conventions

| Plugin | For | What it configures |
|---|---|---|
| `muxe.java` | Applied by all the others | Java 25, UTF-8, compiler warnings, JUnit, AssertJ, Testcontainers BOM and the dependency rules. |
| `muxe.library` | Libraries | A plain Java library. |
| `muxe.bukkit-library` | Libraries that use the Spigot API | The Spigot API at compile time. |
| `muxe.server-plugin` | Applied by the plugin conventions | Shadow jar named after the module, relocations, merged service files, and the `serverPlugin` artifact. |
| `muxe.bukkit-plugin` | Spigot plugins | The Spigot API, Bukkit-only relocations and `plugin.yml` with `${version}` filled in. |
| `muxe.velocity-plugin` | Velocity plugins | The Velocity API and `velocity-plugin.json` with `${version}` filled in. |
| `muxe.network` | The root project | The `network { }` block and the server assembly tasks. |

## Running a local network

### 1. Start PostgreSQL and Redis

```bash
cp .env.example .env
docker compose up -d
```

Edit `.env` first and set your own passwords and a messaging secret of at least 32 characters. Both services listen only on `127.0.0.1`.

### 2. Export the same variables for the servers

Every server reads its secrets from the environment (`env:` values in `config.yml`):

```bash
export MUXE_DATABASE_PASSWORD=...
export MUXE_REDIS_PASSWORD=...
export MUXE_MESSAGING_SECRET=...
```

### 3. Prepare the servers

| Folder | Software | `config.yml` of Muxe-Core or the proxy |
|---|---|---|
| `proxy` | Velocity 4.3 | `server.id: proxy-1`, `kind: PROXY` |
| `auth` | A Spigot 1.8.8 fork on Java 25 | `server.id: auth-1`, `group: auth`, `kind: AUTH` |
| `lobby` | A Spigot 1.8.8 fork on Java 25 | `server.id: lobby-1`, `group: lobby`, `kind: LOBBY` |
| `skywars` | A Spigot 1.8.8 fork on Java 25 | `server.id: skywars-1`, `group: skywars`, `kind: MINIGAME` |

- The `server.id` must equal the server name in `velocity.toml`.
- Give each server a different `metrics.port`, or disable metrics.
- In `velocity.toml`: `player-info-forwarding-mode = "legacy"` (1.8 servers do not support modern forwarding) and `online-mode = false`. Premium players are switched to online mode individually by Muxe, as described in [Authentication](../features/authentication/README.md).
- In each `spigot.yml`: `settings.bungeecord: true`. On the auth server also set `commands.log: false` so passwords never reach the console.
- Copy `build/servers/<name>/plugins/*` into each server's `plugins` folder.

### 4. Start everything

Start the Spigot servers, then Velocity. Each server logs `Muxe-Core ready as <id>` and `/muxe servers` lists every live server.
