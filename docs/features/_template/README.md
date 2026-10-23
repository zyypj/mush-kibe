**English** | [Português](README.pt-BR.md)

# Feature name

One or two sentences on what the feature does and who it is for.

<!-- Keep every section. When one does not apply, write "None." so readers know it was considered. -->

## User stories

- As a <player | staff member | operator>, I want <capability>, so that <benefit>.

## How it works

How the feature behaves from the player's and staff's point of view, including edge cases and limits.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/example <player>` | What it does. | `muxe.example` |

## Permissions

| Permission | Grants |
|---|---|
| `muxe.example` | What it allows. |

## Configuration

| Key | Default | Description |
|---|---|---|
| `example.enabled` | `true` | What it controls. |

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `area.name` | proxy | standard | Why it is sent. |

## Data

Tables, schemas and Redis keys owned by the feature.

## Metrics

Metrics added by the feature, also listed in [Observability](../../architecture/observability/README.md).

## Integrations

Modules, features and external services this feature depends on or affects.

## Acceptance criteria

The observable behaviors that prove the feature works. Each one can be checked in a test or in game.

- [ ] When a player does X, Y happens.

## Failures and recovery

What happens when an operation cannot be completed: a player disconnects halfway, a server restarts, the database or Redis does not answer. For each case, what the player sees and how the system recovers.

## Known limitations

What the feature does not do yet, or does on purpose in a way that may surprise someone.
