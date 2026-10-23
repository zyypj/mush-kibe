[English](0007-jedis-as-redis-client.md) | **Português**

# 0007. Jedis como cliente do Redis

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

O cliente do Redis é embutido em cada jar de plugin. Os servidores Spigot 1.8 trazem o seu próprio Netty, e o cliente precisa suportar Pub/Sub binário, `SET NX PX` e TLS.

## Decisão

Usar o Jedis 8: `RedisClient` (com pool) para os comandos e uma conexão `Jedis` dedicada para a assinatura, para que o transporte possa forçar uma reconexão quando um ping de verificação de saúde falhar.

## Alternativas consideradas

- **Lettuce:** totalmente assíncrono, mas traz o Netty e o Project Reactor, que precisam ser realocados para não conflitar com o Netty do servidor e deixam cada jar vários megabytes maior.
- **Redisson:** muitos recursos de alto nível que a Muxe não precisa, e um tamanho ainda maior.

## Consequências

- Chamadas bloqueantes não são problema, porque rodam em virtual threads.
- O cliente é usado apenas dentro do `core:common` (`RedisConnector`, o transporte e o armazenamento de idempotência do Redis), então pode ser trocado sem mexer nas features.
