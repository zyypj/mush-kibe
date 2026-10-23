[English](README.md) | **Português**

# Muxe

Muxe é uma rede de minigames de Minecraft open-source para o **Minecraft 1.8**: um proxy Velocity, um servidor de autenticação, um lobby e servidores de minigames, todos construídos como plugins em um monorepo Gradle.

É um projeto independente, inspirado nas redes clássicas brasileiras de minigames. Não contém código, assets, mapas nem marcas de nenhuma outra rede. Veja a [Política legal e de sala limpa](docs/legal/README.pt-BR.md).

## Destaques

- **Um monorepo, vários plugins.** Um core compartilhado mais um plugin por servidor, com regras de dependência garantidas pelo build.
- **Pacotes entre servidores pelo Redis.** Envelopes binários assinados com HMAC, com requisição/resposta, idempotência (local, durável e exclusiva), expiração e uma métrica para cada etapa. Veja [Mensageria](docs/architecture/messaging/README.pt-BR.md).
- **Camada de aplicação CQRS.** Commands mudam estado, queries leem, e ambos passam por buses com middlewares e métricas. Veja a [Visão geral da arquitetura](docs/architecture/overview/README.pt-BR.md).
- **PostgreSQL como fonte da verdade.** HikariCP, migrações Flyway versionadas por módulo e transações que nunca rodam na thread principal. Veja [Persistência](docs/architecture/persistence/README.pt-BR.md).
- **Observabilidade embutida.** Métricas Prometheus para pacotes, requisições, commands, transações, ticks e a JVM. Veja [Observabilidade](docs/architecture/observability/README.pt-BR.md).
- **Documentado em inglês e português**, para pessoas e para agentes de IA.

## Stack

| Peça | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Servidores de jogo | Spigot API 1.8.8, rodando em um fork da 1.8.8 que suporta Java 25 (como o PandaSpigot) |
| Proxy | Velocity 4.3 |
| Banco de dados | PostgreSQL 18 |
| Mensageria e cache compartilhado | Redis 8 |
| Build | Gradle 9 com convention plugins em Kotlin DSL |
| Testes | JUnit 6, AssertJ, Testcontainers |

## Início rápido

```bash
cp .env.example .env
docker compose up -d
./gradlew build
```

No Windows, use `gradlew.bat build`. O build compila todos os módulos, roda todos os testes (incluindo testes de integração contra containers reais de PostgreSQL e Redis) e monta os plugins de cada servidor em `build/servers/`. Veja [Desenvolvimento](docs/development/README.pt-BR.md) para rodar uma rede local.

## Estado atual

Início do desenvolvimento. A plataforma (core, mensageria, persistência, métricas, roteamento no proxy, autenticação e o básico do lobby) funciona e está testada. Os minigames têm o motor e o esqueleto dos plugins; a jogabilidade ainda não foi implementada.

## Documentação

- [docs/](docs/README.pt-BR.md): arquitetura, guia de desenvolvimento, processo, decisões e features.
- [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md): branches, commits, documentação e pull requests.
- [AGENTS.pt-BR.md](AGENTS.pt-BR.md): regras para agentes de IA que trabalham neste repositório.
- [SECURITY.pt-BR.md](SECURITY.pt-BR.md): como reportar uma vulnerabilidade.

## Licença

[MIT](LICENSE). Veja também a [política legal e de clean-room](docs/legal/README.pt-BR.md).
