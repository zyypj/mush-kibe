**English** | [Português](AGENTS.pt-BR.md)

# AI agents

Rules for AI agents working on this repository. Everything in [CONTRIBUTING.md](CONTRIBUTING.md) also applies to agents; this file adds what is specific to them. When a rule here conflicts with a generic habit or default, this file wins.

## Orientation

Read these before changing code, in this order:

1. [Architecture overview](docs/architecture/overview/README.md): layers, CQRS, package organization.
2. [Conventions](docs/development/conventions/README.md): Java, naming, packages, configuration and message style.
3. [Development](docs/development/README.md): modules, dependency rules and commands.
4. The architecture document of the area you touch ([messaging](docs/architecture/messaging/README.md), [persistence](docs/architecture/persistence/README.md), [observability](docs/architecture/observability/README.md), [security](docs/architecture/security/README.md), [game engine](docs/architecture/game-engine/README.md)).
5. The feature documentation under [docs/features/](docs/features/).

## Legal

- Never add code, assets, maps, names or text copied from another network, decompiled from a plugin or taken from a leak. See [Legal and clean-room policy](docs/legal/README.md).
- Never use the name of another network in code, configuration or player-facing text. The project is called **Muxe**.

## Build

- Run `./gradlew build` before declaring a change done. It compiles every module, runs the tests and checks the dependency rules.
- Never write a version in a module's build file. Versions live only in `gradle/libs.versions.toml`.
- Respect the module dependency rules in `build-logic/src/main/kotlin/ModuleRules.kt`. Never weaken a rule to make a dependency compile.
- Third-party libraries are shaded and relocated to `br.com.muxe.libs`. A new library bundled into a plugin must be added to `Relocations` in `build-logic/src/main/kotlin/ServerPlugins.kt`.

## Code

- Java 25. Records, sealed types, pattern matching and virtual threads are welcome.
- The Bukkit API is **1.8.8**. Never call APIs that do not exist in 1.8.8 (no Adventure, no `PersistentDataContainer`, no `api-version`).
- Comments are forbidden, except `TODO` and `FIXME` with the reason and the issue when there is one. No Javadoc, no block comments, no commented-out code. Let names carry the meaning.
- Never write fully qualified class names inline. Always import.
- Keep classes small and in a sub-package that names their role (`domain`, `application/command`, `application/query`, `infrastructure/postgres`, `listener`, `command`, `bootstrap`...). Never pile unrelated classes in one package.
- Plugin entry points stay thin: they create a `*Bootstrap` that wires everything with explicit constructor injection. No dependency injection framework, no static singletons besides `MuxeCore.get()`.
- Business rules live in plain Java classes that can be tested without a server. Bukkit and Velocity classes only adapt events and commands to them.
- State changes go through a `Command` and its handler; reads go through a `Query`. Expected business failures return `Result.failure(...)` with a problem code; exceptions are for infrastructure failures.
- Never block the main thread. Database and Redis work runs on the core's async executor; anything that touches Bukkit goes back through `MuxeCore.mainThread()`.
- Never reach Redis directly from a feature. Use `PacketBus` and register a `PacketType` with a codec and a delivery policy.
- Player-facing text lives in each plugin's `config.yml`, under `messages`, in Portuguese with `&` color codes.
- YAML uses expanded block style: no inline maps, no inline lists.

## Tests

- Every behavior change comes with tests that cover it.
- A bug fix starts with a test that reproduces the bug and fails before the fix.
- Test business logic with fast unit tests using the fakes in `core/common/src/testFixtures` (`DirectTransactions`, `MutableClock`, `InMemoryAccountRepository`, `TestNetwork`).
- Behavior that depends on PostgreSQL or Redis is covered by integration tests with Testcontainers (`@Testcontainers(disabledWithoutDocker = true)`).
- Tests are deterministic: inject `Clock`, never sleep to wait for real time unless the test is about a real network round trip.
- Never delete, skip, disable or weaken a test or an assertion to make it pass.
- Run the tests that cover the change before declaring it done, and report the commands and results. Never claim a validation passed without running it.

## Commits and pull requests

- Conventional Commits in English, one logical change per commit, as in [CONTRIBUTING.md](CONTRIBUTING.md#commits).
- Create work branches from `development` and open pull requests against `development`. Never commit directly to `production` or `homologation`.
- Never force-push, rewrite pushed history or skip hooks.
- Fill in every section of the [pull request template](.github/pull_request_template.md). **Validations** lists only checks that actually ran.

## Documentation

- Every documentation change is made in English and Portuguese in the same change.
- Before finishing a task, check whether any documentation describes something you changed (features, packets, metrics, configuration, commands, permissions), and update it.
- A new packet type, metric, command, permission or configuration key is documented in the feature that owns it.
