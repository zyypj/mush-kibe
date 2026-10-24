[English](README.md) | **Português**

# Autenticação

Como os jogadores provam quem são. Contas premium (originais) são verificadas pela Mojang e não usam senha. Contas piratas resolvem um desafio anti-bot e depois se registram ou fazem login no servidor de autenticação. Um login com senha é lembrado por duas horas para o mesmo IP.

Fica no `core:proxy` (detecção de premium, sessões, a passagem) e no `servers:auth` (`Muxe-Auth`: desafio, `/registrar`, `/logar`).

## Histórias de usuário

- Como jogador premium, quero entrar sem digitar senha, porque a Mojang já me verificou.
- Como jogador pirata, quero criar uma senha, para que ninguém mais jogue com o meu nome.
- Como jogador pirata, quero reconectar em até duas horas do mesmo lugar sem fazer login de novo.
- Como operador, quero que bots sejam barrados antes da etapa de senha, e que senhas erradas repetidas também sejam barradas.
- Como membro da equipe, quero escolher onde os jogadores aparecem no servidor de autenticação e onde o código do desafio é desenhado, sem editar arquivos.

## Como funciona

### 1. Modo de login (proxy, antes do login)

Quando uma conexão começa, o proxy decide o seu **modo de login**:

| Situação | Modo |
|---|---|
| Existe uma conta com esse nome e ela é premium | Premium |
| Existe uma conta com esse nome e ela é pirata, e o nome não é premium na Mojang | Pirata |
| Existe uma conta com esse nome e ela é pirata, mas o nome agora é premium na Mojang | Premium: o dono na Mojang recupera o nome |
| Não há conta, e a Mojang tem um perfil com esse nome | Premium |
| Não há conta, e a Mojang não tem perfil com esse nome | Pirata |
| Não há conta, e não foi possível consultar a Mojang | Conexão recusada com `service-unavailable` |

