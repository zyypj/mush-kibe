[English](README.md) | **Português**

# BedWars

BedWars para a rede Muxe. Fica no `minigames:bedwars` (`Muxe-BedWars`) e roda nos servidores do grupo `bedwars`.

## Histórias de usuário

- Como jogador, quero proteger a cama do meu time e destruir a dos outros, para que o meu time seja o último de pé.

## Como funciona

Hoje o plugin carrega os seus modos de jogo da configuração, cria o [motor de jogo](../../architecture/game-engine/README.pt-BR.md) para o BedWars e registra as suas métricas. A jogabilidade (camas, geradores, loja, times, vitória) ainda não foi implementada.

## Comandos

Nenhum ainda.

## Permissões

Nenhuma ainda.

## Configuração

`plugins/Muxe-BedWars/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `modes.<id>.display-name` | `BedWars Solo`, `BedWars Quartetos` | Nome mostrado aos jogadores. |
| `modes.<id>.min-players` | `4` (solo), `8` (quartetos) | Jogadores necessários para iniciar a contagem regressiva. |
| `modes.<id>.max-players` | `8` (solo), `16` (quartetos) | Jogadores por partida. |
| `modes.<id>.countdown` | `20s`, `30s` | Contagem regressiva antes de a partida começar. |

## Pacotes

Nenhum ainda.

## Dados

Nenhum ainda.

## Métricas

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrações

- [Lobby](../lobby/README.pt-BR.md): `/jogar bedwars`.
- [Motor de jogo](../../architecture/game-engine/README.pt-BR.md).

## Critérios de aceitação

- [x] Os modos são carregados da configuração (testes do motor).
- [ ] Os critérios de jogabilidade serão escritos junto com as histórias de jogabilidade.

## Falhas e recuperação

Se o Muxe-Core não estiver habilitado, o plugin se desabilita.

## Limitações conhecidas

A jogabilidade não foi implementada. As políticas dos modos (reconexão, sair antes do início, compensação em desligamento e em crash) serão documentadas aqui quando existirem.
