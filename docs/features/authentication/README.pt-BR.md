[English](README.md) | **Português**

# Autenticação

Como os jogadores provam quem são. Contas premium (originais) são verificadas pela Mojang e não usam senha. Contas piratas resolvem um desafio anti-bot e depois se registram ou fazem login no servidor de autenticação. Um login com senha é lembrado por duas horas para o mesmo IP.

Fica no `core:proxy` (detecção de premium, sessões, a passagem) e no `servers:auth` (`Muxe-Auth`: desafio, `/registrar`, `/logar`).

## Histórias de usuário

- Como jogador premium, quero entrar sem digitar senha, porque a Mojang já me verificou.
- Como jogador pirata, quero criar uma senha, para que ninguém mais jogue com o meu nome.
- Como jogador pirata, quero reconectar em até duas horas do mesmo lugar sem fazer login de novo.
- Como operador, quero que bots sejam barrados antes da etapa de senha, e que senhas erradas repetidas também sejam barradas.

## Como funciona

### 1. Modo de login (proxy, antes do login)

Quando uma conexão começa, o proxy decide o seu **modo de login**:

| Situação | Modo |
|---|---|
| Existe uma conta com esse nome e ela é premium | Premium |
| Existe uma conta com esse nome e ela é pirata | Pirata |
| Não há conta, e a Mojang tem um perfil com esse nome | Premium |
| Não há conta, e a Mojang não tem perfil com esse nome | Pirata |
| Não há conta, e não foi possível consultar a Mojang | Conexão recusada com `service-unavailable` |

- Conexões **premium** são forçadas ao modo online: o Velocity verifica a sessão do jogador com a Mojang. Um cliente pirata usando um nome premium falha nessa verificação e não consegue entrar.
- Conexões **piratas** são forçadas ao modo offline.
- O modo é decidido uma única vez, no primeiro login, e gravado na conta. As respostas da Mojang ficam em cache no Redis por `authentication.premium-cache`.
- Com `authentication.premium-auto-login: false`, toda conta nova é pirata.

### 2. Autenticação automática (proxy, no login)

Depois que a conta é registrada, o proxy autentica o jogador na hora quando:

- a conta é **premium**, ou
- o jogador tem uma **sessão de login** válida: ele fez login com senha há menos de `authentication.session-duration` (2 horas por padrão), com a mesma conta e do mesmo IP.

Jogadores autenticados automaticamente vão direto para um lobby. Os demais vão para um servidor de autenticação.

### 3. Desafio (servidor de autenticação)

1. Ao entrar, o jogador fica escondido dos outros, é teleportado para o spawn e fica parado.
2. Um código aleatório (4 caracteres por padrão, sem `0`, `O`, `1` e `I`) é desenhado com partículas na frente do jogador, e o chat diz:
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

Enquanto não está autenticado, o jogador não pode se mover (só olhar em volta), conversar, usar outros comandos, interagir, largar ou pegar itens, abrir inventários, quebrar ou colocar blocos, levar dano ou perder fome.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/registrar <senha> <senha>` (`/register`) | Cria a senha da conta. | Nenhuma |
| `/logar <senha>` (`/login`) | Faz login. | Nenhuma |

## Permissões

Não se aplica.

## Configuração

`config.yml` do proxy:

| Chave | Padrão | Descrição |
|---|---|---|
| `authentication.premium-auto-login` | `true` | Detecta nomes premium e os deixa entrar sem senha. |
| `authentication.premium-lookup-timeout` | `3s` | Tempo limite da consulta à Mojang. |
| `authentication.premium-cache` | `6h` | Por quanto tempo as respostas da Mojang ficam em cache no Redis. |
| `authentication.session-duration` | `2h` | Por quanto tempo um login com senha é lembrado para a mesma conta e o mesmo IP. |

`plugins/Muxe-Auth/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `challenge.enabled` | `true` | Pede o código antes da senha. |
| `challenge.length` | `4` | Quantidade de caracteres do código. |
| `challenge.max-attempts` | `3` | Códigos errados antes de o jogador ser expulso. |
| `challenge.distance` | `4` | Blocos entre os olhos do jogador e o código. |
| `reminder.interval` | `5s` | De quanto em quanto tempo o pedido de senha e o título se repetem. |
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

## Dados

| Onde | O quê |
|---|---|
| PostgreSQL `auth.credentials` | `player_id`, `password_hash` (`pbkdf2_sha256$<iterations>$<salt>$<hash>`), `created_at`, `updated_at`, `last_login_at`. |
| PostgreSQL `core.accounts.premium` | Se a conta faz login pela Mojang. Veja [Contas](../accounts/README.pt-BR.md). |
| Redis `muxe:auth:session:<player id>` | SHA-256 do id do jogador e do IP do último login com senha. Expira depois de `session-duration`. O IP puro nunca é guardado. |
| Redis `muxe:auth:premium:<lowercase name>` | `PREMIUM` ou `CRACKED`, vindo da Mojang. Expira depois de `premium-cache`. |

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

- [x] Contas conhecidas mantêm o seu modo de login; nomes novos seguem a Mojang; uma Mojang inacessível recusa só nomes novos.
- [x] As respostas da Mojang ficam em cache, e um cache indisponível não bloqueia logins.
- [x] Um login com senha só é retomado para a mesma conta, do mesmo IP, dentro da duração da sessão, e o IP é guardado como hash.
- [x] O desafio aceita o código certo ignorando maiúsculas, minúsculas e espaços, renova o código depois de uma resposta errada e falha depois do máximo de tentativas.
- [x] Os códigos só usam caracteres sem ambiguidade que a fonte de partículas consegue desenhar.
- [x] Um jogador novo consegue se registrar; um jogador registrado não pode se registrar de novo; senhas fracas são recusadas antes do hash.
- [x] A senha certa faz login e registra o horário do login; a errada é recusada; o limite bloqueia o jogador.
- [ ] Um jogador premium entra direto em um lobby, sem senha (conferido no jogo).
- [ ] Um cliente pirata usando um nome premium é recusado pelo Velocity (conferido no jogo).
- [ ] O código fica legível na frente do jogador, e o título e a action bar aparecem (conferido no jogo).
- [ ] Reentrar em até duas horas do mesmo IP pula o servidor de autenticação (conferido no jogo).

## Falhas e recuperação

- **Mojang inacessível:** contas existentes fazem login normalmente; nomes novos são recusados com `service-unavailable` até a Mojang responder, para que um nome premium nunca seja registrado como pirata por engano.
- **Redis indisponível:** sessões não podem ser retomadas, então jogadores piratas fazem login com a senha; as consultas de premium vão direto para a Mojang.
- **Banco indisponível:** o login é recusado com `service-unavailable`; nada fica gravado pela metade.
- **O proxy não recebe `auth.authenticated`:** a publicação falha e o jogador é expulso com `service-unavailable`, para nunca ficar esperando para sempre.
- **Servidor sem suporte a action bar:** a action bar é pulada e todo o resto funciona; os títulos usam os tempos padrão do servidor.

## Limitações conhecidas

- Uma conta pirata registrada antes do dono premium do mesmo nome fica com o nome; o dono premium não consegue entrar com ele. A equipe precisa resolver esses casos.
- Jogadores premium ainda não podem escolher proteger a conta também com senha. Essa preferência está planejada e vai reaproveitar as mesmas regras de sessão.
- Os limites de senha errada são contados por jogador em cada servidor de autenticação, em memória.
- Ainda não há troca nem recuperação de senha.
- Action bars e tempos de título usam os internos do servidor 1.8 por reflexão; ficam desativados, com um aviso, se o servidor não os expuser.
