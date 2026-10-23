[English](0006-cqrs-with-in-process-buses.md) | **Português**

# 0006. CQRS com barramentos assíncronos dentro do processo e valores `Result`

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

O código de plugins tende a misturar tratamento de eventos, regras de negócio e acesso ao banco na mesma classe, o que dificulta os testes e facilita bloquear a thread principal. Escritas e leituras também têm necessidades diferentes: escritas precisam de transações e validação; leituras precisam de velocidade e podem vir de caches.

## Decisão

- Mudanças de estado são `Command`s e leituras são `Query`s, cada um com exatamente um handler, registrado em um `CommandBus` ou `QueryBus`.
- Os barramentos executam os handlers de forma assíncrona em virtual threads e retornam `CompletableFuture`s.
- Comportamentos transversais (métricas hoje, auditoria ou tracing depois) são adicionados como middleware.
- Falhas de negócio esperadas são retornadas como `Result.failure(Problem)` com códigos estáveis; exceções são para bugs e infraestrutura.

## Alternativas consideradas

- **Serviços chamados diretamente pelos listeners:** menos cerimônia, mas chamadas bloqueantes vazam para a thread principal e as preocupações transversais se repetem.
- **Event sourcing:** trilha de auditoria forte, mas muito mais complexidade do que a rede precisa hoje.

## Consequências

- A lógica de negócio é testada com JUnit puro e implementações falsas em memória.
- O código de plataforma sempre lida com futures e precisa voltar à thread principal para mexer no Bukkit.
- Todo comando e toda query são medidos sem esforço extra.
