[English](README.md) | **Português**

# BedWars

BedWars para a rede Muxe. Fica no `minigames:bedwars` (`Muxe-BedWars`) e roda nos servidores do grupo `bedwars`.

## Histórias de usuário

- Como jogador, quero proteger a cama do meu time e destruir a dos outros, para que o meu time seja o último de pé.

## Como funciona

O plugin carrega os nove modos, cria o [motor de jogo](../../architecture/game-engine/README.pt-BR.md) do BedWars e dá à staff o [setup de arenas](../../architecture/game-engine/README.pt-BR.md#setup-de-arenas). Partidas, camas, geradores, loja, melhorias e vitória estão sendo construídos.

### Modos

| Modo | Mínimo | Máximo | Por time | Times |
|---|---:|---:|---:|---|
| Solo | 6 | 8 | 1 | 8 das 9 cores, escolhidas no setup |
| Duplas | 8 | 16 | 2 | 8 das 9 cores, escolhidas no setup |
| Trios | 6 | 12 | 3 | Azul, Vermelho, Verde, Amarelo |
| Quartetos | 8 | 16 | 4 | Azul, Vermelho, Verde, Amarelo |
| 1v1 | 2 | 2 | 1 | Azul, Vermelho |
| 2v2 | 4 | 4 | 2 | Azul, Vermelho |
| 3v3 | 6 | 6 | 3 | Azul, Vermelho |
| 4v4 | 8 | 8 | 4 | Azul, Vermelho |
| 4s | 8 | 8 | 4 | Azul, Vermelho, num mapa com mais duas ilhas de recursos e NPCs |

### Setup de arenas

Uma arena se chama `<modo>_<mapa>`, como `1v1_slip` ou `quartetos_air_nation`. `/arena criar 1v1_slip` converte uma pasta de mundo chamada `1v1_slip` ou abre um mundo vazio.

| Passo | Por time | Tipo | Obrigatório |
|---|---|---|---|
| `espera`: spawn da espera | não | onde o jogador está | sim |
| `area-espera`: área da espera, removida quando a partida começa | não | dois cantos | sim |
| `espectador`: spawn dos espectadores | não | onde o jogador está | sim |
| `void`: altura do void | não | o Y do jogador | sim |
| `limite`: bloco mais alto que os jogadores podem colocar | não | o Y do jogador | não |
| `diamante`, `esmeralda`: geradores | não | o bloco sob o jogador | não, até 16 cada |
| `spawn` | sim | onde o jogador está | sim |
| `cama` | sim | o bloco para onde o jogador olha | sim |
| `loja`, `melhorias`: NPCs da loja e das melhorias | sim | onde o jogador está | sim |
| `ferro`, `ouro`: geradores da ilha | sim | o bloco sob o jogador | pelo menos 1, até 4 |
| `loja-extra`, `melhorias-extra`, `ferro-extra`, `ouro-extra` | não | como acima | só no 4s, opcionais |

## Comandos

| Comando | Permissão | Descrição |
|---|---|---|
| `/arena ...` | `muxe.minigame.setup` | Cria e configura arenas. Veja o [setup de arenas](../../architecture/game-engine/README.pt-BR.md#setup-de-arenas). |

## Permissões

| Permissão | Padrão | Descrição |
|---|---|---|
| `muxe.minigame.setup` | op | Permite o `/arena`. |

## Configuração

`plugins/Muxe-BedWars/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `modes.<id>.display-name` | `Solo`, `Duplas`, `1v1`... | Nome mostrado aos jogadores. |
| `modes.<id>.min-players` | veja a tabela acima | Jogadores necessários para iniciar a contagem. |
| `modes.<id>.max-players` | veja a tabela acima | Jogadores por partida. |
| `modes.<id>.team-size` | veja a tabela acima | Jogadores por time. |
| `modes.<id>.fixed-teams` | nenhum (Solo, Duplas) | Cores dos times nos modos em que eles são fixos. Sem ela, o setup escolhe `max-players / team-size` cores. |
| `modes.<id>.extra-islands` | `true` no 4s | Adiciona ao setup os passos de loja, melhorias e geradores extras. |
| `messages.setup-*` | as da engine | Sobrescreve qualquer mensagem do setup. |

## Pacotes

Nenhum ainda.

## Dados

| Arquivo | O que guarda |
|---|---|
| `plugins/Muxe-BedWars/maps/<arena>.mxw` | O mapa, no [formato de mapa da Muxe](../../decisions/0009-own-map-format-and-match-cells.pt-BR.md). Copiado para cada servidor de BedWars. |
| `plugins/Muxe-BedWars/arenas/<arena>.yml` | Modo, nome de exibição, times, pontos e alturas, com `ready: true` quando não falta nada. Copiado junto com o mapa. |

## Métricas

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrações

- [Lobby](../lobby/README.pt-BR.md): `/jogar bedwars`.
- [Motor de jogo](../../architecture/game-engine/README.pt-BR.md).

## Critérios de aceitação

- [x] Os modos são carregados da configuração (testes do motor).
- [x] Os nove modos têm os limites e os times da especificação (`BedWarsSchemaTest`).
- [x] Uma arena pode ser configurada no jogo, com uma checklist do que falta (testes do motor).
- [ ] Os critérios de jogabilidade serão escritos junto com as histórias de jogabilidade.

## Falhas e recuperação

Se o Muxe-Core não estiver habilitado, o plugin se desabilita.

## Limitações conhecidas

A jogabilidade não foi implementada. As políticas dos modos (reconexão, sair antes do início, compensação em desligamento e em crash) serão documentadas aqui quando existirem.
