[English](README.md) | **Português**

# Lobby

O lobby principal, onde os jogadores chegam depois de fazer login e escolhem um jogo. Fica no `servers:lobby` (`Muxe-Lobby`).

## Histórias de usuário

- Como jogador, quero chegar a um spawn limpo e seguro, para escolher o que jogar.
- Como jogador, quero que `/jogar <modo>` me envie para um jogo.
- Como construtor, quero editar o lobby com permissão, enquanto jogadores comuns não conseguem quebrá-lo.

## Como funciona

- Ao entrar: modo de jogo Aventura, inventário e armadura limpos, vida e fome cheias, experiência zerada, teleporte para o spawn configurado (ou para o spawn do mundo principal quando nenhum estiver definido).
- As mensagens de entrada e saída ficam escondidas.
- Os jogadores não levam dano nem perdem fome. A chuva é cancelada. Cair abaixo de `protection.void-level` teleporta de volta para o spawn.
- Quebrar, colocar e dropar itens exige `muxe.lobby.build`.
- `/jogar <modo>` confere se o modo está em `play.games` e pede ao proxy que envie o jogador para o servidor com menos jogadores daquele grupo.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/jogar <mode>` (`/play`) | Envia o jogador para um grupo de jogo. | Nenhuma |
| `/setspawn` | Salva a posição atual como spawn do lobby. | `muxe.lobby.setspawn` |

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.lobby.build` | Quebrar, colocar e dropar itens no lobby. | op |
| `muxe.lobby.setspawn` | Usar o `/setspawn`. | op |

## Configuração

`plugins/Muxe-Lobby/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `play.games` | `skywars`, `bedwars` | Grupos permitidos no `/jogar`. |
| `protection.void-level` | `0` | Y abaixo do qual os jogadores voltam para o spawn. |
| `spawn.*` | não definido | Gravado pelo `/setspawn`: world, x, y, z, yaw, pitch. |
| `messages.*` | Textos em português | Mensagens deste plugin; as mensagens compartilhadas vêm do Muxe-Core. |

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `player.connect` | proxy | standard | Enviado pelo `/jogar`. |

## Dados

Não se aplica.

## Métricas

As métricas de servidor Spigot do Muxe-Core.

## Integrações

- O [Roteamento da rede](../network-routing/README.pt-BR.md) leva o jogador.

## Critérios de aceitação

- [x] O spawn sobrevive a ser salvo e lido da configuração.
- [ ] Os jogadores chegam ao spawn em modo Aventura com o inventário vazio (conferido no jogo).
- [ ] `/jogar skywars` envia o jogador para um servidor de SkyWars (conferido no jogo).
- [ ] Jogadores sem permissão não conseguem quebrar nem colocar blocos (conferido no jogo).

## Falhas e recuperação

- Proxy ou Redis indisponível: `/jogar` mostra `play-failed` e o jogador continua no lobby.
- Mundo do spawn configurado ausente: o spawn do mundo principal é usado.

## Limitações conhecidas

- Ainda não há item seletor de jogos, scoreboard nem NPCs.
