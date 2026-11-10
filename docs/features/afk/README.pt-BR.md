[English](README.md) | **Português**

# Servidor AFK

Jogadores que não são VIP e ficam parados por 5 minutos em um lobby são levados a um pequeno servidor AFK, onde não podem falar no chat, e voltam ao servidor exato de onde vieram assim que se movem. Fica em `servers:lobby` (`Muxe-Lobby`, pacote `afk`), que vigia os lobbies, e em `servers:afk` (`Muxe-AFK`), que roda o servidor AFK. O AFK durante as partidas funciona de outro jeito e ainda não está coberto aqui.

## Histórias de usuário

- Como operador, quero os jogadores parados fora dos lobbies, para que os lobbies tenham espaço e as contagens mostrem quem está jogando de verdade.
- Como jogador que se afastou, quero voltar ao lobby em que estava só me movendo.
- Como VIP, quero ficar no lobby por quanto tempo ficar parado.

## Como funciona

### Nos lobbies

- Todo servidor com o Muxe-Lobby vigia os seus jogadores: os lobbies principais (`lobby-1`, `lobby-2`, …) e todo lobby de jogo (`bedwars-lobby-1`, …). Servidores de jogo, de autenticação e o próprio servidor AFK não.
- Um jogador conta como ativo ao chegar e sempre que anda, pula, fala no chat, usa um comando, clica em um bloco ou no ar, ou clica em um inventário. Só virar a cabeça, ou cair reto para baixo, não conta.
- Uma vez por segundo (`afk.check-interval`) o lobby procura jogadores sem atividade há 5 minutos (`afk.idle-after`). Jogadores com `muxe.afk.bypass` ficam de fora; o VIP e todo cargo acima dele têm essa permissão (ela é dada ao `vip`, então `mvp`, `pro`, `ultra`, os partners e a staff herdam; o `admin` tem `*`).
- Um jogador parado é enviado ao servidor menos cheio do grupo `afk` (`afk.server-group`) com espaço, sem mensagem. Antes disso, o lobby guarda em qual servidor ele está (`afk:return:<jogador>` no `MuxeCore.store()`, por `afk.origin-ttl`, 12 horas).
- Quando nenhum servidor AFK está vivo ou com espaço, o jogador fica onde está. A contagem recomeça a cada tentativa, então um jogador que continua no lobby é tentado de novo 5 minutos depois.

### No servidor AFK

- O jogador chega no spawn do mapa (`spawn`, 8.5, 63, 8.5 de frente para a placa), no modo Aventura, com vida e fome cheias e o inventário vazio. Dano, fome, quebrar, colocar, dropar, pegar itens, clicar em blocos e em inventários ficam bloqueados para todos fora do [modo construção](../lobby/README.pt-BR.md).
- Os jogadores no servidor AFK não se veem. Isso também impede que se empurrem, o que contaria como movimento.
- Ao chegar, veem o título `&c&lVOCÊ ESTÁ AFK` com o subtítulo `&7Mova-se para voltar ao lobby.`, e no chat:

```text
VOCÊ ESTÁ AFK!
Mova-se para voltar ao lobby.
```

- As mensagens de entrada e saída ficam ocultas.
- O chat fica fechado: o que o jogador digita é descartado sem resposta. Ele continua recebendo toda mensagem enviada a ele (anúncios, mensagens da staff). Os comandos continuam funcionando, então `/lobby` também sai.
- Assim que o jogador se move (anda ou pula; virar a cabeça não conta) ele é enviado de volta ao servidor guardado pelo lobby. Se esse servidor caiu ou está cheio, ou nada foi guardado, vai para o lobby principal menos cheio (`return.fallback-group`).
- O movimento no primeiro segundo depois de chegar (`return.arrival-grace`) é ignorado, para que o teleporte de chegada nunca mande o jogador de volta.
- A volta é pedida uma vez. Se o jogador continua no servidor AFK 5 segundos depois (`return.retry-after`), por exemplo porque o proxy não tinha para onde mandá-lo, o próximo movimento pede de novo.
- Sair do servidor AFK esquece o servidor guardado.

## Comandos

Nenhum.

## Permissões

| Permissão | Concede |
|---|---|
| `muxe.afk.bypass` | Nunca ser enviado ao servidor AFK por ficar parado em um lobby. Dada ao `vip` pela migração do core `V17__grant_afk_bypass_to_vip.sql`. |

## Configuração

`config.yml` do Muxe-Lobby:

| Chave | Padrão | Descrição |
|---|---|---|
| `afk.enabled` | `true` | Liga ou desliga a vigia de AFK do lobby. |
| `afk.idle-after` | `5m` | Quanto tempo o jogador precisa ficar sem atividade para ser enviado. |
| `afk.server-group` | `afk` | Grupo dos servidores AFK. |
| `afk.check-interval` | `1s` | De quanto em quanto tempo o lobby procura jogadores parados. |
| `afk.origin-ttl` | `12h` | Por quanto tempo o servidor de onde o jogador veio fica guardado. |

`config.yml` do Muxe-AFK:

