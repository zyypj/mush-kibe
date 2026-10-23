[English](README.md) | **Português**

# Autenticação

Os jogadores se registram e fazem login com uma senha no servidor de autenticação antes de poder chegar a qualquer outro servidor. Fica no `servers:auth` (`Muxe-Auth`).

## Histórias de usuário

- Como jogador novo, quero criar uma senha, para que ninguém mais jogue com o meu nome.
- Como jogador que volta, quero fazer login com a minha senha e ir direto para o lobby.
- Como operador, quero que senhas erradas repetidas sejam barradas, para que as contas não sofram força bruta.

## Como funciona

1. O proxy envia toda nova conexão para o servidor de autenticação com menos jogadores.
2. Ao entrar, o jogador fica escondido dos outros jogadores, é teleportado para o spawn do mundo e fica congelado. O servidor verifica se o jogador tem senha e pede `/register` ou `/login`.
3. `/register <password> <password>` valida a senha (confirmação igual, tamanho, diferente do nome), gera o hash e o grava.
4. `/login <password>` confere a senha. Uma senha errada mostra quantas tentativas restam; ao atingir o limite, o jogador é expulso.
5. Com sucesso, o servidor de autenticação publica `auth.authenticated` para o proxy, que abre a passagem e envia o jogador para um lobby.
6. Um jogador que não se autentica a tempo é expulso.

Enquanto não está autenticado, o jogador não pode se mover (só olhar ao redor), conversar, usar outros comandos, interagir, dropar ou pegar itens, abrir inventários, quebrar ou colocar blocos, levar dano ou perder fome.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/register <password> <password>` (`/registrar`) | Cria a senha da conta. | Nenhuma |
| `/login <password>` (`/logar`) | Faz login. | Nenhuma |

## Permissões

Não se aplica.

## Configuração

`plugins/Muxe-Auth/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `security.pbkdf2-iterations` | `600000` | Iterações do PBKDF2-HMAC-SHA256 para novos hashes. Aumentar o valor atualiza os hashes antigos no próximo login. |
| `security.login-timeout` | `60s` | Tempo para se autenticar antes de ser expulso. |
| `security.max-attempts` | `5` | Senhas erradas permitidas por janela. |
| `security.attempt-window` | `5m` | Janela para contar as senhas erradas. |
| `password.min-length` | `6` | Tamanho mínimo da senha. |
| `password.max-length` | `64` | Tamanho máximo da senha. |
| `messages.*` | Textos em português | Todas as mensagens mostradas ao jogador. |

Obrigatório no `spigot.yml` do servidor de autenticação: `commands.log: false`, para que as senhas nunca sejam escritas no console.

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `auth.authenticated` | proxy | `DURABLE`, janela de 5 min, idade máxima de 15 s | Avisa o proxy que o jogador se registrou (`REGISTER`) ou fez login (`LOGIN`). |

## Dados

Schema `auth`, que pertence ao `servers:auth`:

| Tabela | Colunas |
|---|---|
| `credentials` | `player_id` UUID chave primária, `password_hash` (`pbkdf2_sha256$<iterations>$<salt>$<hash>`), `created_at`, `updated_at`, `last_login_at`. |

## Aplicação

| Mensagem | Tipo | Resultado |
|---|---|---|
| `RegisterCredentialCommand` | Comando | `Result<Void>`; falhas `auth.already_registered`, `auth.passwords_do_not_match`, `auth.password_too_short`, `auth.password_too_long`, `auth.password_equals_username`. |
| `VerifyCredentialCommand` | Comando | `Result<Void>`; falhas `auth.not_registered`, `auth.wrong_password`. Registra o login e refaz hashes fracos. |
| `HasCredentialQuery` | Query | `Boolean`. |

## Métricas

`muxe_cqrs_handle_seconds` para as três mensagens acima, `muxe_db_transaction_seconds{operation="auth.*"}` e as métricas de pacote do `auth.authenticated`.

## Integrações

- [Roteamento da rede](../network-routing/README.pt-BR.md): a passagem do proxy que espera pelo `auth.authenticated`.
- [Contas](../accounts/README.pt-BR.md): a conta é criada pelo proxy antes de o jogador chegar ao servidor de autenticação.

## Critérios de aceitação

- [x] Um jogador novo consegue se registrar e então fica autenticado.
- [x] Um jogador registrado não consegue se registrar de novo.
- [x] Senhas fracas são recusadas antes do hash.
- [x] A senha certa faz login e registra o horário do login; a errada é recusada.
- [x] Depois do número máximo de senhas erradas dentro da janela, o jogador é bloqueado.
- [x] Hashes feitos com menos iterações são atualizados no login.
- [ ] Jogadores não autenticados não conseguem se mover, conversar nem usar outros comandos (conferido no jogo).
- [ ] Depois de se autenticar, o jogador é enviado para um lobby (conferido no jogo).

## Falhas e recuperação

- **Banco indisponível:** o jogador é expulso com `service-unavailable`; nada fica gravado pela metade.
- **O proxy não recebe o `auth.authenticated`** (Redis fora do ar): a publicação falha e o jogador é expulso com `service-unavailable`, para que nunca fique esperando para sempre. Ele faz login de novo quando o Redis voltar.
- **O jogador desconecta enquanto um comando roda:** o resultado é ignorado e a sessão é limpa.

## Limitações conhecidas

- Os limites de senha errada são contados por jogador em cada servidor de autenticação, em memória; eles zeram quando o servidor reinicia.
- Ainda não há troca de senha, recuperação nem login automático de contas premium.
- O Spigot registra os comandos por padrão; os operadores precisam definir `commands.log: false`.
