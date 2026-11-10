[English](README.md) | **Português**

# Motor de jogo

A parte da plataforma que roda as partidas dentro de cada servidor de jogo. Cada servidor roda várias partidas do mesmo jogo ao mesmo tempo, então a principal tarefa do motor é mantê-las isoladas e com ciclos de vida previsíveis.

Código: `minigames/engine/src/main/java/br/com/muxe/minigame/engine`.

## Conceitos

| Conceito | Classe | O que é |
|---|---|---|
| Modo de jogo | `GameMode` | As regras de uma variante de um jogo, como BedWars solo: id, nome de exibição, mínimo e máximo de jogadores e jogadores por time (`team-size`, `1` por padrão). Carregado do `config.yml` do jogo pelo `GameModeLoader`. |
| Contagem para iniciar | `StartCountdown` | A contagem antes de uma partida, igual em todos os jogos e modos. Veja abaixo. |
| Motor | `MinigameEngine` | Guarda os modos de um jogo e registra as métricas dele, que leem o que o jogo informa pelo `track(activeMatches, readyArenas)`. |

## Ciclo de vida da partida

O motor dá as peças de baixo nível que todo jogo precisa (modos, a contagem para iniciar, o setup e o armazenamento das arenas, as células das partidas, a divisão dos times, os códigos de sala). A [API de minigames](../minigame-api/README.pt-BR.md) roda as partidas com elas (`WAITING → STARTING → PLAYING → ENDING`): entrada e saída, times, mortes, renascimento, espectadores, o placar, títulos, matchmaking e a fiação do servidor. Os jogos são feitos sobre a API de minigames, não direto sobre o motor.

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

## Vida abaixo do nome

`HealthDisplay` mostra a vida dos jogadores vivos embaixo do nome, como `20 ❤`, e como um número amarelo à direita do nome no tab, em todos os minigames. O jogo chama `show(placar, vivos)` para o placar de cada jogador algumas vezes por segundo (o BedWars a cada 5 ticks), com os nomes dos vivos e a vida arredondada para cima (`points(vida)`). Quem sai da lista perde as duas na hora. O texto (o coração) vem das mensagens do jogo.

## Subida de nível

`LevelUps` parabeniza o jogador que chega a um nível novo, em todos os minigames. O jogo cria a classe com a mensagem dele (com `{level}`) e o emblema de cada nível, e chama `announce(jogador, nível)` sempre que mostra o nível. O primeiro nível visto de um jogador só fica guardado; um nível maior manda a mensagem com o emblema do nível novo, uma vez só mesmo quando pula vários níveis. `forget(jogador)` esquece o jogador quando ele sai.

## Idiomas

- O `TeamColor` tem um nome em português e outro em inglês. O `label()` é um `Text`, e o `displayName(Language)` monta o nome no idioma pedido. As letras dos times não mudam.
- O `GameModeLoader.modes(texts)` lê os modos a partir do `LocalizedConfig` do jogo. O `ModeNames.from(texts)` dá o nome de cada modo em cada idioma; o conector e o item de jogar de novo usam esses nomes.
- O `LevelUps(messages.text("level-up"), badges, languages)` parabeniza cada jogador no idioma dele.
- O setup de arenas (`minigame-setup.yml`, `/arena`) é só da staff e continua em português.

## Setup de arenas

Cada jogo descreve o que uma arena precisa num `SetupSchema`, e a engine dá a todos os jogos o mesmo comando `/arena`, os mesmos hologramas e a mesma checklist.

