**English** | [Português](README.pt-BR.md)

# Documentation

Everything about Muxe is documented here, in English and Portuguese. The rules for documentation are in [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

## Start here

- [Development](development/README.md): requirements, modules, dependency rules, build commands and how to run a local network.
- [Conventions](development/conventions/README.md): Java style, package organization, naming, configuration and messages.
- [Core API](development/core-api/README.md): what Muxe-Core offers to plugins, including titles, action bars and particle text.
- [Process](process/README.md): how work is planned and delivered, and what "ready" and "done" mean.
- [Legal and clean-room policy](legal/README.md): what may and may not enter this repository.

## Architecture

- [Overview](architecture/overview/README.md): principles, layers, CQRS and how a request flows through the code.
- [Network](architecture/network/README.md): which servers exist and how players move between them.
- [Messaging](architecture/messaging/README.md): the packet system over Redis, envelopes, signatures, idempotency and request/response.
- [Persistence](architecture/persistence/README.md): PostgreSQL, transactions, migrations and the Redis cache.
- [Observability](architecture/observability/README.md): every metric the network exports.
- [Security](architecture/security/README.md): what is exposed and how servers, services and secrets are protected.
- [Game engine](architecture/game-engine/README.md): matches, arenas and their lifecycles.

## Decisions

- [Architecture decision records](decisions/README.md): why the main technical choices were made.

## Features

- [Accounts](features/accounts/README.md): one account per player, created on the first login.
- [Authentication](features/authentication/README.md): premium auto-login, anti-bot challenge, `/registrar`, `/logar` and 24-hour sessions.
- [Network routing](features/network-routing/README.md): how the proxy picks servers, `/lobby` and the access gate.
- [Network administration](features/network-admin/README.md): `/muxe` commands for staff.
- [Lobby](features/lobby/README.md): spawn, flight, protection, scoreboard, boss bar and `/jogar`.
- [Hotbar](features/hotbar/README.md): the lobby hotbar from Muxe-Core, with the game selector, profile, lobby selector and player visibility.
- [NPCs and holograms](features/npcs-and-holograms/README.md): NPCs and text holograms placed by command and shared by the server group.
- [Social media heads](features/social-heads/README.md): a player head that cycles through the network's social media and sends the link on click, shared by the server group.
- [Shop](features/shop/README.md): `/loja`, the checkout menus, cash registers and the network promotion.
- [SkyWars](features/skywars/README.md): plugin skeleton and modes.
- [BedWars](features/bedwars/README.md): plugin skeleton and modes.

## Documenting a new feature

1. Copy [`features/_template/`](features/_template/) to `features/<feature>/`, using the same name as the commit scope.
2. Fill in both `README.md` and `README.pt-BR.md`.
3. Add the feature to the list above, in both this file and [README.pt-BR.md](README.pt-BR.md).
