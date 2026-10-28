[English](README.md) | **Português**

# Parkour

Um parkour cronometrado no lobby: um início, checkpoints numerados e um fim, marcados em placas de pressão do mapa. Os jogadores recebem itens para reiniciar, voltar ao último checkpoint ou parar, e guardam um recorde pessoal por mapa. A equipe marca os pontos por comando em qualquer servidor. Ele fica no Muxe-Core (`core:common` pacotes `br.com.muxe.core.common.parkour` e `br.com.muxe.core.common.placement`, `core:bukkit` pacote `parkour`), então todo lobby de todo modo o recebe, e os pontos são compartilhados pelo grupo de servidores como os [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md).

## Histórias de usuário

- Como jogador, quero fazer o parkour do lobby contra o relógio, com checkpoints, para poder bater o meu próprio recorde.
- Como jogador, quero ver o meu recorde pessoal acima do início, e saber quando eu o bato.
- Como jogador, quero reiniciar, voltar ao último checkpoint ou parar com um clique, sem precisar voltar andando.
- Como membro da equipe, quero marcar o início, os checkpoints e o fim por comando, em todos os servidores de um grupo de uma vez.
- Como operador, quero trocar os itens, os hologramas, o fogo de artifício e as mensagens na config, sem mexer no código.

## Como funciona

### O percurso

