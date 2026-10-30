[English](README.md) | **Português**

# SkyWars

SkyWars para a rede Muxe. Fica no `minigames:skywars` (`Muxe-SkyWars`) e roda nos servidores do grupo `skywars`.

## Histórias de usuário

- Como jogador, quero lutar contra outros jogadores em ilhas flutuantes até que só o meu time sobreviva.

## Como funciona

Hoje o plugin carrega os seus modos de jogo da configuração, cria o [motor de jogo](../../architecture/game-engine/README.pt-BR.md) para o SkyWars e registra as suas métricas. A jogabilidade (ilhas, baús, kits, abates, vitória) ainda não foi implementada.

## Comandos

Nenhum ainda.

## Permissões

Nenhuma ainda.

## Configuração

`plugins/Muxe-SkyWars/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `modes.<id>.display-name` | `SkyWars Solo`, `SkyWars Duplas` | Nome mostrado aos jogadores. |
| `modes.<id>.min-players` | `2` (solo), `4` (duplas) | Jogadores necessários para iniciar a contagem regressiva. |
| `modes.<id>.max-players` | `12` (solo), `24` (duplas) | Jogadores por partida. |
| `modes.<id>.team-size` | `1` (solo), `2` (duplas) | Jogadores por time. |

## Pacotes

Nenhum ainda.

## Dados

Nenhum ainda.

## Métricas

`muxe_matches_active{game="skywars"}`, `muxe_arenas_available{game="skywars"}`, `muxe_arenas_disabled{game="skywars"}`.

## Integrações

- [Lobby](../lobby/README.pt-BR.md): `/jogar skywars`.
- [Motor de jogo](../../architecture/game-engine/README.pt-BR.md).

## Critérios de aceitação

- [x] Os modos são carregados da configuração (testes do motor).
- [ ] Os critérios de jogabilidade serão escritos junto com as histórias de jogabilidade.

## Falhas e recuperação

Se o Muxe-Core não estiver habilitado, o plugin se desabilita.

## Limitações conhecidas

A jogabilidade não foi implementada. As políticas dos modos (reconexão, sair antes do início, compensação em desligamento e em crash) serão documentadas aqui quando existirem.