| Classe | O que é |
|---|---|
| `SetupSchema` | Os modos do jogo, a regra de times de cada modo (`TeamRule`: times fixos ou uma quantidade de cores a escolher) e os passos de cada modo. Os métodos padrão leem o modo e o nome de exibição do nome da arena: `1v1_slip` é o modo `1v1`, mapa `Slip`. `teamAnchor(modo)` diz qual passo de time posiciona os outros (BedWars: o spawn), e `linkedBlocks(modo)` os passos de bloco achados sozinhos em volta dele (`LinkedBlock`: passo, materiais, raio; BedWars: a cama a até 20 blocos). |
| `SetupStep` | Uma coisa a marcar: id, rótulo, tipo, se repete por time, mínimo e máximo, e as linhas do holograma (`{team}`, `{index}`). |
| `StepKind` | `LOCATION` (onde o jogador está, centralizado, com a direção arredondada para 45°), `CENTER` (o bloco sob o jogador), `BLOCK` (o bloco para onde o jogador olha), `HEIGHT` (o Y do jogador) e `REGION` (Pos 1 e Pos 2, dois cantos opostos guardados como `<passo>.pos1` e `<passo>.pos2`). |
| `ArenaDraft` | A arena em configuração: modo, nome de exibição, times, pontos e alturas. |
| `SetupChecklist` | O que está feito e o que falta. Uma arena está pronta quando os times estão completos e todo passo obrigatório tem o seu mínimo. |
| `ArenaFiles` | Gera e lê a definição da arena em YAML expandido, com `ready: true` quando não falta nada, e lê e grava `arenas/<nome>.yml`. |
| `ArenaStore` | Onde as arenas e os mapas ficam. `FileArenaStore` guarda `arenas/<nome>.yml` e `maps/<nome>.mxw` na pasta do plugin; `DatabaseArenaStore(game, commands, queries)` guarda os dois no PostgreSQL (`core.arenas`, pelo `SaveArenaCommand`, `FindArenaQuery` e `ListArenasQuery`), então todo servidor do jogo lê as mesmas arenas. `saveDefinition` salva os pontos e mantém o mapa guardado. |
| `ArenaSavedPacket` | `minigame.arena_saved` (jogo, nome): enviado depois do `/arena salvar` para os servidores de jogo recarregarem aquela arena. |
| `TeamPicker` | Escolhe o time com o ponto âncora mais perto de uma posição, e o bloco livre mais perto que nenhum outro time usa (as duas metades de uma cama contam como usadas). |
| `SetupService` | Abre os mundos de setup, aplica os comandos, mostra os hologramas e a action bar, e salva. |
| `TeamColor` | As nove cores de time: Azul `&9`, Amarelo `&e`, Ciano `&b`, Cinza `&8`, Vermelho `&c`, Rosa `&d`, Verde `&a`, Branco `&f`, Laranja `&6`, cada uma com a cor da lã e do couro. |

`/arena criar <nome> [modo]` abre uma arena:

1. Se o armazenamento tem um mapa com blocos para `<nome>`, ele é carregado dele.
2. Senão (sem mapa, ou com um mapa salvo vazio), se a pasta do servidor tem uma pasta de mundo chamada `<nome>`, ela é convertida para o formato de mapa da Muxe na hora, guardada e carregada.
3. Senão, um mundo vazio é criado, e o jogador chega em `0 60 0` para construir ou colar o mapa.

O mundo de setup é um mundo `muxe_setup_<nome>`, editado no criativo e voando. Vários membros da staff podem editá-lo juntos. `SetupService.onEnter` e `onLeave` rodam quando um jogador começa e para de editar; um jogo pode usá-los, por exemplo, para colocar a staff no modo construção num lobby. Cair abaixo do Y 0 leva o editor de volta ao início do mundo de setup em vez de tirá-lo dele, e o chat fica dentro de cada setup: os editores só ouvem os editores da mesma arena, e quem está fora de um setup não os ouve.

