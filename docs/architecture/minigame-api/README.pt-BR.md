[English](README.md) | **Português**

# API de minigames

A camada sobre a qual todo jogo é feito. O [motor de jogo](../game-engine/README.pt-BR.md) dá as peças de baixo nível (mundos, mapas, células, setup de arena, modos, a contagem). A API de minigames roda as partidas com elas: o ciclo de vida, entrada e saída, times, mortes, renascimento, espectadores, o placar, títulos, chat, matchmaking, a fiação do servidor e os [cosméticos](#cosméticos) compartilhados por todos os jogos. Um jogo declara a arena, as regras e os ganchos do que só ele faz.

Código: `minigames/api/src/main/java/br/com/muxe/minigame/api`. Módulo `minigames:api`, embutido no jar de cada jogo junto com o motor.

## Criando um jogo

Um jogo é feito destas peças:

| Peça | O que o jogo escreve |
|---|---|
| Schema da arena | Os passos do `/arena`, com `ArenaSchema.builder(modes)`. |
| Arena | A arena tipada (um `MatchArena`) lida do rascunho salvo com `ArenaView`, ou `StandardArena` quando os passos padrão bastam. |
| `Minigame<A, S>` | O id, as `MatchRules`, como ler uma arena e como abrir o estado de uma partida nova. |
| `MatchState` | O que acontece numa partida que só esse jogo faz, por ganchos. Todo gancho tem um padrão. |
| `GameHandlers<S>` | Opcional. Os eventos de uma partida em jogo: blocos, interações, explosões, dano, coleta de itens. |

```java
public final class SkyWarsGame implements Minigame<StandardArena, SkyWarsMatch> {

    public String id() {
        return "skywars";
    }

    public MatchRules rules() {
        return MatchRules.defaults("mxsw").breakingAnything();
    }

    public StandardArena arena(LoadedArena loaded) {
        return StandardArena.from(loaded);
    }

    public SkyWarsMatch open(MatchRuntime<SkyWarsMatch> runtime, StandardArena arena) {
        return new SkyWarsMatch(runtime, arena);
    }
}
```

```java
ArenaSchema schema = ArenaSchema.builder(modes)
    .waiting()
    .spectator()
    .voidHeight()
    .teamSpawn()
    .step(CHESTS)
    .fixedTeams(modesSection)
    .build();
```

O bootstrap liga o resto:

```java
MinigameBootstrap minigame = new MinigameBootstrap(plugin, core, "skywars", modes, messages, commands, config);
if (minigame.isLobby()) {
    minigame.setup(schema);
    return;
}
MatchServer<StandardArena, SkyWarsMatch> server = minigame.matches(new SkyWarsGame(), MatchSettings.from(config), MatchEvents.NONE);
server.listen(new SkyWarsHandlers());
```

## Schema da arena

`ArenaSchema` é o `SetupSchema` do motor, montado de forma declarativa:

| Builder | Adiciona |
|---|---|
| `waiting()` | `espera` (spawn da área de espera) e `area-espera` (a região apagada quando a partida começa). |
| `spectator()` | `espectador`, onde mortos e espectadores aparecem. |
| `voidHeight()` | `void`, a altura abaixo da qual o jogador morre. |
| `teamSpawn()` | `spawn` de cada time, também a âncora que posiciona os outros passos de time. |
| `step(...)` | Os passos do próprio jogo, na ordem da checklist. |
| `stepsFor(modos, ...)` | Passos que só alguns modos têm. |
| `linked(...)` | Blocos achados sozinhos em volta da âncora, como uma cama. |
| `fixedTeams(...)` | Os times fixos de um modo, à mão ou de `modes.<id>.fixed-teams`. A quantidade precisa bater com o modo. |

`ArenaView` lê o rascunho salvo com tipos: `point(passo)`, `point(passo, time)` (falha quando falta), `points(...)`, `height(passo)`, `optionalHeight(passo)` e `perTeam(leitor)`. `StandardArena` lê os passos padrão para um jogo que não precisa de mais nada.

## Ciclo de vida da partida

`WAITING → STARTING → PLAYING → ENDING`, depois a partida fecha e libera a célula. A API roda cada passo; o jogo só preenche os ganchos.

| Quando | O que a API faz | Ganchos |
|---|---|---|
| Um jogador entra | Volta ele ao sobrevivência, avisa `room-joined` na sala, teleporta para o spawn da espera, dá a hotbar da espera, o placar e o perfil de knockback do modo (tirado quando sai da sala). | `arrived` |
| O mínimo é atingido | Roda a [contagem de início](../game-engine/README.pt-BR.md#contagem-para-iniciar), com os títulos de `countdown.titles` (sem fade de entrada, 20 ticks na tela, 20 ticks de fade de saída), o som `countdown` e, nos mesmos segundos, `countdown` no chat (`countdown-one` no último segundo). | |
| A contagem acaba | Sorteia os times (cores favoritas de `Minigame.teamPreferences`), protege todos, coloca cada jogador no spawn do time, apaga a área de espera, avisa `match-started` e toca `match-start`. Times sem ninguém param de renascer e estão fora. | `starting`, `spawned`, `started` |
| A cada segundo | Conta o tempo, atualiza a contagem para renascer (`died-title` sobre `respawn-subtitle`, `respawn-subtitle-one` no último segundo, sem fade, então só o número muda, e a mesma linha no chat), renasce quem já pode (`respawned` no chat e como subtítulo), elimina quem não voltou a tempo, atualiza o placar. | `everySecond`, `playing` |
| A cada 5 ticks | Mostra a vida abaixo do nome. | `fastTick` |
| Um jogador morre | Acha quem matou (último inimigo que bateu em até 15 segundos; numa morte final, quem tirou o renascimento do time), credita kills e assistências, manda a mensagem de morte, deixa o jogador espectador onde morreu e agenda o renascimento (com o som `died`) ou elimina. O jogador não é teleportado: é jogado para cima a 1,5 bloco por tick e fica flutuando ali (`Stage.knockUp`); quem caiu no void volta para a altura do spawn do time, no mesmo x e z (`Stage.raiseTo`). | `died`, `deathMessage` |
| Um time sai | Avisa `team-eliminated`. Quando sobra um time (ou nenhum, numa partida forçada com um time só) a partida acaba. | `standingsChanged` |
| A partida acaba | Toca os sons de fim, dá a hotbar dos vencedores e dos espectadores, mostra o título de vitória ou derrota, sorteia quem faz o cosmético de vitória, manda o resultado centralizado com os rankings. | `ending`, `ended`, `score`, `area` |
| `timings.restart-seconds` depois | Manda todos para o `lobby-group` e fecha a partida 3 segundos depois. | `closed` |

### Renascimento e kills finais

Um time renasce ou não (`MatchTeam.respawns`). `MatchRules.respawning()` faz os times começarem renascendo; o jogo desliga isso para um time com `stopRespawning(quem)`, e quem tirou o renascimento ganha as kills finais que ninguém mais ganhou. Uma morte enquanto o time renasce é uma morte; senão é morte final e elimina o jogador. No BedWars, quebrar a cama para o renascimento do time.

### Sair e voltar

- Sair antes do início deixa a sala (`room-left`) e pode cancelar a contagem.
- Sair enquanto o time renasce guarda a vaga por `timings.rejoin-seconds` e grava `match:return:<jogador>` para o `/retornar` funcionar. Ao chegar de novo, o jogador volta como espectador e renasce depois de `timings.respawn-seconds` (`reconnected`).
- Sair quando o time não renasce é morte final, creditada a quem bateu por último.
- Não voltar a tempo elimina o jogador.

### Parties

Veja [Party](../../features/party/README.pt-BR.md) para o lado do jogador.

- **Procura de salas.** `MatchConnector.join` consulta o `PartyCheck`: um membro que não é o líder recebe `party-leader-only`; um líder cuja party online não cabe no `maxPlayers` do modo recebe `party-too-big`. Caso contrário, `Matchmaker.plan(..., vagas)` só escolhe salas com uma vaga livre para o líder e para cada acompanhante online (`RoomInfo.hasRoom(vagas)`), e cria uma sala quando nenhuma serve. O pedido leva os acompanhantes; o servidor do jogo reserva uma vaga para cada um na mesma sala, e o lobby manda todos para lá. O "jogar novamente" num servidor de partidas faz o mesmo localmente.
- **Times.** `MatchEnvironment.parties` (`PartyGroups`, vindo do `MuxeCore.parties()`) dá as parties presentes quando a partida começa, e `Match.start` as passa ao `TeamAssigner`, que mantém cada party em um time enquanto couber. `Match.splitParties` acha as parties que ainda ficaram em mais de um time: seus jogadores recebem `Match.unrank` (`Match.ranked(id)` fica `false`) e a partida recebe `party-split`. O jogo deixa os jogadores sem ranking fora das estatísticas.
- **Teleporte.** Num servidor de partidas, o `MatchServer` responde o `MuxeCore.parties().onGather`: `MatchService.gather` reserva vagas na sala do líder enquanto ela não começou e tem lugar, ou guarda uma vaga de espectador por 30 segundos depois que começou. Membros que já estão no servidor são movidos na hora; os outros chegam pelo proxy. Uma reserva ou vaga de espectador vale mais que voltar a uma partida antiga.
- **Espectadores.** `MatchRuntime.watch` adiciona um jogador que não participa: ele fica como espectador, recebe a hotbar de espectador e o placar, recebe as mensagens da partida e fala com todos nela, nunca recebe título de fim e é esquecido ao sair.

### Regras

`MatchRules` guarda o que muda de jogo para jogo, com padrões:

| Regra | Padrão | Significado |
|---|---|---|
| `respawn` | desligado | Os times começam renascendo. |
| `breakOnlyPlaced` | ligado | Só blocos colocados na partida podem ser quebrados ou explodidos. Tentar quebrar outro avisa `cannot-break`. |
| `hunger` | desligado | A fome muda. |
| `crafting` | desligado | Pode craftar. |
| `enterBeds` | desligado | Camas podem ser usadas no mundo das partidas. |
| `moveArmor` | desligado | Os slots de armadura podem ser clicados. |
| `undroppable` | nada | Itens que não podem ser dropados. |
| `nameTagPrefix` | | Prefixo dos times de placar das name tags (BedWars: `mxbw`). |

Sempre ligado: fogo amigo desligado, quem não está vivo não bate, o void mata, a proteção de spawn acaba quando o jogador bate em alguém, espectadores não mexem no inventário.

## Peças

| Pacote | Classes | Servidor? |
|---|---|---|
| `match` | `Match`, `Participant`, `MatchTeam`, `MatchPhase`, `TeamStatus`, `ChatView`, `Ranking`, `MatchArena` | Não |
| `combat` | `KillCredit` | Não |
| `timeline` | `Timeline<E>`: eventos em segundos fixos e o próximo para o placar | Não |
| `arena` | `ArenaSchema`, `ArenaView`, `StandardArena` | Não |
| `runtime` | `Minigame`, `MatchState`, `MatchRules`, `MatchRuntime`, `MatchService`, `Matches`, `Stage`, `Cells`, `Death`, `MatchEvents`, `TeamPreferences` | Sem chamadas estáticas ao servidor |
| `settings` | `MatchSettings` e suas partes, lidas do `config.yml` do jogo | Só configuração |
| `view` | `MatchBoard` (placar), `NameTagStyle`, `EndTitles`, `TitleSweep` | |
| `spectator` | `Ghosts`, `Spectating`, `TeleporterMenu`, `SpectatorTargets` | |
| `listener` | `MatchListener`, `GameHandlers` | Sim |
| `network`, `matchmaking` | Pacotes de salas, `RoomPublisher`, `MatchConnector`, `Matchmaker`, `RoomChoice`, `RoomDirectory`, `Reservations` | Sim |
| `bukkit` | `BukkitStage`, `MatchWorldCells`, `MatchTicker` | Sim |
| `bootstrap` | `MinigameBootstrap`, `MatchServer` | Sim |
| `command` | `GlobalChatCommand` (`/g`) | Sim |
| `cosmetic` | `GameCosmetics`, `CosmeticKind`, `CosmeticDefaults`, `CosmeticCatalog`, perfis, menus, prévias, auras, rastros, mensagens de kill e as animações de cena | Sim |

- **`MatchRuntime`** roda uma partida. Nunca chama o servidor direto: tudo o que faz com os jogadores passa pela porta `Stage` (`BukkitStage` no servidor, uma falsa nos testes), então o ciclo inteiro é testado sem servidor.
- **`MatchService`** guarda as partidas do servidor: cria numa arena pronta sorteada, escolhe a sala aberta mais cheia, segura reservas por 30 segundos, devolve quem chega à partida para a qual pode voltar, mantém as visões de chat e as name tags, fecha partidas vazias depois de `timings.empty-room-seconds` e fecha uma partida cuja lógica lança exceção. Os jogos leem ele por `Matches<S>` (`of(jogador)`, `at(local)`, `all()`, `playing(jogador)`).
- **`MatchListener`** trata os eventos que todo jogo compartilha e chama `GameHandlers` só para um jogador vivo numa partida em jogo, então o jogo nunca repete as checagens de fase e de vivo. Também cuida da hotbar da espera (`menu`, `leave`), da hotbar de espectador (teleportador, jogador aleatório, jogador em combate, jogar novamente, sair), do aviso dos comandos de lobby (`left-match`), do void, fome, drops, crafting, camas, slots de armadura e do chat da partida com `/g`.
- **`MatchEvents`** avisa os cosméticos sobre vitórias e kills finais.

## Cosméticos

Efeitos de vitória, efeitos de kill final, auras, rastros de projétil e mensagens de kill são compartilhados por todos os jogos (`CosmeticKind`), junto com o catálogo, os perfis de posse, os menus de cosméticos e as prévias do lobby. Um jogo acrescenta os próprios por cima (BedWars: destruição de cama e tipos de madeira).

| Tipo | Chave | Categoria de posse | Seção da prévia no lobby |
|---|---|---|---|
| `VICTORY_EFFECT` | `victory-effect` | `<jogo>-vitoria` | `victory-preview` |
| `FINAL_KILL` | `final-kill` | `<jogo>-kill-final` | `final-kill-preview` |
| `AURA` | `aura` | `<jogo>-aura` | `aura-preview` |
| `PROJECTILE_TRAIL` | `projectile-trail` | `<jogo>-rastro` | `projectile-trail-preview` |
| `KILL_MESSAGE` | `kill-message` | `<jogo>-mensagem-kill` | nenhuma |

- **Um catálogo só.** O `cosmetics.yml`, dentro do jar do api, declara cada cosmético compartilhado uma vez: nome, ícone, estrelas, tag, disponibilidade, descrição, os preços por estrelas e os conjuntos de mensagens de kill (`kill-messages`).
- **Posse por jogo.** Um cosmético comprado num jogo não vale em outro: cada jogo guarda os cosméticos em `<jogo>-<tipo>`, gerado a partir do id do jogo. O BedWars mantém `bedwars-vitoria` e as outras que já tinha; um jogo novo ganha as próprias categorias sem nenhuma configuração.
- **Sobrescrever.** Um jogo pode sobrescrever o catálogo no próprio `config.yml`, em `cosmetics.<chave>`: `prices`, qualquer campo de um cosmético, `hidden: true` para tirar um, ou um cosmético que só esse jogo tem. `kill-messages` sobrescreve os conjuntos de mensagens. O `CosmeticDefaults` junta os dois.
- **Uma chamada.** `GameCosmetics.start(setup)` recebe o id do jogo e os tipos que ele usa, e monta os catálogos, os perfis, os efeitos tocados na partida, as auras, os rastros, as mensagens de kill e, no lobby do jogo, as prévias (lidas das seções de prévia acima). Ele entrega ao jogo a categoria de menu de cada tipo (`category(tipo, menus)`) e é o `MatchEvents` do jogo.
- **Menus.** `CosmeticMenuSettings.from(menus, listaBase, chaves)` lê o menu de categorias do jogo, e o `CosmeticMenus` mostra ele e as listas: compra, seleção, prévias e um caminho de volta para o menu do próprio jogo.
- **Mensagens.** `cosmetic-selected`, `cosmetic-bought`, `cosmetic-not-enough`, `cosmetic-already-owned`, `cosmetic-unavailable`, `cosmetic-no-preview`, `cosmetic-failed`, `preview-only-lobby`, `preview-busy`.
- **Cosméticos do próprio jogo.** As animações deles podem usar os auxiliares do api (`Kit`, `Body`, `Flight`, `HeadLantern`...). Os auxiliares de teste de palco (`RecordingStage`, `TracedStage`, `FakeTrail`) são test fixtures do api: `testImplementation(testFixtures(projects.minigames.api))`.

## Configuração

Lida por `MatchSettings.from(config)` do `config.yml` do jogo:

| Chave | Padrão | Descrição |
|---|---|---|
| `lobby-group` | `lobby` | Para onde os jogadores vão depois da partida e com os itens de sair. |
| `default-mode` | `solo` | Modo de quem chega sem reserva. |
| `timings.respawn-seconds` | `5` | Tempo para renascer. |
| `timings.rejoin-seconds` | `180` | Quanto tempo quem saiu pode voltar. |
| `timings.restart-seconds` | `30` | Segundos entre o fim e mandar todos para o lobby. |
| `timings.respawn-protection-seconds` | `3` | Proteção de spawn no início e depois de renascer. |
| `timings.empty-room-seconds` | `60` | Uma partida vazia na espera fecha depois disso. |
| `countdown.titles` | | Título de cada segundo da contagem. |
| `scoreboard.*` | | Título e linhas de cada fase (`waiting`, `starting`, `playing`, `playing-eight-teams`) e a linha de time. Placeholders: `{code}`, `{map}`, `{players}`, `{max}`, `{seconds}`, `{mode}`, `{winstreak}`, `{kills}`, `{final-kills}`, `{teams}`, `{event}` e os do próprio jogo (`MatchState.placeholders`). |
| `hotbar.waiting.menu`, `hotbar.waiting.leave` | | Itens da hotbar da espera. |
| `hotbar.spectator.*` | | Itens de espectador: `teleporter`, `random-player`, `player-in-combat`, `play-again`, `leave`. |
| `hotbar.teleporter.title`, `hotbar.teleporter.entry-lore` | | Menu do teleportador. |
| `sounds.countdown`, `sounds.match-start`, `sounds.died`, `sounds.eliminated`, `sounds.team-eliminated`, `sounds.match-end`, `sounds.match-end-firework` | | Sons da partida. |
| `end-titles.victory`, `end-titles.defeat` | `VITÓRIA`, `DERROTA` | Palavra, cores e som dos títulos de fim. |
| `knockback.default` | nenhum | Nome do [perfil de knockback](../../development/core-api/README.pt-BR.md#knockback) de todos os modos. Sem ele, fica o knockback do próprio servidor. |
| `knockback.modes.<modo>` | | Perfil de um modo, sobre o `knockback.default`. BedWars e SkyWars usam `bedwars` e `skywars`. |

Mensagens, em `messages`: `room-joined`, `room-left`, `countdown`, `countdown-one`, `died-title`, `respawn-subtitle`, `respawn-subtitle-one`, `respawned`, `spectator-subtitle`, `cannot-break`, `reconnected`, `team-eliminated`, `match-started`, `result` (com linhas `{top-<ranking>}`), `result-draw`, `result-nobody`, `result-rank-1` a `result-rank-3`, `kill` e `death` (quando o jogo não tem mensagens de morte próprias), `no-arena`, `left-match`, `spectator-no-target`, `spectator-no-combat`, `global-prefix`, `health-below-name`, `room-requesting`, `room-sending`, `room-created`, `room-unavailable`. O Muxe-Core dá `party-leader-only`, `party-too-big` e `party-split` a todos os jogos.

## Pacotes

Cada jogo tem seus três pacotes, com o nome do id do jogo (`MatchPackets`):

| Pacote | Entrega | O que leva |
|---|---|---|
| `<jogo>.rooms` | volátil, 10 s | A cada 2 s, de cada servidor do jogo para o `lobby-group` e o grupo do jogo: as salas (código, modo, arena, aberta, jogadores, máximo) e as arenas prontas. |
| `<jogo>.join_request` | 5 s | De um lobby para um servidor do jogo: reservar uma vaga num modo, arena ou sala, mais uma para cada acompanhante da party. Versão 2 (os acompanhantes foram adicionados). |
| `<jogo>.join_reply` | 5 s | Se a vaga foi reservada, o código da sala e se a sala foi criada. |

O BedWars mantém `bedwars.rooms`, `bedwars.join_request` e `bedwars.join_reply`.

## Como o BedWars usa

| Classe do BedWars | Na API |
|---|---|
| `BedWarsGame` | O `Minigame`: regras com renascimento, as ferramentas que não podem ser dropadas, name tags `mxbw`, cores favoritas. |
| `BedWarsSchema` | O `ArenaSchema` com geradores, camas, lojas, melhorias e as ilhas extras do `4s`. |
| `BedWarsArena` | O `MatchArena`, lido com `ArenaView`. |
| `Room` | Envolve o `Match` com as camas (cama quebrada para o renascimento do time), melhorias de time, tiers de ferramenta e contadores de missão, resultados e a área do mapa. |
| `BedWarsMatch` | O `MatchState`: forjas, NPCs, baús das ilhas, baús do fim, armadilhas e cura, os eventos da timeline, missões, níveis, estatísticas e as mensagens de kill. |
| `BedWarsHandlers` | Os `GameHandlers`: camas, folhagem, TNT, bolas de fogo, estilingue, traças, depósito nos baús e coleta das forjas. |
| `CosmeticFeatures` | O `GameCosmetics` com os cinco tipos compartilhados, mais a destruição de cama (`BedBreakCosmetics`, as animações e a prévia dela), os tipos de madeira e o menu do BedWars (`BedWarsMenu`: missões, compra rápida, hotbar, preferências, cosméticos). |

## Testes

- `MatchTest`, `ChatViewTest`, `TeamStatusTest`, `KillCreditTest`, `TimelineTest`, `RankingTest`: as regras da partida.
- `MatchRuntimeTest`: o ciclo inteiro com uma `Stage` falsa (entrada, contagem, início, mortes no lugar e no void, renascimento, kills finais, quebra de blocos, saída, volta, tempo esgotado, o fim e o fechamento).
- `MatchKnockbackTest`: o perfil de knockback do modo dado na entrada, tirado na saída e trocado para a sala toda.
- `MatchServiceTest`: chegada, reservas, volta, chat, comandos da staff, partidas vazias e troca de modo.
- `CosmeticDefaultsTest`, `CosmeticMenuSettingsTest`, `LifetimeTallyTest` e os testes de cada animação de cena, aura e rastro: os cosméticos.
- `PartyMatchTest`, `PartyCheckTest`, `JoinRoomRequestTest`: vagas da party, times, o aviso de party dividida, o teleporte e os espectadores.
- `ArenaSchemaTest`, `MatchmakerTest`, `ReservationsTest`, `JoinNoticesTest`, `TitleSweepTest`, `TitleRunsTest`, `SpectatorTargetsTest`, `BukkitStageTest`.