- Um percurso tem um início, quantos checkpoints quiser numerados a partir de 1, e um fim. Cada ponto é uma placa de pressão construída no mapa; o core só guarda onde ela está e observa os jogadores entrando naquele bloco (um `PlayerMoveEvent` que muda de bloco). O core nunca coloca nem tira a placa.
- O percurso só funciona quando tem um início e um fim. Os checkpoints são opcionais; quando há vários, eles são alcançados na ordem dos números (pode haver buracos na numeração).
- Acima de cada ponto fica um holograma que existe só em pacotes, como os [suportes de exibição](../../development/core-api/README.pt-BR.md#suportes-de-exibição), centralizado na placa:

| Ponto | Linhas (de cima para baixo) |
|---|---|
| Início | `&b&lPARKOUR`, `&aInício`, `&eRecorde pessoal: &7{record}` |
| Checkpoint | `&b&lPARKOUR`, `&eCheckpoint &a#{number}` |
| Fim | `&b&lPARKOUR`, `&cFim` |

- O `{record}` é diferente para cada jogador que olha: o melhor tempo dele neste grupo de servidores, ou `parkour.no-record` (`...`) quando ele não tem nenhum. Ele é carregado na entrada e atualizado depois de cada conclusão.

### Uma corrida

- Pisar no início começa a corrida: `&b&lPARKOUR &aIniciado!`. O jogador recebe os itens do parkour e perde o voo (veja abaixo).
- Pisar no início de novo durante a corrida reinicia o temporizador onde o jogador está: `&b&lPARKOUR &aTemporizador reiniciado!`.
- Pisar no próximo checkpoint mostra o tempo desde o início: `&b&lPARKOUR &aVocê completou o &echeckpoint #2 &aem &71m 22,704s&a!`. Pisar num checkpoint posterior antes do anterior, ou num que já foi alcançado, não faz nada, sem nenhuma mensagem.
- O fim só conta depois de todos os checkpoints. Pisar nele antes não faz nada.
- Ao terminar:
  - aparece `&b&lPARKOUR &aVocê finalizou o &eparkour &aem &7{time}&a!`;
  - um fogo de artifício explode bem em cima da placa do fim (verde, ciano e vermelho, bola grande, por padrão);
  - o tempo é salvo. Quando ele bate um recorde anterior, o jogador também vê `&b&lPARKOUR &eVocê atingiu um novo &brecorde! &a{previous} &7-> &a{time}`. A primeira conclusão de um jogador vira o recorde dele sem essa mensagem;
  - a corrida acaba e o jogador recebe de volta os itens e o voo que tinha.
- Sair do servidor encerra a corrida. As corridas ficam só na memória: trocar de lobby começa do zero.

### Itens

Durante a corrida, o jogador tem três itens na hotbar. Os itens que estavam nesses slots (o baú de cosméticos da [hotbar](../hotbar/README.pt-BR.md) no slot 5) são guardados e voltam quando a corrida acaba.

| Slot | Item | Nome | Ação |
|---|---|---|---|
| 4 | Disco de música (`RECORD_9`) | `&aReiniciar Parkour` | Teleporta para o início e reinicia o temporizador (`parkour-restarted`). |
| 5 | Placa de pressão de ouro | `&aVoltar ao Checkpoint` | Teleporta para o último checkpoint alcançado (`parkour-returned`). Antes do primeiro checkpoint, teleporta para o início e reinicia o temporizador (`parkour-restarted`). |
| 6 | Redstone | `&aParar Parkour` | Encerra a corrida e devolve os itens e o voo (`parkour-stopped`). |

- Os itens são usados com o botão direito, no ar ou num bloco, e só agem no próprio slot e com o próprio material. O clique é cancelado, então a placa de ouro nunca é colocada.
- Os cliques nos itens usam o anti-spam do Muxe-Core (`spam.*`), o mesmo da hotbar, dos NPCs, das caixas registradoras e das cabeças de redes sociais: três usos dentro de 1 segundo bloqueiam por 3 segundos com `&cAguarde 2,9s para usar novamente.`
- Os teleportes colocam o jogador no meio da placa, virado para onde o ponto foi marcado, e zeram a distância de queda.

### Voo

- Começar uma corrida desliga o voo. O core lembra se o jogador podia voar e se estava voando, e devolve os dois quando a corrida acaba (conclusão, `Parar Parkour` ou o servidor desligando).

### Tempo

Os tempos aparecem em minutos, segundos e milissegundos com vírgula: `1m 22,704s`. Abaixo de um minuto, só os segundos: `8,050s`. A partir de uma hora, as horas também: `1h 0m 5,000s`.

### Recordes por mapa

- Os recordes pertencem ao grupo de servidores (`server.group`), como os pontos: o `lobby-1` e o `lobby-2` dividem o recorde do lobby principal, enquanto o lobby de outro modo tem o seu próprio percurso e os seus próprios recordes.
- Só o melhor tempo de cada jogador é guardado.

### Compartilhado pelo grupo de servidores

- Os pontos são posicionamentos do tipo `PARKOUR` (`ParkourPointPlacement(group, point, position)`, com `ParkourPoint` `START`, `CHECKPOINT` ou `END`): são salvos no PostgreSQL e sincronizados pelo `placement.changed`, exatamente como descrito em [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#compartilhados-pelo-grupo-de-servidores). Os ids são `start`, `end` e `checkpoint-<n>`.
- Marcar um ponto que já existe o move. Removê-lo tira o holograma dele em todos os servidores do grupo que estão ligados.
- Um ponto só aparece se o mundo dele estiver carregado naquele servidor.

## Montando um percurso

1. Construa o percurso no mapa com uma placa de pressão no início, em cada checkpoint e no fim.
2. Fique em cima da placa do início, virado para onde os jogadores devem olhar depois de reiniciar, e use `/parkour inicio`.
3. Fique em cima de cada placa de checkpoint, na ordem, e use `/parkour checkpoint 1`, `/parkour checkpoint 2`, e assim por diante.
4. Fique em cima da placa do fim e use `/parkour fim`.
5. Confira com `/parkour listar`. Os hologramas aparecem em todos os servidores do grupo.

O ponto é salvo no bloco onde você está, centralizado nele, com a sua direção (yaw) e o olhar reto (pitch zero). Os pontos também podem ser trazidos pelo `import.yml` (veja [Importando o que já existia](../npcs-and-holograms/README.pt-BR.md#importando-o-que-já-existia)):

```yaml
parkour:
  start:
    world: lobby
    x: 12.5
    y: 33.0
    z: 4.5
    yaw: 90.0
    pitch: 0.0
  checkpoints:
    1:
      world: lobby
      x: 20.5
      y: 36.0
      z: 4.5
      yaw: 90.0
      pitch: 0.0
  end:
    world: lobby
    x: 30.5
    y: 40.0
    z: 4.5
    yaw: 90.0
    pitch: 0.0
```

Os checkpoints são identificados pelo número. Entradas quebradas são puladas com um aviso.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/parkour inicio` | Marca o início no bloco onde você está. `início` também funciona. | `muxe.parkour` |
| `/parkour checkpoint <n>` | Marca o checkpoint `n` (1 ou mais) no bloco onde você está. | `muxe.parkour` |
| `/parkour fim` | Marca o fim no bloco onde você está. | `muxe.parkour` |
| `/parkour remover <inicio\|fim\|n>` | Remove um ponto. `remover checkpoint <n>` também funciona. | `muxe.parkour` |
| `/parkour listar` | Lista os pontos deste grupo de servidores: início, checkpoints em ordem, fim. | `muxe.parkour` |

Sem argumentos, ou com um argumento errado, o comando mostra o seu uso (`messages.parkour-usage`). Só jogadores podem usá-lo. Os subcomandos e os nomes dos pontos (`inicio`, `fim`, `checkpoint`) completam com tab; veja [Comandos](../../development/commands/README.pt-BR.md).

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.parkour` | Usar `/parkour`. | op |

Fazer o parkour não precisa de permissão.

## Configuração

`plugins/Muxe-Core/config.yml`:

```yaml
parkour:
  no-record: "..."
  items:
    restart:
      slot: 4
      item:
        material: RECORD_9
        name: "&aReiniciar Parkour"
        hide-flags: false
    checkpoint:
      slot: 5
      item:
        material: GOLD_PLATE
        name: "&aVoltar ao Checkpoint"
    stop:
      slot: 6
      item:
        material: REDSTONE
        name: "&aParar Parkour"
  holograms:
    start:
      - text: "&b&lPARKOUR"
        height: 2.075
      - text: "&aInício"
        height: 1.79375
      - text: "&eRecorde pessoal: &7{record}"
        height: 1.2625
    checkpoint:
      - text: "&b&lPARKOUR"
        height: 1.54375
      - text: "&eCheckpoint &a#{number}"
        height: 1.2625
    end:
      - text: "&b&lPARKOUR"
        height: 1.54375
      - text: "&cFim"
        height: 1.2625
  finish-firework:
    type: BALL_LARGE
    colors:
      - "#55FF55"
      - "#55FFFF"
      - "#FF5555"
    flicker: false
    trail: false
```

| Chave | Padrão | Descrição |
|---|---|---|
| `parkour.no-record` | `...` | Mostrado como `{record}` para um jogador sem recorde. |
| `parkour.items.restart`, `.checkpoint`, `.stop` | Slots 4, 5 e 6 | `slot` (1 a 9) e `item` (como os [itens configurados](../../development/core-api/README.pt-BR.md#itens)). Os três itens precisam de slots diferentes. |
| `parkour.holograms.start`, `.checkpoint`, `.end` | Veja acima | Linhas de cada holograma: `text` (com cores `&`) e `height` acima da base do bloco da placa. `{number}` é o número do checkpoint; `{record}` o recorde de quem olha. |
| `parkour.finish-firework.type` | `BALL_LARGE` | Formato do fogo de artifício: `BALL`, `BALL_LARGE`, `STAR`, `BURST` ou `CREEPER`. |
| `parkour.finish-firework.colors` | `#55FF55`, `#55FFFF`, `#FF5555` | Cores no formato `#RRGGBB`. Pelo menos uma. |
| `parkour.finish-firework.fade-colors` | nenhuma | Cores para as quais ele desbota, no formato `#RRGGBB`. |
| `parkour.finish-firework.flicker`, `.trail` | `false`, `false` | Cintilar e rastro do fogo de artifício. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam compartilhado com a hotbar, os NPCs, as caixas registradoras e as cabeças de redes sociais. |

Sem a seção `parkour`, o parkour fica desligado naquele servidor: o core registra `The parkour is off: config.yml has no 'parkour' section`, não mostra nenhum holograma e ignora as placas. O `/parkour` continua salvando os pontos, que os outros servidores do grupo mostram. Um item ou uma lista de holograma faltando, dois itens no mesmo slot, uma linha de holograma sem `text` ou `height`, ou um fogo de artifício sem cores é um erro ao ligar.

### Mensagens

Todas em `messages` do `plugins/Muxe-Core/config.yml`:

| Chave | Padrão |
|---|---|
| `parkour-started` | `&b&lPARKOUR &aIniciado!` |
| `parkour-restarted` | `&b&lPARKOUR &aTemporizador reiniciado!` |
| `parkour-checkpoint` | `&b&lPARKOUR &aVocê completou o &echeckpoint #{number} &aem &7{time}&a!` |
| `parkour-returned` | `&b&lPARKOUR &aVocê voltou para o checkpoint #{number}.` |
| `parkour-finished` | `&b&lPARKOUR &aVocê finalizou o &eparkour &aem &7{time}&a!` |
| `parkour-new-record` | `&b&lPARKOUR &eVocê atingiu um novo &brecorde! &a{previous} &7-> &a{time}` |
| `parkour-stopped` | `&b&lPARKOUR &cVocê saiu do parkour.` |
| `parkour-usage` | Lista: `&eUso do /parkour:` e uma linha por subcomando. |
| `parkour-invalid-point` | `&cUse inicio, fim ou o número do checkpoint (1 ou mais).` |
| `parkour-saved` | `&aPonto &f{point} &ado parkour marcado onde você está.` |
| `parkour-unknown` | `&cO parkour deste grupo não tem &f{point}&c.` |
| `parkour-removed` | `&aPonto &f{point} &aremovido do parkour.` |
| `parkour-list-empty` | `&7O parkour deste grupo não tem pontos. Use &f/parkour inicio&7.` |
| `parkour-list-header` | `&ePontos do parkour do grupo &f{group} &e({count}):` |
| `parkour-list-line` | `&8- &f{point} &8{location}` |
| `parkour-point-start`, `parkour-point-end`, `parkour-point-checkpoint` | `início`, `fim`, `checkpoint #{number}`: o `{point}` das mensagens da equipe. |
| `placement-failed`, `spam-cooldown`, `players-only`, `no-permission` | Compartilhadas com outras funcionalidades. |

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `placement.changed` | o grupo de servidores | padrão | Um ponto do grupo foi marcado ou removido; todo servidor do grupo recarrega do PostgreSQL. |

As corridas e os recordes não enviam pacote.

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.parkour_placements` | `server_group`, `id` (`start`, `end`, `checkpoint-<n>`), posição (`world`, `x`, `y`, `z`, `yaw`, `pitch`). Chave primária `(server_group, id)`. |
| `core.parkour_records` | `server_group`, `player_id`, `best_millis`, `achieved_at`. Chave primária `(server_group, player_id)`. |

Criadas pela migração do core `V7__create_parkour.sql`. Os pontos são salvos e removidos pelo `SavePlacementCommand` e pelo `RemovePlacementCommand` (`PlacementKind.PARKOUR`) e lidos com a `FindPlacementsQuery`. Os recordes passam pelo `SubmitParkourTimeCommand(group, playerId, time)` (transação `parkour.submit_time`), que só guarda o tempo quando ele bate o salvo e responde um `ParkourOutcome` (tempo, recorde anterior e se é um novo recorde), e pela `FindParkourRecordQuery(group, playerId)` (transação `parkour.find_record`), os dois registrados pelo `ParkourModule`. As corridas em andamento ficam só na memória.

## Métricas

Nenhuma própria. `muxe_db_transaction_seconds{operation="parkour.submit_time"|"parkour.find_record"}` e `muxe_cqrs_handle_seconds` cobrem o comando e a consulta dos recordes; marcar e remover pontos conta nas métricas de posicionamento listadas em [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#métricas).

## Integrações

- [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md): o mesmo armazenamento de posicionamentos, a mesma sincronização por grupo e o mesmo `import.yml`.
- [Hotbar](../hotbar/README.pt-BR.md): os slots 4 a 6 ficam com os itens do parkour durante a corrida; o baú de cosméticos volta depois.
- [Lobby](../lobby/README.pt-BR.md): o voo fica desligado durante a corrida e volta depois.
- A [API do core](../../development/core-api/README.pt-BR.md) fornece o `DisplayService` para os hologramas (com texto por jogador para o `{record}`), o `FireworkBurst` para o fogo de artifício do fim e o `SpamGuard` para o anti-spam.

## Critérios de aceitação

- [x] Pisar no início começa uma corrida; pisar de novo reinicia o temporizador.
- [x] Os checkpoints só contam em ordem; um checkpoint posterior ou repetido é ignorado.
- [x] O fim só conta depois de todos os checkpoints, e dá o tempo desde o início.
- [x] Voltar ao checkpoint teleporta para o último alcançado, ou reinicia no início antes do primeiro.
- [x] Um tempo só é salvo quando bate o recorde guardado, e o resultado informa o recorde anterior.
- [x] Os pontos podem ser marcados, movidos e removidos, e os pontos e os recordes sobrevivem a ir e voltar do PostgreSQL.
- [x] Cada grupo de servidores tem os seus próprios recordes.
- [x] Os tempos são formatados como `1m 22,704s`.
- [x] Os pontos são marcados no bloco onde o membro da equipe está, centralizados, e o `import.yml` lê o início, os checkpoints e o fim.
- [x] Os itens do parkour substituem os slots 4 a 6 e o voo, e os dois voltam quando a corrida acaba.
- [ ] Os hologramas, o recorde pessoal, os itens, o fogo de artifício e as mensagens funcionam no jogo, e um ponto marcado num lobby aparece em outro lobby do grupo (conferido no jogo).

## Falhas e recuperação

- PostgreSQL indisponível quando um comando da equipe roda: a equipe vê `&cNão foi possível salvar agora. Tente novamente em instantes.` e nada muda.
- PostgreSQL indisponível quando um jogador termina: ele ainda vê a mensagem de conclusão e o fogo de artifício, mas o tempo não é salvo e nenhuma mensagem de novo recorde é enviada; um aviso vai para o log.
- PostgreSQL indisponível na entrada: o recorde não é carregado e o holograma do início mostra `...` para aquele jogador até ele terminar uma corrida.
- PostgreSQL indisponível ao ligar, ou Redis indisponível: igual aos [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#falhas-e-recuperação).
- Mundo não carregado num servidor: aquele ponto é pulado naquele servidor com um aviso.
- Servidor desligando: todas as corridas acabam e os jogadores recebem os itens e o voo de volta.
- Internos do servidor ausentes (outra versão de servidor): o fogo de artifício sobe um pouco antes de explodir, em vez de estourar em cima da placa.

## Limitações conhecidas

- O core não coloca as placas de pressão: construir o percurso é trabalho do mapa, e um ponto marcado onde não há placa funciona do mesmo jeito.
- Só um percurso por grupo de servidores.
- Não há ranking: só o melhor tempo de cada jogador é guardado e mostrado para ele.
- As corridas não passam de um servidor para outro: trocar de lobby encerra a corrida.
- Os pontos são encontrados pelo bloco, então dois pontos não podem dividir o mesmo bloco.
- Ligar o modo construção não encerra a corrida; pare a corrida antes.
