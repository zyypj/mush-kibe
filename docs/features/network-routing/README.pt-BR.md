[English](README.md) | **Português**

# Roteamento da rede

Como os jogadores são colocados nos servidores e levados de um para outro. Fica no `core:proxy` (`muxe-core-proxy`) e no comando `/lobby` do `core:bukkit`.

## Histórias de usuário

- Como jogador, quero cair em um servidor funcionando e ser levado para um lobby depois de fazer login.
- Como jogador, quero que `/lobby` me leve de volta a um lobby de qualquer lugar.
- Como operador, quero os jogadores distribuídos entre os servidores do mesmo grupo, e que ninguém consiga pular a autenticação.

## Como funciona

- **Servidor inicial:** o proxy escolhe o servidor de autenticação com menos jogadores que tenha vaga. Se nenhum estiver vivo, o jogador é desconectado com `no-auth-server`.
- **Passagem:** até o proxy receber `auth.authenticated` de um servidor de autenticação confiável, toda conexão para um servidor fora do grupo de autenticação é negada com `must-authenticate`.
- **Depois da autenticação:** o proxy envia o jogador para o lobby com menos jogadores. Se nenhum estiver vivo, o jogador continua no servidor de autenticação e vê `no-lobby-server`.
- **Mudando depois:** qualquer servidor pode publicar `player.connect` para enviar um jogador a um id de servidor ou ao servidor com menos jogadores de um grupo. O proxy ignora o pacote para jogadores não autenticados e avisa o jogador com `no-server` quando não há destino.
- **Menos jogadores:** entre os servidores vivos do grupo que têm vaga, o que tem menos jogadores; empates são desfeitos pelo id.
- **Desconexão:** a passagem esquece o jogador.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/lobby` (`/hub`, `/l`) | Envia o jogador para um lobby. Disponível em todos os servidores Spigot. | Nenhuma |

## Permissões

Não se aplica.

## Configuração

`config.yml` do proxy:

| Chave | Padrão | Descrição |
|---|---|---|
| `routing.auth-group` | `auth` | Grupo dos servidores de autenticação. |
| `routing.lobby-group` | `lobby` | Grupo para onde os jogadores vão depois de se autenticar. |
| `messages.no-auth-server`, `no-lobby-server`, `no-server`, `must-authenticate` | Textos em português | Mensagens mostradas pelo proxy. |

`config.yml` do Muxe-Core:

| Chave | Padrão | Descrição |
|---|---|---|
| `network.lobby-group` | `lobby` | Grupo usado pelo `/lobby`. |

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `server.status` | broadcast | `NONE` | Alimenta o diretório de servidores usado no roteamento. |
| `server.shutdown` | broadcast | standard | Remove na hora um servidor que está parando. |
| `player.connect` | proxy | standard | Leva um jogador para um servidor ou grupo. |
| `auth.authenticated` | proxy | `DURABLE` | Abre a passagem para um jogador. |

## Dados

Não se aplica. A passagem fica na memória do proxy.

## Métricas

`muxe_proxy_authenticated_players`, `muxe_network_servers_alive`, `muxe_network_players_online` e as métricas de pacote dos pacotes acima.

## Integrações

- A [Autenticação](../authentication/README.pt-BR.md) publica `auth.authenticated`.
- O [Lobby](../lobby/README.pt-BR.md) publica `player.connect` para o `/jogar`.

## Critérios de aceitação

- [x] Jogadores não autenticados só alcançam servidores de autenticação.
- [x] Jogadores autenticados alcançam todos os servidores.
- [x] Só servidores de autenticação são confiáveis para autenticar jogadores.
- [x] O servidor com menos jogadores e com vaga é escolhido, e servidores mortos são ignorados.
- [ ] Depois do login o jogador chega a um lobby (conferido no jogo).

## Falhas e recuperação

- **Nenhum servidor do grupo de destino vivo:** o jogador fica onde está e vê uma mensagem.
- **Redis fora do ar:** os pedidos de roteamento dos servidores falham; os jogadores ficam no servidor atual. O diretório se recupera poucos segundos depois da reconexão.
- **Reinício do proxy:** os jogadores reconectam e se autenticam de novo.

## Limitações conhecidas

- A escolha do servidor considera só a quantidade de jogadores; ainda não há noção de party, fila ou região.
- Com vários proxies, um jogador autenticado em um proxy precisa se autenticar de novo se reconectar por outro.
