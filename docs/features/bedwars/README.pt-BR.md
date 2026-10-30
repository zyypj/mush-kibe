[English](README.md) | **Português**

# BedWars

BedWars para a rede Muxe. Fica no `minigames:bedwars` (`Muxe-BedWars`) e roda nos servidores do grupo `bedwars`.

## Histórias de usuário

- Como jogador, quero proteger a cama do meu time e destruir a dos outros, para que o meu time seja o último de pé.

## Como funciona

O plugin carrega os nove modos, dá à staff o [setup de arenas](../../architecture/game-engine/README.pt-BR.md#setup-de-arenas) e roda as partidas do servidor, várias ao mesmo tempo, cada uma na própria [célula](../../decisions/0009-own-map-format-and-match-cells.pt-BR.md) do mundo das partidas.

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

### Partidas

Quem chega num servidor de BedWars volta para a partida que deixou, se ainda puder, e senão vai para uma sala do `default-mode` (ou do modo escolhido com `/bedwars entrar <modo>`): a sala mais cheia esperando jogadores, ou uma sala nova numa arena pronta aleatória desse modo.

| Fase | O que acontece |
|---|---|
| Espera | Os jogadores esperam na área de espera acima do mapa, no modo aventura, sem dano nem fome. A hotbar tem a esmeralda `&aMenu do Bed Wars` (slot 5, ainda não faz nada) e `&aVoltar ao Lobby` (slot 9). O placar segue o anexo 2, e o anexo 3 quando a contagem roda. |
| Contagem | A [contagem para iniciar](../../architecture/game-engine/README.pt-BR.md#contagem-para-iniciar) da engine, com os títulos `&a30 segundos`, `&610 segundos` e `&c5` a `&c1`. |
| Jogando | Os times são sorteados, a área de espera é removida, os jogadores nascem na ilha com couro completo na cor do time e uma espada de madeira. Os NPCs da loja e das melhorias aparecem com os hologramas, e os geradores começam. |
| Fim | Quando sobra um time, todos recebem o resultado (anexo 11), centralizado: vencedor, tempo da partida, top kills finais e top camas quebradas, com os nomes na cor da tag. Depois de 30 segundos todos vão para o `lobby-group` e a sala fecha. |

**Durante a partida:**

- **Times:** o placar segue o anexo 5 (2 ou 4 times) ou o anexo 12 (8 times). Um time mostra `&a✔` enquanto tem cama e alguém conectado, `&e✔` quando todos saíram mas ainda podem voltar, o número de jogadores vivos depois que a cama cai, e `&c✘` quando é eliminado. Times sem ninguém já começam fora.
- **Name tags e tab:** a letra e a cor do time (`A` em azul), pelo `MuxeCore.nameTags()`, mais a vida no tab.
- **Chat:** só a sala vê, com o time antes do formato de sempre: `[A] medalha tag nome`.
- **Camas:** quebrar a cama de um inimigo manda `CAMA DESTRUÍDA` para todos e `+100 XP` para quem quebrou. Um integrante do time que estava fora é eliminado, e a kill final vai para quem quebrou a cama. Ninguém quebra a própria cama.
- **Mortes:**
  - A kill vai para o último inimigo que bateu nos últimos 15 segundos. Uma morte final sem golpe vai para quem quebrou a cama.
  - Quem mata recebe o ferro, ouro, diamantes e esmeraldas da vítima, com linhas como `+28 Ferros` e `+12 XP` (kill) ou `+60 XP` (kill final).
  - Cair abaixo da altura do void da arena mata na hora (`morreu no void`).
  - Com cama, o jogador assiste como espectador e renasce depois de 5 segundos, protegido por 4 segundos, com a armadura comprada e a picareta e o machado um nível abaixo. Sem cama, o jogador sai (`KILL FINAL!`), e o último jogador do time a sair elimina o time (`TIME ELIMINADO`).
- **Blocos:** só blocos colocados durante a partida podem ser quebrados. Nada pode ser colocado perto dos spawns (5 blocos), dos NPCs (1) e dos geradores (1), nem acima do limite de construção da arena (`Você não pode colocar blocos aqui.`). Explosões só quebram blocos colocados, nunca vidro. A TNT acende sozinha e a bola de fogo é lançada com o botão direito.
- **Geradores:** o ferro da ilha cai a cada 0,9 s, tantos lingotes quanto jogadores por time (até 4), o ouro a cada 7 s, cada um com um limite de itens no chão num raio de 3 blocos. Diamante e esmeralda mostram nível e contagem num holograma e aceleram aos 6, 12, 18 e 24 minutos. Ferro e ouro coletados são copiados para os companheiros a até 2 blocos.
- **Eventos:** depois da Esmeralda III, todas as camas são destruídas (30 min), a morte súbita é anunciada (40 min; os dragões ainda não entraram) e a partida termina (42 min), empatada se sobrar mais de um time.
- **Desconexão:** quem sai com a cama de pé pode voltar em até 3 minutos e renasce 5 segundos depois (`reconectou`). O `/l` avisa antes `Você desconectou de uma partida...`. Sair sem cama elimina o jogador, dando a kill ao último golpe ou a quem quebrou a cama.

### Loja

O NPC da loja abre os menus dos exports: Compra Rápida (o layout do export) e as sete categorias, com a aba da categoria aberta marcada por um vidro verde.

- Um item custa ferro, ouro ou esmeraldas do inventário. O nome fica vermelho quando não dá para comprar, com `Você não tem <recurso> o suficiente.`
- Cada compra diz `Você comprou <item>.`
- A lã vem na cor do time. Uma espada substitui a de madeira. A armadura dá bota e calça e não pode ser comprada abaixo da que o jogador tem. A picareta e o machado sobem um nível por compra. A tesoura é permanente.
- Ainda não funcionam: o shift-click (editar a compra rápida), o editor de hotbar, e os itens especiais Percevejo, Golem de Ferro, Leite Mágico, Torre Compacta, Ovo das Pontes, Rastreador e Estilingue, que avisam que chegam em breve e não cobram nada.

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

## Comandos

| Comando | Permissão | Descrição |
|---|---|---|
| `/arena ...` | `muxe.minigame.setup` | Cria e configura arenas. Veja o [setup de arenas](../../architecture/game-engine/README.pt-BR.md#setup-de-arenas). |
| `/bedwars entrar <modo>` (`/bw`) | nenhuma | Leva o jogador para uma sala desse modo, enquanto espera. |
| `/bedwars iniciar` | `muxe.bedwars.admin` | Começa a contagem em 10 segundos sem o mínimo de jogadores, para testes. |

## Permissões

| Permissão | Padrão | Descrição |
|---|---|---|
| `muxe.minigame.setup` | op | Permite o `/arena`. |
| `muxe.bedwars.admin` | op | Permite o `/bedwars iniciar`. |

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
| `lobby-group` | `lobby` | Para onde os jogadores vão quando a partida acaba ou quando saem da espera. |
| `default-mode` | `solo` | O modo da sala em que o jogador entra ao chegar. |
| `timings.*` | 5 s renascer, 180 s voltar, 30 s reiniciar, 4 s proteção, raio da ilha 17, raios de proteção 5/1/1 | Tempos da partida e áreas protegidas. |
| `countdown.titles` | `30`, `10`, `5`...`1` | Títulos da contagem para iniciar. |
| `forges.*` | os valores da especificação | Ferro, ouro e esmeralda da ilha, e os três níveis de diamante e esmeralda com o segundo de início. |
| `events.*` | 1800, 2400, 2520 | Segundos dos eventos de camas destruídas, morte súbita e fim de jogo, e os nomes deles no placar. |
| `xp-rewards.*` | 10, 5, 250, 100, 12, 60 | XP por minuto, por companheiro, vitória, cama, kill e kill final. |
| `hotbar.waiting.*` | esmeralda e cama | Itens da sala de espera. |
| `scoreboard.*` | anexos 2, 3, 5 e 12 | Linhas do placar; `{teams}` e `{event}` viram as linhas dos times e o próximo evento. |
| `holograms.*` | anexos 7 e 8 | Hologramas dos NPCs e dos geradores. |
| `shop.*`, `upgrades.*` | os exports | Categorias, itens, preços e textos da loja; melhorias, níveis e preços. |
| `messages.*` | anexos 4, 6, 9, 10, 11, 13, 14, 15 | Todas as mensagens da partida. `setup-*` sobrescreve as mensagens de setup da engine. |

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
- [x] Placares, indicadores dos times e as regras de forja, linha do tempo, kills, níveis, XP e ranking seguem a especificação (`RoomTest`, `GameRulesTest`, `GeneratorTimerTest`).
- [x] Nomes e lores da loja e das melhorias seguem os exports (`ShopCatalogTest`, `UpgradeCatalogTest`).
- [ ] Uma partida completa conferida no jogo.

## Falhas e recuperação

Se o Muxe-Core não estiver habilitado, o plugin se desabilita.

## Limitações conhecidas

- As salas só existem no servidor onde o jogador chega; o lobby do BedWars, os NPCs dos modos, a escolha de mapa e o `/retornar` a partir do lobby ainda não entraram, então o jogador escolhe o modo com `/bedwars entrar`.
- Estatísticas, níveis e XP aparecem mas não são salvos.
- Os itens especiais da loja, o editor da compra rápida, os dragões, os blocos girando sobre os geradores e os títulos de vitória e derrota ainda não entraram.
- Um reinício ou crash encerra todas as partidas do servidor sem compensação.
