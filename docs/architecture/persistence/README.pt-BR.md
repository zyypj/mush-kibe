[English](README.md) | **Português**

# Persistência

Como a Muxe guarda dados. O PostgreSQL é a fonte da verdade; o Redis é um transporte e cache que pode ser esvaziado a qualquer momento sem perder nada.

Código: `core/common/src/main/java/br/com/muxe/core/common/persistence`.

## Blocos de construção

| Classe | Responsabilidade |
|---|---|
| `DatabaseConfig` | URL, credenciais, tamanho do pool, timeout de conexão, se roda as migrações. |
| `Database` | Um pool HikariCP sobre `PGSimpleDataSource`, com métricas do pool. |
| `Transactions` | A porta usada pelos handlers: `inTransaction(operation, work)` e `readOnly(operation, work)`. |
| `JdbcTransactions` | A implementação JDBC. |
| `TransactionContext` | Dá aos repositórios a conexão da transação atual. |
| `MigrationRunner` | Roda as migrações Flyway de um módulo no seu próprio schema. |
| `ExpiringStore` | Valores de curta duração no Redis (`RedisExpiringStore`), cada um com um tempo de vida. As chaves recebem o prefixo `muxe:`. |

## Transações

```java
return transactions.inTransaction("account.record_login", () -> {
    Optional<Account> holder = accounts.findByUsername(command.username());
    accounts.save(account);
    return Result.success(account);
});
```

- A conexão é vinculada com um **`ScopedValue`** do Java 25 durante o trabalho. Os repositórios chamam `TransactionContext.connection()`; chamar fora de uma transação falha com uma mensagem clara.
- Uma transação iniciada dentro de outra **se junta** a ela, então um handler pode compor chamadas de repositório livremente.
- O trabalho faz commit quando retorna e rollback quando lança exceção. Exceções checadas viram `PersistenceException`; exceções de runtime se propagam sem mudança.
- `readOnly` marca a conexão como somente leitura, para o PostgreSQL otimizar e para escritas acidentais falharem.
- **Nunca na thread principal.** No Spigot, `JdbcTransactions` usa um `MainThreadGuard` e lança `BlockingOnMainThreadException` se for chamado pela thread do servidor. Os handlers rodam nas virtual threads dos buses de CQRS.
- Toda transação é medida em `muxe_db_transaction_seconds{operation, outcome}`. O nome da operação é uma string estável e de baixa cardinalidade, como `auth.register`.

## Repositórios

- A interface fica no pacote `domain` da feature; a implementação em `infrastructure/postgres`.
- O SQL é escrito à mão, em text blocks, com `PreparedStatement`. Sem ORM: o custo de cada consulta fica visível.
- As tabelas são sempre qualificadas pelo schema (`core.accounts`, `auth.credentials`).
- Testes unitários usam repositórios em memória com `DirectTransactions`; testes de integração usam o repositório real contra um container do PostgreSQL.

## Migrações

- Cada módulo é dono de um **schema** do PostgreSQL com o mesmo nome e das suas migrações em `src/main/resources/db/migration/<module>/`, nomeadas `V<n>__<description>.sql`.
- `MigrationRunner.migrate(module, classLoader)` cria o schema se necessário e mantém o histórico em `<module>.schema_history`.
- O core migra o schema `core` na inicialização quando `database.run-migrations` é true. Um plugin migra seu próprio schema ao habilitar: `core.migrations().migrate("auth", getClass().getClassLoader())`.
- O Flyway usa um lock do PostgreSQL, então vários servidores iniciando juntos nunca aplicam a mesma migração duas vezes.
- Uma migração já lançada nunca é editada. Mudanças são novas migrações.
- Em produção, as migrações podem ser aplicadas por um único servidor designado (ou por uma etapa de deploy) com um usuário privilegiado, e desabilitadas em todos os outros, como descrito em [Segurança](../security/README.pt-BR.md).

| Schema | Dono | Tabelas |
|---|---|---|
| `core` | `core:common` | `accounts` (V1), `accounts.premium` (V2), `accounts.username` anulável para nomes liberados (V3) |
| `auth` | `servers:auth` | `credentials` |

## Tipos de dados

| Tipo | Exemplos | Estratégia |
|---|---|---|
| Crítico | Credenciais, coins, compras, recompensas | Gravado em uma transação do PostgreSQL **antes** de o jogador ser informado de que aconteceu. |
| Não crítico | Preferências, opções | Aplicado em memória e gravado em segundo plano. Um crash pode perder os últimos segundos. |
| Estado de partida | Times, kills, timers | Só em memória, no servidor que roda a partida. |
| Estado efêmero da rede | Status de servidores, reservas de idempotência, sessões de login, consultas de premium | Redis ou pacotes, sempre com expiração. |

## Regras

- **Nunca bloqueie a thread principal.** Chamadas ao banco e ao Redis rodam fora dela; resultados que mexem no jogo são agendados de volta.
- **Dado crítico só é confirmado depois do commit.**
- **Saldos mudam atomicamente no SQL** (`UPDATE … SET coins = coins - 100 WHERE coins >= 100`), nunca escrevendo um valor lido antes.
- **A unicidade é garantida pelo banco**, não só pelo código. Exemplo: `accounts_username_lower_key` torna impossíveis duas contas cujos nomes diferem só em maiúsculas/minúsculas, mesmo em uma condição de corrida.
- **Dado durável nunca vive só no Redis.** Toda chave do Redis que não é cache tem expiração.
- **As chaves do Redis compartilham um prefixo:** `muxe:<area>:<id>`, como `muxe:idempotency:auth.authenticated:…`.
- **Nada de consultas dentro de laços.** Carregue o necessário em uma consulta.
- **Toda consulta frequente tem um índice.**
- **Os pools ficam pequenos.** Cada conexão do PostgreSQL é um processo; 4 conexões por servidor é o padrão.

## Planejado: dono da sessão

Para permitir salvar dados de jogadores entre servidores com segurança, o plano é que os dados de cada jogador online tenham exatamente um servidor dono por vez, registrado no PostgreSQL com um número de geração que só aumenta, e que toda escrita carregue a geração do dono, para que um servidor travado ou desatualizado nunca sobrescreva dados mais novos. As transferências vão gravar e liberar a sessão antes de o destino assumi-la. Isso ainda não está implementado; será documentado aqui quando estiver.
