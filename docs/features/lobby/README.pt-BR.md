[English](README.md) | **Português**

# Lobby

O hub principal, onde os jogadores chegam depois de fazer login e escolhem um jogo. Fica no `servers:lobby` (`Muxe-Lobby`). A rede pode ter vários lobbys (`lobby-1`, `lobby-2`, …), todos no grupo `lobby`; cada jogo também terá os seus próprios servidores de lobby.

## Histórias de usuário

- Como jogador, quero chegar a um spawn limpo e seguro, ver a rede num relance e escolher o que jogar.
- Como jogador, quero trocar de lobby principal e esconder os outros jogadores quando o lobby estiver cheio.
- Como membro da equipe, quero voar no lobby.
- Como construtor, quero entrar num modo de construção para editar o lobby, enquanto jogadores comuns não conseguem quebrá-lo.

## Como funciona

### Chegada

- Depois do login, o proxy manda o jogador para o lobby com menos jogadores (veja [Roteamento da rede](../network-routing/README.pt-BR.md)).
- Ao entrar: modo Aventura, inventário e armadura limpos, vida e fome cheias, experiência zerada, e teleporte para o spawn configurado (ou o spawn do mundo principal).
- Quem tem `muxe.lobby.fly` chega `arrival.fly-height` blocos (5) acima do spawn, já voando. Os outros chegam no spawn. Cair abaixo de `protection.void-level` manda o jogador de volta do mesmo jeito.
- As mensagens de entrada e saída ficam escondidas.
- Meio segundo depois de entrar, o jogador ouve um som de level up e vê o título `&b&lMUXE` com o subtítulo `&eBoas-vindas!`. Essa saudação vem do Muxe-Core e acontece em todo servidor `LOBBY`, nunca nos servidores de autenticação ou de jogo.
- Trocar de lobby pelo seletor, ou usar `/lobby`, não mostra mensagem de "conectando": o jogador só é movido.

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
- Ainda não existe sistema de tag nem de créditos: todo jogador mostra `scoreboard.default-tag` e `scoreboard.default-credits`. Os valores vêm do `PlayerInfoSource`, onde esses sistemas vão se plugar.

### Hotbar

| Slot | Item | Nome | Ação |
|---|---|---|---|
| 1 | Bússola | `&aSelecionar jogo` | Nenhuma por enquanto |
| 2 | Cabeça do jogador | `&aMeu perfil` | Nenhuma por enquanto |
| 5 | Baú | `&aCosméticos` | Nenhuma por enquanto |
| 8 | Corante | `&fJogadores: …` | Visibilidade dos jogadores, veja abaixo |
| 9 | Estrela do Nether | `&aSelecionar lobby` | Abre o seletor de lobby |

- Os itens são usados com o botão direito. Eles não podem ser movidos, largados nem pegos.
- Usar os itens da hotbar três vezes dentro de `spam.window` (1 segundo) bloqueia o uso por `spam.cooldown` (3 segundos). Enquanto isso, o jogador vê `&cAguarde 2,9s para usar novamente.`

### Seletor de lobby

- Título `Selecionar lobby (1/1)`. Todo lobby vivo de `lobby-selector.group` aparece em ordem de número, centralizado na linha do meio: um painel de vidro para cada, verde-limão para o lobby onde o jogador está e cinza para os outros.
- Cada painel se chama `&aLobby Principal #<número>`, com a lore `&fJogadores &a<online>/<máximo>`.
- Clicar em outro lobby manda o jogador para lá pelo proxy. Clicar no lobby atual ou num lobby cheio só mostra uma mensagem.
- Mais de 21 lobbys se dividem em páginas com setas.

### Visibilidade dos jogadores

O corante alterna entre três opções e a escolha fica salva, então ela acompanha o jogador em todos os lobbys:

| Opção | Item | Mensagem |
|---|---|---|
| Mostrar todos | Corante verde-limão, `&fJogadores: &aAtivados` | `&aVocê agora está vendo todos os jogadores.` |
| Apenas amigos | Corante ciano, `&fJogadores: &bApenas amigos` | `&bVocê agora está vendo apenas seus amigos.` |
| Esconder todos | Corante cinza, `&fJogadores: &7Desativados` | `&cVocê escondeu todos os jogadores.` |

