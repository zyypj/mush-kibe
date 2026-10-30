[English](README.md) | **Português**

# Motor de jogo

A parte da plataforma que roda as partidas dentro de cada servidor de jogo. Cada servidor roda várias partidas do mesmo jogo ao mesmo tempo, então a principal tarefa do motor é mantê-las isoladas e com ciclos de vida previsíveis.

Código: `minigames/engine/src/main/java/br/com/muxe/minigame/engine`.

## Conceitos

| Conceito | Classe | O que é |
|---|---|---|
| Modo de jogo | `GameMode` | As regras de uma variante de um jogo, como BedWars solo: id, nome de exibição, mínimo e máximo de jogadores e jogadores por time (`team-size`, `1` por padrão). Carregado do `config.yml` do jogo pelo `GameModeLoader`. |
| Contagem para iniciar | `StartCountdown` | A contagem antes de uma partida, igual em todos os jogos e modos. Veja abaixo. |
| Instância de arena | `ArenaInstance` | Uma cópia jogável de um modelo de mapa, usada por uma partida de cada vez. |
| Pool de arenas | `ArenaPool` | As instâncias de um servidor e quais estão disponíveis. |
| Partida | `Match` | Um jogo em andamento: seu modo, arena, participantes e estado. |
| Serviço de partidas | `MatchService` | Cria partidas, adiciona e remove jogadores e move as partidas pelo ciclo de vida. |
| Escopo de tarefas | `MatchTaskScope` | Tarefas do Bukkit que pertencem a uma partida e são todas canceladas quando ela fecha. |
| Motor | `MinigameEngine` | Guarda os modos, partidas e arenas de um jogo e registra suas métricas. |

## Ciclo de vida da partida

```text
WAITING → STARTING → RUNNING → ENDING → CLOSED
   ↑          │
   └──────────┘   (jogadores saíram durante a contagem)

WAITING, STARTING, RUNNING, ENDING → CANCELLED → CLOSED
```

| Estado | O que acontece |
|---|---|
| `WAITING` | Aceita jogadores. Atingir o mínimo do modo leva a `STARTING`. |
| `STARTING` | Contagem regressiva. Ainda aceita jogadores até lotar. Cair abaixo do mínimo volta para `WAITING`. |
| `RUNNING` | O jogo está sendo jogado. Nenhum jogador novo entra. |
| `ENDING` | O vencedor é decidido, os resultados são salvos, os jogadores veem o resultado. |
| `CANCELLED` | Interrompida por uma falha ou um desligamento. |
| `CLOSED` | Os jogadores são liberados, as tarefas canceladas, a arena liberada. A partida é esquecida. |

- Os estados só mudam pelas transições acima; qualquer outra lança `IllegalTransitionException`.
- **Uma partida por jogador.** `MatchService.join` recusa um jogador que já está em uma partida (`match.already_in_match`). Outras recusas: `match.not_found`, `match.not_joinable`, `match.full`.
- O `MatchService` é usado só a partir da thread principal.

## Ciclo de vida da arena

```text
AVAILABLE → RESERVED → IN_USE → RESETTING → AVAILABLE
                                    │
                                    └─→ DISABLED → (enable) → RESETTING
```

- Uma partida reserva uma instância disponível quando é criada e a libera quando fecha.
- Depois de uma partida, a instância é resetada a partir do seu modelo antes de ser usada de novo.
- Um reset que falha é tentado de novo com um atraso crescente (`nextRetryDelay`: o atraso base dobra a cada falha consecutiva, limitado a 5 minutos).
- A instância é **desativada** quando a falha é permanente (um erro de configuração ou um modelo corrompido) ou depois de `maxFailures` falhas consecutivas (5 por padrão). Ela fica fora de uso até a equipe corrigir e reativá-la.
- Um reset bem-sucedido zera a contagem de falhas.

## Contagem para iniciar

A `StartCountdown` segue as mesmas regras em todos os jogos e modos:

| Jogadores | O que acontece |
|---|---|
| Abaixo do mínimo | A sala aguarda. |
| O mínimo é atingido | Começa uma contagem de 30 segundos. |
| A sala lota | A contagem cai para 10 segundos. Ela nunca volta a subir. |
| Um jogador sai e a sala fica abaixo do mínimo | A contagem é cancelada e a sala volta a aguardar. |

