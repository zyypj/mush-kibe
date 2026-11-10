[English](README.md) | **Português**

# Sala custom

Uma sala privada que um líder de party Ultra+ cria para a sua party, com todas as regras do jogo abertas para mudar: times, geradores, loja, melhorias, eventos, itens, knockback e outros. Uma contagem de 30 segundos começa quando um segundo jogador está na sala; o líder pode pausar, iniciar a partida na hora, reiniciar a sala e fechar.

## Histórias de usuário

- Como jogador Ultra+, quero criar uma sala com as minhas regras, para jogar com meus amigos do nosso jeito.
- Como líder de party, quero que quem eu convido entre na minha sala, para nunca precisar mandar um código.
- Como líder da sala, quero salvar as opções e compartilhá-las por código, para usar de novo ou passar para outra pessoa.

## Como funciona

**Criar uma sala (lobby).**

1. `Sala customizada (/salacustom)` no `Selecionar jogo` (slot 35) ou `/salacustom`. Quem não tem `muxe.customroom.create` (Ultra+) recebe a loja do Ultra+. Um membro de party que não é o líder recebe `custom-room-not-leader`.
2. `Selecionar modo` lista os jogos da referência (Bed Wars, Duels, Sky Wars, CTF, Party, Block Party, Murder, Build Battle, Quick Builders, Hide and Seek, Nascó Mandou, Spleef). Bed Wars abre `Selecionar modo (Bed Wars)` e Duels abre `Selecionar modo (Duels)`; os outros não fazem nada. Um modo só funciona quando algum servidor de jogo o publica (hoje os modos do BedWars); Mega, `Modos temporários` e todos os modos de Duels não fazem nada.
3. `Criando sala...`: o modo (`Clique para trocar de jogo.`), `Mapas`, `Opções`, `Criar sala`, `Voltar` e `Predefinições`.
4. `Criar sala` diz `Criando sala...`, pede uma sala a um servidor do jogo (`custom_room.create_request`) e, quando ele responde, diz `Enviando para <código>...` e manda o líder e os membros online da party para lá.

**Mapas.** `Selecione mapas (n/m)` lista as arenas prontas do modo. Um clique seleciona só aquele mapa (ou limpa quando era o único), shift adiciona ou remove. Com vários mapas, cada reinício sorteia um; sem nenhum, qualquer arena do modo. `Carregar mapa externo` não faz nada. Dentro da sala a mudança vale no próximo reinício.

**Opções.** Todo menu de opções é desenhado a partir do esquema que o servidor de jogo publica, com os textos da referência: valores em `&a`, valores da loja e das melhorias alterados com `*` e ícone brilhando, opções liga/desliga verdes e brilhando quando ligadas, vermelhas quando desligadas. Um decimal ou um nome abre uma bigorna com um papel com o valor atual; ao digitar, o resultado vira `&a<opção>: &f<valor>` e clicar no resultado aplica e reabre o menu. Um valor inválido diz `Valor inválido.`.

