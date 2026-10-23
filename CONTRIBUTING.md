**English** | [Português](CONTRIBUTING.pt-BR.md)

# Contributing

These rules apply to everyone who changes this repository, people and AI agents alike. AI agents also follow [AGENTS.md](AGENTS.md).

Code, commits and pull requests are in English. Documentation is in English and Portuguese. Player-facing messages are in Portuguese and live in configuration files.

## Before you start

- Read the [legal and clean-room policy](docs/legal/README.md). Contributions copied from another network, decompiled or leaked are rejected.
- Read the [process](docs/process/README.md): how work is planned, what "ready" and "done" mean.
- Read the [architecture overview](docs/architecture/overview/README.md) and the [conventions](docs/development/conventions/README.md).

## Branches

| Branch | What it holds |
|---|---|
| `production` | What is running in production. It is the default branch. |
| `homologation` | What is deployed to the test servers and being validated. |
| `development` | What was developed and is not in production yet. |

Changes move in one direction:

```text
work branch → development → homologation → production
```

- Work branches are created from `development` and merged back into it through a pull request.
- When a set of changes is ready to be tested, `development` is merged into `homologation` through a pull request.
- After it is validated on the test servers, `homologation` is merged into `production` through a pull request.
- `production` and `homologation` only receive changes through pull requests.

Work branch names: `<type>/<scope>-<short-description>`, such as `feat/auth-premium-bypass` or `fix/proxy-lobby-routing`.

## Commits

Commits follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) and are written in **English**.

```text
<type>(<scope>): <description>

[body]

[footer]
```

### Header

- `type` is one of the types below.
- `scope` is the module or feature affected, in kebab-case. Feature scopes match the feature's documentation folder (`authentication` ↔ `docs/features/authentication/`). Platform scopes are listed below. Omit the scope only for changes that span the whole repository.
- `description` uses the imperative mood ("add", not "added" or "adds"), starts in lowercase and has no trailing period.
- The whole header fits in 72 characters.

| Type | When to use |
|---|---|
| `feat` | A new feature or a new capability in an existing one. |
| `fix` | A bug fix. |
| `docs` | Documentation only. |
| `refactor` | A code change that neither fixes a bug nor changes behavior. |
| `perf` | A change that improves performance. |
| `test` | Adding or fixing tests only. |
| `build` | Build system or dependencies. |
| `ci` | CI configuration and workflows. |
| `chore` | Maintenance that fits none of the above. |
| `revert` | Reverts a previous commit. |

| Platform scope | Area |
|---|---|
| `build` | `build-logic`, Gradle files and the version catalog. |
| `messaging` | Packets, envelopes, transports, idempotency. |
| `persistence` | Database, transactions and migrations. |
| `metrics` | Metric registry and exporters. |
| `cqrs` | Command and query buses. |
| `config` | Configuration loading and secrets. |
| `core` | Runtime wiring shared by every server. |
| `bukkit` | The `core:bukkit` plugin. |
| `proxy` | The `core:proxy` Velocity plugin. |
| `engine` | The minigame engine. |

### Body and footer

- The body explains **why** the change was made, not what the diff already shows. Wrap it at 72 characters.
- Breaking changes add `!` after the type or scope (`feat(messaging)!: ...`) and a `BREAKING CHANGE:` footer describing what breaks and how to migrate.
- Reference issues in the footer with `Refs #N` or `Closes #N`.

### Examples

```text
feat(authentication): lock accounts after five wrong passwords
fix(proxy): keep unauthenticated players on the auth server
docs(messaging): document the exclusive idempotency level
refactor(persistence)!: bind transactions with scoped values
```

### Organized commits

Catch-all commits are forbidden. Work is split into separate commits, each one grouping a single logical change.

- One logical change per commit. Do not mix unrelated changes in the same commit.
- Group by change, not by file: a commit can touch many files when they belong to the same change.
- Vague descriptions like `update files`, `fixes`, `changes` or `wip` are forbidden.
- Tests and documentation for a change ship with that change, in the same commit or the same pull request.

### History

- Never force-push or rewrite history that was already pushed to a shared branch.

## Documentation

Every documentation file exists in **English and Portuguese**. The two versions say the same thing, and neither is merged without the other.

| Language | File |
|---|---|
| English | `README.md`, `CONTRIBUTING.md`, `AGENTS.md`, `SECURITY.md`, `docs/**/README.md`, `docs/decisions/*.md` |
| Portuguese | `README.pt-BR.md`, `CONTRIBUTING.pt-BR.md`, `AGENTS.pt-BR.md`, `SECURITY.pt-BR.md`, `docs/**/README.pt-BR.md`, `docs/decisions/*.pt-BR.md` |

`CLAUDE.md` is not documentation: it only points Claude Code to `AGENTS.md`.

### Every feature has its own documentation

Each feature lives in its own folder under [`docs/features/`](docs/features/), named after the commit scope.

- **New feature:** the pull request that adds it also creates its documentation, starting from [`docs/features/_template/`](docs/features/_template/).
- **Changed feature:** the pull request that changes behavior, commands, permissions, configuration, packets, metrics or integrations also updates the feature's documentation, in both languages.
- **Removed feature:** the pull request that removes it also removes its documentation.

Documentation describes how the feature works **today**. History lives in git and in pull requests. Decisions that shape the architecture are recorded as [ADRs](docs/decisions/README.md).

## Pull requests

- Work pull requests target `development`.
- The title follows the commit convention.
- The body follows the [template](.github/pull_request_template.md): **Problem**, **Solution**, **How it was done** and **Validations** (only checks that actually ran, with their results).
- The documentation checklist in the template is mandatory.
- CI must be green: `./gradlew build` compiles, tests and checks the module rules.
