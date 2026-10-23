[English](README.md) | **Português**

# Contas

Todo jogador tem uma conta, criada na primeira vez que ele chega ao proxy e atualizada a cada login. As contas são a identidade sobre a qual as outras features são construídas.

## Histórias de usuário

- Como jogador, quero que o meu progresso fique ligado ao meu nome, para encontrá-lo de novo toda vez que eu entrar.
- Como operador, quero que dois jogadores nunca compartilhem um nome que difira só em maiúsculas e minúsculas, para que as identidades do modo offline não se confundam.
- Como jogador premium, quero manter o meu progresso quando troco de nome na Mojang, e recuperar o meu nome se outra pessoa estiver usando ele aqui.

## Como funciona

- Quando um jogador entra no proxy, o proxy despacha `RecordLoginCommand(playerId, username, premium)`.
- O primeiro login cria a conta com `first_seen_at`, `last_seen_at` e se ela é **premium** (verificada pela Mojang) ou pirata. Os logins seguintes atualizam o nome e o `last_seen_at`; a marca de premium nunca muda.
- Contas premium usam o UUID da Mojang do jogador; contas piratas usam o UUID do modo offline derivado do nome. A marca decide como a conta faz login a partir daí, como descrito em [Autenticação](../authentication/README.pt-BR.md).

### Dono do nome

Uma identidade verificada pela Mojang sempre fica com o nome:

| Situação | O que acontece |
|---|---|
| Um jogador premium troca de nome na Mojang e volta | Mesmo UUID da Mojang, mesma conta: o nome é atualizado e todo o progresso é mantido. |
| Um jogador premium entra com um nome que está com outra conta (premium ou pirata, com quaisquer maiúsculas e minúsculas) | O nome passa para o jogador premium. A outra conta mantém os seus dados, mas fica sem nome até o dono dela entrar de novo. |
| Um jogador pirata entra com um nome que está com outra conta, com outras maiúsculas e minúsculas | Recusado com `name-case-conflict`. |
| Um jogador pirata tenta um nome que está com uma conta premium | O proxy força o modo online e o Velocity recusa a conexão. |

Uma conta sem nome mantém o seu id, a marca de premium e os dados. Um dono premium que trocou de nome recebe o nome novo no próximo login. Uma conta pirata que perdeu o nome para o dono premium não consegue mais entrar com esse nome.
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
| `accounts` | `id` UUID chave primária, `username` (vazio enquanto o nome está liberado), `premium`, `first_seen_at`, `last_seen_at`. Índice único em `LOWER(username)`. |

## Aplicação

| Mensagem | Tipo | Resultado |
|---|---|---|
| `RecordLoginCommand` | Comando | `Result<RecordedLogin>`: a conta e, quando um login premium tomou o nome, o id da conta que o perdeu. Falhas `account.invalid_username`, `account.name_case_conflict`. |
| `FindAccountQuery` | Query | `Optional<Account>` pelo id. |
| `FindAccountByUsernameQuery` | Query | `Optional<Account>` pelo nome, ignorando maiúsculas e minúsculas. |

## Métricas

`muxe_cqrs_handle_seconds{message="record_login_command"}`, `muxe_db_transaction_seconds{operation="account.record_login"}` e `muxe_accounts_names_reclaimed_total`, contada pelo proxy sempre que um jogador premium toma um nome de outra conta (também registrada no log com os dois ids).

## Integrações

- O `LoginListener` do proxy registra todo login.
- Qualquer plugin pode fazer a `FindAccountQuery` pelo `MuxeCore.queries()`.

## Critérios de aceitação

- [x] Um jogador novo ganha uma conta no primeiro login.
- [x] Um jogador que volta mantém o `first_seen_at` e recebe um novo `last_seen_at`.
- [x] Um nome que difere só em maiúsculas e minúsculas de uma conta existente é recusado, no código e pelo índice do banco.
- [x] Um nome inválido é recusado sem tocar no banco.
- [x] A marca de premium é gravada no primeiro login e nunca muda.
- [x] Um jogador premium que trocou de nome mantém a mesma conta.
- [x] Um login premium toma o seu nome de qualquer outra conta, premium ou pirata, com quaisquer maiúsculas e minúsculas, e a outra conta mantém os seus dados sem nome.
- [x] Um login pirata nunca toma o nome de outra conta.
- [x] Um nome pode ser liberado e tomado por outra conta na mesma transação (teste de integração).

## Falhas e recuperação

- Se o PostgreSQL não responde, o login é negado com `service-unavailable`. Nada fica gravado pela metade: o comando roda em uma única transação.
- Dois logins simultâneos com nomes que diferem só em maiúsculas e minúsculas: o índice único faz um deles falhar; esse jogador é recusado e pode tentar de novo.

## Limitações conhecidas

- Os jogadores não podem mudar de nome nem juntar contas.
- Um jogador pirata que perde um nome para o dono premium mantém os dados na conta antiga, mas não consegue chegar a eles com um nome novo. A equipe pode movê-los manualmente; ainda não há comando para isso.