- Os modos em que o mínimo é igual ao máximo (1v1, 2v2, 3v3, 4v4, 4s) estão lotados ao atingir o mínimo, então contam 10 segundos.
- `players(quantidade)` informa `STARTED`, `SHORTENED`, `CANCELLED` ou `NONE`.
- `tick()`, chamado uma vez por segundo, informa os segundos restantes e quando a contagem termina.

## Setup de arenas

Cada jogo descreve o que uma arena precisa num `SetupSchema`, e a engine dá a todos os jogos o mesmo comando `/arena`, os mesmos hologramas e a mesma checklist.

| Classe | O que é |
|---|---|
| `SetupSchema` | Os modos do jogo, a regra de times de cada modo (`TeamRule`: times fixos ou uma quantidade de cores a escolher) e os passos de cada modo. Os métodos padrão leem o modo e o nome de exibição do nome da arena: `1v1_slip` é o modo `1v1`, mapa `Slip`. |
| `SetupStep` | Uma coisa a marcar: id, rótulo, tipo, se repete por time, mínimo e máximo, e as linhas do holograma (`{team}`, `{index}`). |
| `StepKind` | `LOCATION` (onde o jogador está, centralizado, com a direção arredondada para 45°), `CENTER` (o bloco sob o jogador), `BLOCK` (o bloco para onde o jogador olha), `HEIGHT` (o Y do jogador) e `REGION` (dois cantos). |
| `ArenaDraft` | A arena em configuração: modo, nome de exibição, times, pontos e alturas. |
| `SetupChecklist` | O que está feito e o que falta. Uma arena está pronta quando os times estão completos e todo passo obrigatório tem o seu mínimo. |
| `ArenaFiles` | Lê e grava `arenas/<nome>.yml` em YAML expandido, com `ready: true` quando não falta nada. |
| `SetupService` | Abre os mundos de setup, aplica os comandos, mostra os hologramas e a action bar, e salva. |
| `TeamColor` | As nove cores de time: Azul `&9`, Amarelo `&e`, Ciano `&b`, Cinza `&8`, Vermelho `&c`, Rosa `&d`, Verde `&a`, Branco `&f`, Laranja `&6`, cada uma com a cor da lã e do couro. |

`/arena criar <nome> [modo]` abre uma arena:

1. Se `maps/<nome>.mxw` existe, o mapa é carregado dele.
2. Senão, se a pasta do servidor tem uma pasta de mundo chamada `<nome>`, ela é convertida para `.mxw` na hora e carregada.
3. Senão, um mundo vazio é criado, e o jogador chega em `0 60 0` para construir ou colar o mapa.

O mundo de setup é um mundo `muxe_setup_<nome>`, editado no criativo e voando. Vários membros da staff podem editá-lo juntos.

| Comando | O que faz |
|---|---|
| `/arena` ou `/arena ajuda` | A checklist, com uma linha clicável por passo e time: `✔` feito, `✘` faltando, `○` opcional. `[Definir]`, `[+]` e `[-]` executam o comando, e o hover diz como se posicionar. |
| `/arena definir <passo> [time]` | Marca o passo na posição do jogador. Um ponto único é substituído, uma lista cresce até o máximo, e uma região recebe dois cantos e recomeça no terceiro. |
| `/arena remover <passo> [time]` | Remove o ponto mais perto do jogador, ou o passo inteiro quando ele tem um ponto só. |
| `/arena time <adicionar\|remover> <cor>` | Escolhe os times nos modos em que eles não são fixos. Remover um time remove os pontos dele. |
| `/arena nome <nome>` | Muda o nome de exibição do mapa. |
| `/arena salvar` | Salva todos os chunks do mundo de setup em `maps/<nome>.mxw` e os pontos em `arenas/<nome>.yml`. |
| `/arena sair` | Sai do setup e volta para onde o jogador estava. O mundo fecha quando o último editor sai; as mudanças no mapa que não foram salvas se perdem. |
| `/arena listar` | Lista as arenas, prontas ou incompletas, com um botão para editar cada uma. |

