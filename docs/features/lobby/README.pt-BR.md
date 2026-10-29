[English](README.md) | **Português**

# Lobby

O hub principal, onde os jogadores chegam depois de fazer login e escolhem um jogo. Fica no `servers:lobby` (`Muxe-Lobby`). A rede pode ter vários lobbys (`lobby-1`, `lobby-2`, …), todos no grupo `lobby`; cada jogo também terá os seus próprios servidores de lobby.

O plugin do lobby cuida do spawn e da chegada, do voo, da proteção, da scoreboard, da boss bar e do `/jogar`. A hotbar, os seus menus (seletor de jogo, perfil, seletor de lobby) e a visibilidade dos jogadores vêm do Muxe-Core, então todo lobby da rede os recebe: veja [Hotbar](../hotbar/README.pt-BR.md).

## Histórias de usuário

- Como jogador, quero chegar a um spawn limpo e seguro, ver a rede num relance e escolher o que jogar.
- Como VIP ou membro da equipe, quero voar no lobby e desligar o voo quando quiser.
- Como construtor, quero entrar num modo de construção para editar o lobby, enquanto jogadores comuns não conseguem quebrá-lo.

## Como funciona

### Chegada

- Depois do login, o proxy manda o jogador para o lobby com menos jogadores (veja [Roteamento da rede](../network-routing/README.pt-BR.md)).
- Ao entrar: modo Aventura, vida e fome cheias, experiência zerada, e teleporte para o spawn configurado (ou o spawn do mundo principal). O Muxe-Core limpa o inventário e a armadura e dá a [hotbar](../hotbar/README.pt-BR.md).
- Quem tem `muxe.lobby.fly` chega `arrival.fly-height` blocos (5) acima do spawn, já voando. Os outros chegam no spawn. Cair abaixo de `protection.void-level` manda o jogador de volta do mesmo jeito.
- O `/fly` (`/voar`) liga o voo (`&eVocê ativou o &bmodo voar&e!`) ou desliga (`&cVocê desativou o modo voar!`). Desligar faz quem está voando descer. Entrar no lobby ou cair no void devolve o voo a todos que têm `muxe.lobby.fly`.
- O [parkour](../parkour/README.pt-BR.md) desliga o voo enquanto o jogador faz a corrida e o devolve quando ela acaba. Ninguém consegue levantar voo durante a corrida, nem depois do `/fly`.
- As mensagens de entrada e saída ficam escondidas.
- Meio segundo depois de entrar, o jogador ouve um som de level up e vê o título `&b&lMUXE` com o subtítulo `&eBoas-vindas!`. Essa saudação vem do Muxe-Core e acontece em todo servidor `LOBBY`, nunca nos servidores de autenticação ou de jogo.
- Trocar de lobby pelo [seletor de lobby](../hotbar/README.pt-BR.md#seletor-de-lobby), ou usar `/lobby`, não mostra mensagem de "conectando": o jogador só é movido.

### Boss bar e scoreboard

- A boss bar mostra `MUXE` em `&e&l` e troca para `&6&l` a cada meio segundo (`bossbar.frames`, `bossbar.interval`).
- A scoreboard, com o título `&b&lMUXE`, mostra:

```text

Tag: Membro
Créditos: 0

Lobby: #1
Jogadores: 2211

muxe.com.br
```

- `{lobby}` é o número no fim do id do servidor (`lobby-4` mostra `#4`), e `{players}` conta todos os jogadores nos servidores Spigot da rede. As linhas são atualizadas a cada `scoreboard.update-interval`.
- `Tag` mostra a tag que o jogador exibe (`&aVIP`, `&dUltra`, `&cNatal`...), como descrito em [Cargos e tags](../ranks-and-tags/README.pt-BR.md); ela muda em até um segundo depois de um `/tag` ou de um cargo novo. `scoreboard.default-tag` aparece enquanto os cargos do jogador não carregaram. Ainda não existe sistema de créditos: todo jogador mostra `scoreboard.default-credits`. Os valores vêm do `PlayerInfoSource` (`RankInfoSource`).

### Hotbar, menus e visibilidade dos jogadores

Os itens da hotbar (seletor de jogo, perfil, cosméticos, corante de visibilidade e seletor de lobby), os menus que eles abrem e a visibilidade salva dos jogadores vêm do Muxe-Core e são iguais em todos os lobbys. Veja [Hotbar](../hotbar/README.pt-BR.md).

### NPCs, hologramas e loja

NPCs, hologramas e a loja com as caixas registradoras vêm do Muxe-Core e funcionam em todos os servidores, não só aqui. O que a equipe coloca num lobby aparece em todos os lobbys do grupo `lobby`. Veja [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md) e [Loja](../shop/README.pt-BR.md).

### NPCs de jogo

Doze NPCs de jogo ficam em fila na frente do spawn, do x 12.5 ao -9.5 no y 33.375, cada um virado para o spawn e segurando um item do seu jogo. Acima de cada um, o nome em `&b&l` e `&e{players} jogando`, os jogadores online nos servidores vivos do seu grupo (0 quando nenhum está vivo). Clicar num deles manda o jogador para o servidor com menos jogadores do grupo, como o seletor de jogo, ou responde `&cNenhuma sala de &f{name} &cdisponível no momento.`

| NPC | Id | Grupo | Na mão | Linhas |
|---|---|---|---|---|
| HG | `hg` | `hg` | Sopa de cogumelo | nome, contagem |
| MURDER | `murder` | `murder` | Redstone | nome, contagem |
| PVP | `pvp` | `pvp` | Peitoral de ferro | nome, contagem |
| BUILD | `build` | `buildbattle` | Tábuas de carvalho | nome, contagem |
| TREINO: PONTE | `treinoponte` | `treinoponte` | Frasco de experiência | nome, `&eClique aqui!` |
| DUELS | `duels` | `duels` | Espada de diamante | `&d&lNOVIDADE!`, nome, contagem |
| BED WARS | `bedwars` | `bedwars` | Cama | nome, contagem |
| SKY WARS | `skywars` | `skywars` | Olho do fim | nome, contagem |
| THE BRIDGE | `thebridge` | `thebridge` | Argila tingida branca | nome, contagem |
| PARTY | `party` | `party` | Bolo | nome, contagem |
| HIDE AND SEEK | `hideandseek` | `hideandseek` | Bloco de melancia | nome, contagem |
| CTF | `ctf` | `ctf` | Lã vermelha | nome, contagem |

- São NPCs de jogo comuns do grupo `lobby`, então o `lobby-1`, o `lobby-2` e todo lobby que vier depois os mostram.
- Eles ficam no `servers/lobby/placements/import.yml`, no [formato de importação](../npcs-and-holograms/README.pt-BR.md#importando-o-que-já-existia). O grupo de cada NPC é a chave `group`; troque ali antes de importar, ou depois com `/npc acao <id> <grupo>`.
- Skins: todo NPC tem `skin-name: ""` e mostra a skin padrão. As skins dos NPCs de referência foram feitas por outras pessoas e não têm licença para o Muxe, então não estão no repositório (veja [Jurídico](../../legal/README.pt-BR.md)). Para dar uma skin a um NPC, coloque em `skin-name` um nome original cuja skin o Muxe possa usar e importe de novo, ou use `/npc skin <id> <nome>`.

#### Colocando os NPCs

1. Copie o `servers/lobby/placements/import.yml` para `plugins/Muxe-Core/import.yml` em um lobby (qualquer um: os NPCs pertencem ao grupo).
2. Use `/npc importar` nesse lobby, ou reinicie-o. Ele responde `&a12 itens de &fimport.yml &aimportados no grupo &flobby&a.`
3. Confira com `/npc listar`. Importar de novo substitui os doze NPCs pelo que está no arquivo.

### Proteção

Os jogadores não levam dano, não perdem fome, não quebram, não colocam, não largam nem pegam itens, nem movem itens no inventário, então a hotbar fica no lugar. A chuva é cancelada. O fogo nunca queima blocos, nunca se espalha e nunca começa sozinho (lava, raio, espalhamento); só a staff no modo construção consegue acender fogo com a mão, e esse fogo também não queima nada (`FireRules`). O mapa é estático: a física dos blocos fica desligada, então blocos fora do lugar (cana na terra, flor sem chão, tocha sem parede) nunca quebram nem caem; folhas nunca somem, gelo e neve nunca derretem, e água e lava nunca escorrem. Ao ligar, o lobby coloca `randomTickSpeed` em `0` e `doFireTick` em `false` em todos os mundos (`StaticWorld`): no 1.8, cana, flores, plantações e mudas conferem o chão nos ticks aleatórios e quebram sem nenhum evento, e as mudas virariam árvores. Um fogo aceso pela staff ainda pode apagar. Nenhum mob nasce no lobby (spawn natural, spawners, ovos e reprodução são cancelados); só entidades criadas por um plugin (`SpawnReason.CUSTOM`) são aceitas, e os NPCs e hologramas são pacotes, não entidades.

Os jogadores ficam sempre no modo Aventura: qualquer troca para outro modo de jogo é cancelada, exceto o Criativo para quem está no modo construção.

### Modo construção

O `/build` vem do Muxe-Core e funciona em todos os servidores, como descrito na [API do core](../../development/core-api/README.pt-BR.md#modo-construção). No lobby:

- `/build on` limpa a hotbar, coloca no Criativo e deixa o jogador quebrar, colocar e mover itens, com `&aModo construção ativado` na action bar enquanto o modo estiver ligado.
- `/build off` volta para o Aventura. O Muxe-Core devolve a hotbar (com o corante de visibilidade atual) e o lobby deixa quem tem `muxe.lobby.fly` voar de novo (`FlightBuildHook`).
- O modo construção acompanha o jogador nos outros servidores: ir para outro lobby mantém o modo ligado. As ações da hotbar não fazem nada enquanto ele estiver ligado.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/jogar <modo>` (`/play`) | Envia o jogador para um grupo de jogo. | Nenhuma |
| `/setspawn` | Salva a posição atual como spawn do lobby. | `muxe.lobby.setspawn` |
| `/fly` (`/voar`) | Liga ou desliga o voo. | `muxe.lobby.fly` |
| `/build <on\|off>` | Liga ou desliga o modo construção em qualquer servidor; sem argumento, alterna. Vem do Muxe-Core. | `muxe.build` |

O `/jogar` completa os jogos configurados com tab.

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.lobby.fly` | Usar o `/fly`, chegar acima do spawn, voando, e voar de novo depois do modo construção. Semeada no `vip`, então todo cargo acima do VIP também tem. | op |
| `muxe.build` | Usar o `/build` (Muxe-Core) para quebrar, colocar, largar e pegar itens no Criativo. | op |
| `muxe.lobby.setspawn` | Usar `/setspawn`. | op |

## Configuração

`plugins/Muxe-Lobby/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Grupos permitidos no `/jogar`. |
| `protection.void-level` | `0` | Altura abaixo da qual o jogador volta ao spawn. |
| `arrival.fly-height` | `5` | Blocos acima do spawn para quem tem `muxe.lobby.fly`. |
| `bossbar.frames`, `bossbar.interval` | `&e&lMUXE`, `&6&lMUXE`, `500ms` | Textos da boss bar e quanto tempo cada um fica. |
| `scoreboard.title`, `scoreboard.lines` | Veja acima | Texto da scoreboard. Placeholders: `{tag}`, `{credits}`, `{lobby}`, `{players}`. |
| `scoreboard.update-interval` | `1s` | De quanto em quanto tempo a scoreboard é atualizada. |
| `scoreboard.default-tag`, `scoreboard.default-credits` | `&7Membro`, `0` | Tag mostrada enquanto os cargos do jogador não carregaram, e os créditos mostrados até existir uma carteira. |
| `spawn.*` | não definido | Escrito pelo `/setspawn`. |
| `messages.*` | Textos em português | Mensagens do `/jogar`, do `/setspawn` e do `/fly` (`fly-enabled`, `fly-disabled`); as mensagens compartilhadas vêm do Muxe-Core. |

`spam`, `hotbar`, `lobby-selector` e as mensagens de visibilidade e do seletor de lobby não são mais lidos deste arquivo: eles ficam no `plugins/Muxe-Core/config.yml`, descritos em [Hotbar](../hotbar/README.pt-BR.md#configuração).

`plugins/Muxe-Core/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `join-greeting.server-kinds` | `LOBBY` | Tipos de servidor que recebem o jogador com a saudação. |
| `join-greeting.delay` | `500ms` | Espera depois de entrar antes da saudação. |
| `join-greeting.sound`, `volume`, `pitch` | `LEVEL_UP`, `1.0`, `1.0` | Som tocado para o jogador (um nome de `Sound` da 1.8). Vazio para não tocar som. |
| `join-greeting.fade-in`, `stay`, `fade-out` | `10`, `50`, `20` | Tempos do título em ticks. |
| `messages.join-title`, `messages.join-subtitle` | `&b&lMUXE`, `&eBoas-vindas!` | Título e subtítulo da saudação. |

As chaves da hotbar e dos seus menus, dos NPCs, dos hologramas e da loja também ficam no `plugins/Muxe-Core/config.yml`, descritas em [Hotbar](../hotbar/README.pt-BR.md#configuração), [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#configuração) e [Loja](../shop/README.pt-BR.md#configuração).

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `player.connect` | proxy | padrão | Enviado pelo `/jogar`. |

## Dados

O schema `lobby` não tem tabelas. A migração do lobby `V2__drop_player_settings.sql` apaga a `lobby.player_settings`: a visibilidade dos jogadores foi para `core.player_visibility` (veja [Hotbar](../hotbar/README.pt-BR.md#dados)). O Muxe-Core inicia antes do lobby e copia as linhas primeiro.

## Métricas

As métricas de servidor Spigot do Muxe-Core. As métricas de visibilidade foram junto com ela e estão em [Hotbar](../hotbar/README.pt-BR.md#métricas).

## Integrações

- O [Roteamento da rede](../network-routing/README.pt-BR.md) traz os jogadores para cá depois do login e os move para outros lobbys e jogos.
- A [API do core](../../development/core-api/README.pt-BR.md) fornece a boss bar e a scoreboard.
- A [Hotbar](../hotbar/README.pt-BR.md), com os seus menus e a visibilidade dos jogadores, vem do Muxe-Core.
- [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md) e a [Loja](../shop/README.pt-BR.md) vêm do Muxe-Core e são colocados no lobby por comando.
- O [Parkour](../parkour/README.pt-BR.md) vem do Muxe-Core; a equipe marca os pontos dele no lobby por comando.

## Critérios de aceitação

- [x] Quem tem a permissão de voar chega 5 blocos acima do spawn, voando; os outros chegam nele.
- [x] O `/fly` liga e desliga o voo com as mensagens de referência, faz quem está voando descer e precisa de `muxe.lobby.fly`.
- [x] A saudação de entrada só fica ativa em servidores `LOBBY`, com o título e o subtítulo de referência.
- [x] O arquivo dos NPCs de jogo tem os doze NPCs nos lugares e direções exportados, com o grupo, o item na mão e as linhas.
- [x] A boss bar alterna `&e&lMUXE` e `&6&lMUXE` a cada 10 ticks, e a scoreboard preenche as linhas de referência.
- [ ] Boss bar, scoreboard e o voo depois do modo construção funcionam no jogo (conferido no jogo).
- [ ] Os doze NPCs de jogo aparecem nos dois lobbys com as linhas nas alturas de referência, a contagem acompanha os servidores de jogo e o clique manda o jogador (conferido no jogo).

## Falhas e recuperação

- Proxy ou Redis indisponíveis: o `/jogar` mostra uma mensagem de falha e o jogador continua no lobby.
- Internos do servidor ausentes (outra versão de servidor): a boss bar é pulada com um aviso.
- Mundo do spawn ausente: usa-se o spawn do mundo principal.

## Limitações conhecidas

- Créditos ainda não existem; a scoreboard mostra `scoreboard.default-credits`.
- Os NPCs de jogo mostram a skin padrão até a equipe escolher skins que o Muxe possa usar.
- Ainda não existe grupo de servidores de jogo além de `skywars` e `bedwars`, então os outros NPCs de jogo mostram `0 jogando` e respondem que não há sala disponível.
- As limitações da hotbar e dos seus menus estão em [Hotbar](../hotbar/README.pt-BR.md#limitações-conhecidas).
