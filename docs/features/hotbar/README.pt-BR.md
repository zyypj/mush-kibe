[English](README.md) | **Português**

# Hotbar

Os itens que todo lobby dá aos jogadores e os menus que eles abrem: seletor de jogo, perfil, cosméticos, visibilidade dos jogadores e seletor de lobby. Fica no Muxe-Core (`core:bukkit`), então todo lobby da rede recebe a mesma hotbar sem precisar configurá-la: o lobby principal hoje e, mais tarde, o lobby de cada jogo.

## Histórias de usuário

- Como jogador, quero os mesmos itens em todos os lobbys, para sempre saber onde estão o seletor de jogo, o meu perfil e o seletor de lobby.
- Como jogador, quero escolher um jogo num menu que mostra quantas pessoas estão jogando cada um.
- Como jogador, quero ver o meu perfil, com o meu primeiro login e os meus créditos.
- Como jogador, quero esconder os outros jogadores quando o lobby estiver cheio e manter essa escolha em todos os lobbys.
- Como jogador, quero trocar para outro lobby do mesmo grupo.
- Como operador, quero adicionar um jogo ao seletor editando só a configuração.

## Como funciona

### Hotbar

Num servidor cujo tipo está em `hotbar.server-kinds` (só `LOBBY` por padrão; os servidores de autenticação e de jogo não recebem hotbar), o Muxe-Core limpa o inventário e a armadura ao entrar, dá a hotbar, seleciona o slot 1 e aplica a visibilidade salva do jogador.

| Slot | Item | Nome | Ação |
|---|---|---|---|
| 1 | Bússola | `&aSelecionar jogo` | Abre o seletor de jogo |
| 2 | Cabeça do jogador | `&aMeu perfil` | Abre o perfil |
| 5 | Baú | `&aCosméticos` | Nenhuma por enquanto |
| 8 | Corante | `&fJogadores: …` | Troca a visibilidade dos jogadores, veja abaixo |
| 9 | Estrela do Nether, brilhando | `&aSelecionar lobby` | Abre o seletor de lobby |