| Chave | Padrão | Descrição |
|---|---|---|
| `spawn.world`, `x`, `y`, `z`, `yaw`, `pitch` | `afk`, 8.5, 63, 8.5, 0, 0 | Onde os jogadores chegam. Sem ela, o centro do bloco de spawn do primeiro mundo. |
| `return.fallback-group` | `lobby` | Para onde vai o jogador quando o servidor de onde veio caiu, está cheio ou é desconhecido. |
| `return.arrival-grace` | `1s` | Movimento ignorado logo depois de chegar. |
| `return.retry-after` | `5s` | Quando uma volta que não aconteceu pode ser pedida de novo. |
| `messages.title`, `messages.subtitle` | `&c&lVOCÊ ESTÁ AFK`, `&7Mova-se para voltar ao lobby.` | Título mostrado ao chegar. |
| `messages.arrived` | As duas linhas de chat acima | Linhas de chat ao chegar, uma lista YAML. |

O `lang/en.yml` do Muxe-AFK tem os `messages.*` em inglês, nos mesmos caminhos. Cada jogador lê o título e as linhas de chat no seu próprio [idioma](../language/README.pt-BR.md).

`config.yml` do Muxe-Core no servidor AFK: `server.group: afk` e `server.kind: AFK`. O tipo `AFK` deixa a hotbar do lobby, a saudação e a loja fora desse servidor.

## Pacotes

| Id | Destino | Política | Para quê |
|---|---|---|---|
| `player.connect` | proxy | padrão | Envia o jogador parado ao grupo `afk`, e de volta ao seu servidor ou a um lobby. Veja [Roteamento da rede](../network-routing/README.pt-BR.md). |

## Dados

| Armazenamento | Chave | Valor | Duração |
|---|---|---|---|
| `MuxeCore.store()` (Redis) | `afk:return:<uuid do jogador>` | Id do servidor de onde o jogador veio | `afk.origin-ttl` (12 horas), apagada quando o jogador sai do servidor AFK |

`perm_group_nodes` ganha `muxe.afk.bypass` no `vip` (migração do core `V17`).

## Métricas

Nenhuma além das métricas de pacote do `player.connect`.

## Integrações

- [Lobby](../lobby/README.pt-BR.md): a vigia roda dentro do Muxe-Lobby, em todo lobby principal e de jogo.
- [Roteamento da rede](../network-routing/README.pt-BR.md): move o jogador com `player.connect`.
- [Cargos e tags](../ranks-and-tags/README.pt-BR.md): `muxe.afk.bypass` vem com o VIP.
- `IdleTracker`, `Displacement` e `AfkReturns` ficam no `core:common` (pacotes `activity` e `afk`), para outras features reaproveitarem.

## Critérios de aceite

- [x] Um jogador não VIP sem atividade por 5 minutos em um lobby é enviado ao grupo `afk` e o lobby em que estava fica guardado.
- [x] Um jogador ativo há menos de 5 minutos, um VIP, ou qualquer um enquanto nenhum servidor AFK tem espaço, fica no lobby.
- [x] Mover-se no servidor AFK manda o jogador de volta ao servidor de onde veio, ou ao lobby principal quando ele caiu, está cheio ou é desconhecido.
- [x] O movimento no primeiro segundo depois de chegar é ignorado, e a volta é pedida uma só vez até falhar ou passarem 5 segundos.
- [x] O título e as linhas de chat são iguais aos prints de referência.
- [ ] Um jogador parado no `lobby-1` chega ao servidor AFK depois de 5 minutos, vê o título e as linhas de chat, não consegue falar no chat, e andar o leva de volta ao `lobby-1` (verificado no jogo).
- [ ] O mesmo a partir do `bedwars-lobby-1` leva o jogador de volta ao `bedwars-lobby-1` (verificado no jogo).
- [ ] Um VIP parado em um lobby nunca é movido (verificado no jogo).

## Falhas e recuperação

- **Nenhum servidor AFK vivo ou todos cheios:** os jogadores parados ficam no lobby e são tentados de novo 5 minutos depois.
- **O servidor de onde o jogador veio caiu:** ao se mover, ele vai para o lobby principal menos cheio.
- **Nenhum lobby vivo também:** o proxy responde `no-server`, o jogador fica no servidor AFK, e se mover depois de 5 segundos tenta de novo.
- **Redis fora do ar:** o lobby não consegue guardar a origem, e o pedido de movimento não chega ao proxy, então o jogador fica no lobby. No servidor AFK, a leitura da origem falha e a volta é tentada de novo no próximo movimento.
- **O servidor AFK cai:** o proxy leva os jogadores dele a um lobby, como em qualquer outro servidor (veja [Roteamento da rede](../network-routing/README.pt-BR.md)).

## Limitações conhecidas

- O AFK nas partidas (com avisos) é uma feature separada, ainda não feita.
- Quando o servidor de onde o jogador veio caiu, ele vai para um lobby principal, não para outro lobby do mesmo jogo.
- Ser empurrado por outro jogador no lobby conta como movimento.
- Todo servidor da rede precisa rodar um Muxe-Core que conhece o tipo `AFK` antes de o servidor AFK ligar; um core antigo não lê o status dele e não envia jogadores para ele.