| Comando | O que faz |
|---|---|
| `/arena` ou `/arena ajuda` | A checklist, com uma linha clicável por passo e time: `✔` feito, `✘` faltando, `○` opcional. `[Definir]`, `[+]` e `[-]` executam o comando, e o hover diz como se posicionar. Quando o schema tem uma âncora de time, cada outro passo de time vira uma linha só, com quantos times estão feitos, e os botões vão para o time mais perto. A checklist é enviada de novo a cada mudança. |
| `/arena definir <passo> [time]` | Marca o passo na posição do jogador. Um ponto único é substituído e uma lista cresce até o máximo. Sem o time, um passo de time vai para o time com a âncora mais perto. Marcar a âncora de um time também marca os blocos ligados a ela, como a cama mais perto. |
| `/arena definir <região> <1\|2>` | Marca a Pos 1 ou a Pos 2 de uma região; a ajuda mostra os botões `[Pos 1]` e `[Pos 2]`. Sem o número, marca o primeiro canto que falta. |
| `/arena remover <passo> [time]` | Remove o ponto mais perto do jogador, ou o passo inteiro quando ele tem um ponto só. Sem o time, usa o time mais perto, como o `definir`. |
| `/arena time <adicionar\|remover> <cor>` | Escolhe os times nos modos em que eles não são fixos. Remover um time remove os pontos dele. |
| `/arena nome <nome>` | Muda o nome de exibição do mapa. |
| `/arena salvar` | Salva todos os chunks do mundo de setup e os pontos no armazenamento de arenas. Os pontos também são salvos conforme são marcados, mantendo o mapa guardado. |
| `/arena sair` | Sai do setup e volta para onde o jogador estava. O mundo fecha quando o último editor sai; as mudanças no mapa que não foram salvas se perdem. |
| `/arena listar` | Lista as arenas, prontas ou incompletas, com um botão para editar cada uma. |
| `/arena clonar <origem> <nome> [modo]` (`duplicar`) | Copia uma arena salva, com mapa e pontos, para um novo nome de mapa (`/arena clonar solo_majestic duplas_majestic`). Só o modo muda, e com ele a quantidade de jogadores; o nome de exibição continua o mesmo. O modo é o informado, senão o prefixo do novo nome, senão o da origem. Um modo com times fixos fica só com os times dele e os pontos deles; um modo com times escolhidos fica com os primeiros até a quantidade dele. Salva a cópia pronta ou com o que falta (`/arena criar <nome>` completa) e avisa os servidores de jogo com `minigame.arena_saved`. Copia a versão salva, não mudanças não salvas de uma arena aberta no setup, e recusa um nome que já existe. |
| `/arena apagar <nome> [confirmar]` | Apaga uma arena e o mapa dela do armazenamento (`DeleteArenaCommand`). Sem o `confirmar` ele só pergunta, com um botão; uma arena aberta no setup não pode ser apagada. Os servidores de jogo são avisados com o `minigame.arena_saved` e a tiram. |

- Cada ponto mostra um holograma para os editores, como `SPAWN` com o nome do time, ou `FERRO #2`.
- A action bar mostra o próximo item que falta e quantos ainda faltam, ou que está tudo pronto.
- Os pontos são gravados no arquivo da arena a cada mudança.
- Permissão: `muxe.minigame.setup`.
- As mensagens vêm do `minigame-setup.yml` dentro da engine, em `messages`. Um jogo sobrescreve qualquer uma delas no próprio `config.yml`.

## Salas

Peças que a [API de minigames](../minigame-api/README.pt-BR.md) usa para rodar as salas:

| Classe | O que faz |
|---|---|
| `RoomCodes` | Códigos de sala como os da referência: 6 caracteres, dígitos e letras minúsculas, começando com um dígito (`2v7bss`), nunca repetidos no servidor. |
| `TeamAssigner` | Divide os jogadores de uma sala nos times quando a partida começa. As parties (`PartyGroups`) ficam juntas, as maiores primeiro; depois os jogadores com um `TeamColor` preferido (a cor preferida do jogo, opcional) vão para esse time quando a arena o tem e ele tem vagas, agrupados por cor: quando mais jogadores o querem do que há vagas, um sorteio com o `RandomGenerator` injetado decide quem fica com elas e os outros seguem adiante; os demais vão para o time com menos jogadores, na ordem de entrada. Uma party fica com a cor que a maioria dos membros prefere (a primeira num empate) se esse time ainda estiver vazio, e nunca é dividida. Uma party maior que um time transborda para o próximo. Sem preferências o resultado é o da ordem de entrada. |
| `PartyGroups` | O gancho do sistema de parties. `PartyGroups.NONE` até as parties existirem. |
| `CountdownTitles` | Os títulos da contagem para iniciar, por segundo, lidos de `countdown.titles`. |
| `MatchWorld` | O mundo das partidas de um jogo: um mundo `muxe_<jogo>` com as células, mais uma tarefa que descarrega a cada 5 segundos os chunks que ninguém está vendo (64 no máximo), guardando o que as partidas mudaram. |
| `ArenaCatalog` | As arenas que um servidor pode jogar: toda arena do seu `ArenaStore` marcada como pronta, com um mapa que cabe numa célula. `reload(name)` pega uma arena salva no setup. |