- Os itens são usados com o botão direito, no ar ou num bloco. Um item só age no seu próprio slot e com o seu próprio material.
- Os cliques com o botão direito usam o anti-spam do Muxe-Core (`spam.*`), o mesmo dos NPCs, das caixas registradoras, das cabeças de redes sociais e dos itens do parkour: três usos dentro de 1 segundo bloqueiam por 3 segundos com `&cAguarde 2,9s para usar novamente.`
- Os itens da hotbar não fazem nada enquanto o jogador está no [modo construção](../../development/core-api/README.pt-BR.md#modo-construção). Quando um membro da equipe sai do modo construção, a hotbar volta, com o corante da visibilidade atual.
- Enquanto o jogador faz o [parkour](../parkour/README.pt-BR.md), os slots 4 a 6 ficam com os itens do parkour (`Reiniciar Parkour`, `Voltar ao Checkpoint`, `Parar Parkour`): o baú de cosméticos do slot 5 é guardado e volta quando a corrida acaba.
- Manter os itens no lugar (sem mover, largar nem pegar) é papel da proteção do servidor; no lobby principal, o [Lobby](../lobby/README.pt-BR.md#proteção) faz isso.
- Se o `config.yml` não tiver a seção `hotbar`, o Muxe-Core registra `The hotbar is off: config.yml has no 'hotbar' section` no log e não dá hotbar.

### Seletor de jogo

- Título `Selecionar jogo`, 4 linhas. Aberto pela bússola.
- Cada botão pode mandar o jogador para um jogo ou abrir a loja:
  - `game: <grupo>`: o clique fecha o menu e manda o jogador para o servidor vivo com menos jogadores desse grupo, com `&7Procurando uma sala de &b<nome>&7...`. Quando o grupo não tem servidor vivo, o jogador vê `&cNenhuma sala de &f<nome> &cdisponível no momento.` no lugar. `<nome>` é o nome do botão sem cores.
  - `shop: <categoria>`: abre a [loja](../shop/README.pt-BR.md) nessa categoria. Uma categoria vazia ou desconhecida abre a loja em `shop.default-category` (Ranks).
- `{players}` no nome ou na lore de um botão é a soma dos jogadores online nos servidores vivos do seu grupo, lida quando o menu abre.
- Por padrão, ele lista 14 jogos (Bed Wars, HG, Duels, PvP, Sky Wars, The Bridge, CTF, Party, Murder, Quick Builders, Build Battle, Hide and Seek, Treino: Ponte e Nascó Mandou) e um botão `Sala customizada`, no canto inferior direito, que abre a loja na categoria Ultra+.

### Perfil

- Título `Meu perfil`, 6 linhas. Aberto pela cabeça.
- A cabeça no topo (slot 13) mostra a skin do jogador, o nome e a lore:

```text

Primeiro login: 27/10/2026 14:03
Créditos: 0
```

- O primeiro login vem da [conta](../accounts/README.pt-BR.md) do jogador e é formatado com `first-login.format` no fuso `first-login.time-zone`. Quando a conta não pode ser lida, aparece `first-login.unknown` (`-`).
- Os créditos mostram `default-credits` até existir uma carteira.
- Botões: `Amigos`, `Ver estatísticas`, `Preferências`, `Medalhas`, `Sua skin`, `Nick (/nick)` e `Menu do Ultra+`. O `Nick` abre a loja na categoria Ultra+; os outros ainda não fazem nada, porque os seus menus não existem.

### Seletor de lobby

- Título `Selecionar lobby (1/1)`. Aberto pela estrela do Nether.
- Todo servidor vivo do grupo do servidor atual (ou de `menus.lobby-selector.group`, quando definido) aparece em ordem de número, centralizado na linha do meio: um painel de vidro para cada, verde-limão para o lobby onde o jogador está e cinza para os outros.
- Cada painel se chama `&aLobby Principal #<número>`, com a lore `&fJogadores &a<online>/<máximo>`.
- Clicar em outro lobby manda o jogador para lá pelo proxy, sem mensagem de "conectando". Clicar no lobby atual mostra `&cVocê já está conectado neste lobby.` e num lobby cheio, `&cEste lobby está cheio.`
- Mais de 21 lobbys se dividem em páginas com setas.

### Visibilidade dos jogadores

O corante alterna entre três opções. A escolha fica salva no PostgreSQL, então ela acompanha o jogador em todos os lobbys:

| Opção | Item | Mensagem |
|---|---|---|
| Mostrar todos | Corante verde-limão (`data: 10`), `&fJogadores: &aAtivados` | `&aVocê agora está vendo todos os jogadores.` |
| Apenas amigos | Corante azul-claro (`data: 12`), `&fJogadores: &bApenas amigos` | `&bVocê agora está vendo apenas seus amigos.` |
| Esconder todos | Corante cinza (`data: 8`), `&fJogadores: &7Desativados` | `&cVocê escondeu todos os jogadores.` |

- Ao entrar, o jogador recebe na hora o corante de "mostrar todos"; a escolha salva é carregada em segundo plano e depois aplicada, com o seu corante.
- A escolha também vale para os jogadores que entram depois.
- Ainda não existe sistema de amigos, então "Apenas amigos" esconde todo mundo. Os amigos virão do `FriendDirectory`.

## Comandos

Nenhum. O `/lobby`, que manda o jogador para o lobby com menos jogadores, está descrito em [Roteamento da rede](../network-routing/README.pt-BR.md).

## Permissões

Nenhuma.

## Configuração

`plugins/Muxe-Core/config.yml`. Os itens usam o [formato do `ItemBuilder`](../../development/core-api/README.pt-BR.md#itens).

### Hotbar

| Chave | Padrão | Descrição |
|---|---|---|
| `hotbar.server-kinds` | `LOBBY` | Tipos de servidor que dão a hotbar. |
| `hotbar.<item>.slot`, `hotbar.<item>.item` | Veja acima | Slot (1 a 9) e item de `game-selector`, `profile`, `cosmetics` e `lobby-selector`. O item do perfil recebe a skin do jogador. |
| `hotbar.visibility.slot` | `8` | Slot do corante de visibilidade. |
| `hotbar.visibility.all`, `friends`, `none` | Veja acima | Um item por opção de visibilidade. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam compartilhado pelos cliques na hotbar, nos NPCs, nas caixas registradoras, nas cabeças de redes sociais e nos itens do parkour. |

Dois itens da hotbar no mesmo slot impedem o core de iniciar.

```yaml
hotbar:
  server-kinds:
    - LOBBY
  game-selector:
    slot: 1
    item:
      material: COMPASS
      name: "&aSelecionar jogo"
  visibility:
    slot: 8
    all:
      material: INK_SACK
      data: 10
      name: "&fJogadores: &aAtivados"
```

### Menus

Os slots em `menus` são slots do inventário, de 0 (canto superior esquerdo) a 53, como na loja. Um botão fora das linhas do menu, ou dois botões no mesmo slot, impedem o core de iniciar.

| Chave | Padrão | Descrição |
|---|---|---|
| `menus.game-selector.title`, `rows` | `Selecionar jogo`, `4` | Título e altura do seletor de jogo. |
| `menus.game-selector.buttons.<id>` | 14 jogos e `custom-room` | `slot`, `item` e, opcionalmente, `game` (um grupo de servidores) ou `shop` (o id de uma categoria da loja; vazio para a padrão). Placeholder: `{players}`. |
| `menus.profile.title`, `rows` | `Meu perfil`, `6` | Título e altura do perfil. |
| `menus.profile.head` | Slot `13`, a cabeça do jogador | `slot` e `item` da cabeça. Placeholders: `{player}`, `{first-login}`, `{credits}`. |
| `menus.profile.buttons.<id>` | Os 7 botões do perfil | As mesmas chaves dos botões do seletor de jogo. |
| `menus.profile.first-login.format`, `time-zone`, `unknown` | `dd/MM/yyyy HH:mm`, `America/Sao_Paulo`, `-` | Como o primeiro login é escrito e o que aparece quando ele é desconhecido. |
| `menus.profile.default-credits` | `0` | Créditos mostrados até existir uma carteira. |
| `menus.lobby-selector.group` | não definido (o grupo do servidor) | Grupo listado no seletor de lobby. |
| `menus.lobby-selector.title`, `current`, `other`, `previous-page`, `next-page` | Veja acima | Título e itens do seletor. Placeholders: `{page}`, `{pages}`, `{number}`, `{online}`, `{max}`. |

```yaml
menus:
  game-selector:
    title: "Selecionar jogo"
    rows: 4
    buttons:
      skywars:
        slot: 14
        game: skywars
        item:
          material: EYE_OF_ENDER
          name: "&b&lSKY WARS"
          lore:
            - "&e{players} jogando agora!"
            - " "
            - "&aClique para entrar!"
      custom-room:
        slot: 35
        shop: ultra-plus
        item:
          material: BREWING_STAND_ITEM
          name: "&aSala customizada (/salacustom)"
```

### Adicionar um jogo ao seletor

1. Suba os servidores do jogo com `server.group` igual ao grupo do jogo (como `skywars`), para que eles apareçam no diretório de servidores.
2. Adicione um botão em `menus.game-selector.buttons` com um `slot` livre, `game: <grupo>` e o seu `item`, usando `{players}` na lore se quiser a contagem de jogadores.
3. Aumente `rows` se o slot passar da última linha e reinicie os lobbys.

### Mensagens

| Chave | Texto |
|---|---|
| `messages.game-connecting` | `&7Procurando uma sala de &b{name}&7...` (também usada pelos NPCs de jogo) |
| `messages.game-unavailable` | `&cNenhuma sala de &f{name} &cdisponível no momento.` (também usada pelos NPCs de jogo) |
| `messages.connect-failed` | `&cNão foi possível enviar você agora. Tente novamente em instantes.` |
| `messages.visibility-all`, `visibility-friends`, `visibility-none` | Veja a tabela de visibilidade. |
| `messages.lobby-current`, `lobby-full` | Veja o seletor de lobby. |
| `messages.lobby-failed` | `&cNão foi possível trocar de lobby agora. Tente novamente.` |
| `messages.shop-unavailable` | `&cA loja não está disponível neste servidor.` |
| `messages.spam-cooldown` | `&cAguarde {seconds}s para usar novamente.` |

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `player.connect` | proxy | padrão | Enviado pelo seletor de jogo e pelo seletor de lobby para mover o jogador. |

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.player_visibility` | `player_id` (chave primária), `visibility` (`ALL`, `FRIENDS`, `NONE`), `updated_at`. Gravada quando o jogador troca a visibilidade e lida ao entrar. Criada pela migração do core `V5__create_player_visibility.sql`, que copia as linhas da antiga `lobby.player_settings`. |

Comandos e consultas: `ChangeVisibilityCommand`, `FindVisibilityQuery` (quem não tem linha vê todo mundo), registrados pelo `VisibilityModule`.

O perfil lê a conta com `FindAccountQuery`.

## Métricas

`muxe_db_transaction_seconds{operation="visibility.change"|"visibility.find"}` e `muxe_cqrs_handle_seconds` para o comando e a consulta acima.

## Integrações

- A [API do core](../../development/core-api/README.pt-BR.md) fornece os menus, os itens, o `ConfiguredButton`, o `GameConnector` e o `ShopAccess`.
- O [Roteamento da rede](../network-routing/README.pt-BR.md) move o jogador para o jogo ou o lobby escolhido.
- A [Loja](../shop/README.pt-BR.md) abre pelos botões `shop`.
- As [Contas](../accounts/README.pt-BR.md) dão o primeiro login mostrado no perfil.
- O [Lobby](../lobby/README.pt-BR.md) mantém os itens no lugar e devolve o voo depois do modo construção.
- O [Parkour](../parkour/README.pt-BR.md) usa os slots 4 a 6 durante a corrida.

## Critérios de aceitação

- [x] A hotbar só é dada nos tipos de servidor de `hotbar.server-kinds`, nos slots 1, 2, 5, 8 e 9, com os nomes de referência.
- [x] O corante de visibilidade alterna entre as três opções, e a escolha é salva no PostgreSQL e lida de volta.
- [x] O seletor de jogo mostra os botões configurados, com `{players}` como a soma dos jogadores do grupo, manda o jogador para o grupo e abre a loja nos botões `shop`.
- [x] O perfil mostra a cabeça, o primeiro login e os créditos do jogador.
- [x] O seletor de lobby lista os lobbys do grupo atual em ordem de número, centralizados, verde-limão para o atual e cinza para os outros.
- [x] Três usos rápidos iniciam um cooldown de 3 segundos com o tempo restante como `2,9`.
- [x] As linhas de `lobby.player_settings` são copiadas para `core.player_visibility`.
- [ ] Hotbar, menus e visibilidade funcionam no jogo (conferido no jogo).

## Falhas e recuperação

- PostgreSQL indisponível: a visibilidade começa como "mostrar todos" e as mudanças são aplicadas mas não salvas, com um aviso no log. O perfil mostra `-` como primeiro login.
- Proxy ou Redis indisponíveis: o seletor de jogo mostra `connect-failed`, o seletor de lobby `lobby-failed`, e o jogador continua no lobby.
- Nenhum servidor vivo no grupo de um jogo: `game-unavailable`, e o jogador continua onde está.
- Loja desligada neste tipo de servidor: os botões `shop` respondem `shop-unavailable`.
- Configuração inválida (dois itens no mesmo slot, um botão fora do menu, uma seção ausente): o core não inicia, com o motivo no log.

## Limitações conhecidas

- O item de cosméticos ainda não faz nada.
- Os botões do perfil `Amigos`, `Ver estatísticas`, `Preferências`, `Medalhas`, `Sua skin` e `Menu do Ultra+` ainda não fazem nada.
- Não existe sistema de amigos, então "Apenas amigos" esconde todo mundo.
- Ainda não existem créditos: o perfil mostra `default-credits`.
- O `{players}` do seletor de jogo é lido quando o menu abre e não é atualizado enquanto ele está aberto.
