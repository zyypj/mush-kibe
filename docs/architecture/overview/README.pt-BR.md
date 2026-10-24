[English](README.md) | **Português**

# Visão geral da arquitetura

Os princípios por trás do código, como ele é dividido em camadas e como uma requisição percorre o sistema.

## Princípios

| Princípio | Como aparece na Muxe |
|---|---|
| **Responsabilidade única** | Classes pequenas com um único motivo para mudar: `EnvelopeCodec` só codifica envelopes, `IdempotencyGuard` só decide duplicatas, `PacketPublisher` só publica. |
| **Aberto/fechado** | Novos pacotes, comandos, consultas e middlewares são adicionados registrando-os, sem editar os buses. |
| **Substituição de Liskov** | Toda porta tem implementações intercambiáveis: `MessageTransport` (Redis ou em memória), `IdempotencyStore` (Redis ou memória), `Transactions` (JDBC ou direta). |
| **Segregação de interfaces** | Interfaces estreitas: `ServerLoad` expõe três números, `PacketContext` expõe o pacote, seus metadados e `reply`. |
| **Inversão de dependência** | O código de aplicação depende de portas (`AccountRepository`, `PacketBus`, `Transactions`), e o bootstrap conecta as implementações concretas. |
| **CQRS** | Mudanças de estado são `Command`s, leituras são `Query`s, cada uma com exatamente um handler e seu próprio bus. |
| **Escalabilidade** | Servidores sem estado coordenados por Redis e PostgreSQL; qualquer tipo de servidor pode rodar em várias cópias. |
| **Manutenibilidade** | Regras de módulos garantidas pelo build, sub-pacotes por papel, nada de mágica escondida, testes em todas as camadas, documentação em dois idiomas. |

## Camadas

```text
┌──────────────────────────────────────────────────────────────┐
│ Platform adapters   listener · command · bootstrap           │  Bukkit / Velocity
├──────────────────────────────────────────────────────────────┤
│ Application         commands · queries · handlers · flows    │
├──────────────────────────────────────────────────────────────┤
│ Domain              records · rules · repository ports       │  Java puro
├──────────────────────────────────────────────────────────────┤
│ Infrastructure      postgres · redis · crypto · transports   │
└──────────────────────────────────────────────────────────────┘
```

- **Domínio** guarda o vocabulário do negócio (`Account`, `Credential`, `Match`, `ArenaInstance`) e as regras que nunca mudam com a plataforma.
- **Aplicação** coordena o domínio por meio de comandos e consultas. Ela define o que acontece dentro de uma transação e qual `Result` é retornado.
- **Infraestrutura** implementa as portas com tecnologia real.
- **Adaptadores de plataforma** traduzem eventos do Bukkit e do Velocity em comandos e consultas, e os resultados de volta em mensagens para o jogador.

As dependências sempre apontam para baixo. Nada no domínio ou na aplicação importa Bukkit ou Velocity, então eles são testados com JUnit puro.

## CQRS

```text
Command ──► CommandBus ──► middlewares ──► CommandHandler ──► Transactions ──► Repository
Query   ──► QueryBus   ──► middlewares ──► QueryHandler   ──► Transactions.readOnly
```

- **Comandos** expressam intenção (`RecordLoginCommand`, `RegisterCredentialCommand`) e retornam um `Result<T>`: sucesso com um valor, ou falha com um código de `Problem`.
- **Consultas** nunca mudam estado (`FindAccountQuery`, `HasCredentialQuery`).
- **Handlers** são registrados uma vez, pela classe da mensagem. Registrar dois handlers para a mesma mensagem falha na inicialização.
- **Os buses são assíncronos.** `dispatch` e `ask` retornam um `CompletableFuture` e executam os handlers no executor de virtual threads do core, então o código de plataforma nunca bloqueia a thread principal.
- **Middlewares** envolvem todo handler. `MetricsMiddleware` registra `muxe_cqrs_handle_seconds` com o nome da mensagem e o resultado (`success`, `failure`, `error`). Novas preocupações transversais (auditoria, tracing) são novos middlewares.
- **Modelos de leitura podem divergir dos modelos de escrita** conforme as features crescem: um handler de consulta pode ler do Redis ou de uma tabela desnormalizada enquanto os comandos escrevem nas tabelas normalizadas.

## Runtime

`CoreRuntime` (em `core:common`) monta tudo o que um servidor precisa, em uma ordem fixa, e fecha tudo na ordem inversa:

1. Registro de métricas e métricas da JVM.
2. Executores: um executor de virtual threads para I/O e handlers, e um agendador.
3. Pool do PostgreSQL, depois as migrações do schema `core`.
4. Conexão com o Redis e o transporte Redis.
5. Registro de pacotes, guarda de idempotência, codec de envelopes e o bus de pacotes.
6. Diretório de servidores, buses de CQRS e o módulo de contas.
7. Heartbeat e o endpoint HTTP de métricas.

Se qualquer passo falhar, tudo o que já foi aberto é fechado e a inicialização falha com `RuntimeStartupException`. O core do Spigot desliga o servidor (configurável), e o proxy para: um nó que não alcança o PostgreSQL ou o Redis não deve aceitar jogadores.

`core:bukkit` e `core:proxy` só adicionam o que é específico da plataforma: leitura do arquivo de configuração, a guarda da thread principal, o `ServerLoad`, comandos e listeners.

## Acompanhando uma requisição: login

```text
Jogador digita /login senha  (servidor de auth, thread principal)
  └─ AuthCommands ─► AuthFlow.login
       └─ CommandBus.dispatch(VerifyCredentialCommand)          virtual thread
            └─ VerifyCredentialHandler
                 ├─ Transactions.readOnly ─► PostgresCredentialRepository.find
                 ├─ Pbkdf2PasswordHasher.matches
                 └─ Transactions.inTransaction ─► atualiza o último login
       └─ de volta na thread principal: AuthFlow.complete
            └─ PacketBus.publish(proxy, PlayerAuthenticatedPacket)  envelope assinado via Redis
                 └─ proxy: ProxyPacketHandlers.onAuthenticated
                      ├─ AccessPolicy.isTrustedAuthOrigin(origin)
                      ├─ AuthenticationGate.authenticate(player)
                      └─ ServerRouter.connect(player, grupo lobby)
```

## Estendendo a plataforma

| Para adicionar | Faça isto |
|---|---|
| Um pacote | Um record que implementa `Packet` com um `static final PacketType<…> TYPE` (id, codec, política de entrega). Assine com `packets().subscribe(TYPE, handler)`. Documente-o na feature. |
| Um comando ou consulta | Um record que implementa `Command<R>`/`Query<R>`, uma classe de handler e um registro no `*Module` da feature. |
| Uma tabela | Uma migração Flyway em `src/main/resources/db/migration/<module>/` e um repositório em `infrastructure/postgres`. Rode `migrations().migrate("<module>", classLoader)` ao habilitar. |
| Um utilitário reutilizável | Adicione-o ao `core:bukkit` ou ao `core:common` e documente-o na [API do core](../../development/core-api/README.pt-BR.md). |
| Uma métrica | Peça a `metrics()` um counter, timer ou gauge com nome `muxe_`, e documente-a em [Observabilidade](../observability/README.pt-BR.md). |
| Um plugin de servidor | Um módulo em `servers/` ou `minigames/`, um `*Bootstrap` e uma linha no `build.gradle.kts` da raiz. Veja [Desenvolvimento](../../development/README.pt-BR.md). |