| Menu | O que muda |
|---|---|
| `Opções` | `Tamanho do time` (até os jogadores do modo: 8 no Solo, 16 em Duplas...) e os links abaixo. |
| `Times` | `Quantidade de times` (de 2 até os times do modo) e cada `Time #N`: botão esquerdo ativa ou desativa, direito renomeia (bigorna), Q restaura o nome. Um time renomeado mostra `Nome original` e aparece com o novo nome e a primeira letra dele no placar, e essa letra também vira o prefixo dos jogadores do time na etiqueta, no tab e no chat. |
| `Geradores` | Velocidade da forja, geradores de diamante e esmeralda ligados ou não, a velocidade deles (passos de 0,25x) e quantos itens soltam. |
| `Itens da loja (n/m)` | Todo item da loja, cada nível separado. `Editando <item>...`: status, preço (esquerdo: valor na bigorna, direito: moeda ferro → ouro → esmeralda → diamante) e limite (direito: nenhum → por time → por jogador → no inventário; esquerdo: quantidade). `Tudo grátis` deixa tudo grátis. |
| `Melhorias (1/1)` | Toda melhoria do time, mais a `Proteção da Cama` (desligada por padrão, só na sala custom). Uma melhoria desativada aparece como barreira. `Editando <melhoria>...`: status e o preço e a moeda de cada nível. `Tudo grátis`. Linhas de nível compridas quebram na última vírgula que cabe em 40 caracteres, como na referência (`Nível 2: Madeira, Lã, Pedra do Fim,` / `Grátis`). |
| `Eventos` | Diamante II, Esmeralda II, Diamante III, Esmeralda III, Sem Camas, Deathmatch e Fim do Jogo: o tempo depois do evento anterior (esquerdo +30s, direito -30s, shift ±1min) e Q para desligar (menos o fim do jogo). |
| `Itens` | `Tipo da Bola de fogo` (`Padrão` ou `Fireball Fight`: 1,5x mais para cima e metade do dano) e `Recarga do Estilingue`. |
| `Alterar knockback` | Os catorze valores do [perfil de knockback](../../development/core-api/README.pt-BR.md#knockback) do modo, aplicados na hora a todos da sala. |
| `Outros` | `Totem` (`Cama` ou `Ovo do Dragão`: o ovo fica onde estava a cama e quebra com o botão esquerdo) e `Criativo` (todos no criativo). |

**Predefinições.** `Predefinições` lista as predefinições do jogador para o jogo (PostgreSQL, sem limite). Botão esquerdo carrega (modo, mapas e opções), direito mostra o código no chat, Q exclui. `Salvar novo` pede o nome numa bigorna; `Usar código` pede um código e carrega aquela predefinição.

**Na sala (servidor de jogo).**

- O código aparece no placar como `<código> (C)`. A sala nunca aparece no matchmaking e só recebe jogadores com lugar reservado para ela.
- Hotbar da espera: `Selecionar time` (slot 4), o menu do jogo (slot 5), `Menu da sala (/menu)` (slot 8, só o líder) e `Voltar ao Lobby` (slot 9).
- `Selecionar time`: cada time ativo com `Jogadores: n/tamanho`, e `Espectador`. A escolha vale até o início; quem não escolheu vai para os times mais vazios; espectadores assistem a partida.
- `Sua sala` (`/menu` ou o item, só o líder): o modo (trocar reinicia uma sala em espera), `Mapas`, `Opções`, `Iniciar sala` (inicia a partida na hora, o líder pode iniciar sozinho), `Pausar contador` (para a contagem até o `Iniciar sala`; quem chega depois não a recomeça), `Reiniciar sala`, `Fechar sala` e `Predefinições`. Durante a partida: `Pular evento` (roda o próximo evento na hora), `Reiniciar sala` e `Fechar sala`; o líder também encontra o `Menu da sala (/menu)` (o item `hotbar.room-menu`) no primeiro slot da última linha da `Compra rápida` do BedWars, e clicar nele abre `Sua sala`.
- Uma partida custom não conta nada: nem estatísticas, XP, missões ou winstreak.
- A contagem começa em 30 segundos quando chega o segundo jogador e para se a sala voltar a ter um só.
- Quando a partida acaba, a sala volta para a espera com o mesmo código e as mesmas opções (`A sala foi reiniciada.`), e a contagem recomeça.
- **Proteção da Cama** monta uma pirâmide em volta da cama do time (ou do ovo do dragão) quando comprada: o nível I põe madeira encostada na cama, lã do time em volta e vidro da cor do time por fora; o nível II usa pedra do fim no lugar do vidro; o nível III acrescenta uma camada de vidro por fora da pedra do fim. Cada camada fica um bloco mais longe da cama (contando pelos eixos), da altura da cama para cima. Só ar e blocos do nível anterior são trocados, e todo bloco pode ser quebrado como um bloco colocado. Quando ativada, ela fica no slot 19 de `Melhorias do Time`, que passa de 3 para 4 linhas.
- **Itens só da sala custom**: a Obsidian funciona (desligada por padrão). Troca Congelante, Ponte de Gelo, Para-quedas, Rebobinar, Spider, Loja Portátil, Baú do Fim e Ressurreição aparecem com o texto, o preço e a moeda da referência, começam desligados e respondem "em breve" na compra; estão anotados no `pendencias.txt`.

**Party.** Quem entra na party do líder (convidado ou pelo `/party abrir`) é puxado para a sala; quem sai da party vai para o lobby. A sala fecha (`A sala foi fechada.`, todos para o lobby) quando o líder sai da sala ou do servidor, ou deixa de liderar a party.

### Idiomas

Todos os menus, opções, valores e mensagens da sala seguem o [idioma](../language/README.pt-BR.md) de quem lê.

- Os textos fixos dos menus da sala ficam em `custom-room.texts`, no `config.yml` e no `lang/en.yml` do Muxe-Core.
- O esquema que um jogo manda aos lobbies pode trazer uma seção `lang.en`, nos mesmos caminhos do esquema. O `SchemaReader` coloca essa seção por cima do português para quem lê em inglês: títulos das páginas, nomes das opções, descrições, rótulos dos valores e nomes das moedas.
- A página de knockback lê os textos de `custom-room.knockback`, no config do jogo.
- Os nomes de time digitados pelo líder aparecem como foram digitados.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/salacustom` (`/salacustomizada`, `/criarsala`) | Abre o `Selecionar modo` para criar uma sala custom. Só nos lobbies. | `muxe.customroom.create` |
| `/menu` (`/sala`) | Abre `Sua sala`. Só o líder de uma sala custom. | Nenhuma |

## Permissões

| Permissão | Concede |
|---|---|
| `muxe.customroom.create` | Criar salas custom. Dada ao `ultra+` pela migração V19. |

## Configuração

`config.yml` do Muxe-Core, `custom-room`:

| Chave | Padrão | Descrição |
|---|---|---|
| `selector.*` | a referência | `Selecionar modo`: título, linhas, slot e título do voltar, lore e os jogos (`menu` aponta o menu de modos). |
| `mode-menus.<id>.*` | Bed Wars, Duels | Título, linhas, slot do voltar, `game` e os modos (pela id do modo). |
| `room-menu.*` | a referência | `Criando sala...` e `Sua sala`: slots e itens (`skull-texture` nas cabeças), `playing-slot` de reiniciar e fechar, `skip-event`. |
| `maps-menu.*`, `presets-menu.*`, `teams-menu.*` | a referência | Slots, itens e textos dos menus de mapas, predefinições e times. |
| `hotbar.team`, `hotbar.room-menu` | slots 3 e 7 | Itens da espera de uma sala custom. |
| `texts.*` | veja `RoomTexts` | Qualquer linha dos menus de opções (`value`, `click-change`, `limit-team`...). |
| `lobby-group` | `lobby` | Grupo que recebe os catálogos. |
| `code-suffix` | ` (C)` | Depois do código da sala no placar. |

Mensagens (`messages` do Muxe-Core): `custom-room-creating`, `custom-room-sending`, `custom-room-unavailable`, `custom-room-not-leader`, `custom-room-invalid-value`, `custom-room-only-leader`, `custom-room-not-in-room`, `custom-room-closed`, `custom-room-restarted`, `custom-room-paused`, `custom-room-team-full`, `custom-room-team-chosen`, `custom-room-spectator-chosen`, `custom-room-event-skipped`, `custom-room-mode-changed`, `custom-room-preset-*`.

`config.yml` do BedWars: `shop.categories.<id>.custom-items` e `upgrades.custom-list` guardam os itens e melhorias que só a sala custom lista, e `enabled: false` num item ou melhoria faz ele começar desligado. `custom-room`: as páginas fixas (`Opções`, `Times`, `Geradores`, `Eventos`, `Itens`, `Outros`), os títulos das páginas geradas e a moeda diamante. As páginas da loja vêm de `shop`, as melhorias de `upgrades`, os tempos dos eventos de `forges`/`events` e a página de knockback do perfil do modo. O BedWars ganha a mensagem `purchase-limit`.

## Pacotes

| Id | Destino | Política | Para quê |
|---|---|---|---|
| `custom_room.catalog` | grupos `lobby` e o `lobby-group` do jogo (BedWars: `bedwars-lobby`) | volátil, 30 s | A cada 10 s de cada servidor de jogo: o jogo, o esquema das opções (YAML em pedaços), os modos e as arenas prontas. |
| `custom_room.create_request` | um servidor de jogo | 5 s | Líder, jogo, modo, mapas, opções e membros online da party. |
| `custom_room.create_reply` | o lobby | 5 s | Se a sala foi criada e o código. |

## Dados

`core.custom_room_presets` (V19): `id`, `code` (único, 8 caracteres), `owner`, `game`, `name`, `mode`, `maps` (separados por vírgula), `options` (linhas `chave=valor`), `created_at`.

## Métricas

Nenhuma.

## Integrações

- [Party](../party/README.pt-BR.md): o líder, os membros puxados, a sala fechando quando a liderança é perdida.
- [API de minigames](../../architecture/minigame-api/README.pt-BR.md#salas-custom): a sala no servidor de jogo.
- [BedWars](../bedwars/README.pt-BR.md): aplica todas as opções.
- [Knockback](../../development/core-api/README.pt-BR.md#knockback): a página de knockback e o perfil da sala.

## Critérios de aceite

- [ ] Um jogador Ultra+ clica em `Sala customizada`, escolhe Bed Wars Solo e `Criar sala`; o chat diz `Criando sala...` e depois `Enviando para <código>...` e ele chega numa sala de espera com `<código> (C)` no placar.
- [ ] Um jogador sem Ultra+ recebe a loja do Ultra+.
- [ ] Um segundo jogador na sala inicia uma contagem de 30 segundos; `Pausar contador` para e `Iniciar sala` inicia a partida na hora.
- [ ] Um membro convidado para a party depois da sala criada é mandado para ela.
- [ ] O líder saindo fecha a sala e manda todos para o lobby.
- [ ] Mudar `Vertical` para 0.8 em `Alterar knockback` muda o knockback de todos da sala.
- [ ] O fim da partida volta a sala para a espera.
- [ ] Nada de uma partida custom chega às estatísticas.

## Falhas e recuperação

- Nenhum servidor publica o jogo: o modo não faz nada, ou `Criar sala` diz `custom-room-unavailable`.
- O servidor de jogo não responde em 5 s: `custom-room-unavailable`; nada foi criado.
- O líder nunca chega: a sala vazia fecha depois de `timings.empty-room-seconds`.
- Um servidor de jogo reinicia: as salas dele somem; os jogadores voltam a um lobby como em qualquer reinício.
- Salvar uma predefinição falha: `custom-room-preset-failed`.

## Limitações conhecidas

- Só modos do BedWars podem ser criados; os outros jogos e modos dos menus aparecem e não fazem nada.
- `Carregar mapa externo` não faz nada.
- Troca Congelante, Ponte de Gelo, Para-quedas, Rebobinar, Spider, Loja Portátil, Baú do Fim, Ressurreição, Torre Compacta, Ovo das Pontes e Rastreador respondem "em breve". Os limites padrão da referência desses itens não estão marcados.
- O limitador de cliques do knockback pula empurrões que vêm cedo demais depois do último; os ticks de cada nível são um palpite (`knockback.click-limiter-ticks`).
