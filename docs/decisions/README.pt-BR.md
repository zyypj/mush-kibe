[English](README.md) | **Português**

# Registros de decisões de arquitetura

Registros curtos das decisões que moldam a arquitetura: o contexto, a decisão e suas consequências. Eles explicam o **porquê**, para que quem chegar depois não precise adivinhar nem repetir discussões antigas.

| ADR | Decisão | Status |
|---|---|---|
| [0001](0001-redis-pubsub-for-packets.pt-BR.md) | O Redis Pub/Sub transporta os pacotes entre servidores | Aceita |
| [0002](0002-java-25-on-a-1-8-fork.pt-BR.md) | Java 25 em tudo, API do Spigot 1.8.8 sobre um fork moderno, Velocity 4 | Aceita |
| [0003](0003-postgresql-with-schemas-per-module.pt-BR.md) | PostgreSQL com migrações do Flyway em um schema por módulo | Aceita |
| [0004](0004-own-metrics-registry.pt-BR.md) | Um registro de métricas próprio e pequeno, com exportação no formato texto do Prometheus | Aceita |
| [0005](0005-signed-binary-envelopes.pt-BR.md) | Envelopes binários com codecs explícitos, assinados com HMAC-SHA256 | Aceita |
| [0006](0006-cqrs-with-in-process-buses.pt-BR.md) | CQRS com barramentos assíncronos dentro do processo e valores `Result` | Aceita |
| [0007](0007-jedis-as-redis-client.pt-BR.md) | Jedis como cliente do Redis | Aceita |

## Escrevendo uma nova ADR

1. Copie [`_template.md`](_template.md) e [`_template.pt-BR.md`](_template.pt-BR.md) para `NNNN-titulo-curto.md` e `NNNN-titulo-curto.pt-BR.md`, com o próximo número.
2. Preencha os dois idiomas e adicione a ADR à tabela acima, nos dois arquivos de índice.
3. Uma ADR aceita não é editada para mudar sua decisão. Uma nova ADR a substitui, e o status da antiga passa a ser "Substituída pela NNNN".
