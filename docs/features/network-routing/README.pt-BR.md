[English](README.md) | **Português**

# Roteamento da rede

Como os jogadores são colocados nos servidores e levados de um para outro. Fica no `core:proxy` (`muxe-core-proxy`) e no comando `/lobby` do `core:bukkit`.

## Histórias de usuário

- Como jogador, quero cair em um servidor funcionando e ser levado para um lobby depois de fazer login.
- Como jogador, quero que `/lobby` me leve de volta a um lobby de qualquer lugar.
- Como operador, quero os jogadores distribuídos entre os servidores do mesmo grupo, e que ninguém consiga pular a autenticação.

## Como funciona

- **Passagem:** um jogador é autenticado quando a conta é premium, quando uma [sessão de login](../authentication/README.pt-BR.md#2-autenticação-automática-proxy-no-login) é retomada, ou quando o proxy recebe `auth.authenticated` de um servidor de autenticação confiável. Até lá, toda conexão para um servidor fora do grupo de autenticação é negada com `must-authenticate`.
- **Rede fechada:** enquanto nenhum servidor de autenticação estiver vivo, ninguém entra, nem jogadores premium nem sessões retomadas. O proxy recusa o login antes de qualquer outra coisa, então o jogador vê a tela de `no-auth-server` em "Failed to connect to the server" e nenhuma conta é registrada.
- **Servidor inicial:** jogadores autenticados começam no lobby com menos jogadores; os demais começam no servidor de autenticação com menos jogadores que tenha vaga. Se nenhum lobby estiver vivo, o jogador autenticado começa no servidor de autenticação com menos jogadores e vê `no-lobby-server` assim que conecta. Um jogador sem servidor para entrar é desconectado com `no-lobby-server` ou `no-auth-server`. Jogadores não autenticados nunca caem num lobby.
- **Depois de se autenticar no servidor de autenticação:** o proxy envia o jogador para o lobby com menos jogadores. Se nenhum estiver vivo, o jogador continua no servidor de autenticação e vê `no-lobby-server`.
- **Quando o servidor do jogador desliga ou o expulsa:** o proxy escolhe para onde o jogador vai (`Fallbacks`), nunca o servidor de onde ele saiu.
  - Um jogador logado vai para o lobby com menos jogadores e vê `server-moved`. Um jogador expulso de propósito por um servidor que continua ligado também vai para um lobby, e vê o motivo da expulsão desse servidor.
  - Se nenhum outro lobby estiver vivo, mesmo com o servidor de autenticação ligado, o jogador logado é desconectado com a tela `server-restarting`. A mesma tela aparece quando todos os servidores desligam.
  - Um jogador que ainda está fazendo login só vai para outro servidor de autenticação quando o dele está desligando; sem outro, recebe `server-restarting`. Uma expulsão de propósito por um servidor de autenticação ligado (senha errada, tempo de login esgotado) continua como antes.
  - Um servidor conta como desligando quando já saiu da lista de servidores (o core dele avisou o desligamento), quando a expulsão não tem motivo ou quando o motivo é a mensagem de desligamento do Bukkit (`Server closed`).
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
| `messages.no-auth-server` | Tela de seis linhas: não foi possível conectar, motivo, dica para tentar de novo, uma linha em branco, onde acompanhar as notícias e o Discord da equipe | Mostrada quando o login é recusado porque nenhum servidor de autenticação está vivo. Uma lista YAML, um item por linha. |
| `messages.server-restarting` | Tela de cinco linhas: desconectado, motivo (servidor reiniciando), dica para tentar de novo, uma linha em branco e o Discord para mais informações | Mostrada quando o servidor do jogador desliga e não há outro lobby, ou nenhum servidor. Uma lista YAML, um item por linha. |
| `messages.server-moved` | `&eO servidor em que você estava está reiniciando. Você foi levado para outro servidor.` | Mensagem no chat depois de ser levado para outro servidor porque o seu desligou. |
| `messages.no-lobby-server`, `no-server`, `must-authenticate` | Textos em português | Mensagens mostradas pelo proxy. |
| `authentication.*` | veja [Autenticação](../authentication/README.pt-BR.md#configuração) | Detecção de premium e sessões de login. |

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
- [x] Enquanto nenhum servidor de autenticação estiver vivo, a rede recusa todo login, e um servidor de autenticação cheio ainda conta como ligado.
- [x] Um jogador cujo servidor desliga vai para outro lobby, e é desconectado com `server-restarting` quando só sobra o servidor de autenticação, ou nada.
- [ ] Depois do login o jogador chega a um lobby (conferido no jogo).
- [ ] Reiniciar um lobby leva os jogadores dele para o outro lobby (conferido no jogo).
- [ ] Jogadores premium e sessões retomadas começam direto em um lobby (conferido no jogo).

## Falhas e recuperação

- **Nenhum servidor de autenticação vivo:** os logins são recusados para todos com `no-auth-server`; quem já está online continua onde está. Os logins voltam a abrir poucos segundos depois que um servidor de autenticação informa o seu status.
- **Nenhum servidor do grupo de destino vivo:** o jogador fica onde está e vê uma mensagem.
- **O servidor do jogador desliga:** ele vai para outro lobby, ou é desconectado com `server-restarting` quando não há nenhum.
- **Redis fora do ar:** os pedidos de roteamento dos servidores falham; os jogadores ficam no servidor atual. O diretório se recupera poucos segundos depois da reconexão.
- **Reinício do proxy:** os jogadores reconectam e se autenticam de novo.

## Limitações conhecidas

- A escolha do servidor considera só a quantidade de jogadores; ainda não há noção de party, fila ou região.
- Com vários proxies, a passagem fica na memória de cada proxy, mas as sessões de login ficam no Redis, então um jogador pirata que reconecta por outro proxy dentro da duração da sessão ainda entra.