- Cada ponto mostra um holograma para os editores, como `SPAWN` com o nome do time, ou `FERRO #2`.
- A action bar mostra o próximo item que falta e quantos ainda faltam, ou que está tudo pronto.
- Os pontos são gravados no arquivo da arena a cada mudança.
- Permissão: `muxe.minigame.setup`.
- As mensagens vêm do `minigame-setup.yml` dentro da engine, em `messages`. Um jogo sobrescreve qualquer uma delas no próprio `config.yml`.

## Mundos e mapas

As partidas nunca usam pastas de mundo. Veja a [ADR 0009](../../decisions/0009-own-map-format-and-match-cells.pt-BR.md).

| Classe | Pacote | O que faz |
|---|---|---|
| `MapTemplate` | `world.map` | Um mapa na memória: os chunks dele, cada um comprimido. `chunk(x, z)` descomprime um. |
| `MapFiles` | `world.map` | Lê e grava arquivos `.mxw`. |
| `AnvilImporter` | `world.anvil` | Transforma uma pasta de mundo (`region/*.mca`) num `MapTemplate`, sem servidor. As entidades ficam de fora. |
| `EditableMap` | `world.map` | Um mapa em edição no setup. Recebe cada chunk salvo, e o `snapshot()` devolve o mapa a gravar. |
| `MatchCells` | `world.cell` | Divide o mundo das partidas em células e serve a cada partida o mapa dela em volta do centro da célula. Os chunks que a partida altera ficam guardados até o `release`. |
| `Cell` | `world.cell` | A célula de uma partida. Converte coordenadas do mapa para o mundo e de volta. |
| `MuxeWorlds` | `world.server` | Cria um mundo vazio chamado `muxe_<nome>` cujos chunks vêm de uma `ChunkSource`. Também limpa uma célula, destrói um mundo e apaga pastas `muxe_` que sobraram. |

- Um mundo criado pelo `MuxeWorlds` nunca grava arquivos de região, não mantém os chunks do spawn carregados e tem o salvamento automático desligado.
- Os padrões dele: horário fixo ao meio-dia, sem chuva, sem mobs nascendo, sem ticks aleatórios, sem fogo se espalhando e sem mobs destruindo blocos.
- Uma partida pega uma célula com `MatchCells.allocate(mapa)`.
- Quando a partida acaba, `MuxeWorlds.clear(mundo, célula)` descarrega os chunks da célula sem salvar, e `MatchCells.release(célula)` a libera.
- O `MatchCells` recusa um mapa que não cabe na célula com a margem.
- Os chunks são montados com as classes do servidor, então o `minigames:engine` usa o `muxe.server-internals`.

## Isolamento

- Todo evento envolvendo um jogador, entidade, bloco ou mundo precisa ser roteado para a partida dona dele; interações entre partidas são canceladas.
- Chat, tab list e visibilidade ficam restritos à partida.
- Toda tarefa agendada passa pelo `MatchTaskScope` da partida, então nada continua rodando depois que uma partida fecha.
- Um erro na lógica de uma partida cancela só aquela partida.

Todas as partidas de um servidor compartilham o mesmo processo e a mesma thread principal, então problemas fatais (falta de memória, laço infinito, chamada bloqueante) afetam todas as partidas dele. Por isso cada jogo roda em seus próprios servidores. Veja [Rede](../network/README.pt-BR.md).

## Resultados

O fim de uma partida será salvo em **uma única transação do PostgreSQL** que registra o resultado com o id único da partida, concede as recompensas críticas e marca a partida como finalizada. Salvar o mesmo id de partida duas vezes não tem efeito, e as recompensas só são mostradas depois do commit. Isso faz parte do trabalho de gameplay que ainda não foi implementado.

## Métricas

`muxe_matches_active{game}`, `muxe_arenas_available{game}` e `muxe_arenas_disabled{game}`. Veja [Observabilidade](../observability/README.pt-BR.md#minigames).

## Estado atual

Implementado e testado: modos, ciclos de vida de partida e de arena, a regra de uma partida por jogador, o pool de arenas, os escopos de tarefas e as métricas. Também implementado: o formato de mapa `.mxw`, a importação de pastas de mundo, o carregador de chunks em memória e as células das partidas (testes unitários e uma execução no PandaSpigot 226). Ainda não implementado: roteamento de eventos por partida, gameplay, resultados e recompensas.