- Conexões **premium** são forçadas ao modo online: o Velocity verifica a sessão do jogador com a Mojang. Um cliente pirata usando um nome premium falha nessa verificação e não consegue entrar.
- Conexões **piratas** são forçadas ao modo offline.
- A marca de premium é gravada na conta no primeiro login. Nomes que estão com contas piratas são verificados de novo na Mojang a cada login, então um nome que virou premium vai para o seu dono. Quem fica com cada nome está descrito em [Contas](../accounts/README.pt-BR.md#dono-do-nome).
- As respostas da Mojang ficam em cache no Redis: respostas premium por `authentication.premium-cache`, respostas piratas pelo tempo menor de `authentication.cracked-cache`, então um nome comprado na Mojang fica protegido em minutos.
- Com `authentication.premium-auto-login: false`, toda conta nova é pirata.

### 2. Autenticação automática (proxy, no login)

Depois que a conta é registrada, o proxy autentica o jogador na hora quando:

- a conta é **premium**, ou
- o jogador tem uma **sessão de login** válida: ele fez login com senha há menos de `authentication.session-duration` (2 horas por padrão), com a mesma conta e do mesmo IP.

Jogadores autenticados automaticamente vão direto para um lobby. Os demais vão para um servidor de autenticação.

### 3. Desafio (servidor de autenticação)

1. Ao entrar, o servidor de autenticação pergunta ao proxy se o jogador já está autenticado (`auth.status_request`). Se estiver, o desafio e a senha são pulados e o jogador vê `Você já está autenticado.` Caso contrário, o jogador fica escondido dos outros, é teleportado para o spawn configurado (ou o spawn do mundo). O jogador pode andar enquanto se autentica.
2. Um código aleatório (4 caracteres por padrão, sem `0`, `O`, `1` e `I`) é desenhado com partículas na posição do código configurada, virado para o jogador, ou 4 blocos à frente do jogador quando nenhuma posição está configurada. O chat diz:
   - `DESAFIO:` / `Digite o código em frente no chat!`, com uma dica para ativar as partículas.
   - A action bar repete `Digite o código em frente no chat!` a cada segundo.
3. O jogador digita o código no chat (maiúsculas, minúsculas e espaços são ignorados). As mensagens do chat nunca aparecem para os outros jogadores.
4. Um código errado gera um novo. Depois de `challenge.max-attempts` respostas erradas, o jogador é expulso.
5. O código certo mostra `Desafio completado com sucesso!` e passa para a etapa de senha.

O código é desenhado com partículas em vez de entidades de texto, para que bots simples não consigam lê-lo pelo protocolo.

### 4. Senha (servidor de autenticação)

- O chat mostra `NUNCA COMPARTILHE A SUA SENHA COM NINGUÉM!` e `Faça o login utilizando /logar <senha>` (ou `/registrar <senha> <senha>`), e a tela mostra o título `MUXE` com `Use /logar <senha>`. Os dois se repetem a cada `reminder.interval`.
- `/registrar <senha> <senha>` valida a senha (confirmação igual, tamanho, diferente do nome), gera o hash e o grava.
- `/logar <senha>` confere a senha. Uma senha errada mostra quantas tentativas restam; ao atingir o limite, o jogador é expulso.
- Em caso de sucesso: `Você se autenticou com sucesso!` (ou `Você se registrou com sucesso!`) e `Enviando ao lobby...`. O servidor de autenticação publica `auth.authenticated`; o proxy abre a passagem, **lembra a sessão de login** e envia o jogador para um lobby.
- Um jogador que não termina em `security.login-timeout` é expulso.

Enquanto não está autenticado, o jogador só pode andar, digitar o código no chat e usar `/logar` e `/registrar`. Ele não pode conversar com os outros, usar outros comandos, ver outros jogadores, interagir com blocos ou entidades (molduras, suportes de armadura, pinturas), usar baldes, largar ou pegar itens, abrir inventários, quebrar ou colocar blocos, levar dano ou perder fome. Quem cair abaixo de `protection.void-level` volta para o spawn. Depois de autenticar, os comandos são liberados para todos; construir e interagir são liberados só para jogadores com `muxe.auth.setup`. O chat continua fechado para todos no servidor de autenticação.

### 5. Configurando o servidor de auth (equipe)

1. Faça login normalmente e volte ao servidor de autenticação com o `/server auth-1` do Velocity. O proxy confirma que você está autenticado, então você pula o desafio e, com `muxe.auth.setup`, pode andar livremente.
2. Fique onde os jogadores devem aparecer, olhando para onde eles devem olhar, e use `/authsetup spawn`.
3. Fique onde o código deve aparecer (ele fica centralizado nos seus olhos) e use `/authsetup codigo`. O código é virado para o spawn automaticamente.
4. Use `/authsetup testar` para ver um código de exemplo por 10 segundos, visto a partir do spawn.

As posições são salvas em `plugins/Muxe-Auth/config.yml`, em `locations`, e valem para os próximos jogadores que entrarem.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/registrar <senha> <senha>` (`/register`) | Cria a senha da conta. | Nenhuma |
| `/logar <senha>` (`/login`) | Faz login. | Nenhuma |
| `/authsetup spawn` | Define o spawn na sua posição e direção. | `muxe.auth.setup` |
| `/authsetup codigo` | Define a posição do código do desafio nos seus olhos. | `muxe.auth.setup` |
| `/authsetup testar` | Mostra um código de exemplo por 10 segundos, visto a partir do spawn. | `muxe.auth.setup` |
| `/authsetup info` | Mostra as duas posições. | `muxe.auth.setup` |
| `/authsetup limpar <spawn\|codigo>` | Remove uma posição e volta ao padrão. | `muxe.auth.setup` |

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.auth.setup` | `/authsetup`, e andar, construir e interagir no servidor de autenticação depois do login. Só vale depois que o jogador está autenticado, então um cliente pirata usando o nome de um operador não ganha nada. | op |

## Configuração

`config.yml` do proxy:

| Chave | Padrão | Descrição |
|---|---|---|
| `authentication.premium-auto-login` | `true` | Detecta nomes premium e os deixa entrar sem senha. |
| `authentication.premium-lookup-timeout` | `3s` | Tempo limite da consulta à Mojang. |
| `authentication.premium-cache` | `6h` | Por quanto tempo as respostas "premium" da Mojang ficam em cache no Redis. |
| `authentication.cracked-cache` | `10m` | Por quanto tempo as respostas "não premium" ficam em cache. É curto, para que um nome que vira premium fique protegido rápido. |
| `authentication.session-duration` | `2h` | Por quanto tempo um login com senha é lembrado para a mesma conta e o mesmo IP. |

`plugins/Muxe-Auth/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `challenge.enabled` | `true` | Pede o código antes da senha. |
| `challenge.length` | `4` | Quantidade de caracteres do código. |
| `challenge.max-attempts` | `3` | Códigos errados antes de o jogador ser expulso. |
| `challenge.distance` | `4` | Blocos entre os olhos do jogador e o código. |
| `reminder.interval` | `5s` | De quanto em quanto tempo o pedido de senha e o título se repetem. |
| `protection.void-level` | `0` | Jogadores abaixo dessa altura voltam para o spawn. |
| `locations.spawn` | não definido | Gravado por `/authsetup spawn`: mundo, x, y, z, yaw, pitch. Sem ele, é usado o spawn do mundo. |
| `locations.challenge-code` | não definido | Gravado por `/authsetup codigo`. Sem ele, o código aparece `challenge.distance` blocos à frente do jogador. |
| `security.pbkdf2-iterations` | `600000` | Iterações de PBKDF2-HMAC-SHA256 para novos hashes. Aumentar o valor atualiza os hashes antigos no próximo login. |
| `security.login-timeout` | `60s` | Tempo para se autenticar antes de ser expulso. |
| `security.max-attempts` | `5` | Senhas erradas permitidas por janela. |
| `security.attempt-window` | `5m` | Janela de contagem das senhas erradas. |
| `password.min-length` | `6` | Tamanho mínimo da senha. |
| `password.max-length` | `64` | Tamanho máximo da senha. |
| `messages.*` | Textos em português | Todo texto mostrado ao jogador, incluindo o título (`title`, `subtitle-login`, `subtitle-register`) e a action bar (`challenge-action-bar`). |

Obrigatório:

- `velocity.toml`: `online-mode = false`. Jogadores premium são colocados em modo online individualmente pelo proxy.
- `spigot.yml` do servidor de autenticação: `commands.log: false`, para que senhas nunca sejam escritas no console.

## Pacotes

| Id | Destino | Política | Para quê |
|---|---|---|---|
| `auth.authenticated` | proxy | `DURABLE`, janela de 5 min, idade máxima de 15 s | Avisa o proxy que o jogador se registrou (`REGISTER`) ou fez login (`LOGIN`). |
| `auth.status_request` | proxy | padrão, idade máxima de 5 s | Pergunta se um jogador que acabou de entrar no servidor de autenticação já está autenticado. Só responde o proxy em que o jogador está conectado. |
| `auth.status_response` | quem pediu | padrão, idade máxima de 5 s | A resposta. Nenhuma resposta dentro de `messaging.request-timeout` significa "não autenticado". |

## Dados

| Onde | O quê |
|---|---|
| PostgreSQL `auth.credentials` | `player_id`, `password_hash` (`pbkdf2_sha256$<iterations>$<salt>$<hash>`), `created_at`, `updated_at`, `last_login_at`. |
| PostgreSQL `core.accounts.premium` | Se a conta faz login pela Mojang. Veja [Contas](../accounts/README.pt-BR.md). |
| Redis `muxe:auth:session:<player id>` | SHA-256 do id do jogador e do IP do último login com senha. Expira depois de `session-duration`. O IP puro nunca é guardado. |
| Redis `muxe:auth:premium:<lowercase name>` | `PREMIUM` ou `CRACKED`, vindo da Mojang. Expira depois de `premium-cache` ou `cracked-cache`. |

## Aplicação

| Mensagem | Tipo | Resultado |
|---|---|---|
| `RegisterCredentialCommand` | Command | `Result<Void>`; falhas `auth.already_registered`, `auth.passwords_do_not_match`, `auth.password_too_short`, `auth.password_too_long`, `auth.password_equals_username`. |
| `VerifyCredentialCommand` | Command | `Result<Void>`; falhas `auth.not_registered`, `auth.wrong_password`. Registra o login e refaz hashes fracos. |
| `HasCredentialQuery` | Query | `Boolean`. |
| `FindAccountByUsernameQuery` | Query | `Optional<Account>`, usada pelo proxy para decidir o modo de login. |

## Métricas

| Métrica | O que mostra |
|---|---|
| `muxe_auth_login_modes_total{mode}` | Modos de login decididos pelo proxy: `premium`, `cracked`, `undecided`. |
| `muxe_auth_logins_total{mode}` | Como os jogadores entraram: `premium`, `session` (retomada) ou `password` (enviados ao servidor de autenticação). |
| `muxe_auth_premium_lookups_total{result,source}` | Consultas à Mojang por `premium`/`cracked`/`unknown`, respondidas pelo `cache` ou pela consulta `remote`. |
| `muxe_auth_challenges_total{outcome}` | Respostas ao desafio: `passed`, `wrong`, `failed`. |

Além de `muxe_cqrs_handle_seconds` para os comandos e consultas acima, `muxe_db_transaction_seconds{operation="auth.*"}` e as métricas de pacote de `auth.authenticated`.

## Integrações

- [Roteamento da rede](../network-routing/README.pt-BR.md): a passagem do proxy e a escolha do primeiro servidor.
- [Contas](../accounts/README.pt-BR.md): a conta e a sua marca de premium.
- Utilitários do core: `Title`, `ActionBar` e `ParticleText`, descritos na [API do core](../../development/core-api/README.pt-BR.md).
- API de perfis da Mojang (`api.mojang.com/users/profiles/minecraft/<name>`).

## Critérios de aceitação

- [x] Contas premium sempre fazem login pela Mojang; contas piratas continuam piratas enquanto o nome não é premium e vão para o dono na Mojang quando passa a ser; nomes novos seguem a Mojang; uma Mojang inacessível recusa só nomes novos.
- [x] As respostas da Mojang ficam em cache, as piratas por menos tempo que as premium, e um cache indisponível não bloqueia logins.
- [x] Um login com senha só é retomado para a mesma conta, do mesmo IP, dentro da duração da sessão, e o IP é guardado como hash.
- [x] O desafio aceita o código certo ignorando maiúsculas, minúsculas e espaços, renova o código depois de uma resposta errada e falha depois do máximo de tentativas.
- [x] Os códigos só usam caracteres sem ambiguidade que a fonte de partículas consegue desenhar.
- [x] Um jogador novo consegue se registrar; um jogador registrado não pode se registrar de novo; senhas fracas são recusadas antes do hash.
- [x] A senha certa faz login e registra o horário do login; a errada é recusada; o limite bloqueia o jogador.
- [ ] Um jogador premium entra direto em um lobby, sem senha (conferido no jogo).
- [ ] Um cliente pirata usando um nome premium é recusado pelo Velocity (conferido no jogo).
- [ ] O código fica legível na frente do jogador, e o título e a action bar aparecem (conferido no jogo).
- [ ] Reentrar em até duas horas do mesmo IP pula o servidor de autenticação (conferido no jogo).
- [x] O servidor de autenticação só pula o fluxo quando o proxy diz que o jogador está autenticado; nenhuma resposta, ou uma resposta sobre outro jogador, significa não autenticado.
- [x] Uma posição de código configurada fica virada para quem olha; sem ela, o código aparece na frente do jogador.
- [ ] A equipe consegue definir o spawn e a posição do código com `/authsetup` e pré-visualizar (conferido no jogo).
- [x] Só a equipe autenticada pode interagir; jogadores não autenticados só usam os comandos de login, mesmo com prefixo `plugin:`.
- [ ] Jogadores não autenticados conseguem andar, mas não interagem, não constroem, não falam no chat e não veem os outros (conferido no jogo).

## Falhas e recuperação

- **Mojang inacessível:** contas existentes fazem login normalmente (nomes piratas não podem ser verificados de novo até a Mojang responder); nomes novos são recusados com `service-unavailable`, para que um nome premium nunca seja registrado como pirata por engano.
- **Redis indisponível:** sessões não podem ser retomadas, então jogadores piratas fazem login com a senha; as consultas de premium vão direto para a Mojang.
- **Banco indisponível:** o login é recusado com `service-unavailable`; nada fica gravado pela metade.
- **O proxy não recebe `auth.authenticated`:** a publicação falha e o jogador é expulso com `service-unavailable`, para nunca ficar esperando para sempre.
- **Servidor sem suporte a action bar:** a action bar é pulada e todo o resto funciona; os títulos usam os tempos padrão do servidor.

## Limitações conhecidas

- Um nome comprado na Mojang ainda pode ser usado por um jogador pirata por até `cracked-cache` (10 minutos por padrão) depois de uma resposta "não premium" em cache.
- Quando um cliente pirata tenta um nome premium, a mensagem de recusa é o próprio erro de modo online do Velocity.
- Jogadores premium ainda não podem escolher proteger a conta também com senha. Essa preferência está planejada e vai reaproveitar as mesmas regras de sessão.
- Os limites de senha errada são contados por jogador em cada servidor de autenticação, em memória.
- Ainda não há troca nem recuperação de senha.
- Action bars e tempos de título usam os internos do servidor 1.8 por reflexão; ficam desativados, com um aviso, se o servidor não os expuser.
