[English](README.md) | **Português**

# Contas

Todo jogador tem uma conta, criada na primeira vez que ele chega ao proxy e atualizada a cada login. As contas são a identidade sobre a qual as outras features são construídas.

## Histórias de usuário

- Como jogador, quero que o meu progresso fique ligado ao meu nome, para encontrá-lo de novo toda vez que eu entrar.
- Como operador, quero que dois jogadores nunca compartilhem um nome que difira só em maiúsculas e minúsculas, para que as identidades do modo offline não se confundam.

## Como funciona

- Quando um jogador entra no proxy, o proxy despacha `RecordLoginCommand(playerId, username, premium)`.
- O primeiro login cria a conta com `first_seen_at`, `last_seen_at` e se ela é **premium** (verificada pela Mojang) ou pirata. Os logins seguintes atualizam o nome e o `last_seen_at`; a marca de premium nunca muda.
- Contas premium usam o UUID da Mojang do jogador; contas piratas usam o UUID do modo offline derivado do nome. A marca decide como a conta faz login a partir daí, como descrito em [Autenticação](../authentication/README.pt-BR.md).
- Se outra conta já usa o mesmo nome com maiúsculas e minúsculas diferentes, o login é recusado com uma mensagem pedindo que o jogador use o nome exato.
- Os nomes precisam seguir `[A-Za-z0-9_]{3,16}`; qualquer outra coisa é recusada.

## Comandos

Não se aplica.

## Permissões

Não se aplica.

## Configuração

Mensagens no `config.yml` do proxy:

| Chave | Descrição |
|---|---|
| `messages.name-case-conflict` | Mostrada quando o nome difere só em maiúsculas e minúsculas de uma conta existente. |
| `messages.invalid-username` | Mostrada quando o nome não é permitido. |
| `messages.service-unavailable` | Mostrada quando a conta não pode ser registrada. |

## Pacotes

Não se aplica.

## Dados

Schema `core`, que pertence ao `core:common`:

| Tabela | Colunas |
|---|---|
| `accounts` | `id` UUID chave primária, `username`, `premium`, `first_seen_at`, `last_seen_at`. Índice único em `LOWER(username)`. |

## Aplicação

| Mensagem | Tipo | Resultado |
|---|---|---|
| `RecordLoginCommand` | Comando | `Result<Account>`; falhas `account.invalid_username`, `account.name_case_conflict`. |
| `FindAccountQuery` | Query | `Optional<Account>` pelo id. |
| `FindAccountByUsernameQuery` | Query | `Optional<Account>` pelo nome, ignorando maiúsculas e minúsculas. |

## Métricas

`muxe_cqrs_handle_seconds{message="record_login_command"}` e `muxe_db_transaction_seconds{operation="account.record_login"}`.

## Integrações

- O `LoginListener` do proxy registra todo login.
- Qualquer plugin pode fazer a `FindAccountQuery` pelo `MuxeCore.queries()`.

## Critérios de aceitação

- [x] Um jogador novo ganha uma conta no primeiro login.
- [x] Um jogador que volta mantém o `first_seen_at` e recebe um novo `last_seen_at`.
- [x] Um nome que difere só em maiúsculas e minúsculas de uma conta existente é recusado, no código e pelo índice do banco.
- [x] Um nome inválido é recusado sem tocar no banco.
- [x] A marca de premium é gravada no primeiro login e nunca muda.

## Falhas e recuperação

- Se o PostgreSQL não responde, o login é negado com `service-unavailable`. Nada fica gravado pela metade: o comando roda em uma única transação.
- Dois logins simultâneos com nomes que diferem só em maiúsculas e minúsculas: o índice único faz um deles falhar; esse jogador é recusado e pode tentar de novo.

## Limitações conhecidas

- Os jogadores não podem mudar de nome nem juntar contas.
- Uma conta pirata que registrou um nome primeiro fica com ele, mesmo que um jogador premium seja dono desse nome na Mojang.