## Mundos e mapas

As partidas nunca usam pastas de mundo. Veja a [ADR 0009](../../decisions/0009-own-map-format-and-match-cells.pt-BR.md).

| Classe | Pacote | O que faz |
|---|---|---|
| `MapTemplate` | `world.map` | Um mapa na memória: os chunks dele, cada um comprimido. `chunk(x, z)` descomprime um. |
| `MapFiles` | `world.map` | Lê e grava arquivos `.mxw`. |
| `AnvilImporter` | `world.anvil` | Transforma uma pasta de mundo (`region/*.mca`) num `MapTemplate`, sem servidor. As entidades ficam de fora. |
| `EditableMap` | `world.map` | Um mapa em edição no setup. Recebe cada chunk salvo, e o `snapshot()` devolve o mapa a gravar. |
| `MatchCells` | `world.cell` | Divide o mundo das partidas em células e serve a cada partida o mapa dela em volta do centro da célula. Os chunks que a partida altera ficam guardados até o `release`: o servidor entrega uma cópia, que já é servida como está e é comprimida (nível rápido) numa thread de fundo, então descarregar chunks nunca comprime na thread principal. |
| `Cell` | `world.cell` | A célula de uma partida. Converte coordenadas do mapa para o mundo e de volta. |
| `BlockBox`, `RegionEraser` | `world.cell`, `world.server` | Esvazia uma caixa de blocos de uma vez: zera os arrays de blocos das seções de chunk que ela cobre, tira as tile entities de dentro, recalcula a luz de cada chunk uma vez e reenvia cada chunk alterado como um pacote só aos jogadores que o veem. Usado para remover as áreas de espera. |
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
- As partidas rodam pelas tarefas compartilhadas do `MatchService` da API de minigames (a cada segundo e a cada 5 ticks), então nada sobrevive a uma partida.
- Eventos assíncronos, como o chat, leem retratos imutáveis montados na thread principal, nunca o estado vivo da partida.
- Um erro na lógica de uma partida cancela só aquela partida.

Todas as partidas de um servidor compartilham o mesmo processo e a mesma thread principal, então problemas fatais (falta de memória, laço infinito, chamada bloqueante) afetam todas as partidas dele. Por isso cada jogo roda em seus próprios servidores. Veja [Rede](../network/README.pt-BR.md).

## Resultados

Um jogo salva o fim de uma partida em **uma única transação do PostgreSQL** que primeiro reivindica o id único da partida: salvar o mesmo id duas vezes não tem efeito. O BedWars dá a cada partida um id aleatório quando ela começa e guarda os ids reivindicados em `bedwars.recorded_matches`.

## Métricas

`muxe_matches_active{game}` e `muxe_arenas_available{game}`, lidas do que o jogo informa com o `MinigameEngine.track`. Veja [Observabilidade](../observability/README.pt-BR.md#minigames).

## Estado atual

Implementado e testado: modos, ciclos de vida de partida e de arena, a regra de uma partida por jogador, o pool de arenas, os escopos de tarefas e as métricas. Também implementado: o formato de mapa `.mxw`, a importação de pastas de mundo, o carregador de chunks em memória e as células das partidas (testes unitários e uma execução no PandaSpigot 226). O ciclo de vida da partida, o roteamento de eventos por partida e as regras de jogo compartilhadas ficam na [API de minigames](../minigame-api/README.pt-BR.md); resultados e recompensas ainda são de cada jogo.
