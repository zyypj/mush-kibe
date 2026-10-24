**English** | [Português](README.pt-BR.md)

# Conventions

How code, configuration and messages are written in Muxe. The goal is that any file looks like it was written by the same person.

## Java

- **Java 25.** Use records for values and messages, sealed interfaces for closed hierarchies, `switch` with patterns, `var` only when the type is obvious, virtual threads through the core's executors.
- **Bukkit 1.8.8 API.** The servers run a 1.8.8 fork. Never call API that does not exist in Spigot 1.8.8.
- **No comments.** No Javadoc, block or line comments, and no commented-out code. `TODO` and `FIXME` are the only exceptions, with the reason and the issue. A piece of code that seems to need a comment becomes a well-named method or variable.
- **No inline fully qualified names.** Always import, except to disambiguate two classes with the same simple name in one file.
- **Constructor injection, explicit wiring.** Each plugin has a `*Bootstrap` that builds its object graph. No DI framework, no static state besides `MuxeCore.get()`.
- **Final by default.** Classes are `final` unless designed for extension. Fields are `final` unless they must change.
- **Immutability for values.** Records copy mutable inputs (`List.copyOf`, `clone()` for arrays).
- **Fail fast.** Validate in record constructors and throw `IllegalArgumentException` with a message that says what was expected.
- **Expected failures are values.** Business outcomes such as "wrong password" are `Result.failure(Problem)`, with a stable `code` like `auth.wrong_password`. Exceptions are for bugs and infrastructure failures.

## Packages

Muxe prefers several small, named sub-packages over big flat ones. A package name says the role of its classes.

```text
br.com.muxe.auth
├── bootstrap                      wiring of the plugin
├── command                        Bukkit command executors
├── config                         settings records read from config.yml
├── credential                     a bounded context
│   ├── domain                     entities, value objects, repository interfaces, problem codes
│   ├── policy                     business rules that are not entities
│   ├── crypto                     hashing
│   ├── application
│   │   ├── command                commands and their handlers
│   │   └── query                  queries and their handlers
│   └── infrastructure
│       └── postgres               repository implementations
├── flow                           orchestration of the player experience
├── listener                       Bukkit listeners
└── session                        in-memory state of online players
```

Rules:

- **`domain` depends on nothing** but the JDK and `core.common.result`.
- **`application` depends on `domain`** and on ports such as `Transactions`, never on Bukkit or Velocity.
- **`infrastructure` implements `domain` ports** (repositories, hashers, transports).
- **Platform adapters** (`listener`, `command`, `bootstrap`) translate Bukkit or Velocity into commands and queries.
- **`internal` packages** (such as `messaging.bus.internal`) are implementation details. Other modules never import them.

## Naming

| Thing | Pattern | Example |
|---|---|---|
| Command | verb + noun + `Command` | `RecordLoginCommand` |
| Command handler | command name with `Handler` | `RecordLoginHandler` |
| Query | `Find…Query`, `Has…Query` | `FindAccountQuery`, `HasCredentialQuery` |
| Packet | noun phrase + `Packet`, or `…Request`/`…Response` | `ConnectPlayerPacket`, `PlayerLocateRequest` |
| Packet id | `area.name`, lowercase with underscores | `player.connect`, `auth.authenticated` |
| Problem code | `area.snake_case` | `account.name_case_conflict` |
| Metric | `muxe_<area>_<what>_<unit>`, counters end with `_total`, timers with `_seconds` | `muxe_packets_published_total` |
| Migration | `V<n>__<description>.sql` in `db/migration/<module>/` | `V1__create_credentials.sql` |
| Plugin | `Muxe-<Name>` | `Muxe-Auth` |
| Jar | `muxe-<name>.jar` | `muxe-auth.jar` |

## Tests

- Test classes mirror the package of the class under test and end with `Test`. Integration tests end with `IntegrationTest`.
- Test method names describe behavior in plain English: `refusesANameThatDiffersOnlyByCase`.
- Arrange, act, assert, separated by blank lines. AssertJ for every assertion.
- Inject `Clock` and use `MutableClock` instead of sleeping.

## Configuration

- Each plugin has a `config.yml` with defaults that work locally.
- YAML is written in expanded block style, two spaces per level. No inline maps (`{ a: 1 }`) and no inline lists (`[a, b]`).
- Keys are `kebab-case`.
- Durations are strings such as `500ms`, `5s`, `2m`, `1h`.
- Secrets are never written in plain text in the defaults. They use `env:NAME` (environment variable) or `file:/path` (a file such as a Docker secret).

## Messages

- Player-facing text lives under `messages` in the plugin's `config.yml`, never in Java code.
- Messages are in Portuguese, with `&` color codes, and placeholders written as `{name}`.
- Keys are `kebab-case` and grouped by feature when it helps (`admin.servers-line`).
- A message shown as several lines at once is one YAML list under one key, never one key per line. A blank line is an empty item (`""`).
- When a message is copied from a reference screenshot, keep its colors, its order and its blank lines.
- Problem codes from the application layer are mapped to message keys in the platform layer.
