[English](README.md) | **Português**

# BedWars

BedWars para a rede Muxe. Fica no `minigames:bedwars` (`Muxe-BedWars`) e roda nos servidores do grupo `bedwars`.

## Histórias de usuário

- Como jogador, quero proteger a cama do meu time e destruir a dos outros, para que o meu time seja o último de pé.

## Como funciona

O plugin carrega os nove modos, dá à staff o [setup de arenas](../../architecture/game-engine/README.pt-BR.md#setup-de-arenas) e roda as partidas do servidor, várias ao mesmo tempo, cada uma na própria [célula](../../decisions/0009-own-map-format-and-match-cells.pt-BR.md) do mundo das partidas. O ciclo de vida da partida, entrada e saída, mortes, renascimento, espectadores, o placar, títulos, chat e matchmaking vêm da [API de minigames](../../architecture/minigame-api/README.pt-BR.md); o BedWars acrescenta as camas, forjas, loja, melhorias, armadilhas e as outras regras dele por cima (`BedWarsGame`, `BedWarsMatch`, `BedWarsHandlers`).

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

As arenas são configuradas no lobby do BedWars, não nos servidores de partida, e salvas no PostgreSQL: todo servidor de BedWars as carrega ao ligar e recarrega uma quando o lobby a salva (`minigame.arena_saved`). Para adicionar um mapa, envie a pasta do mundo para a raiz de um servidor de lobby do BedWars e rode `/arena criar <modo>_<mapa>` lá. Editar coloca o membro da staff no modo construção (para o lobby deixar voar no criativo e quebrar e colocar blocos), e `/arena sair` tira.

Uma arena se chama `<modo>_<mapa>`, como `1v1_slip` ou `quartetos_air_nation`. `/arena criar 1v1_slip` converte uma pasta de mundo chamada `1v1_slip` ou abre um mundo vazio. O nome da pasta precisa ser exatamente o nome da arena.

Marque primeiro o `spawn` de cada time: ele também marca a cama mais perto, a até 20 blocos, que nenhum outro time tem. Depois disso, os outros passos de time (cama, loja, melhorias, ferro, ouro) são uma linha cada na checklist e vão para o time com o spawn mais perto.

| Passo | Por time | Tipo | Obrigatório |
|---|---|---|---|
| `espera`: spawn da espera | não | onde o jogador está | sim |
| `area-espera`: área da espera, removida quando a partida começa | não | Pos 1 e Pos 2 | sim |
| `espectador`: spawn dos espectadores | não | onde o jogador está | sim |
| `void`: altura do void | não | o Y do jogador | sim |
| `limite`: bloco mais alto que os jogadores podem colocar | não | o Y do jogador | não |
| `diamante`, `esmeralda`: geradores | não | o bloco sob o jogador | não, até 16 cada |
| `spawn` | sim | onde o jogador está | sim |
| `cama` | sim | o bloco para onde o jogador olha | sim |
| `loja`, `melhorias`: NPCs da loja e das melhorias | sim | onde o jogador está | sim |
| `ferro`, `ouro`: geradores da ilha | sim | o bloco sob o jogador | pelo menos 1, até 4 |
| `loja-extra`, `melhorias-extra`, `ferro-extra`, `ouro-extra` | não | como acima | só no 4s, opcionais |

### Partidas

Quem chega num servidor de BedWars volta para a partida que deixou, se ainda puder, e senão vai para uma sala do `default-mode` (ou do modo escolhido com `/bedwars entrar <modo>`): a sala mais cheia esperando jogadores, ou uma sala nova numa arena pronta aleatória desse modo.

| Fase | O que acontece |
|---|---|
| Espera | Os jogadores esperam na área de espera acima do mapa, no modo sobrevivência como na partida, sem dano nem fome e sem quebrar ou colocar blocos. A hotbar tem a esmeralda `&aMenu do Bed Wars` (slot 5, abre o menu) e `&aVoltar ao Lobby` (slot 9). O placar segue o anexo 2, e o anexo 3 quando a contagem roda. Uma sala sem ninguém dentro e sem ninguém a caminho (nenhuma reserva pendente) fecha depois de `timings.empty-room-seconds` (60) e libera a célula. |
| Contagem | A [contagem para iniciar](../../architecture/game-engine/README.pt-BR.md#contagem-para-iniciar) da engine, com os títulos `&a30 segundos`, `&610 segundos` e `&c5` a `&c1`. Cada título toca um clique de botão (`sounds.countdown`) e manda `O jogo inicia em <n> segundos!` no chat (`segundo` no último). |
| Jogando | Os times são sorteados, os jogadores nascem na ilha com couro completo na cor do time e uma espada de madeira. Os NPCs da loja e das melhorias aparecem com os hologramas, e os geradores começam. Um time que ficou sem jogadores perde a cama (tirada quando achada a até 2 blocos do ponto marcado), mas a ilha dele mantém os geradores e os NPCs da loja e das melhorias, que qualquer um pode usar. Logo depois, a área de espera entre a Pos 1 e a Pos 2 some de uma vez, chunk por chunk (`RegionEraser`). |
| Fim | Quando sobra um time, todos recebem o resultado (anexo 11), centralizado: vencedor, tempo da partida, top kills finais e top camas quebradas, com os nomes na cor da tag. O time vencedor vê o título `VITÓRIA` e todos os outros da sala, eliminados inclusive, veem `DERROTA` (`end-titles.*`): a palavra aparece branca, uma varredura dourada (vermelha na derrota) passa letra por letra a cada 2 ticks e depois ela pisca entre a cor e o branco a cada 7 ticks por uns 6 segundos; a vitória toca o rugido do dragão. Empate não mostra título. Depois de 30 segundos todos vão para o `lobby-group` e a sala fecha. |

**Durante a partida:**

- **Times:** o placar segue o anexo 5 (2 ou 4 times) ou o anexo 12 (8 times). Um time mostra `&a✔` enquanto tem cama e alguém conectado, `&e✔` quando todos saíram mas ainda podem voltar, o número de jogadores vivos depois que a cama cai, e `&c✘` quando é eliminado. Times sem ninguém já começam fora.
- **Entrar e sair da sala:** as mensagens de entrada e saída do próprio servidor ficam escondidas. Ao entrar na sala de espera, a sala recebe `tadeu entrou na sala (3/8).` (`messages.room-joined`), e quem sai antes da partida começar gera `tadeu saiu da sala.` (`messages.room-left`), com o nome na cor da tag do jogador.
- **Nível:** desde a sala de espera, a barra de XP mostra o nível do BedWars como nível de XP e o progresso até o próximo nível como a barra, contando o XP ganho na partida. Ela é atualizada a cada segundo, e o XP do fim da partida entra quando a partida acaba.
- **Subir de nível:** ao chegar a um nível novo, na partida ou no lobby, o jogador recebe `&b&lPARABÉNS &7Você subiu para o nível [904⚝]&7!` com o emblema do nível novo (`messages.level-up`), pela [subida de nível](../../architecture/game-engine/README.pt-BR.md#subida-de-nível) da engine.
- **Name tags e tab:** a letra do time em negrito e a cor (`A` em azul), pelo `MuxeCore.nameTags()`. O placar e o chat mostram a letra sem negrito. O tab mostra a vida dos jogadores vivos como um número amarelo à direita do nome.
- **Vida abaixo do nome:** os jogadores vivos mostram a vida embaixo do nome (`20 ❤`, com o coração de `messages.health-below-name`), pela [vida abaixo do nome](../../architecture/game-engine/README.pt-BR.md#vida-abaixo-do-nome) da engine. Espectadores e quem espera renascer não mostram nada.
- **Chat:** só a sala vê, com o time antes do formato de sempre: `[A] medalha tag nome`. Nos modos solo todos da sala leem. Nos modos com time, durante a partida, o chat chega só ao time do jogador; `/g <mensagem>` fala com a sala toda, com `&6[G]` antes (`messages.global-prefix`). Toda mensagem começa com o emblema do nível de quem fala (`[904⚝]`), também no lobby de BedWars; quem desligou `Mostrar nível no chat` aparece como `[0✫]` para todos. O chat roda fora da thread principal, então lê um retrato de cada sala (quem ouve quem e o prefixo do time) refeito na thread principal a cada segundo e sempre que alguém entra ou sai.
- **Camas:** quebrar a cama de um inimigo manda `CAMA DESTRUÍDA` (no [conjunto de mensagens](#mensagens-de-kill) de quem quebrou) para todos e `+100 XP` para quem quebrou. Um integrante do time que estava fora é eliminado, e a kill final vai para quem quebrou a cama. Ninguém quebra a própria cama.
- **Mortes:**
  - A kill vai para o último inimigo que bateu nos últimos 15 segundos. Uma morte final sem golpe vai para quem quebrou a cama.
  - Quem mata recebe o ferro, ouro, diamantes e esmeraldas da vítima, com linhas como `+28 Ferros` e `+12 XP` (kill) ou `+60 XP` (kill final).
  - Cair abaixo da altura do void da arena mata na hora (`morreu no void`). Os textos de kill, morte e kill final vêm dos [conjuntos de mensagens de kill](#mensagens-de-kill), o `Padrão` a menos que o jogador escolha outro.
  - O jogador não é teleportado ao morrer: é jogado para cima onde morreu (1,5 bloco por tick) e fica flutuando ali como espectador; quem caiu no void volta para a altura do spawn do time. Com cama, o título `VOCÊ MORREU` fica na tela com `Renascendo em <n> segundos.` embaixo, só o número muda a cada segundo, a mesma linha vai para o chat, a morte toca `ZOMBIE_UNFECT` com tom 2 (`sounds.died`), e o jogador renasce depois de 5 segundos (`Você renasceu!` no chat e como subtítulo), protegido por 3 segundos, com a armadura comprada e a picareta e o machado um nível abaixo. Sem cama, o jogador sai (`KILL FINAL!`), e o último jogador do time a sair elimina o time (`TIME ELIMINADO`).
- **Blocos:** só blocos colocados durante a partida podem ser quebrados, além do mato do mapa (grama alta, plantas duplas e arbustos secos), que não dropa nada; tentar quebrar outro bloco avisa `Você não pode quebrar este bloco.`. Nada pode ser colocado perto dos spawns (5 blocos), dos NPCs (1) e dos geradores (1) (`timings.spawn-protection-radius`, `npc-protection-radius`, `generator-protection-radius`), nem acima do limite de construção da arena (`Você não pode colocar blocos aqui.`). Explosões só quebram blocos colocados, nunca vidro, e os blocos quebrados podem dropar. A TNT acende sozinha, espera 60 ticks (3 s) e explode com força 3. A bola de fogo é lançada com o botão direito a partir dos olhos do jogador, sai parada e acelera 0,1 bloco por tick na direção do olhar (com o atrito vanilla, até cerca de 1,9 bloco por tick), e explode com força 2. A TNT ou bola de fogo do próprio jogador nunca o machuca: mostra a animação de dano e o lança para longe da explosão, a bola de fogo com 3,125 na linha da explosão até os pés, a TNT com 1,875 pelo chão, as duas com 0,7 para cima (1,0 se já estiver no ar) (`ExplosionKnockback`). Os outros jogadores levam o dano e o empurrão vanilla.
- **Estilingue:** botão direito com o `Estilingue` (magma cream, comprado na loja por 7 ouros) gasta um e lança o jogador para frente: 1,5625 vezes o olhar em x e z, 1,0 para cima. Mostra o efeito de quebrar bloco de slime e toca `mob.slime.small` quatro vezes, a cada 4 ticks, com tons 1,25, 1,5, 1,75 e 2,0. Depois espera 7 segundos, mostrados na action bar como `ESTILINGUE: 7,0s` em vermelho, com a parte sublinhada do nome diminuindo conforme o tempo passa; usar nesse tempo não faz nada. O dano de queda não é cancelado (`Slingshot`).
- **Espectadores:** mortos e eliminados não ficam no modo espectador: ficam no aventura, voando e invisíveis, escondidos de quem ainda está no jogo (espectadores se veem), e flechas e bolas de fogo passam por eles. Quem espera para renascer não recebe nada; o eliminado recebe `Teleportador` (bússola, slot 0, abre um menu com a cabeça de cada jogador vivo; um clique teleporta até ele), `Teleportar: Jogador aleatório` (espada de ferro, slot 3), `Teleportar: Jogador em combate` (espada de diamante, slot 4, um jogador vivo que bateu ou apanhou nos últimos 5 segundos), `Jogar novamente` (papel, slot 7) e `Voltar ao Lobby` (cama, slot 8) (`hotbar.spectator`, `hotbar.teleporter`). Sem ninguém para ir, dizem `Nenhum jogador para teleportar.` ou `Nenhum jogador em combate agora.`. Espectadores não dropam, movem nem usam itens, não abrem baús nem lojas, não quebram nem colocam blocos.
- **Fim da partida:** os vencedores recebem `Jogar novamente` e `Voltar ao Lobby` nos slots 7 e 8, por cima do que estava lá; o resto do inventário fica. Espectadores mantêm ou recebem os itens de espectador. `Jogar novamente` abre o mesmo menu de modo do NPC do lobby do BedWars, do modo que acabou de ser jogado; a lista de salas vem de todos os servidores de partida, e uma sala no mesmo servidor é entrada direto. `Voltar ao Lobby` manda o jogador para o lobby.
- **Geradores:** o ferro da ilha cai a cada 0,9 s, tantos lingotes quanto jogadores por time (até 4), o ouro a cada 7 s, cada um com um limite de itens no chão num raio de 3 blocos. Diamante e esmeralda mostram nível e contagem num holograma e aceleram aos 6, 12, 18 e 24 minutos. Ferro e ouro coletados são copiados para os companheiros a até 2 blocos, que ouvem o som de coleta.
- **AFK:** quem não se mexe, não clica e não fala por 45 segundos está AFK: não pega itens dos geradores e não recebe nada da coleta de um companheiro, até agir de novo.
- **Chão do ferro:** quando a partida começa, cada gerador de ferro da ilha procura um chão de lajes embaixo dele (de um bloco acima a dois abaixo, sem nada sólido em cima). Um chão 3x3 solta o ferro em cruz (frente, lados, centro, trás); um chão 2x3, cujo ponto do gerador é o centro da frente, passa pela fileira da frente e pelo meio, e os cantos de trás ficam vazios. A frente é o lado virado para o spawn do time; um chão 2x3 virado para outro lado também vale. Cada lingote cai na próxima célula, então 4 lingotes de uma vez caem em 4 células. Sem um chão 2x3 ou 3x3 o ferro cai no ponto e um aviso vai para o log. O ouro sempre cai no ponto.
- **Itens:** caem 1,3 bloco acima do ponto, sem velocidade e sem espera para pegar. Com `forges.stack-items: false` os itens de cada célula são marcados para não se juntarem com os de outra. Pegar um recolhe de uma vez todos os itens do mesmo tipo a até meio bloco, sem a marca.
- **Diamante e esmeralda:** um bloco de diamante ou de esmeralda gira acima de cada gerador (6° por tick, uma volta a cada 3 s, subindo e descendo 0,08 bloco), com as três linhas do holograma acima dele, a 0,3 de distância.
- **Eventos:** depois da Esmeralda III, todas as camas são destruídas (30 min), a morte súbita é anunciada (40 min; os dragões ainda não entraram) e a partida termina (42 min), empatada se sobrar mais de um time.
- **Início:** quando a contagem acaba, cada jogador vai para a sua ilha, ouve `NOTE_PLING` com tom 2 (`sounds.match-start`) e o chat recebe `O jogo iniciou!` e o `AVISO:` de que aliança com outros jogadores é proibida e punida (`messages.match-started`).
- **Combate:** jogadores de times diferentes sempre podem se bater; o mundo da partida liga o PvP sozinho, não importa o `pvp` do `server.properties`. Colegas de time não se machucam, e espectadores, eliminados e quem está fora da partida não batem em ninguém.
- **Proteção de spawn:** todo jogador fica protegido por `timings.respawn-protection-seconds` (3 por padrão) quando a partida começa e depois de cada renascimento. Um jogador protegido não leva dano. A proteção acaba antes assim que o jogador bate em um inimigo, mesmo que ele também esteja protegido.
- **Baús:** um baú dentro do raio da ilha (17 blocos) do spawn de um time é do time mais próximo. Só esse time abre enquanto está na partida; depois que o time é eliminado, qualquer um abre (`Este baú é do Time ...`). Baús longe das ilhas são de todos. Espectadores não abrem baú.
- **Bater para guardar:** os baús das ilhas e os baús do fim mostram `&eBata para` / `&eguardar!` (`messages.chest-hologram`) para quem pode usar: todo baú do fim para todo jogador vivo, o baú de uma ilha para o time dela (ou para todos depois que esse time sai). Bater num deles com um item guarda a pilha toda (o que couber) e diz `Você guardou: <item> (<n>x) em seu baú.`; o baú do fim guarda no `Baú do Fim` do próprio jogador. Um baú que o jogador não pode abrir diz `Este baú é do Time ...` e o item fica com ele. Picaretas, machados, tesoura e a espada de madeira não podem ser guardados (`Você não pode guardar este item.`, `messages.chest-kept`) nem dropados, e a única espada do inventário também não pode ser guardada (`Você não pode guardar só um deste item.`, `messages.chest-last-one`). As mesmas regras valem ao mover itens para um baú da ilha ou para o `Baú do Fim` clicando, com shift, com as teclas numéricas ou arrastando. A preferência `Guardar itens clicando no baú` desliga os hologramas ou a função inteira.
- **Temporizador da TNT:** uma TNT acesa mostra os segundos que faltam acima dela, atualizados a cada tick (`2,95s`, duas casas com vírgula), verde-escuro, verde, amarelo e vermelho conforme o pavio queima (a partir de 90%, 60% e 30% restantes; `tnt-timer.*`), para quem está a até 24 blocos e deixou `Mostrar temporizador da TNT` ligado.
- **Nomes dos itens:** todo item que a partida entrega tem o nome em português (`Barra de Ferro`, `Bola de Fogo`, `Estilingue`, `Espada de Madeira`...), de `item-names` ou, para itens da loja, o nome da loja.
- **Baú do fim:** todo baú do fim abre o `Baú do Fim` do próprio jogador, o mesmo em qualquer baú do fim do mapa, mantido entre as mortes. Quando o jogador é eliminado, os itens dele caem no primeiro gerador de ferro da ilha do time (ou no spawn, se não houver).
- **Desconexão:** quem sai com a cama de pé pode voltar em até 3 minutos e renasce 5 segundos depois (`reconectou`). O `/l` leva o jogador para o lobby de BedWars e avisa antes `Você desconectou de uma partida...`. O `/retornar`, em qualquer servidor, manda o jogador de volta para essa partida: o servidor da partida guarda `match:return:<jogador>` no `MuxeCore.store()` enquanto o jogador pode voltar e apaga quando ele volta, perde a cama, o tempo acaba ou a sala fecha.
- **Invisibilidade:** a poção de invisibilidade não mostra partículas. Enquanto dura, inimigos e espectadores não veem a armadura do jogador (a armadura de verdade continua nele, então os golpes são reduzidos como sempre), nem o nome, só as pegadas que ele deixa no chão a cada 0,8 bloco andado. Os companheiros de time veem o jogador meio transparente, com a armadura. Sair sem cama elimina o jogador, dando a kill ao último golpe ou a quem quebrou a cama.

### Cor preferida

Cada jogador pode escolher uma cor de time preferida em `Cosméticos do Bed Wars` (botão `Cor preferida`, slot 35, que abre `Cor preferida (1/1)`). O menu tem `Nenhuma` (barreira, slot 10) e uma lã por cor de time, nesta ordem: `Vermelho`, `Azul`, `Verde`, `Amarelo`, `Ciano`, `Branco` (slots 11 a 16), `Laranja`, `Rosa` e `Cinza` (slots 19 a 21). Todas as opções dizem `Clique para selecionar.`; a escolhida (`Nenhuma` enquanto nada foi escolhido) diz `Selecionada!` e brilha. Clicar em uma opção salva, avisa `Você selecionou <nome>.` (`messages.cosmetic-selected`) e atualiza o menu; clicar na que já está escolhida não faz nada. `Voltar` (slot 48) volta ao menu de cosméticos. Por enquanto o botão está liberado para todos, embora o lore ainda diga `Exclusivo para ULTRA`.

A escolha é salva no PostgreSQL (`bedwars.favourite_team_color`, uma linha por jogador; escolher `Nenhuma` apaga a linha) e carregada na memória quando o jogador entra num servidor de BedWars, então a sala nunca espera o banco quando a partida começa.

Quando a partida começa, o `TeamAssigner` coloca os jogadores no time preferido quando consegue:

1. Primeiro as parties, como sempre, e uma party nunca é dividida. Se a maioria dos membros da party prefere a mesma cor (a primeira cor vence um empate) e esse time da arena ainda está vazio, a party inteira fica nele; caso contrário vai para o time mais vazio. Uma party maior que um time começa no time preferido e transborda para os outros.
2. Depois os jogadores sem party que têm cor preferida, cor por cor: os que preferem uma cor que é time da arena e ainda tem vagas ocupam as vagas. Quando mais jogadores querem uma cor do que ela tem vagas (time de 1 no solo, 2 em duplas, 4 em quartetos), um sorteio aleatório decide quem fica com as vagas e os outros seguem para o passo 3. Em modos de time, jogadores sem party que preferem a mesma cor acabam jogando juntos.
3. Todos os outros vão para o time com menos jogadores, na ordem de entrada, como antes.

Uma cor que a arena não tem, ou um jogador cuja preferência não pôde ser carregada, conta como sem preferência. Sem ninguém com preferência, os times são exatamente os da ordem de entrada.

### Loja

O NPC da loja abre os menus dos exports: Compra Rápida (o layout do export) e as sete categorias, com a aba da categoria aberta marcada por um vidro verde.

- Um item custa ferro, ouro ou esmeraldas, tirados primeiro do inventário, depois do `Baú do Fim` do jogador e depois dos baús da ilha do time, então 5 ouros na mão e 7 no baú compram uma armadura de 12 ouros. As melhorias custam diamantes dos mesmos lugares e também do inventário dos companheiros vivos; nenhum outro recurso é dividido. O nome fica vermelho quando nem tudo isso basta, com `Você não tem <recurso> o suficiente.`
- Tentar comprar o que o jogador já tem e não pode comprar de novo (armadura já vestida, a tesoura, uma ferramenta no último nível) diz `Você já tem este item.` (`messages.already-have`). Blocos, espadas e os outros itens podem ser comprados de novo.
- Cada compra diz `Você comprou <item>.` e toca `NOTE_PLING` (`sounds.bought`); comprar uma melhoria toca `LEVEL_UP` com tom 2 (`sounds.upgrade`); guardar o item da mão batendo no baú do time toca `CHICKEN_EGG_POP` com tom 1,5 (`sounds.deposited`); quebrar uma cama toca `ENDERDRAGON_GROWL` para quem quebrou (`sounds.bed-destroyed`) e `WITHER_DEATH` para todos do time que perdeu a cama (`sounds.bed-lost`); uma kill toca `NOTE_PLING` com tom 2 para quem matou (`sounds.kill`, `sounds.final-kill` numa kill final) e `ZOMBIE_UNFECT` com tom 2 para quem foi eliminado (`sounds.eliminated`); um time eliminado toca `EXPLODE` para a sala toda (`sounds.team-eliminated`); o fim da partida, com vitória ou derrota, toca `ENDERDRAGON_GROWL` e `FIREWORK_LAUNCH` para a sala toda (`sounds.match-end`, `sounds.match-end-firework`); uma compra sem recursos suficientes toca `VILLAGER_NO` (`sounds.cannot-afford`).
- **Compra rápida:** cada jogador tem a sua, salva no PostgreSQL (`bedwars.quick_buy`) e carregada quando ele entra num servidor de BedWars, então uma mudança feita numa partida vale na próxima. Quem nunca mudou recebe `shop.quick-buy`. Ela tem 21 posições; uma posição vazia mostra `&cEspaço vazio!`.
  - Shift-click num item que já está na compra rápida tira ele, pela página Compra Rápida ou pela categoria dele.
  - Shift-click em qualquer outro item abre `Adicionando à Compra Rápida...`: o item em cima e as 21 posições embaixo, cada uma com `Clique para substituir!` ou `Espaço vazio! Clique para adicionar!`. Clicar numa posição coloca o item lá (e tira de onde ele estava) e abre a Compra Rápida de novo.
  - Itens que a loja não vende mais aparecem como posições vazias.
- A lã vem na cor do time. Uma espada substitui a de madeira. A armadura dá bota e calça e não pode ser comprada abaixo da que o jogador tem. A picareta e o machado sobem um nível por compra. A picareta tem cinco níveis: madeira (Eficiência I, 10 ferros), pedra (Eficiência I, 10 ferros), ferro (Eficiência II, 10 ferros), ouro (Eficiência III, 3 ouros) e diamante (Eficiência III, 6 ouros). A tesoura é permanente. O que o jogador já tem mostra `Já adquirido.`.
- **Editor de hotbar:** cada jogador define para onde vão os itens comprados na hotbar, salvo no PostgreSQL (`bedwars.hotbar_layout`) e carregado quando ele entra num servidor de BedWars. Abre pelo `Editor de hotbar` do Menu do Bed Wars (slot 31) e pelo botão `&aEditor de hotbar` da página Compra Rápida da loja (slot 53, `shop.hotbar-editor`); `Voltar` retorna ao lugar de onde foi aberto. Quem nunca editou tem o layout vazio e nada muda: o item comprado entra no primeiro espaço livre, como antes.
  - `Editando hotbar...` (3 linhas) mostra os 9 slots da hotbar nos slots 9 a 17 como `&aSlot #N`. Um slot com entradas mostra o ícone de uma delas e lista todas em `Selecionado:` ou `Selecionados:`, a que aparece em verde; com mais de uma entrada, o ícone e a linha verde passam para a próxima entrada a cada segundo (`shop.hotbar-menus.frame-ticks`, 20). Um slot vazio é um painel vermelho. Botão esquerdo adiciona, botão direito remove.
  - `Adicionado no slot #N...` (6 linhas) tem as abas `Categorias` e `Itens` nos slots 0 e 1, a aberta marcada por um painel verde embaixo. Categorias lista todas as categorias da loja menos `armadura`; Itens lista todos os itens que não são armadura com o visual da loja, 21 por página (setas `Página N` nos slots 48 e 50, a seta da página anterior no lugar de `Voltar`). Clicar em um adiciona ao fim do slot, salva e volta ao editor. Uma entrada fica em um slot só: adicioná-la em outro slot a move. Um slot guarda no máximo 14 entradas; um slot cheio responde `messages.hotbar-slot-full`.
  - `Removendo do slot #N...` (4 linhas) lista as entradas do slot a partir do slot 10. Clicar em uma remove e salva; o menu continua aberto enquanto sobrarem entradas e volta ao editor quando a última sai.
  - **Regra de colocação:** quando uma compra entrega um item, os slots são vistos do #1 ao #9; o primeiro slot que lista o item ou uma categoria dele e está vazio, ou tem uma pilha do mesmo item com espaço para a compra inteira, recebe o item. Se não houver nenhum, o item é adicionado como antes. Armadura não é afetada. A regra é `HotbarPlacement` em `br.com.muxe.minigame.bedwars.hotbar.domain`; `HotbarDelivery` aplica no inventário. A checagem de "inventário cheio" da compra não mudou.
  - Entradas que a loja não vende mais são descartadas quando o layout é carregado.
- Ainda não funcionam: os itens especiais Golem de Ferro, Leite Mágico, Torre Compacta, Ovo das Pontes, e Rastreador, que avisam que chegam em breve e não cobram nada.

### Melhorias do time

O NPC das melhorias abre `Melhorias do Time` com as sete melhorias dos exports, pagas com diamantes:

| Melhoria | Efeito |
|---|---|
| Afiação nas Espadas I–II | Afiação em todas as espadas e machados do time. |
| Reforço na Armadura I–IV | Proteção em todas as peças de armadura. |
| Sem tempo Irmão I–II | Pressa permanente. |
| Forja I–IV | Ferro e ouro da ilha 1,5×, 2×, um gerador de esmeralda na ilha, depois 3× e esmeraldas em dobro. |
| Regeneração na Ilha | Regeneração para o time a até 17 blocos do spawn. |
| É uma Cilada! | O próximo inimigo a até 17 blocos de um time com cama recebe Cegueira e Lentidão por 8 s. Usada uma vez. |
| Fadiga I–II | O próximo inimigo recebe Fadiga por 7 ou 10 s. Usada uma vez. |

O time inteiro vê `<jogador> adquiriu a melhoria <melhoria>.`

### Cosméticos de destruição de cama

O jogador escolhe uma animação que toca quando ele quebra uma cama inimiga. Os menus seguem os exports gravados:

- **Menu do Bed Wars** (esmeralda): na sala de espera dos servidores de BedWars e no slot 3 da hotbar do lobby de BedWars. Cinco linhas, como no export de referência: `Missões` (slot 11), `Cosméticos` (baiacu, slot 13), `Lista de Níveis` (slot 15, com as insígnias de nível na descrição), `Compra rápida` (29), `Editor de hotbar` (31) e `Preferências` (33). `Editor de hotbar` abre o editor de hotbar descrito na seção Loja.
- **Preferências** (slot 33) abre `Preferências do Bed Wars` (4 linhas): `Mostrar nível no chat` (slot 11), `Mostrar temporizador da TNT` (13) e `Guardar itens clicando no baú` (15: `Ativado`, `Sem hologramas`, `Desativado`), cada um com um corante embaixo mostrando o valor (verde-limão ligado, cinza desligado, azul-claro sem hologramas). Clicar em qualquer um troca o valor; `Voltar` volta ao menu do Bed Wars. Elas ficam salvas em `bedwars.preferences` e são carregadas em todo servidor de BedWars.
- **Compra rápida** (slot 29 do menu do Bed Wars) abre `Editando compra rápida...` com as 21 posições do jogador. Botão esquerdo troca o item, botão direito tira, `Voltar` volta para o menu do Bed Wars e `Importar compra rápida` abre uma bigorna com `Insira o usuário`: o nick digitado e retirado do slot de resultado abre `Importando compra rápida...` com a compra rápida desse jogador (a padrão se ele nunca mudou, todas as posições vazias se o jogador não existe), `Cancelar` (volta ao editor) e `Confirmar`, que copia ela para a do jogador. Trocar abre `Selecionando item... (1/2)`, todos os itens que a loja vende na ordem das categorias, 21 por página; clicar num deles coloca ele naquela posição. Picareta e machado aparecem como `Picareta` e `Machado` de diamante, e `editor-name`, `editor-material` e `editor-description` de um item da loja mudam como ele aparece aqui. As mudanças são salvas como as feitas numa partida.
- **Cosméticos do Bed Wars**: a categoria **Destruição de Cama** mostra `possuídos/total (porcentagem%)` e o cosmético selecionado. **Cor preferida** (slot 35) abre o menu de cor de time preferida descrito em Cor preferida. **Tipos de madeira** e **Mensagens de kill** (slots 29 e 31) abrem as listas descritas em Tipos de madeira e Mensagens de kill. As outras categorias e o botão **Loja** respondem "em breve".
- **Destruição de Cama (página/páginas)**: 28 itens por página. A página 1 começa com **Nenhuma** e **Aleatório** (o padrão). O ícone do **Aleatório** troca a cada 10 ticks pelos ícones dos cosméticos que ele pode sortear (os possuídos com animação), mantendo o nome e a descrição; sem nenhum, ele fica com o próprio ícone. Primeiro vêm os cosméticos possuídos em ordem alfabética, depois todos os outros em ordem alfabética (`CosmeticOrder`, a regra de todo menu de cosméticos da rede).
  - Possuído: botão esquerdo seleciona, botão direito visualiza. O selecionado brilha e mostra `Já selecionado.`
  - À venda: botão esquerdo compra com créditos (preço por estrelas: 750, 900, 1.300, 1.750, 2.000), botão direito visualiza. Quando faltam créditos, a lore mostra `Faltam <n> créditos!`.
  - Indisponível (cosméticos de evento que o jogador não tem, ou cosméticos ainda sem animação): botão direito visualiza quando existe animação.
  - Os cosméticos `Incluso no ULTRA` são de todos que têm `muxe.cosmetic.ultra`.
  - Quem tem `muxe.cosmetic.all` (ops, por padrão) possui todos os cosméticos. Os que não têm animação continuam `Indisponível`.
  - Na prévia, acertar a cama a tira de verdade do mundo da prévia (assim dá para andar onde ela estava sem ficar preso) até o último jogador que a quebrou voltar; enquanto isso, os outros na prévia continuam vendo a cama como um bloco falso por um `BlockMask`, e também podem quebrá-la.
- São 31 cosméticos e todos têm animação (Ajudante de Papai Noel, Avalanche, Coelhos, Passarinhos, Friaca, Fogos de Artifício, Colheita de Abóboras, Apavorante, Deserto, Tinta, Bola de Canhão, Enderman, Sal na Ferida, Espiral de Coelhos, Six Seven, Ghor, Raio, Tesouro Encontrado, Garota Mágica, Quarto Abandonado, Tornado, Tubarão, Vulcânica, Carro, Anoitecer, Assombrado, Balões, Chuva de Bolos, Encantado, Explosão da Vitória, Kraken). Na Espiral de Coelhos a cama e uma abóbora iluminada sobem cada vez mais rápido enquanto três coelhos giram em volta a dois blocos, deixando nuvens e soltando cabeças de ovo de Páscoa que caem até o chão. No Six Seven um `6` e um `7` em negrito flutuam sobre a cama, que quica, e balançam como gangorra cada vez mais rápido, com uma nota a cada batida, até se juntarem a um `67`, explodirem num fogo de artifício branco e choverem catorze `6` e `7` pela ilha. No Ghor uma fenda roxa se abre cinco blocos depois da cabeceira da cama, virada para ela, e dela sai um olho gigante feito de um cubo 3×3×3 de cabeças, babando lava, que encara a cama, depois fuzila com o olhar quem a quebrou e volta faminto para a cama com um rugido (as nove cabeças da frente trocam de skin a cada expressão). Quatro tentáculos de dezoito cabeças saem um depois do outro e agarram os cantos da cama, sacodem e a puxam cada vez mais rápido para o olho, que a come com um estalo, mastiga, arrota, pisca e volta de ré para a fenda ainda encarando quem quebrou a cama; a fenda então se fecha com um chiado, uns seis segundos depois da cama quebrar. No Raio três raios atingem a cabeceira da cama (na quebra e cerca de um segundo e um segundo e meio depois), cada um com um som de explosão, enquanto uma explosão enorme e um anel de chamas saem da cama. No Tesouro Encontrado a cama afunda no chão enquanto o piso em volta vira arenito e um X de lã vermelha marca o lugar; uma pá de diamante cai assobiando do céu, girando, e se crava com uma explosão que lança dez diamantes e dez barras de ouro, que tilintam e brilham onde caem, e a pá é arrancada com um puf três segundos depois da quebra. Na Garota Mágica um coração rosa de brilhos é desenhado sobre a cama a partir da ponta, virado para quem a quebrou; um fogo de artifício rosa sobe dele e explode, e oito cabeças de estrela caem do céu uma depois da outra com notas cada vez mais graves, girando e deixando rastros de arco-íris, pousam na cama e somem com um chiado. No Quarto Abandonado dezesseis morcegos saem da cama numa nuvem de fumaça e voam para cima e em volta da ilha, guinchando, até sumirem todos juntos em menos de três segundos. No Tornado um funil de nuvens e poeira cresce da cama enquanto as duas metades dela giram uma em volta da outra e sobem no vento; depois de uns dois segundos a cama dispara para o céu com um zunido enquanto o funil se desfaz, e uns trinta blocos acima explode em lã vermelha e um fogo de artifício verde. No Tubarão um fogo de artifício azul estoura e quatro tubarões saltam da cama em leque na direção de quem a quebrou, espirrando água, mergulham de volta no chão e se afastam nadando só com as barbatanas de fora, balançando e espirrando, até sumirem em nuvens uns três segundos depois da quebra. Na Vulcânica uma explosão joga a cama quatro blocos e meio para cima sobre um jato de chamas e lava; ela balança na erupção com som de lava borbulhando até esfriar com um chiado e sumir uns quatro segundos depois da quebra. No Carro a cama sobe do chão com uma abóbora iluminada na cabeceira e quatro rodas de cabeça que giram soltando fumaça, e sai em disparada para o lado oposto à cabeceira, freando uns 28 blocos depois, menos de um segundo após a cama quebrar. No Anoitecer o céu pisca para noite por um momento e volta a ser dia quando a lua se desfaz, enquanto uma lua crescente amarela brilha três blocos acima da cama e uma coruja chega voando de lado, pousa na cama e a leva para cima e para a direita, soltando penas, até sumir numa nuvem. A lua então se desfaz em faíscas. No Assombrado 36 ossos saltam da cama, e cada um que cai transforma o bloco do chão embaixo dele em quartzo, lã branca ou diorito. Três fantasmas sobem da cama um depois do outro, vagam em volta dela deixando nuvens e somem com um chiado. Em Balões 25 balões amarrados a um nó ao lado da cama levam a cama e a abóbora para cima, cada vez mais rápido, balançando de leve, até sumirem de vista. Na Chuva de Bolos seis bolos são lançados da cama em pares, a sessenta graus um do outro, deixando um rastro branco e vermelho, e caem em volta dela entre itens de açúcar e bolo. Depois faíscas se espalham rente ao chão. No Encantado um girassol gigante cai de doze blocos de altura sobre a cama, soltando pétalas. A cama vira folhas, primeiro o pé e depois a cabeceira, e explode num fogo de artifício verde, pétalas e 24 flores. Na Explosão da Vitória a cama se estilhaça como ouro. Dezessete girassóis voam em círculo, cada estalo mais agudo que o anterior, enquanto dez troféus são lançados em volta, girando uma vez antes de pousar no chão. No Kraken (cinco estrelas, à venda; a cabeça do kraken é só o ícone do menu) a cama quebrada flutua num mar que cresce em volta dela, com as duas metades balançando alternadas entre respingos, névoa e som de água. Cerca de um segundo depois da quebra quatro tentáculos de lã roxa, magenta e rosa saem do chão em volta da cama um depois do outro, cada um com um estalo e um respingo forte, e balançam pingando água, curvados para a sua metade da cama. Depois se enrolam e batem na cama, o kraken ruge espirrando tinta, e eles a apertam três vezes com um estalo de madeira, afundando-a um pouco a cada vez, até que um último rugido e uma explosão de água arrastam a cama e depois os tentáculos para baixo, deixando dois anéis de ondas, uns seis segundos depois da quebra.
- As animações são feitas só com pacotes (armor stands, falling blocks, itens, fogos, partículas, sons e blocos falsos), então o mundo real nunca muda. São reproduções nossas do comportamento gravado, com sprites originais e cabeças da comunidade.

**Na partida** a seleção de quem quebrou toca na cama quebrada para todos da sala. `Aleatório` sorteia uma animação que o jogador possui; `Nenhuma` não toca nada.

**Prévia** (só no lobby de BedWars; em outro lugar o jogador recebe `Você só pode visualizar cosméticos no lobby.`):

1. O jogador é salvo (lugar, inventário, armadura, modo de jogo, scoreboard) e teleportado para o mundo da prévia, com o inventário vazio, exceto pela cama `&aVoltar ao Lobby` no slot 9 (`preview.back-item`). Clicar com o botão direito nela encerra a prévia na hora.
2. Quem está em prévia não vê os outros em prévia. A scoreboard mostra `Visualizando:` e o nome do cosmético, e a action bar repete `&eQuebre a cama!`.
3. Ao bater na cama, ela some só para esse jogador e a animação toca só para ele.
4. `preview.return-delay` depois do fim da animação (2 s), ou depois de `preview.timeout` sem quebrar a cama (2 min), o jogador volta para onde estava, com os itens do lobby, e o menu de cosméticos reabre na página em que ele estava. Sair do servidor durante a prévia não abre nada.

### Montando as prévias no jogo

A equipe posiciona as cinco prévias de dentro do lobby de BedWars com `/bwconfig previa <tipo> <ação> [valor]` (permissão `muxe.admin.config`), sem editar o `config.yml`. As pastas dos mundos já precisam estar na raiz do servidor.

| `<tipo>` | Seção | Pasta do mundo |
|---|---|---|
| `cama` | `preview` | `previa_quebra_de_cama` |
| `vitoria` | `victory-preview` | `previa_efeito_de_vitoria` |
| `kill-final` | `final-kill-preview` | `previa_kill_final` |
| `aura` | `aura-preview` | `previa_aura` |
| `rastro` | `projectile-trail-preview` | `previa_aura` |

| Comando | Grava |
|---|---|
| `previa <tipo> spawn [--centro]` | `<seção>.world` com o mundo em que você está e `<seção>.spawn` (`x`, `y`, `z`, `yaw`, `pitch`) com onde você está: coordenadas em centésimos, ângulos em décimos; `--centro` põe `x` e `z` no meio do bloco. |
| `previa cama cama` | `preview.bed.foot` e `preview.bed.head` a partir do bloco de cama para o qual você olha (até 6 blocos). Qualquer metade pode ser a que você olha: a outra é achada pelos dados do bloco. Se não for uma cama, nada muda e a resposta pede para olhar para uma. |
| `previa kill-final distancia <n>` | `final-kill-preview.target.distance`, quantos blocos à frente o alvo fica (acima de 0, até 64; `4,5` é aceito). |
| `previa <tipo> tempo <duração>` | `<seção>.timeout`, como `60s` ou `2m` (no mínimo `1s`). |

Cada atalho salva o arquivo e aplica a mudança ao vivo: cada prévia lê as suas configurações por um `LiveValue` que a mudança troca, e carrega o novo mundo quando ele é diferente. Um jogador que já está numa prévia mantém as configurações e o mundo com que começou e o próximo recebe os novos; uma cama que um espectador quebrou é recolocada no mundo de onde foi tirada. Se a pasta do novo mundo não existir, a resposta avisa e a prévia mantém as configurações anteriores até a pasta existir e o `/bwconfig recarregar` ser usado. Nos servidores de partida, onde as prévias não existem, os atalhos só salvam e a resposta diz que a mudança vale depois de reiniciar. Os subcomandos genéricos alcançam as outras chaves das mesmas seções (`/bwconfig definir victory-preview.countdown-ticks 15`, `/bwconfig ver aura-preview`).

```text
/bwconfig previa kill-final spawn
/bwconfig previa cama cama
/bwconfig previa kill-final distancia 5
/bwconfig previa aura tempo 90s
```

### Efeitos de kill final

`Kill Final` (slot 12 de `Cosméticos do Bed Wars`) lista os 43 efeitos de kill final dos exports em `Kill Final ({page}/{pages})`, duas páginas, na categoria `bedwars-kill-final`, com as mesmas regras das outras categorias. Todos eles têm animação. Todo efeito de cena (vitória e kill final) começa no chão abaixo de onde aconteceu (`Ground.surface`, até 32 blocos para baixo), então toca igual em qualquer mapa e em qualquer altura; um efeito de kill final também sabe a direção do golpe (de quem matou para a vítima), a skin da vítima e a cor de lã do time da vítima (`Scene.dye`, de `TeamColor.woolData`; nenhuma na prévia, cujo alvo não tem time).

- **Na partida:** quando um jogador faz uma kill final (fora do void), o seu efeito de kill final selecionado (ou um aleatório que ele tem) toca onde a vítima morreu, visto pela sala toda.
- **Presenteado:** um presente grande sobe 5 blocos da vítima por 26 ticks com pó vermelho e faíscas e um pling e um estouro de fogos subindo de tom a cada 2 ticks; depois ele explode, um fogo vermelho e branco estoura e oito presentes voam juntos em oito direções e alturas diferentes, sumindo 3 segundos depois.
- **Presentes:** um fogo em estrela vermelho, verde e branco estoura na vítima e oito presentes saem um a cada 2 ticks em oito direções, girando uns 8 blocos para cima e caindo a uns 3 blocos. Eles somem uns 4 segundos depois.
- **Passarinhos:** quatro passarinhos dão uma volta e meia em volta da vítima a 1 bloco, em alturas diferentes e com sons de morcego, depois abrem para 4 blocos subindo mais 2 e estouram em pó de lã (vermelho, azul-claro, amarelo, azul-claro).
- **Brain Freeze:** a cabeça da vítima, dentro de um bloco de gelo, voa para trás do golpe (para longe de quem matou) e para cima por 28 ticks, soltando gelo e com um pling subindo de tom, e então se estilhaça com uma explosão e vidro quebrando.
- **Afogar:** uma lula cai para dentro do chão onde a vítima morreu, com um splash, um jato de gotas d'água e pó de lápis, enquanto uma onda azul e branca de 7 linhas sobe na frente de quem matou, duas linhas por tick, e balança por 2 segundos com sons de água cada vez mais graves, terminando com um splash grave.
- **Anoitecer:** o corpo da vítima fica de pé enquanto uma coruja plana vindo da esquerda de quem matou, desacelerando, agarra o corpo com um pio e uma fumaça e o leva para cima e para a direita cada vez mais rápido, deixando um rastro de penas e bater de asas, até os dois sumirem numa nuvem com um estalo.
- **Apavorante:** um fantasma grande e três pequenos saem do chão; o grande sobe três blocos em espiral balançando a cabeça enquanto os pequenos giram em volta deixando rastros em tons pastel, com sininhos e um grito de ghast, e eles explodem num fogo de artifício pastel que espalha pó de glowstone e açúcar e deixa uma nuvem pastel descendo devagar.
- **Arco-íris:** duas levas de catorze lãs nas cores do arco-íris saltam em volta da vítima com o som de subir de nível, enquanto brilhos que passam por todas as cores enchem o lugar por cerca de 2,5 segundos; as lãs ficam no chão por 5 segundos.
- **Biscoitos e Leite:** um fogo de artifício estrela laranja estoura e quinze baldes de leite e biscoitos voam um por tick em espiral, cada um com uma nota mais aguda, e se desfazem em pó branco ou laranja meio segundo depois de cair, com um estalo e uma explosão.
- **Caçador de Recompensas:** um fogo verde estoura na vítima com um som de experiência, e seis cabeças de recompensa (de dois tipos, alternadas) saltam em seis direções iguais a uns dois blocos de altura, caem a um bloco de distância e ficam no chão por quase 3 segundos.
- **Colheita de Abóboras:** uma abóbora iluminada desenhada em pó colorido aparece acima da vítima virada para quem matou, cresce e começa a tremer e soltar chamas com um pling subindo de tom, e então estoura: os pixels se espalham e somem, doze sementes de abóbora voam em círculo e dez abóboras, abóboras iluminadas e tortas de abóbora são lançadas para cima uma a uma com um plop subindo de tom, cada uma caindo com uma nuvem de pó de abóbora; tudo some uns 4 segundos depois.
- **Colmeia:** um fogo cor de mel estoura e uma colmeia aparece sobre a vítima virada para quem matou; dez abelhas saem dela uma depois da outra, sobem e descem voando em volta com um zumbido, e cada uma estoura numa nuvem depois de 2 segundos, e então a colmeia também some numa nuvem.
- **Decapitado:** a cabeça da vítima sobe 5 blocos em um segundo e meio, girando e sangrando (partículas de redstone) com um pling subindo de tom, e então explode.
- **Decolando:** o corpo da vítima, com a skin dela, decola como um foguete: dispara para longe de quem matou e para cima (6 blocos para trás e 2,5 para cima a cada tick) deixando faíscas de fogos, até se ouvir o estouro de um fogo de artifício.
- **Deserto:** o chão sob a vítima vira areia movediça e uma figura com a cabeça dela e os braços para cima afunda balançando, até ser engolida com um arroto. Enquanto isso sete cactos saltam da areia e somem numa nuvem, e um sol sorridente de óculos escuros e raios girando brilha 4 blocos acima, derramando brilhos até o chão.
- **Dragões Chineses:** um fogo de artifício vermelho e dourado estoura sobre a vítima com um rugido agudo de dragão, e cinco cabeças de dragão saem voando dela em cinco direções, subindo de leve e deixando um rastro de pó vermelho que primeiro sobe reto. Depois de 2 segundos elas param e somem em faíscas de lava.
- **Eletrizante:** um corpo com a skin da vítima fica onde ela morreu, com um esqueleto escondido dentro. Por 31 ticks os dois se alternam a cada tick com um som de dano zumbindo, como num choque, enquanto cinco raios silenciosos caem sobre a cabeça e críticos estouram a cada 5 ticks. Termina no esqueleto, que morre com uma explosão; tudo some 3 segundos depois da kill.
- **Embrulhado:** um fogo verde e amarelo estoura e o corpo da vítima afunda 2 blocos dentro de um presente aberto aos seus pés. 14 ticks depois o presente se fecha com um som de armadura e some com um estalo uns 3 segundos depois da kill.
- **Encantado:** um fogo coral claro estoura e doze folhas saltam para cima, cada uma virando uma nuvenzinha ao cair. Depois nove cogumelos brotam do chão em volta da vítima, um a cada 2 ticks, com estalos subindo de tom; cada um pula e gira, e some numa nuvem cerca de um segundo depois.
- **Esquelético:** a cabeça da vítima desce da altura da cabeça até o chão, girando uma volta em 28 ticks e soltando um osso ou farinha de osso a cada 2 ticks com um grito de esqueleto subindo de tom. Depois ela explode e 20 ossos voam para todos os lados.
- **Esqueleto Amaldiçoado:** com um grito de blaze, um fogo preto e um raio, um círculo preto pulsa a 2 blocos da vítima por mais de 2 segundos. Dezesseis ossos são jogados em volta num círculo girando e quatro crânios de esqueleto em quatro direções, outro crânio cai de cima e um grito de morte de esqueleto fecha o efeito.
- **Explosão de Abóbora:** um fogo de artifício laranja estoura sobre a vítima e solta cinco tortas de abóbora e três abóboras, que ficam no chão por cinco segundos.
- **Explosão de Melancia:** um fogo de artifício verde e vermelho estoura sobre a vítima com uma explosão e um ploc, e oito melancias e fatias de melancia voam em roda e ficam por cinco segundos.
- **Explosão de TNT:** um fogo de artifício vermelho estoura e três TNTs acesas são lançadas da vítima, com cinco ticks de intervalo, cada uma explodindo cerca de um segundo depois.
- **Fantasmas:** uma nuvem de fumaça sai da vítima e três fantasmas aparecem um depois do outro com um guincho de morcego, vagam em volta da vítima por mais de dois segundos deixando um rastro de nuvens e somem com um chiado.
- **Festa Disco:** um globo de discoteca gira acima da vítima por 4 segundos ao som de uma batida de baixo e harpa, notas giram em volta do local e dez discos de música são lançados; termina com dois fogos de artifício e uma explosão.
- **Fogos de Artifício:** um fogo de artifício verde estoura logo acima de onde a vítima morreu.
- **Galático:** um fogo de artifício estoura na vítima e doze estrelas do Nether jorram em círculo com um sino subindo de tom, terminando com o som de subir de nível.
- **Garota Mágica:** um arco-íris nasce da vítima e se curva para a esquerda de quem matou enquanto dez estrelas sobem girando com rastros dourados brilhantes e caem em volta do local.
- **Incendiar:** com um trovão e uma explosão, chamas se espalham, um esqueleto afunda no chão chacoalhando cada vez mais grave e uma chama de pixels queima onde a vítima estava até um som de bola de fogo apagá-la.
- **Jardim Florido:** um fogo rosa estoura um bloco acima da vítima e treze tulipas rosas voam por igual para todos os lados, ficando no chão por uns 5 segundos.
- **Lula Explosiva:** uma lula dispara para cima a partir da vítima, cada vez mais rápido, espirrando água com um pling subindo de tom, e a uns 9 blocos de altura explode em fumaça e solta doze bolsas de tinta, que ficam por uns três segundos.
- **Moedas:** sessenta moedas de ouro (girassóis) sobem em espiral três blocos em volta da vítima, duas por tick com pó dourado e o som de orbe, cada uma jogada para fora. Elas somem 5 segundos depois da kill.
- **Passe de Mágica:** uma cartola preta aparece onde a vítima morreu e, com uma nuvem, um coelho branco pula dela. Ele saltita para longe de quem matou, uns cinco blocos por pulo, a cada 13 ticks, e a cartola e o coelho somem depois de uns 3 segundos.
- **Pinguins na Neve:** com um passo na neve, peixes e cabeças de pinguim saltam da vítima, enquanto sessenta bolas de neve sobem de uma espiral de nuvens que se abre, cada uma com um plop e umas cacarejadas. Tudo some depois de uns 5 segundos.
- **Portal do Fim:** com o grito de um enderman, um anel roxo de bruxa se abre em volta da vítima, o chão se estilhaça como um olho do fim a cada 7 ticks com um redemoinho de portal, e vinte olhos do fim aparecem flutuando em volta da vítima com sons de portal. Um enderman resmunga e os olhos somem depois de uns 6 segundos.
- **Quebrado:** a vítima se estilhaça em vidro tingido da cor do seu time (azul quando não há time, como na prévia): cacos explodem dos pés à cabeça, mais densos na cintura, com o som de vidro quebrando.
- **Raio:** um raio cai no bloco onde a vítima morreu, com trovão e uma explosão, e um anel de 50 chamas se espalha para fora a partir da cintura da vítima.
- **Redstone:** a vítima se desfaz em cacos de bloco de redstone dos pés à cabeça, com cinco sons de pedra quebrando.
- **Six Seven:** um dançarino com a cabeça da vítima aparece onde ela morreu, virado para quem matou, pesando um **6** e um **7** nas mãos e trocando o braço levantado a cada 6 ticks no ritmo de uma batida. Nas três últimas batidas ele chia, pisca vermelho e encolhe duas vezes, e então explode (tick 35). Dez 6 e 7 jorram um por tick e caem em volta, sumindo cerca de 4 segundos depois da kill.
- **Tinta:** tinta respinga na vítima (pó de lã e um anel de tinta crescendo) e quatro bolhas de tinta (verde, amarela, azul, vermelha) são lançadas uma a cada 2 ticks, cada uma com um jato de tinta e um rastro. Quando cada uma desce, um fogo de artifício em estrela da sua cor estoura na vítima e um graveto salta, enquanto uma estrela de tinta cresce e balança acima da vítima, terminando em um fogo de artifício em estrela de quatro cores com brilho. O chão não é pintado; os gravetos somem cerca de 3 segundos depois da kill.
- **Toque de Midas:** um bloco de ouro cai de 6 blocos sobre a vítima e se espatifa numa nuvem de pó de ouro; depois 15 girassóis saltam um por tick com o tom subindo e ficam no chão até 5 segundos depois da kill.
- **Vanilla:** a morte normal: uma cópia da vítima, com a skin dela e virada para quem matou, aparece sem vida, então o cliente a tomba e a deixa vermelha; ela é jogada cerca de 2 blocos para longe de quem matou, caindo 6 ticks depois, o cliente a desfaz em fumaça após 20 ticks e ela é removida no tick 29. Sem som nem partícula extra.
- **Encantado:** os gnomos vêm comemorar e trazem a floresta junto, em volta do ponto 7 blocos à frente do vencedor (para onde ele olha quando a partida acaba). A cada 2 ticks, por 6 segundos, um cogumelo vermelho ou marrom brota no bloco do topo de uma coluna sorteada a até 17 blocos desse ponto, com um pouco de terra e um estalo. Dez cogumelos gigantes, um a cada 8 ticks, crescem a até 17 blocos também: um caule de 4 a 6 blocos de altura, um bloco 2 ticks depois do primeiro e depois um a cada 3 ticks, com um farfalhar cada vez mais agudo e brilhos verdes, e depois o chapéu do mesmo jeito, um chapéu marrom chato de 7 por 7 sem os cantos no topo ou um vermelho de três anéis em volta do topo do caule e um teto de 3 por 3, terminando com um sininho e uma faísca; os blocos que já estão lá ficam. Quinze gnomos (jogadores falsos com uma skin assinada de gnomo), um a cada 3 ticks, aparecem numa nuvem com um estalo a até 10 blocos do ponto e andam em linha reta a 0,225 bloco por tick, virando para um lado sorteado sempre que um bloco ou uma beirada está à frente deles. Tudo cresce no bloco do topo da sua coluna, cogumelos inclusive, e nada cresce sobre o void. Os blocos são só pacotes e voltam no fim, e os gnomos vão embora no fim.
- **Prévia:** o botão direito abre `final-kill-preview.world` (`previa_kill_final`, na raiz do servidor do lobby de BedWars) com uma espada de madeira na mão (`target.weapon`) e um jogador 4 blocos à frente (`final-kill-preview.target.distance`), com a skin de outro jogador online (a do próprio jogador quando está sozinho) e um nick cinza sorteado de `target.names`, e `&eAcerte o jogador!` na barra de ação. Bater nele o remove com o som de dano e toca o efeito ali; depois o jogador volta e a lista abre de novo.

### Efeitos de vitória

`Efeito de Vitória` (slot 16 de `Cosméticos do Bed Wars`) lista os 22 efeitos de vitória do export em `Efeito de Vitória ({page}/{pages})`, com o mesmo layout, ordem, compra e seleção da lista de destruição de cama. Eles ficam na categoria `bedwars-vitoria`, então a posse e a seleção são separadas das de cama; os créditos são os mesmos. Todos eles têm animação (`Tinta`, `Feliz Natal`, `Como treinar seu... porco?`, `Altas aventuras`, `Colheita de Abóboras`, `Apavorante`, `Contagem de Flash`, `Apocalipse Zumbi`, `Aventureiro`, `Necromante`, `Deserto`, `Bruxa`, `Anoitecer`, `Congelado`, `Dragão`, `Festival Dourado`, `Garota Mágica`, `Inferno`, `O End`, `Tsunami`, `Wither`, `Encantado`); só um efeito toca por partida, sorteado entre os vencedores online, e todo efeito dura 10 segundos; o que cai do céu (baldes de tinta, abóboras, doces, presentes, ouro) cai pelo mapa todo, dentro da caixa em volta de todos os pontos das ilhas (spawns, camas, lojas, geradores) mais 12 blocos, em dobro em mapas maiores que 32 por 32 blocos, e o que erra as ilhas cai no void.

- **Na partida:** quando a partida acaba com vencedor, todo jogador do time vencedor toca o seu efeito de vitória selecionado (ou um aleatório que ele tem) na posição dele, visto pela sala toda.
- **Tinta:** 22 baldes de tinta caem um a cada 8 ticks de 12 blocos de altura no chão pelo mapa todo (nunca a menos de 2,5 blocos do vencedor), com brilhos vermelhos, verdes, azuis e amarelos. Quando um cai, espirra, espalha gotas e quatro bolhas de lã (verde-limão, amarelo, azul, vermelho) saltam em quatro direções com um estalo que sobe de tom, cada uma acendendo uma estrela rosa de partículas e um feixe curto da sua cor. Depois quatro fogos em estrela (verde, amarelo, vermelho, azul) e um colorido mais alto estouram ali, cada um jogando uma vareta e pintando o chão em volta com lã da sua cor (uma mancha de 3 por 3 no bloco de cima de cada coluna, mais um respingo a 2 blocos); os fogos seguintes pintam por cima. Os blocos pintados são só pacotes e voltam ao normal quando o efeito acaba, depois de 12 segundos. Um balde para no primeiro bloco que atinge na descida (uma árvore ou um telhado contam); um balde que erra a ilha cai direto no void por 2 segundos e não faz mais nada.
- **Feliz Natal:** todo jogador vivo ganha a própria hiena voadora (uma cabeça num assento invisível), saindo de onde está e virada para onde olha. Cada um guia a sua: segurando W ela voa para onde ele olha (até 1 bloco por tick, acelerando e freando suave); sem W ela só sobe e desce. Ninguém consegue descer. Chovem presentes pelo mapa, 6 a cada 5 ticks, que caem no chão e somem um segundo depois, e fogos em estrela (vermelho, verde, branco) estouram num anel de 15 blocos.
- **Como treinar seu... porco?:** o mesmo voo num porco com sela, um para cada jogador vivo, seguido por um trem de seis porcos (um filhote) que repetem o caminho dele de 5 a 35 ticks atrás, com rastro de nuvem, corações e sons de porco.
- **Altas aventuras:** todo jogador vivo fica sob os próprios 25 balões de cabeça amarrados com guias ao lado dele e sobe devagar; o W de cada um leva os próprios balões para frente a 0,25 bloco por tick. Trinta balões soltos em volta estouram um por um.
- **Colheita de Abóboras:** abóboras girando com rastro de fogo caem pelo mapa a cada 10 ticks e estouram no chão em sementes, tortas, aboborinhas e às vezes um fogo laranja.
- **Apavorante:** anoitece (do meio-dia à meia-noite em 30 ticks, acelerando e freando suave); dois ghasts voam em círculo no alto e cabeças de doce caem pelo mapa com rastros coloridos, estourando em bolo, açúcar e biscoitos ou, às vezes, chiando como creeper e soltando cabeças de fantasma.
- **Contagem de Flash:** carinhas de surpresa feitas de partículas aparecem na frente do vencedor, viradas para ele onde ele estiver, três de cada vez, enquanto flashes brancos de fogos estouram em volta.
- **Apocalipse Zumbi:** anoitece do mesmo jeito; zumbis (alguns filhotes) saem do chão em volta do vencedor e andam até ele, sumindo em fumaça ou explodindo como creepers, com raios e um redemoinho de bruxa.
- **Aventureiro:** todo jogador vivo ganha a guia `Grappler` no primeiro slot da hotbar: o botão esquerdo lança um gancho no primeiro bloco olhado (até 80 blocos) e o direito puxa o jogador até ele. Ela é retirada no fim.
- **Necromante:** caveiras de wither giram em volta do vencedor dentro de um anel roxo e dez mortos (jogadores falsos com nomes cinza) saem do chão, andam até o vencedor e afundam de novo, com raios.
- **Deserto:** o mapa todo vira deserto a partir do vencedor (a área do mapa, ou pelo menos 144 por 144 blocos em volta dele, como na prévia), em 162 ticks qualquer que seja o tamanho (pelo menos 128 colunas por tick): pedra e terra viram arenito em camadas ou argila, o topo vira areia ou areia vermelha, plantas e folhas somem, grama vira arbusto seco e nascem alguns cactos. Todo jogador vivo ganha um oásis (um lago, uma fonte de ouro, palmeiras e cana), construído 10 blocos à frente dele, na altura do chão dele, tanto em cima da ilha quanto do void; quando tem algo acima do chão ali (uma construção, uma árvore, o oásis de outro jogador) ele vira para os lados ou vai mais longe, até 48 blocos, e sem espaço livre nenhum fica bem na frente. Todo jogador vivo anda no próprio tapete voador, que só o W dele move, para onde ele olha, enquanto chovem blocos, barras e pepitas de ouro e maçãs douradas pelo mapa. Tudo é só pacote e volta ao normal no fim.
- **Bruxa:** todo jogador vivo monta a própria vassoura (uma cabeça de cerdas e cinco cabeças deitadas de cabo em suportes invisíveis, com um gatinho preto sentado atrás de quem monta) e ganha a vareta `Varinha Mágica` no quinto slot da hotbar. Quatro bruxas (jogadores falsos, cada um com uma de quatro skins de bruxa assinadas, de vestido roxo, preto, oliva e verde (`WitchSkins`), uma vareta e um funil de ponta-cabeça como chapéu, cada uma na mesma vassoura com um gatinho) aparecem numa nuvem em volta de cada jogador, de 3 a 4 blocos à frente e a 4 e 7 blocos de cada lado. Segurando W a vassoura voa para onde o jogador olha a até 1 bloco por tick, acelerando e freando 30% por tick, subindo ou descendo com o olhar; a vassoura vira um quinto do caminho até o olhar a cada tick e sempre sobe e desce meio bloco (mais ou menos a cada 31 ticks), então sem W ela balança no lugar. Toda bruxa voa do mesmo jeito e balança um tick à frente da anterior. O lugar de uma bruxa fica 3 ou 4 blocos à frente e 4 ou 7 blocos para o lado do sentido em que a vassoura do jogador dela está voando (não para onde o jogador olha, então virar parado não move ninguém); enquanto a bruxa está a mais de 4 blocos do lugar ela voa até ele, virando como a vassoura, e senão só balança, virada para o mesmo lado. Assim as bruxas partem alguns ticks depois do jogador, seguem atrás a toda velocidade e ficam mais para trás nas curvas. Um clique com a varinha (no máximo uma vez a cada 13 ticks) dispara um raio de guardião de trás da vassoura até o primeiro bloco olhado (até 80 blocos, ou 80 blocos no ar); 9 ticks depois ele estoura numa explosão enorme e repinta a lã, a argila tingida, o vidro tingido, os painéis de vidro e os tapetes a até 5,5 blocos com uma cor sorteada entre laranja, magenta, azul-claro, rosa, ciano, roxo, marrom, verde e vermelho, em qualquer lugar do mapa. Um dragão ruge no começo e fogos são lançados a cada 10 ticks. Os blocos pintados são só pacotes e voltam ao normal no fim, e a varinha é retirada.
- **Dragão:** todo jogador vivo monta o seu próprio dragão do End, que aparece 5 blocos acima de onde ele está (o jogador vai sentado nas costas, 6 blocos acima dele) e voa o tempo todo a 1 bloco por tick para onde o jogador olha: vira na hora com o olhar, segurar A ou D o leva de lado (um quarto de volta para esse lado), e ele só sobe ou desce com o olhar quando o jogador olha mais de 23 graus para cima ou para baixo. Ele cava um túnel em tudo o que toca (7 blocos de largura, da altura dele até 8 blocos acima): mais ou menos um bloco em 18 se quebra com as partículas e o som de quebra, e um em 6 é arremessado para fora 2 ticks depois (2 blocos por tick para longe do dragão e 1 para cima) e some ao cair. O dragão ruge de vez em quando. Os blocos cavados são só pacotes e voltam no fim.
- **Inferno:** o mapa todo vira Nether a partir do vencedor (a área do mapa, ou pelo menos 129 por 129 blocos em volta dele, como na prévia), 256 colunas por tick em ordem de distância, cada coluna do fundo até a altura 127: todo bloco inteiro vira netherrack, menos dentro de bolsões de lava sorteados (bolas de raio 3 a 9 blocos, cerca de 15% do chão), onde vira lava parada; vidros, painéis, escadas, lajes, cercas, muros, baús e outros blocos sólidos que não são cubos inteiros viram areia das almas; líquidos, plantas e outros blocos soltos somem; barreiras ficam. A noite cai no primeiro segundo, do meio-dia à meia-noite, desacelerando no fim. Chamas estouram a cada tick em algum ponto até 15 blocos do vencedor. Quinze blazes aparecem num anel a 10 blocos do vencedor, até 19 blocos de altura, e descem devagar até o chão; a cada 5 ticks um deles atira uma bola de fogo grande num jogador vivo sorteado, que faz curva para onde o vencedor olhava no começo. A bola de fogo que acerta um jogador explode e o lança 1,5 bloco por tick na direção dela e 1 para cima; a que acerta o chão abre uma cratera de 3 blocos e arremessa os blocos como netherrack, que some 2 segundos depois. Tudo é só pacote e volta ao normal no fim.
- **O End:** o mapa todo vira End a partir do vencedor (a área do mapa, ou pelo menos 129 por 129 blocos em volta dele, como na prévia), 256 colunas por tick em ordem de distância, cada coluna do fundo até a altura 127: todo bloco inteiro vira pedra do End, menos dentro de manchas sorteadas (bolas de raio 3 a 9 blocos, cerca de 10% do chão), cujo miolo vira tábuas de bétula e o bloco e meio de fora vira arenito; tudo que não é cubo inteiro (vidros, painéis, escadas, lajes, cercas, baús, plantas, teias, líquidos) some; barreiras ficam. Cinco pilares redondos de obsidiana, de 9 blocos de largura, do fundo até a altura 127, ficam só em volta do meio do mapa, a 33 blocos dele (em 33,0; 10,32; -27,19; -27,-19 e 10,-32 a partir dele), esteja o vencedor onde estiver; na partida o meio é a posição média dos geradores de esmeralda (o centro da área do mapa quando o mapa não tem nenhum), na prévia é `victory-preview.middle`; cada um surge inteiro no tick em que a onda chega ao centro dele. Dois endermen aparecem no chão a 18–25 blocos do vencedor; aos 2,35 segundos o primeiro se teleporta para 1,5 a 3,5 blocos ao lado de um jogador vivo sorteado e o encara, resmunga pouco depois e a partir de 5,75 segundos segue andando reto passando por ele até encontrar uma parede ou um buraco. Tudo é só pacote e volta ao normal no fim.
- **Tsunami:** um dragão ruge, todo jogador vivo pode voar até o efeito acabar (quem não podia voar antes para de voar nessa hora) e a chuva começa, um centésimo mais forte a cada tick. Do lado mais distante do mapa no eixo (norte, sul, leste ou oeste) mais próximo de para onde o vencedor olha, água parada varre o mapa inteiro de volta, subindo a cada dois ticks e enchendo todo bloco de ar, grama alta, plantas duplas e flores do y 0 até 2 blocos acima do bloco mais alto do mapa (tochas, cipós, tapetes e todo o resto ficam): a primeira fileira enche inteira no primeiro tick, depois a base da onda anda 4 fileiras por tick e a crista cheia 2 fileiras por tick, com a água em rampa entre elas, então um mapa de 147 blocos fica submerso em uns 3,7 segundos. A água vai em seções inteiras de chunk, como um recarregamento de chunk, e não bloco a bloco. Lulas e guardiões (cerca de um a cada 125 blocos quadrados, até 240, 40% guardiões, da altura do vencedor até a superfície da água) surgem na água conforme ela chega. As lulas nadam em braçadas, e os guardiões flutuam e prendem o laser na lula mais próxima a até 16 blocos por 4 segundos. Splashes cada vez mais agudos tocam a cada 7 ticks e fogos sobem a cada 10 ticks. A água é só pacote e volta ao normal no fim.
- **Wither:** cada jogador vivo monta o próprio wither, sentado 3,375 blocos acima dele. Ele sempre voa meio bloco por tick para onde o jogador olha, ficando reto enquanto o olhar estiver a até 5° do horizonte. A cada 20 ticks ele dispara uma cabeça de wither que salta 10 blocos na direção do olhar e depois perde 5% da velocidade por tick. No primeiro bloco, ou depois de 8 ticks, a cabeça estoura numa explosão enorme, e os blocos a até 20 blocos dela viram argila e areia, dos mais próximos para os mais distantes, uns 45 por tick. Fogos sobem a cada 10 ticks e o wither faz o som dele aos 4 e 8 segundos. Os blocos transformados são só pacote e voltam no fim.
- **Anoitecer:** a noite cai com um trovão e o vencedor monta numa coruja que surge numa fumaça e sobe ao céu em linha reta, segurando as rédeas, deixando um rastro de penas e batendo as asas, enquanto um bando de corujas se junta em volta de quem a monta, uma a cada 4 ticks desde o primeiro tick até serem 12, cada uma surgindo numa fumaça a até 12,5 blocos para o lado da coruja e de 0,75 a 6,75 blocos acima dela, planando sozinha por 50 a 120 ticks e sumindo numa fumaça, trocada por outra em volta de onde a coruja estiver; cada jogador vivo monta a sua própria coruja.
- **Congelado:** o mapa congela a partir do vencedor em anéis em forma de losango, umas 250 colunas por tick, então um mapa inteiro leva uns 5 segundos; os blocos inteiros viram neve, com manchas angulosas de gelo compactado onde cai cerca de uma semente em sete de um padrão aleatório de células de Manhattan em três dimensões, vidros, painéis, escadas, lajes, baús e água viram gelo transparente, e tapetes, botões, cabeças e plantas somem. Até quatro bonecos de neve despertam no primeiro chão congelado em volta do vencedor e passeiam, rangendo na neve e estalando no gelo. Os blocos congelados são só pacotes e voltam ao normal no fim.
- **Festival Dourado:** todo jogador vivo monta a própria ovelha arco-íris (a lã alterna entre vermelho, laranja, amarelo, verde-limão, azul-claro, azul e roxo a cada 4 ticks, com poeira da mesma cor em volta dos pés dela) um bloco abaixo de um assento invisível; segurando W ela voa para onde o jogador olha, como o porco, a até 1 bloco por tick. A ovelha cava um túnel em tudo o que atravessa: todo bloco numa faixa de 5 de largura e 7 de altura, a partir de um bloco abaixo dela, some com um estalo e uma nuvem de neve. A cada 5 ticks, 30 girassóis, blocos de ouro e diamantes (divididos entre os jogadores) são jogados para cima de 10 a 34 blocos à frente de cada jogador, mais alto quanto mais ele olha para cima, e somem de 84 a 100 ticks depois. Fogos são lançados a cada 10 ticks. Os blocos cavados são só pacotes e voltam ao normal no fim.
- **Garota Mágica:** um dragão ruge e todo jogador vivo ganha o fogo de artifício `Foguete` no quinto slot da hotbar; um tick depois ele senta numa grande cabeça de estrela que espera no ar e vira um quinto do caminho até o olhar a cada tick. Um clique com o foguete (no máximo uma vez a cada 5 ticks, com qualquer botão) arremessa a estrela para onde o jogador olha, 1,5 bloco por tick para a frente e 0,8 para cima, com som de lançamento e faíscas; depois ela plana e cai como um corpo e para no primeiro bloco abaixo ou na altura em que começou, até o próximo foguete. A cada 8 ticks um sino toca e uma cabeça de estrela girando cai pelo mapa de 3,7 a 9,5 blocos acima da estrela do vencedor, desenhando uma fita de arco-íris de seis cores com brilhos; quando ela cai, faíscas estouram com um estalo e um cintilar e ela se apaga 4 ticks depois; a estrela que erra as ilhas cai no void. Fogos são lançados a cada 10 ticks, e o foguete é retirado no fim.
- **Prévia:** o botão direito abre o mundo `victory-preview.world` (`previa_efeito_de_vitoria`, que precisa estar na raiz do servidor do lobby de BedWars). O jogador fica em `victory-preview.spawn` e vê `3...`, `2...`, `1...` com um segundo entre eles e o clique da contagem, depois o título `VITÓRIA` e o efeito. Quando acaba (ou na cama `Voltar ao Lobby` do slot 8, ou depois do `timeout`) o jogador volta para onde estava e a lista de efeitos abre de novo; um título `VITÓRIA` que ainda está passando é parado, para não cobrir a contagem da próxima prévia.

### Aura

`Aura` (slot 33 de `Cosméticos do Bed Wars`, uma esmeralda) lista as auras em `Aura ({page}/{pages})`, com o mesmo layout, ordem (as possuídas primeiro, depois o resto, cada grupo em ordem alfabética), seleção e regra do `Voltar` das outras listas: `Nenhuma` (barreira, slot 10), `Aleatório` (name tag, slot 11, cujo ícone passa pelas auras que o jogador possui), as auras a partir do slot 12, `Voltar` (slot 48, `Para Cosméticos do Bed Wars`) e `Selecionado: <nome>` (slot 49). São salvas na categoria `bedwars-aura`, então posse e seleção são separadas das outras categorias. As duas auras são prêmios de torneio (`availability: EVENT`, tag `&6Premiação de torneio`, cinco estrelas): quem não tem uma vê `Indisponível.` e ainda pode visualizá-la com o botão direito. Não são vendidas e `/cosmeticos dar` só dá cosméticos de destruição de cama, então por enquanto o jogador as possui por `muxe.cosmetic.all`.

| Aura | Ícone | Partícula |
|---|---|---|
| `Liga Mestre` (`liga-mestre`) | `REDSTONE` | `FLAME` |
| `Liga Diamante` (`liga-diamante`) | `DIAMOND` | `SPELL_WITCH` |

- **Regra:** enquanto o jogador está jogando (sala em `PLAYING`, jogador vivo, fora do modo espectador) a aura fica sempre ligada, e a única coisa que a esconde é a poção de invisibilidade. Não há aura na sala de espera, para jogadores mortos ou eliminados (eles espectam) nem depois do fim da partida. Todos da sala a veem, o dono inclusive, e o custo é baixo na thread principal: uma partícula por dono por tick.
- **Padrão** (`AuraPattern`, `AuraTrails`): a cada tick, por jogador com aura, uma partícula (quantidade 0, offsets 0, velocidade 1, longa distância). Se o jogador andou na horizontal desde o tick anterior, ela aparece nos pés, em `(x, y + 0.1, z)`. Senão é um ponto de uma hélice em volta do jogador, para um contador `k` que cresce 1 a cada tick, andando ou não (começando em 0 quando a aura passa a aparecer): ângulo `18° · k`, deslocamento `x = sin(ângulo)`, `z = -cos(ângulo)` (raio 1) e `y = 0.1 + ((k + 15) mod 60) / 30`. Assim ela sobe 1/30 de bloco por tick de 0,1 até cerca de 2,07 em três voltas (20 ticks por volta) e recomeça a cada 60 ticks. Só o movimento horizontal conta: pular ou cair parado mantém a hélice.
- **Aleatório:** `Aleatório` sorteia uma aura possuída por jogador e por partida, no primeiro tick em que o jogador está jogando, e a mantém até ele sair da partida (`AuraCosmetics`, `AuraAssignments`); a partida seguinte sorteia de novo. `Nenhuma` não mostra nada, e uma aura escolhida que o jogador não possui nunca é usada.
- **Prévia:** o botão direito abre o mundo `aura-preview.world` (`previa_aura`, que precisa estar na raiz do servidor do lobby de BedWars; sem ele a prévia responde `preview-only-lobby`). O jogador fica em `aura-preview.spawn` (10.5 19 21.5, yaw 180) com a cama `Voltar ao Lobby` no slot 8 e vê aquela aura em si mesmo, só em si mesmo, enquanto anda. Não há contagem nem título. Termina na cama, em `aura-preview.timeout` (`60s`) ou quando o jogador sai, e ele volta de onde estava e a lista de auras abre de novo. Reaproveita a prévia de cenas (`ScenePreview`) com uma `AuraAnimation` por aura.

### Rastros de projétil

`Rastros de Projétil` (slot 27 de `Cosméticos do Bed Wars`, uma flecha com `{owned}/{total} ({percent}%)` e `Selecionado: {selected}`) lista os 23 rastros em `Rastros de Projétil ({page}/{pages})`, com o mesmo layout, ordem (os possuídos primeiro, depois o resto, cada grupo em ordem alfabética), seleção e regra do `Voltar` da lista de auras: `Nenhuma` (barreira, slot 10), `Aleatório` (name tag, slot 11), os rastros a partir do slot 12, `Voltar` (slot 48), `Selecionado: <nome>` (slot 49) e `Loja` (slot 53). São salvos na categoria `bedwars-rastro`. Preços por estrelas: 300, 350, 500, 700 e 800 créditos. `Crítico` é `DEFAULT`, então todo jogador o possui. `Ultra` é um rastro ULTRA que mostra a própria tag, `&7Exclusivo para &d&lULTRA`, em vez de `Incluso no ULTRA`: em todas as categorias, um cosmético VIP, ULTRA ou ULTRA+ com `tag` própria mostra essa tag em vez da do rank.

Um rastro só é animado quando `ProjectileTrails` (pacote `cosmetic.trail`) tem um `ProjectileTrail` com o id dele. Até lá ele mostra `Indisponível.`, não é vendido, o `Aleatório` nunca o sorteia, ninguém o recebe numa partida e a prévia responde `cosmetic-no-preview`; o resto da categoria funciona com o registro vazio.

| Rastro | Ícone | Estrelas | Disponibilidade | Tag |
|---|---|---|---|---|
| `Espiral da Fortuna` (`espiral-da-fortuna`) | `DOUBLE_PLANT` | 5 | `EVENT` | `&6Premiação de torneio` |
| `Crítico` (`critico`) | `DIAMOND_SWORD` | 1 | `DEFAULT` |  |
| `Chamas` (`chamas`) | `BLAZE_POWDER` | 5 | `ULTRA` |  |
| `Ultra` (`ultra`) | `INK_SACK:9` | 5 | `ULTRA` | `&7Exclusivo para &d&lULTRA` |
| `Natalino` (`natalino`) | `SKULL_ITEM:3` | 5 | `EVENT` | `&cNatal 2025` |
| `Ossos` (`ossos`) | `BONE` | 5 | `EVENT` | `&6Halloween 2025` |
| `Espectro Verde` (`espectro-verde`) | `EMERALD` | 5 | `ULTRA_PLUS` |  |
| `Corações` (`coracoes`) | `APPLE` | 5 | `ULTRA_PLUS` |  |
| `Magia` (`magia`) | `CAULDRON_ITEM` | 5 | `ULTRA_PLUS` |  |
| `Sorte Azul` (`sorte-azul`) | `INK_SACK:4` | 4 | `SALE` |  |
| `Faísca Cósmica` (`faisca-cosmica`) | `EYE_OF_ENDER` | 1 | `SALE` |  |
| `Lava` (`lava`) | `LAVA_BUCKET` | 1 | `SALE` |  |
| `Chroma` (`chroma`) | `POTION:0` | 5 | `EVENT` | `&6Premiação mensal` |
| `Fumaça` (`fumaca`) | `COAL` | 3 | `SALE` |  |
| `Vapor` (`vapor`) | `WOOL:0` | 3 | `SALE` |  |
| `Água` (`agua`) | `WATER_BUCKET` | 4 | `SALE` |  |
| `Aldeão Furioso` (`aldeao-furioso`) | `SKULL_ITEM:3` | 4 | `SALE` |  |
| `Fogos de Artifício` (`fogos-de-artificio`) | `FIREWORK` | 3 | `SALE` |  |
| `Notas Musicais` (`notas-musicais`) | `NOTE_BLOCK` | 5 | `EVENT` | `&6Premiação de torneio` |
| `Sangrento` (`sangrento`) | `REDSTONE` | 5 | `SALE` |  |
| `Crítico Encantado` (`critico-encantado`) | `BOOK` | 2 | `SALE` |  |
| `Mega-Loucura` (`mega-loucura`) | `SPONGE` | 4 | `SALE` |  |
| `Arco-íris` (`arco-iris`) | `SKULL_ITEM:3` | 4 | `SALE` |  |

- **Regra:** quando um jogador vivo de uma partida (sala em `PLAYING`, jogador vivo, não espectador) lança um projétil cujo atirador é ele (flecha de arco, bola de neve, ovo, pérola do fim, poção arremessável, anzol de vara de pesca, a bola de fogo da carga de fogo da loja ou qualquer outro projétil), o rastro dele o segue. O tipo que o rastro recebe (`ProjectileKinds`) é `ARROW`, `SNOWBALL`, `EGG`, `ENDER_PEARL`, `POTION` (poções arremessáveis), `FISHING_HOOK`, `FIREBALL` (bolas de fogo grandes e pequenas) ou `OTHER` (qualquer outro, como frasco de experiência ou crânio de wither).
- **Aleatório:** o `Aleatório` sorteia um rastro possuído por jogador e por partida, no primeiro lançamento, e o mantém a partida inteira (`TrailCosmetics`, `TrailAssignments`); a próxima partida sorteia de novo. `Nenhuma` não mostra nada, e um rastro escolhido que o jogador não possui nunca é usado.
- **Voo** (`TrailFlight`): o primeiro tick do servidor depois do lançamento é a idade 0, na posição atual do projétil (onde ele nasceu). A cada tick enquanto o projétil existe e não está no chão, o rastro recebe `tick` com a posição, a velocidade, a idade e o tipo. Quando ele some (acertou, explodiu, foi removido, foi recolhido) ou está no chão (uma flecha cravada num bloco, um anzol parado), `land` é chamado uma vez com a última posição e idade, o palco fica aberto mais `linger()` ticks para o que o rastro criou terminar, e então fecha e remove todo ator que o rastro criou. Um voo que dura 1200 ticks pousa de qualquer jeito; uma flecha que ricocheteia continua voando.
- **Quem vê:** os jogadores online da sala. Cada voo tem o seu `PacketStage` do core. A tarefa (`TrailTask`, a cada tick) monta a lista de quem vê uma vez por sala por tick, e só enquanto algum rastro está voando. Quando a sala sai de `PLAYING`, ou o atirador sai do servidor ou da sala, os voos dele fecham na hora, sem `land`. Um rastro que lança exceção é registrado no log e fechado sem parar os outros.
- **Prévia:** o botão direito abre o mundo `projectile-trail-preview.world` (`previa_aura`, o mapa da aura) no spawn da aura (10.5 19 21.5, yaw 180), com o mesmo scoreboard (`Visualizando:` e o nome). Não há contagem nem cena roteirizada: o jogador recebe o kit da gravação e lança as coisas ele mesmo: bola de neve ×16 (slot 0), poção arremessável de cura (`POTION:16421`) ×16 (slot 1), pérola do fim ×16 (slot 2), um arco brilhante inquebrável (slot 3), uma vara de pesca inquebrável (slot 4), uma flecha (slot 9, o primeiro do inventário principal) e a cama `Voltar ao Lobby` (slot 8). Clicar com o botão direito segurando um item do kit o usa (o bloco clicado não é usado); todo o resto continua travado como nas outras prévias: blocos, drops, coletas, cliques no inventário, fome e dano. `kit.refill-ticks` (2) ticks depois de cada lançamento, cada slot do kit que estiver faltando ou vazio é completado, então os itens são infinitos. Como nas gravações, a pérola teleporta o jogador (o dano de queda é cancelado), a poção estoura mas não aplica nada (`PotionSplashEvent` cancelado, o estouro aparece) e uma flecha que acerta algo é removida. Todo projétil que o jogador lança recebe o rastro da prévia, visto só por ele (`TrailPreview`). Termina na cama, em `projectile-trail-preview.timeout` (`60s`) ou quando o jogador sai; projéteis ainda voando são removidos, e o jogador volta de onde estava com a lista aberta de novo. É o modo kit da prévia de cenas: qualquer seção de `ScenePreview` com `kit` (`PreviewKit`) dá o kit em vez de tocar uma cena, e um gancho `KitPlay` ouve cada lançamento.

Efeitos de cada rastro:

- **Espiral da Fortuna:** pó dourado gira num anel a meio bloco da trajetória, uma volta a cada 9 ticks, e solta um girassol a cada tick, que salta e some 5 ticks depois.
- **Crítico:** uma faísca de golpe crítico segue o projétil a cada tick.
- **Chamas:** uma chama segue o projétil a cada tick.
- **Ultra:** pó rosa denso segue o projétil a cada tick.
- **Natalino:** pó segue o projétil, alternando vermelho, verde e branco a cada tick.
- **Ossos:** a cada dois ticks um osso ou uma farinha de osso salta debaixo do projétil e some 5 ticks depois.
- **Espectro Verde:** um brilho verde de aldeão segue o projétil a cada tick.
- **Corações:** um coração segue o projétil a cada tick.
- **Magia:** um brilho roxo de feitiço de bruxa segue o projétil a cada tick.
- **Sorte Azul:** a cada tick um lápis-lazúli salta debaixo do projétil e some 5 ticks depois.
- **Faísca Cósmica:** cinco partículas de portal giram em volta do projétil a cada tick.
- **Lava:** uma gota de lava estala do projétil a cada tick.
- **Chroma:** algumas partículas de feitiço em cores aleatórias seguem o projétil a cada tick.
- **Fumaça:** uma nuvem de fumaça grande segue o projétil a cada tick.
- **Vapor:** uma pequena nuvem branca segue o projétil a cada tick.
- **Água:** um respingo de água segue o projétil a cada tick.
- **Aldeão Furioso:** uma nuvem de aldeão bravo segue o projétil a cada tick.
- **Fogos de Artifício:** uma faísca de fogos desce atrás do projétil a cada tick.
- **Notas Musicais:** a cada dois ticks uma nota musical surge a até um bloco do projétil, cada nota seguinte mais espalhada e com outra cor.
- **Sangrento:** uma partícula de pó vermelho segue o projétil a cada tick.
- **Crítico Encantado:** uma faísca de crítico encantado segue o projétil a cada tick.
- **Mega-Loucura:** a cada tick uma partícula sorteada entre 28 tipos (explosões, faíscas, respingos, críticos, fumaça, feitiços, gotas, aldeões, notas, portal, chama, lava, nuvem, pó, neve, slime e corações) segue o projétil.
- **Arco-íris:** pó denso segue o projétil, uma cor do arco-íris por tick: vermelho, laranja, amarelo, verde, ciano, azul e magenta.

### Mensagens de kill

`Mensagens de kill` (slot 31 de `Cosméticos do Bed Wars`, papel) lista os conjuntos de mensagens do chat em `Mensagens de kill ({page}/{pages})`, salvos na categoria `bedwars-mensagem-kill`. Os slots seguem a gravação e a ordem é a do catálogo, não a de possuídos primeiro: `Padrão` (papel, slot 10), `Aleatório` (name tag, slot 11) e `Estatístico` (livro, slot 12), depois `Voltar` (slot 48, `Para Cosméticos do Bed Wars`) e `Selecionado: <nome>` (slot 49). Não existe `Nenhuma`: quem não selecionou nada usa o `Padrão`, que todos possuem e ninguém compra (`availability: DEFAULT`). As listas não têm botão direito: a lore de cada conjunto traz `Prévia:` com linhas de exemplo e `Botão esquerdo: Selecionar`.

| Conjunto | Ícone | Disponibilidade |
|---|---|---|
| `Padrão` (`padrao`) | `PAPER` | todos |
| `Estatístico` (`estatistico`) | `BOOK` | premiação de torneio (`availability: EVENT`, `&6Premiação de torneio`, mostrado como `Indisponível.` para quem não possui) |

O conjunto selecionado substitui as mensagens de chat da partida (`KillAnnouncer`, chamado pelo `BedWarsMatch`):

| Evento | De quem é o conjunto | Tipos de mensagem |
|---|---|---|
| Kill | De quem matou | `kill`, `final-kill`, `void-kill`, `void-final-kill` |
| Morte sem autor (também o jogador eliminado por estar fora ou quando todas as camas são destruídas) | De quem morreu | `death`, `final-death`, `void-death`, `void-final-death` |
| Cama destruída | De quem quebrou | `bed`, depois do `kill-messages.bed-prefix` (`CAMA DESTRUÍDA »`) que todos os conjuntos compartilham |

Um jogador que sai com a cama já destruída conta como morte final (ou kill final de quem o acertou por último). As kills finais dos jogadores que uma cama quebrada elimina são kills finais de quem quebrou, com o conjunto dele.

- **Textos** ficam em `kill-messages.sets.<id>.messages`, por tipo, com `{victim}`, `{killer}`, `{player}`, `{team-color}`, `{team}` e `{count}` (o contador, `17.089`, com os milhares separados por ponto). `{br}` marca onde a `Prévia:` quebra uma linha longa em duas (no jogo ele some). Um conjunto só escreve os tipos que muda: um tipo `void-*` ausente usa o tipo normal (o `Estatístico` não tem variações de void). O conjunto padrão (`kill-messages.default`, `padrao`) precisa ter todos e nenhum `{count}`. Toda mensagem começa com `kill-messages.line-color` (`&7`), igual no jogo e na prévia.
- **`Padrão`** mantém os textos que o jogo já tinha, e a kill no void (`morreu no void para <killer>`) é a única nova, já que uma morte no void com autor antes aparecia como kill normal. Os nomes mantêm a cor do time.
- **`Estatístico`** conta com os totais de BedWars do jogador na vida inteira, todos os modos juntos, incluindo o próprio evento: `Fulano foi a 17.089ª kill de tadeu.`, `Fulano foi a 19.215ª kill final de tadeu.`, `tadeu morreu pela 12.538ª vez.`, `tadeu teve sua 2.537ª morte final.`, `A Cama do Vermelho foi a 11.938ª cama destruída de tadeu.` O número fica em `&b` e termina com `ª`. Uma kill soma também no contador de mortes da vítima. Os totais (`kills`, `finalKills`, `deaths`, `finalDeaths`, `bedsBroken` de `bedwars.player_stats`, geral) são carregados quando o jogador entra na sala, junto com o nível (`FindPlayerStatsQuery`), e o `LifetimeTally` soma o que acontece na partida. Se ainda não foram carregados, aquela mensagem sai com o `Padrão`.
- **Aleatório:** o `Aleatório` sorteia um conjunto possuído por jogador e por partida, na primeira vez que uma mensagem precisa dele, e o mantém até a partida ser encerrada (`KillMessages`, `MessageAssignments`). Um conjunto selecionado que o jogador não possui nunca é usado: usa-se o `Padrão`.
- **`Prévia:`** é montada com os textos acima, com o nome de quem vê na cor da tag dele (o `§8tadeu` da gravação), uma vítima `Fulano` em vermelho e o time `Vermelho` (`kill-messages.preview`, `MessagePreview`). O Padrão mostra `kill`, `void-kill`, `death`, `void-death` e `bed`; o Estatístico mostra `kill`, `final-kill`, `death`, `final-death` e `bed` com os contadores de exemplo de `kill-messages.preview.counts`. Um teste confere que cada linha da prévia, juntada, é exatamente o que o jogo envia.
- O `/cosmeticos dar` só dá cosméticos de destruição de cama, então por enquanto o `Estatístico` é possuído por quem tem `muxe.cosmetic.all`.

### Tipos de madeira

`Tipos de madeira` (slot 29 de `Cosméticos do Bed Wars`, tábuas) abre `Tipos de madeira ({page}/{pages})`, salvo na categoria `bedwars-madeira`. Não tem `Nenhuma` nem `Aleatório`, e a ordem é a do catálogo. O item `madeira` da loja (16 tábuas por 4 de ouro) é entregue com a madeira selecionada no lugar do carvalho; a quantidade, o preço, o nome (`Madeira`) e o lugar na loja e na compra rápida não mudam, e ele vai para a hotbar como qualquer compra (`HotbarDelivery`, que só olha o id do item). Colocar e quebrar funcionam como em qualquer bloco colocado na partida.

| Slot | Nome | Item | Incluso em |
|---|---|---|---|
| 10 | `Madeira de carvalho` (padrão) | `WOOD:0` | todos |
| 11 | `Madeira de pinheiro` | `WOOD:1` | `VIP` |
| 12 | `Madeira de eucalipto` | `WOOD:2` | `VIP` |
| 13 | `Madeira da selva` | `WOOD:3` | `ULTRA` |
| 14 | `Madeira de acácia` | `WOOD:4` | `ULTRA` |
| 15 | `Madeira de carvalho escuro` | `WOOD:5` | `ULTRA+` |
| 16 | `Tronco de carvalho` | `LOG:0` | `ULTRA+` |
| 19 | `Tronco de pinheiro` | `LOG:1` | ninguém ainda |
| 20 | `Tronco de eucalipto` | `LOG:2` | premiação de torneio |
| 21 | `Tronco de árvore da selva` | `LOG:3` | ninguém ainda |
| 22 | `Tronco de acácia` | `LOG_2:0` | ninguém ainda |
| 23 | `Tronco de carvalho escuro` | `LOG_2:1` | ninguém ainda |

- **Lore:** `Incluso no VIP` / `ULTRA` / `ULTRA+` nas madeiras de rank (`lore.vip-tag`, `lore.ultra-tag`, `lore.ultra-plus-tag`), `Premiação de torneio` no prêmio, depois `Botão esquerdo: Selecionar`, `Já selecionado.` ou `Indisponível.`. Os nomes possuídos ficam verdes e os bloqueados vermelhos. A madeira selecionada brilha e `Selecionado: <nome>` (slot 49) mostra a própria madeira, com `&7` de lore.
- **Ranks:** `Incluso no X` significa que todo jogador desse rank ou acima possui (`CosmeticAccess`): `muxe.cosmetic.vip` para o `VIP`, `muxe.cosmetic.ultra` para o `ULTRA` e `muxe.cosmetic.ultraplus` para o `ULTRA+` (cada nível cobre os de baixo), e `muxe.cosmetic.all` possui tudo. A permissão vem dos grupos, então dê `muxe.cosmetic.vip` ao grupo `vip` (os ranks acima herdam) e `muxe.cosmetic.ultraplus` a quem tem Ultra+, que ainda não é um grupo próprio. O `muxe.cosmetic.ultra` já é dado ao `ultra`. As madeiras marcadas `ninguém ainda` ou `premiação de torneio` só são possuídas por `muxe.cosmetic.all` ou por uma concessão (`core.owned_cosmetics`).
- **Alternativa:** a madeira que a loja entrega é a selecionada se o jogador ainda a possui, e carvalho caso contrário (rank que expirou, madeira nunca possuída, nada selecionado ou cosméticos ainda não carregados). A seleção é lida quando o jogador entra no servidor, então uma troca feita no lobby vale na próxima partida.

### Lobby do BedWars

O lobby do BedWars (grupo `bedwars-lobby`) roda o Muxe-Lobby e este plugin no modo lobby. O seletor de jogo principal, os NPCs de jogo e o `/jogar bedwars` levam para lá.

- **NPCs de modo**: SOLO, DUPLAS, TRIOS e QUARTETOS abrem o menu do modo; 1V1/2V2/3V3/4V4/4S abre o menu de duelos; MEGA e PILARES ainda não abrem nada. `{players} jogando` conta os jogadores nas salas dos seus modos em todos os servidores de BedWars. As duas linhas de cima do PILARES trocam entre branco e rosa a cada 0,5 s.
- **Menu do modo** (`Bed Wars Solo`...): `Bed Wars (Solo)` mostra os jogadores do modo e o top 3 do winstreak diário, e manda o jogador para uma partida; `Selecionar mapa` abre o menu de mapas para quem tem `muxe.bedwars.map-select` (VIPs) e responde `map-vip-only` para os outros; `Treino: Ponte` responde `coming-soon`. Os modos de duelo também têm `Voltar`, para o menu de duelos.
- **Nível**: a barra de XP mostra o nível do BedWars como nível de XP e o progresso até o próximo nível como a barra, carregados ao entrar e atualizados a cada 30 segundos.
- **Menu de mapas** (`Bed Wars 1v1 (1/1)`): os mapas do modo encontrados nos servidores de BedWars, favoritos primeiro, cada grupo em ordem alfabética. Botão esquerdo pede uma sala naquele mapa; botão direito adiciona ou remove o favorito. `Mapa favorito aleatório` entra num favorito aleatório, `Pesquisar` filtra pelo nome digitado no chat (sem diferenciar acentos) e as setas trocam de página.
- **Matchmaking**: cada servidor de BedWars publica as suas salas e arenas prontas a cada 2 s (`bedwars.rooms`). Entrar escolhe a sala mais cheia do modo (e do mapa) que ainda tem vaga em qualquer servidor; se não houver, pede ao servidor mais vazio que tenha uma arena desse modo (e mapa) para criar uma. O lobby manda `bedwars.join_request` para esse servidor, que reserva uma vaga na sala por 30 s e responde com o código. As reservas pendentes contam como vagas ocupadas, tanto na escolha da sala quanto no que o servidor publica, então uma vaga nunca é prometida duas vezes; as vencidas são descartadas a cada segundo e se criou a sala; o jogador vê `&aEnviando para <código>...` e é movido para lá. Quando o lobby pede uma sala nova, o jogador vê antes `&aSolicitando nova sala...`, e `&aSala criada!` depois da linha da sala quando o servidor de fato a criou. Ao chegar, ele entra na sala reservada, ou em outra sala do modo se ela tiver enchido nesse meio tempo.
- **Tops**: quatro hologramas (kills finais, vitórias, winstreak atual e nível) com o top 100 em 10 páginas. Cada jogador vê a sua própria página: um clique (esquerdo ou direito) na parte de cima (nomes para cima) troca a página, e na parte de baixo (`Clique para trocar!` para baixo) troca o período (Semanal, Mensal, Total; Total primeiro no top de nível). No Total o top de nível mostra a insígnia do nível; no Mensal e no Semanal ele ordena e mostra o XP ganho naquele período (`120.806 XP`, `lobby.tops.xp-value`). Todo clique toca `CLICK` (`lobby.tops.click-sound`). Posições vazias mostram `Ninguém` com `0` (`0 XP` no Mensal e no Semanal do top de nível). Os nomes têm a cor da maior tag que o jogador tem. Os tops recarregam a cada 60 s, e uma falha ao carregar vira um aviso no log.
- **Seletor de modo**: o holograma `SELECIONAR MODO` do export lista cinco modos (Geral, Solo, Duplas, Trios, Quartetos), o escolhido em verde; um clique abre `Selecionar modo`, e a escolha vale para todos os tops com modos (o top de nível não tem). Quando o modo escolhido fica mais abaixo no menu, a lista anda para ele continuar visível.
- **Scoreboard** (anexo 1): `&b&lBED WARS` com o nível, a barra de XP, as vitórias e a winstreak atual dos dois modos que o jogador mais jogou (1v1 e Solo enquanto ele não tem partidas), `Jogadores` (jogadores nos servidores e lobbies de BedWars) e o site. Ela substitui a scoreboard do lobby pelo `core.sidebars()`.
- **NPC de estatísticas**: mostra cada jogador como ele mesmo (com a skin dele), com nível, barra de XP, vitórias, kills, kills finais, camas e winstreak acima. Clicar abre `Suas estatísticas (Bed Wars)`, com Geral (incluindo nível e XP) e cada modo.
- **NPC de missões**: `MISSÕES` / `Clique para ver!` em 24.5 81 8.5 abre `Missões do Bed Wars` sem `Voltar`; o botão `Missões` do `Menu do Bed Wars` abre com `Voltar`. Veja [Missões](#missões).
- **Espaçamento dos hologramas**: as linhas dos NPCs seguem as alturas dos exports. Os NPCs de modo e de missões usam `lobby.npc-labels` (última linha 2.25625 acima dos pés, depois 0.28125, 0.46875 e 0.28125); o NPC de estatísticas usa `lobby.stats-npc.labels`, com os espaços maiores da referência antes de `Vitórias`, `Winstreak` e `SUAS ESTATÍSTICAS`. Os tops usam `lobby.tops.spacing` (0.28125 abaixo do cabeçalho, 0.375 antes do primeiro nome e antes do `Clique para trocar!`, 0.28125 entre os nomes, 0.34375 antes dos períodos) e a lista do seletor `lobby.tops.selector.list-spacing` (0.28125).
- **Regra dos menus**: um menu aberto por outro menu tem `Voltar`; aberto direto (NPC, item, comando) não tem. `Cosméticos do Bed Wars` só mostra `Voltar` quando aberto pelo `Menu do Bed Wars`, não pelo `/cosmeticos`.

### Missões

O BedWars é o primeiro jogo com [missões](../missions/README.pt-BR.md). Ele registra o seu catálogo, os seus objetivos e a sua recompensa (o XP da missão vai para as estatísticas de BedWars do jogador, então conta no nível e nos tops); o Muxe-Core faz o resto.

| Objetivo | Conta | Missões |
|---|---|---|
| `KILL_STREAK` | melhor | Kills seguidas numa partida, voltando a 0 ao morrer. Ninguém me para! (2). |
| `INVISIBLE_BED` | soma | Camas quebradas estando invisível. Quem foi? (1). |
| `END_STONE_DEFENSE` | soma | 1 quando o jogador coloca `end-stone-blocks` (10) Pedras do Fim a até `end-stone-radius` (3) blocos da própria cama numa partida. Proteção do Fim (1). |
| `CLEAN_WIN` | soma | Vitórias sem colocar TNT nem lançar Bola de Fogo. Rei das pontes (1). |
| `BEDBUG_KILLS` | soma | Kills de um inimigo que um Percevejo do time de quem matou acertou nos últimos `bedbug-assist-seconds` (10). Um aliado inesperado (3). |
| `EMERALDS_IN_A_LIFE` | melhor | Esmeraldas pegas dos geradores sem morrer. Colecionador de Esmeraldas (8). |
| `FIREBALL_DAMAGE` | soma | Dano causado em outros jogadores com Bolas de Fogo. Sou eu bola de fogo (1.000). |
| `FINAL_KILLS` | soma | Kills finais. Fim da linha (35). |
| `EXPLOSIVE_BLOCKS` | soma | Blocos colocados destruídos pela TNT ou Bola de Fogo do jogador. Olha a explosão (1.000). |
| `FAST_WIN` | soma | Vitórias em até `fast-win-seconds` (210). Ansiedade mais de 8000 (1). |
| `CLAY_BOUGHT` | soma | Blocos de Argila comprados. Artesão (50). |

As diárias dão 625 XP; as semanais 2.500 XP e 15 créditos; o Complecionista (completar todas as semanais) dá 15 créditos. Cada jogador recebe 3 das 6 diárias e as 5 semanais da semana. As missões de Halloween da referência não entraram.

O Percevejo agora pode ser comprado: arremessada, a bola de neve faz surgir uma traça do time de quem arremessou onde cair. Por 15 segundos ela persegue o inimigo mais próximo a até 16 blocos, nunca quebra blocos, não dropa nada, e as kills dela contam para quem arremessou.

### Estatísticas

- Todo fim de partida salva, para cada jogador que jogou, partidas, camas quebradas e perdidas, kills, mortes, assistências, kills finais, mortes finais, assistências finais, vitórias, derrotas, winstreak e XP, por modo e em `geral`, no dia, na semana (segunda a domingo), no mês e no total, no fuso de São Paulo.
- Uma assistência vai para cada outro inimigo que bateu na vítima nos últimos 15 segundos. O XP de kill, kill final e cama vem de `xp-rewards`; no fim cada jogador também ganha o XP por minuto, por companheiro e por vitória.
- O winstreak cresce a cada vitória e volta a 0 numa derrota; um empate o mantém. O maior winstreak nunca diminui.
- O nível vem do XP total e começa em 1 (0 XP é nível 1): sair de cada nível custa 5000 até o nível 9, 10000 do 10 ao 19 e mais 5000 a cada 10 níveis, então o nível 10 pede 45000 XP. A insígnia segue a lista de níveis (`&7[1✫]`, `&a[10✶]`... `&4[100❤]`... arco-íris a partir do 1000).

## Comandos

| Comando | Permissão | Descrição |
|---|---|---|
| `/arena ...` | `muxe.minigame.setup` | Cria e configura arenas, só no lobby do BedWars. Veja o [setup de arenas](../../architecture/game-engine/README.pt-BR.md#setup-de-arenas). |
| `/bedwars entrar <modo>` (`/bw`) | nenhuma | Leva o jogador para uma sala desse modo, enquanto espera. |
| `/g <mensagem>` | nenhuma | Fala com todos da sala, com `&6[G]` antes da mensagem. Nas partidas com time o chat normal chega só ao time. |
| `/bedwars iniciar` | `muxe.bedwars.admin` | Começa a contagem em 10 segundos sem o mínimo de jogadores, para testes, mostrando o título `10 segundos` na hora (uma contagem que já corre com mais de 10 segundos é encurtada). Uma partida iniciada assim com um time só continua até esse time ser eliminado; com dois times ou mais ela acaba como sempre, quando sobra um time. |
| `/bedwars finalizar` | `muxe.bedwars.admin` | Encerra na hora a partida em andamento, para testes, como uma partida de verdade termina: o time que sobrou vence (o único jogador de uma partida forçada também), com o resultado, os tops, o resumo das missões, o XP e as estatísticas de sempre; com vários times de pé é empate. |
| `/cosmeticos` (`/cosmeticosbw`) | nenhuma | Abre Cosméticos do Bed Wars. |
| `/cosmeticos dar <jogador> <cosmetico>` | `muxe.cosmetics.admin` | Dá um cosmético de destruição de cama, para cosméticos de evento e premiações. |
| `/bwconfig ver\|definir\|adicionar\|remover\|local\|recarregar ...` | `muxe.admin.config` | Vê e altera qualquer chave do `config.yml` deste plugin dentro do jogo, aplicando ao vivo onde um componente permite. Veja o [editor de configuração](../../development/core-api/README.pt-BR.md#editor-de-configuração). |
| `/bwconfig previa <tipo> <ação> [valor]` | `muxe.admin.config` | Monta as prévias dos cosméticos pelo próprio mundo, ao vivo. Veja [Montando as prévias no jogo](#montando-as-prévias-no-jogo). |

## Permissões

| Permissão | Padrão | Descrição |
|---|---|---|
| `muxe.minigame.setup` | op | Permite o `/arena`. |
| `muxe.bedwars.admin` | op | Permite o `/bedwars iniciar` e o `/bedwars finalizar`. |
| `muxe.cosmetics.admin` | op | Permite o `/cosmeticos dar`. |
| `muxe.admin.config` | op | Permite o `/bwconfig`, e também o `/lobbyconfig` e o `/coreconfig`. Declarada pelo Muxe-Core. |
| `muxe.bedwars.map-select` | op | Escolher o mapa nos menus dos modos (VIPs). Dê aos grupos VIP. |
| `muxe.cosmetic.all` | op | Possui todos os cosméticos do BedWars. |
| `muxe.cosmetic.vip` | false | Possui os cosméticos `Incluso no VIP` (as madeiras de pinheiro e eucalipto). Dê ao grupo `vip`; os grupos acima herdam. |
| `muxe.cosmetic.ultra` | false | Possui os cosméticos `Incluso no ULTRA` e os do VIP. O grupo `ultra` tem essa permissão (migração V14). |
| `muxe.cosmetic.ultraplus` | false | Possui os cosméticos `Incluso no ULTRA+` e os do ULTRA e do VIP. O Ultra+ ainda não é um grupo, então dê a esses jogadores diretamente. |

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
| `lobby-group` | `bedwars-lobby` | Para onde os jogadores vão quando a partida acaba ou quando saem da espera, e o grupo que recebe as listas de salas. |
| `default-mode` | `solo` | O modo da sala em que o jogador entra ao chegar. |
| `timings.*` | 5 s renascer, 180 s voltar, 30 s reiniciar, 3 s proteção, raio da ilha 17, raios de proteção 5/1/1, 60 s sala vazia | Tempos da partida e áreas protegidas; `empty-room-seconds` fecha as salas que ficaram vazias. |
| `countdown.titles` | `30`, `10`, `5`...`1` | Títulos da contagem para iniciar. |
| `preferences.*` | 4 linhas, slots 11/13/15 | Textos, slots e corantes do menu de preferências. |
| `favourite-color.*` | 6 linhas, `Nenhuma` no slot 10, cores nos slots 11 a 16 e 19 a 21 | Título, lore das opções (`choose`, `chosen`), slot e nome de cada cor de time (pela chave do id: `vermelho`, `azul`...) e o botão de voltar do menu de cor preferida. |
| `cosmetics.menus.categories.team-color` | slot 35, lã cinza | O botão `Cor preferida` do menu de cosméticos. Sem ele o slot continua um item "em breve". |
| `tnt-timer.*` | `{color}{seconds}s`, 2 casas, 24 blocos, a cada tick | Formato, altura, distância, frequência, limites e cores do temporizador da TNT. |
| `item-names.*` | `IRON_INGOT: Barra de Ferro`... | Nome em português de cada item entregue numa partida. |
| `sounds.*` | `CLICK`, `NOTE_PLING` (tom 2), `ZOMBIE_UNFECT` (tom 2), `NOTE_PLING`, `LEVEL_UP` (tom 2), `VILLAGER_NO`, `CHEST_OPEN`, `CHICKEN_EGG_POP` (tom 1,5), `ENDERDRAGON_GROWL`, `WITHER_DEATH`, `NOTE_PLING` (tom 2) duas vezes, `ZOMBIE_UNFECT` (tom 2), `EXPLODE`, `ENDERDRAGON_GROWL`, `FIREWORK_LAUNCH` | `countdown`, `match-start`, `died`, `bought`, `upgrade`, `cannot-afford`, `ender-chest`, `deposited`, `bed-destroyed`, `bed-lost`, `kill`, `final-kill`, `eliminated`, `team-eliminated`, `match-end` e `match-end-firework`, cada um com `sound`, `volume` e `pitch`. Um som vazio não toca nada. |
| `forges.*` | os valores da especificação | Ferro, ouro e esmeralda da ilha, e os três níveis de diamante e esmeralda com o segundo de início. `stack-items: false` mantém separados os itens de células diferentes; `split-radius` (2) é a distância em que os companheiros dividem ferro e ouro. |
| `events.*` | 1800, 2400, 2520 | Segundos dos eventos de camas destruídas, morte súbita e fim de jogo, e os nomes deles no placar. |
| `xp-rewards.*` | 10, 5, 250, 100, 12, 60 | XP por minuto, por companheiro, vitória, cama, kill e kill final. |
| `hotbar.waiting.*` | esmeralda e cama | Itens da sala de espera. |
| `scoreboard.*` | anexos 2, 3, 5 e 12 | Linhas do placar; `{teams}` e `{event}` viram as linhas dos times e o próximo evento. |
| `holograms.*` | anexos 7 e 8 | Hologramas dos NPCs e dos geradores. |
| `shop.*`, `upgrades.*` | os exports | Categorias, itens, preços e textos da loja; melhorias, níveis e preços. |
| `shop.hotbar-menus.*` | os exports | Títulos, linhas, slots, textos e período de atualização dos três menus do editor de hotbar, e os nomes dos lugares para onde `Voltar` leva. |
| `cosmetics.menus.bedwars.hotbar` | slot 31 | O botão `Editor de hotbar` do Menu do Bed Wars. |
| `messages.match-started` | `O jogo iniciou!` e o `AVISO:` de aliança | Enviada quando a partida começa. |
| `messages.chest-kept`, `messages.chest-last-one` | `&cVocê não pode guardar este item.`, `&cVocê não pode guardar só um deste item.` | Ditas quando uma ferramenta ou a única espada iria para um baú. |
| `messages.already-have` | `&cVocê já tem este item.` | Dita ao tentar comprar algo que o jogador já tem. |
| `messages.hotbar-slot-full` | `&cEste slot da hotbar já tem o máximo de entradas.` | Dita quando um slot já tem 14 entradas. |
| `messages.*` | anexos 4, 6, 9, 10, 11, 13, 14, 15 | Todas as mensagens da partida. `setup-*` sobrescreve as mensagens de setup da engine. `cosmetic-*` e `preview-*` são as mensagens dos cosméticos. |
| `cosmetics.category` | `bedwars-cama` | Categoria em que a posse e a seleção são salvas. |
| `cosmetics.prices.<estrelas>` | 750, 900, 1300, 1750, 2000 | Preço em créditos por estrelas. |
| `cosmetics.bed-destruction.<id>` | os 30 cosméticos | `name`, `icon` (item), `stars`, `tag`, `availability` (`SALE`, `ULTRA`, `EVENT`) e `description`. O id precisa ser o de uma animação para ter animação. |
| `cosmetics.menus.*` | os exports | Títulos, slots, itens e linhas de lore dos três menus. |
| `cosmetics.victory-effect.*` | `bedwars-vitoria`; 300, 400, 500, 700, 800; os 22 efeitos | Categoria, preços por estrelas e efeitos de vitória, com os mesmos campos dos de cama (`stars: 0` esconde as estrelas). Opcional: o catálogo vem da [API de minigames](../../architecture/minigame-api/README.pt-BR.md#cosméticos) (`cosmetics.yml`), e esta seção só sobrescreve (preços, campos, `hidden: true`, cosméticos extras). A categoria é sempre gerada. |
| `cosmetics.final-kill.*` | `bedwars-kill-final`; 750, 900, 1300, 1750, 2000; os 43 efeitos | Categoria, preços por estrelas e efeitos de kill final. Opcional: o catálogo vem da [API de minigames](../../architecture/minigame-api/README.pt-BR.md#cosméticos) (`cosmetics.yml`), e esta seção só sobrescreve (preços, campos, `hidden: true`, cosméticos extras). A categoria é sempre gerada. |
| `final-kill-preview.*` | `previa_kill_final`, -3.5 24 2.5 olhando para leste, alvo 4 blocos à frente, `&eAcerte o jogador!` | Como `victory-preview`, com `target.distance` e `target.action-bar` no lugar da contagem. |
| `end-titles.victory`, `end-titles.defeat` | `VITÓRIA` `&6` `&e` `&f`; `DERROTA` `&c` `&c` `&f` | Palavra, cor depois da varredura, cor da letra que varre, cor inicial e um `sound` opcional. |
| `victory-preview.*` | `previa_efeito_de_vitoria`, -19.5 27 68.5, meio 0.5 0.5, `3...` `2...` `1...` a cada 20 ticks, `60s`, `1s` | Mundo, spawn, meio do mapa (`middle.x`, `middle.z`, usado pelos efeitos montados em volta dele, como os pilares do O End; sem ele o meio é quem assiste), títulos da contagem e ticks entre eles, tempo limite, espera depois do efeito, item de voltar e scoreboard da prévia de vitória. |
| `cosmetics.aura.*` | `bedwars-aura`; 750, 900, 1300, 1750, 2000; `liga-mestre` e `liga-diamante` | Categoria, preços por estrelas e as duas auras, com os mesmos campos dos de cama. O id precisa bater com uma aura de `Auras` para ter efeito. Opcional: o catálogo vem da [API de minigames](../../architecture/minigame-api/README.pt-BR.md#cosméticos) (`cosmetics.yml`), e esta seção só sobrescreve (preços, campos, `hidden: true`, cosméticos extras). A categoria é sempre gerada. |
| `cosmetics.menus.categories.aura`, `cosmetics.menus.aura.*` | slot 33, esmeralda; `Aura ({page}/{pages})`, `Aleatório` como name tag | O botão da categoria e o que a lista de auras muda (o resto segue a lista de destruição de cama). |
| `kill-messages.*` | `default: padrao`, `line-color: &7`, `bed-prefix`, `preview`, os conjuntos `padrao` e `estatistico` | Os conjuntos de mensagens do chat: o conjunto padrão, a cor que começa toda mensagem, o prefixo da mensagem de cama, o exemplo da `Prévia:` (`title`, `victim`, `team-color`, `team`, `counts` por tipo) e `sets.<id>` com os tipos da `preview` e as `messages` por tipo (`kill`, `final-kill`, `void-kill`, `void-final-kill`, `death`, `final-death`, `void-death`, `void-final-death`, `bed`). `{br}` quebra a linha da prévia. Substituem os antigos `messages.kill`, `final-kill`, `death`, `void-*` e `bed-destroyed`. Opcional: o catálogo vem da [API de minigames](../../architecture/minigame-api/README.pt-BR.md#cosméticos) (`cosmetics.yml`), e esta seção só sobrescreve (preços, campos, `hidden: true`, cosméticos extras). A categoria é sempre gerada. |
| `cosmetics.kill-message.*` | `bedwars-mensagem-kill`; `default: padrao`; `padrao` e `estatistico` | Categoria, cosmético padrão e os conjuntos, com os mesmos campos dos de cama (`stars: 0`, sem preços). O id precisa ser de um conjunto de `kill-messages.sets`. Opcional: o catálogo vem da [API de minigames](../../architecture/minigame-api/README.pt-BR.md#cosméticos) (`cosmetics.yml`), e esta seção só sobrescreve (preços, campos, `hidden: true`, cosméticos extras). A categoria é sempre gerada. |
| `cosmetics.wood-type.*` | `bedwars-madeira`; `default: carvalho`; as 12 madeiras | Categoria, cosmético padrão e madeiras. O `icon` (`WOOD` ou `LOG`, `LOG_2` e `data`) é o item que a loja entrega; `availability` é `DEFAULT`, `VIP`, `ULTRA`, `ULTRA_PLUS` ou `EVENT`. |
| `cosmetics.menus.categories.kill-message`, `cosmetics.menus.categories.wood-type` | slot 31 papel; slot 29 tábuas | Os botões das categorias. |
| `cosmetics.menus.kill-message.*`, `cosmetics.menus.wood-type.*` | títulos, `entries`, `lore`, `selected` | As substituições das listas. `entries` tem `none` e `random` (mostrar essas entradas), `random-after` (cosméticos antes do `Aleatório`), `owned-first` (possuídos primeiro, como nas outras listas, ou a ordem do catálogo), `previews` (botão direito) e `selected-icon` (o item selecionado mostra o cosmético). `lore.header` começa a lore de cada cosmético, `lore.vip-tag`, `lore.ultra-tag` e `lore.ultra-plus-tag` são as linhas de rank. As chaves de lore omitidas são herdadas da lista de destruição de cama. |
| `cosmetics.projectile-trail.*` | `bedwars-rastro`; 300, 350, 500, 700, 800; os 23 rastros | Categoria, preços por estrelas e os rastros, com os mesmos campos dos de cama. O id precisa bater com um rastro de `ProjectileTrails` para ser animado. Opcional: o catálogo vem da [API de minigames](../../architecture/minigame-api/README.pt-BR.md#cosméticos) (`cosmetics.yml`), e esta seção só sobrescreve (preços, campos, `hidden: true`, cosméticos extras). A categoria é sempre gerada. |
| `cosmetics.menus.categories.projectile-trail`, `cosmetics.menus.projectile-trail.*` | slot 27, flecha; `Rastros de Projétil ({page}/{pages})`, `Aleatório` como name tag | O botão da categoria e o que a lista de rastros muda (o resto segue a lista de destruição de cama). |
| `projectile-trail-preview.*` | `previa_aura`, 10.5 19 21.5 yaw 180, `60s`, `1s` | Mundo, spawn, tempo limite, item de voltar e scoreboard da prévia de rastro, como a `aura-preview`. |
| `projectile-trail-preview.kit.items.<slot>`, `projectile-trail-preview.kit.refill-ticks` | os seis itens do kit da gravação; `2` | Os itens dados em vez de uma cena roteirizada (mesmos campos de qualquer item; o slot conta a partir de 0, do 9 em diante é o inventário principal) e os ticks depois de um lançamento até as pilhas que faltam serem completadas (no mínimo 1). Qualquer seção de prévia de cenas com `kit` funciona assim. |
| `aura-preview.*` | `previa_aura`, 10.5 19 21.5 yaw 180, `60s`, `1s` | Mundo, spawn, tempo limite, item de voltar e scoreboard da prévia de aura. Não tem contagem. |
| `preview.world` | `previa_quebra_de_cama` | Pasta do mundo da prévia na raiz do servidor. Sem ela a prévia fica desligada. |
| `preview.spawn`, `preview.bed` | 6.5 11 21.5; pé 6 11 17, cabeceira 6 16 | Onde o jogador fica e a cama que ele quebra. |
| `preview.timeout`, `preview.return-delay` | `120s`, `2s` | Tempo para quebrar a cama e espera depois da animação. |
| `preview.back-item` | slot 8 (o 9º), `BED`, `&aVoltar ao Lobby` | O item que encerra a prévia. O slot conta a partir de 0. |
| `preview.action-bar`, `preview.scoreboard` | `&eQuebre a cama!`, MUXE / Visualizando | Textos da prévia; `{name}` é o cosmético. |
| `messages.preview-config-*` | as respostas do `/bwconfig previa` | Textos dos atalhos das prévias; as respostas genéricas são `messages.config.*` no Muxe-Core. |
| `lobby.menu-item` | slot 3, esmeralda | Item Menu do Bed Wars no lobby de BedWars. |
| `levels.badges`, `levels.rainbow` | a lista de níveis | Insígnia de cada faixa de nível; a partir de `rainbow.from` (1000) os dígitos ganham as cores do arco-íris. |
| `lobby.npcs.<id>` | os exports | `action` (um id de modo, `duelos` ou `nenhuma`), `location`, `skin` assinada, `players` (modos contados em `{players}`) e `lines`; `{blink}` pega a próxima cor de `lobby.blink-colors` a cada 0,5 s. |
| `lobby.stats-npc` | 22.5 81 -1.5 | Posição, linhas, tamanho da barra de XP e `labels` (alturas das linhas) do NPC de estatísticas. |
| `lobby.npc-labels` | os exports | `bottom`, `gaps` e `spacing` das linhas dos NPCs de modo e de missões. |
| `lobby.tops.spacing.*` | os exports | Espaços `title`, `list`, `entries`, `click` e `periods` dos tops. |
| `missions.*` | os exports | O catálogo e os textos das [missões](../missions/README.pt-BR.md#configuração), mais `fast-win-seconds` (210), `end-stone-radius` (3), `end-stone-blocks` (10) e `bedbug-assist-seconds` (10). |
| `lobby.tops.boards.<id>` | os quatro tops | `category` (`FINAL_KILLS`, `WINS`, `WINSTREAK`, `LEVEL`), `title`, `location` do suporte do topo, `periods` na ordem de exibição e `modes`. |
| `lobby.tops.*` | os exports | Textos de cabeçalho, posição, vazio (`Ninguém` - `0`), clique e períodos, `label-above-stand` (2.38125, onde o cliente 1.8 desenha o nome de um suporte em tamanho cheio), `refresh-seconds` (60), `click-sound` (`CLICK`, vazio para nenhum) e `xp-value` (`{xp} XP`, o top de nível fora do Total). |
| `lobby.tops.selector.*` | o export | `location` do suporte do `SELECIONAR MODO`, `list-offset` e `footer-offset` (0.375 e 1.90625 abaixo dele), `list-spacing` (0.28125), `visible-modes` (5) e os textos de cabeçalho, rodapé, selecionado, outros e `general-name`. |
| `lobby.scoreboard.*` | anexo 1 | `title`, `lines` (`{level}`, `{bar}`, `{players}`; `{modes}` vira as linhas dos modos), `mode-lines` (`{mode}` e os contadores), `default-modes` (1v1, solo), `shown-modes` (2) e `bar-segments` (10). |
| `lobby.menus.*` | os exports | Menus de modo, duelos, mapas, modo do top e estatísticas. |

## Pacotes

| Tipo | De → para | Entrega | Para quê |
|---|---|---|---|
| `bedwars.rooms` | servidores de BedWars → `lobby-group` | volátil, 10 s | Salas (código, modo, arena, aberta, jogadores, máximo) e arenas prontas do servidor, a cada 2 s. |
| `bedwars.join_request` | lobby do BedWars → um servidor de BedWars | pedido, 5 s | Reserva uma sala de um modo, opcionalmente de uma arena ou uma sala específica. |
| `bedwars.join_reply` | servidor de BedWars → lobby | resposta | Se uma sala foi reservada, o código dela, e se ela foi criada para este pedido. |
| `minigame.arena_saved` | lobby do BedWars → grupo `bedwars` | padrão | Uma arena foi salva; os servidores de BedWars a recarregam do PostgreSQL. |

## Dados

| Arquivo | O que guarda |
|---|---|
| `core.arenas` (jogo `bedwars`) | Uma linha por arena: modo, `ready`, a definição (nome de exibição, times, pontos e alturas, em YAML) e o mapa no [formato de mapa da Muxe](../../decisions/0009-own-map-format-and-match-cells.pt-BR.md). Migração `V16__create_arenas.sql`. Compartilhada por todos os servidores de BedWars. |
| `core.owned_cosmetics`, `core.selected_cosmetics` | Cosméticos possuídos e selecionados, categoria `bedwars-cama` (e `bedwars-vitoria`, `bedwars-kill-final`, `bedwars-aura`, `bedwars-rastro`, `bedwars-mensagem-kill` e `bedwars-madeira`). Veja [Créditos e cosméticos](../credits/README.pt-BR.md). |
| `bedwars.player_stats` | Uma linha por jogador, modo (e `geral`), período (`diario`, `semanal`, `mensal`, `total`) e início do período, com todos os contadores e o XP. Migração `V1__create_player_stats_and_favourite_maps.sql`. |
| `bedwars.recorded_matches` | O id de cada partida já salva, reivindicado na mesma transação das estatísticas, então uma partida salva duas vezes conta uma vez só. Migração `V2__create_recorded_matches.sql`. |
| `bedwars.favourite_maps` | Arenas favoritas de cada jogador. |
| `bedwars.preferences` | As preferências de BedWars de cada jogador (`show_level`, `tnt_timer`, `chest_deposit`). Migração `V4__create_preferences.sql`. |
| `bedwars.quick_buy` | A compra rápida de cada jogador: os 21 ids de item em ordem (`''` numa posição vazia). Migração `V3__create_quick_buy.sql`. |
| `bedwars.favourite_team_color` | A cor de time preferida de cada jogador (`team_color`, o id da cor). Quem não tem preferida não tem linha. Migração `V6__create_favourite_team_color.sql`. |
| `bedwars.hotbar_layout` | O layout da hotbar de cada jogador: 9 textos, um por slot, cada um uma lista de entradas separadas por vírgula (`c:<id da categoria>` ou `i:<id do item>`, `''` num slot vazio). Migração `V5__create_hotbar_layout.sql`. |
| `<server>/previa_quebra_de_cama/` | O mundo da prévia. Vai só para o lobby de BedWars, nunca para o repositório. |
| `<server>/previa_aura/` | O mundo da prévia de aura. Vai só para o lobby de BedWars, nunca para o repositório. |

Comandos e consultas em `br.com.muxe.minigame.bedwars.stats`: `RecordMatchCommand`, `ToggleFavouriteMapCommand`, `FindPlayerStatsQuery`, `FindTopQuery`, `FindFavouriteMapsQuery`.

## Métricas

`muxe_matches_active{game="bedwars"}`, `muxe_arenas_available{game="bedwars"}`, `muxe_arenas_disabled{game="bedwars"}`.

## Integrações

- [Lobby](../lobby/README.pt-BR.md): `/jogar bedwars`.
- [Créditos e cosméticos](../credits/README.pt-BR.md): preços, compras e posse.
- [Motor de jogo](../../architecture/game-engine/README.pt-BR.md).
- [API de minigames](../../architecture/minigame-api/README.pt-BR.md).
- [Party](../party/README.pt-BR.md): só o líder da party escolhe um modo, um mapa ou "jogar novamente"; a party vai para uma sala com vaga para todos e fica em um time. Quando a party precisa ser dividida entre times, `AVISO Esta partida não conta estatísticas.` é dito e seus jogadores ficam fora do `RecordMatchCommand` e do XP da partida.

## Critérios de aceitação

- [x] Os modos são carregados da configuração (testes do motor).
- [x] Os nove modos têm os limites e os times da especificação (`BedWarsSchemaTest`).
- [x] Uma arena pode ser configurada no jogo, com uma checklist do que falta (testes do motor).
- [x] Placares, indicadores dos times e as regras de forja, linha do tempo, kills, níveis, XP e ranking seguem a especificação (`RoomTest`, `GameRulesTest`, `GeneratorTimerTest`, e os testes da API de minigames `MatchTest`, `MatchRuntimeTest`, `KillCreditTest`, `TimelineTest`, `RankingTest`).
- [x] Nomes e lores da loja e das melhorias seguem os exports (`ShopCatalogTest`, `UpgradeCatalogTest`).
- [x] Os menus do editor de hotbar, o layout com uma entrada por slot, o armazenamento e a colocação dos itens comprados seguem as regras acima (`HotbarLayoutTest`, `HotbarPlacementTest`, `HotbarEditorTest`, `HotbarHandlersTest`, `PostgresHotbarRepositoryIntegrationTest`).
- [x] O menu de cor preferida, o armazenamento e a divisão dos times pela cor preferida seguem as regras acima (`FavouriteColorMenuTest`, `FavouriteColorHandlersTest`, `PostgresFavouriteColorRepositoryIntegrationTest`, `TeamAssignerTest`, `RoomTest`).
- [x] As 13 animações terminam sem deixar nada e seguem os tempos gravados (`BedAnimationsTest`).
- [x] Estados, preços, ordem e lore dos cosméticos seguem os exports (`CosmeticCatalogTest`).
- [x] Os conjuntos de mensagens de kill, as prévias gravadas, de quem é o conjunto usado, os contadores da vida inteira, a alternativa e o sorteio seguem as regras acima (`MessageSetsTest`, `KillAnnouncerTest`, `KillMessagesTest`, `LifetimeTallyTest`, `OrdinalTest`, `KillMessageCosmeticTest`).
- [x] O menu de tipos de madeira, os cosméticos por rank e a madeira que a loja entrega seguem as regras acima (`WoodTypeCosmeticTest`, `WoodTypesTest`, `WoodDeliveryTest`, `CosmeticAccessTest`, `CosmeticListSettingsTest`).
- [x] Mortes, assistências, camas perdidas, XP e resultados das partidas são contados (`RoomStatsTest`); períodos, contadores, tops e favoritos são salvos (`StatsHandlersTest`, `StatCountersTest`, `StatsPeriodTest`, `PostgresBedWarsStatsRepositoryIntegrationTest`).
- [x] O matchmaking escolhe a sala aberta mais cheia ou pede uma ao servidor mais vazio (`MatchmakerTest`).
- [x] NPCs, menus, tops, o seletor de modo, insígnias de nível, o NPC de estatísticas e a scoreboard do lobby seguem os exports e o anexo 1 (`LobbyViewTest`, `LobbyFormatTest`).
- [ ] Uma partida completa conferida no jogo.
- [ ] O editor de hotbar e a colocação das compras conferidos no jogo.
- [ ] NPCs, menus, matchmaking, escolha de mapa e tops do lobby conferidos no jogo.
- [x] Os atalhos das prévias gravam as chaves certas, acham as duas metades de uma cama por qualquer uma, recusam valores ruins e aplicam ao vivo enquanto as sessões em andamento mantêm as suas configurações (`PreviewEditsTest`, `BedBlocksTest`, `BedWarsConfigCommandTest`).
- [x] O catálogo e o menu de rastros, a tag do `Ultra`, qual rastro é usado, o ciclo do voo, os tipos de projétil e o kit da prévia seguem as regras acima (`ProjectileTrailCosmeticTest`, `CosmeticIconsTagTest`, `TrailCosmeticsTest`, `TrailFlightTest`, `TrailFlightsTest`, `ProjectileKindsTest`).
- [ ] Todas as animações e a prévia conferidas no jogo.
- [ ] Os rastros de projétil e o kit da prévia deles conferidos no jogo.
- [ ] `/bwconfig previa` conferido no jogo, no lobby.
- [ ] As mensagens de kill numa partida de verdade e os tipos de madeira na loja conferidos no jogo.

## Falhas e recuperação

Se o Muxe-Core não estiver habilitado, o plugin se desabilita.

## Limitações conhecidas

- MEGA e PILARES ainda não são modos, então os NPCs deles não abrem nada.
- O arco do kit da prévia de rastro brilha em vez de ter Infinidade I (encantamentos não podem ser lidos da configuração sem servidor nos testes); a flecha que ele atira volta pelo reabastecimento.
- Quem chega a um servidor de BedWars sem reserva ainda entra numa sala do `default-mode`.
- O lobby só conhece as salas dos servidores que publicaram nos últimos 10 segundos; a lista de um servidor que para é esquecida depois disso.
- Os outros itens especiais da loja, os dragões e os títulos de vitória e derrota ainda não entraram.
- Um reinício ou crash encerra todas as partidas do servidor sem compensação.
- `Skin dos Vendedores`, `Rastros de Projétil` e a loja de cosméticos ainda não existem. A cor preferida é lida quando o jogador entra no servidor, então uma escolha feita em outro servidor só é vista depois que o jogador entrar de novo.
- Uma compra atualiza o menu na hora, mas outro servidor só vê quando o jogador entra nele de novo.
- Os contadores do `Estatístico` são os totais lidos quando o jogador entra na sala mais o que acontece naquela partida; não são atualizados durante ela. O conjunto de mensagens e a madeira selecionados são lidos quando o jogador entra no servidor, como os outros cosméticos.