Ainda não existe sistema de amigos, então "Apenas amigos" esconde todo mundo. Os amigos virão do `FriendDirectory`.

### NPCs, hologramas e loja

NPCs, hologramas e a loja com as caixas registradoras vêm do Muxe-Core e funcionam em todos os servidores, não só aqui. O que a equipe coloca num lobby aparece em todos os lobbys do grupo `lobby`. Veja [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md) e [Loja](../shop/README.pt-BR.md).

### Proteção

Os jogadores não levam dano, não perdem fome, não quebram, não colocam, não largam nem pegam itens, nem movem itens no inventário. A chuva é cancelada.

Os jogadores ficam sempre no modo Aventura: qualquer troca para outro modo de jogo é cancelada, exceto o Criativo para quem está no modo construção.

### Modo construção

O `/build` vem do Muxe-Core e funciona em todos os servidores, como descrito na [API do core](../../development/core-api/README.pt-BR.md#modo-construção). No lobby:

- `/build on` limpa a hotbar, coloca no Criativo e deixa o jogador quebrar, colocar e mover itens, com `&aModo construção ativado` na action bar enquanto o modo estiver ligado.
- `/build off` devolve os itens do lobby (com o corante de visibilidade atual), volta para o Aventura e deixa quem tem `muxe.lobby.fly` voar de novo.
- O modo construção acompanha o jogador nos outros servidores: ir para outro lobby mantém o modo ligado. As ações da hotbar não fazem nada enquanto ele estiver ligado.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/jogar <modo>` (`/play`) | Envia o jogador para um grupo de jogo. | Nenhuma |
| `/setspawn` | Salva a posição atual como spawn do lobby. | `muxe.lobby.setspawn` |
| `/build <on\|off>` | Liga ou desliga o modo construção em qualquer servidor; sem argumento, alterna. Vem do Muxe-Core. | `muxe.build` |

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.lobby.fly` | Chegar acima do spawn, voando. | op |
| `muxe.build` | Usar o `/build` (Muxe-Core) para quebrar, colocar, largar e pegar itens no Criativo. | op |
| `muxe.lobby.setspawn` | Usar `/setspawn`. | op |

## Configuração

`plugins/Muxe-Lobby/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Grupos permitidos no `/jogar`. |
| `protection.void-level` | `0` | Altura abaixo da qual o jogador volta ao spawn. |
| `arrival.fly-height` | `5` | Blocos acima do spawn para quem tem `muxe.lobby.fly`. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam dos itens da hotbar. Os NPCs e as caixas registradoras usam o do Muxe-Core. |
| `bossbar.frames`, `bossbar.interval` | `&e&lMUXE`, `&6&lMUXE`, `500ms` | Textos da boss bar e quanto tempo cada um fica. |
| `scoreboard.title`, `scoreboard.lines` | Veja acima | Texto da scoreboard. Placeholders: `{tag}`, `{credits}`, `{lobby}`, `{players}`. |
| `scoreboard.update-interval` | `1s` | De quanto em quanto tempo a scoreboard é atualizada. |
| `scoreboard.default-tag`, `scoreboard.default-credits` | `&7Membro`, `0` | Mostrados até existirem tags e créditos. |
| `hotbar.<item>.slot`, `hotbar.<item>.item` | Veja acima | Slot (1 a 9) e item de cada item da hotbar. `hotbar.visibility` tem um item por opção (`all`, `friends`, `none`). |
| `lobby-selector.group` | `lobby` | Grupo listado no seletor. |
| `lobby-selector.title`, `current`, `other`, `previous-page`, `next-page` | Veja acima | Título e itens do seletor. Placeholders: `{page}`, `{pages}`, `{number}`, `{online}`, `{max}`. |
| `spawn.*` | não definido | Escrito pelo `/setspawn`. |
| `messages.*` | Textos em português | Mensagens deste plugin; as mensagens compartilhadas vêm do Muxe-Core. |

`plugins/Muxe-Core/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `join-greeting.server-kinds` | `LOBBY` | Tipos de servidor que recebem o jogador com a saudação. |
| `join-greeting.delay` | `500ms` | Espera depois de entrar antes da saudação. |
| `join-greeting.sound`, `volume`, `pitch` | `LEVEL_UP`, `1.0`, `1.0` | Som tocado para o jogador (um nome de `Sound` da 1.8). Vazio para não tocar som. |
| `join-greeting.fade-in`, `stay`, `fade-out` | `10`, `50`, `20` | Tempos do título em ticks. |
| `messages.join-title`, `messages.join-subtitle` | `&b&lMUXE`, `&eBoas-vindas!` | Título e subtítulo da saudação. |

Os itens usam o [formato do `ItemBuilder`](../../development/core-api/README.pt-BR.md#itens). As chaves dos NPCs, dos hologramas e da loja ficam no `plugins/Muxe-Core/config.yml`, descritas em [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#configuração) e [Loja](../shop/README.pt-BR.md#configuração).

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `player.connect` | proxy | padrão | Enviado pelo `/jogar` e pelo seletor de lobby. |

## Dados

| Tabela | Conteúdo |
|---|---|
| `lobby.player_settings` | `player_id`, `visibility` (`ALL`, `FRIENDS`, `NONE`), `updated_at`. Gravada quando o jogador troca a visibilidade e lida ao entrar. |

Comandos e consultas: `ChangeVisibilityCommand`, `FindVisibilityQuery` (quem não tem linha vê todo mundo).

## Métricas

As métricas de servidor Spigot do Muxe-Core, mais `muxe_db_transaction_seconds{operation="lobby.change_visibility"|"lobby.find_visibility"}`.

## Integrações

- O [Roteamento da rede](../network-routing/README.pt-BR.md) traz os jogadores para cá depois do login e os move para outros lobbys e jogos.
- A [API do core](../../development/core-api/README.pt-BR.md) fornece a boss bar, a scoreboard, os menus e os itens.
- [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md) e a [Loja](../shop/README.pt-BR.md) vêm do Muxe-Core e são colocados no lobby por comando.

## Critérios de aceitação

- [x] Quem tem a permissão de voar chega 5 blocos acima do spawn, voando; os outros chegam nele.
- [x] Os itens da hotbar ficam nos slots 1, 2, 5, 8 e 9 com os nomes de referência, e o corante de visibilidade alterna entre as três opções.
- [x] O seletor de lobby lista os lobbys em ordem de número, centralizados, verde-limão para o atual e cinza para os outros, com o nome e a lore de referência.
- [x] Três usos rápidos iniciam um cooldown de 3 segundos com o tempo restante como `2,9`.
- [x] A saudação de entrada só fica ativa em servidores `LOBBY`, com o título e o subtítulo de referência.
- [x] A boss bar alterna `&e&lMUXE` e `&6&lMUXE` a cada 10 ticks, e a scoreboard preenche as linhas de referência.
- [x] A escolha de visibilidade é salva no PostgreSQL e lida de volta.
- [ ] Boss bar, scoreboard, hotbar, seletor e visibilidade funcionam no jogo (conferido no jogo).

## Falhas e recuperação

- Proxy ou Redis indisponíveis: o `/jogar` e o seletor mostram uma mensagem de falha e o jogador continua no lobby.
- PostgreSQL indisponível: a visibilidade começa como "mostrar todos" e as mudanças não são salvas; o lobby continua funcionando.
- Internos do servidor ausentes (outra versão de servidor): a boss bar é pulada com um aviso.
- Mundo do spawn ausente: usa-se o spawn do mundo principal.

## Limitações conhecidas

- Os itens de seleção de jogo, perfil e cosméticos ainda não fazem nada.
- Tags, créditos e amigos ainda não existem; a scoreboard mostra valores padrão e "Apenas amigos" esconde todo mundo.
