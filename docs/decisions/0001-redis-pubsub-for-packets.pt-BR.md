[English](0001-redis-pubsub-for-packets.md) | **Português**

# 0001. O Redis Pub/Sub transporta os pacotes entre servidores

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

Os servidores precisam trocar mensagens: heartbeats, pedidos de roteamento, resultados de autenticação, buscas de jogadores e, mais adiante, eventos de jogo e da loja. As mensagens precisam circular mesmo quando um servidor não tem jogadores, e broadcasts para muitos servidores precisam ser baratos.

## Decisão

Os pacotes trafegam pelo Redis Pub/Sub, por trás da interface `MessageTransport`. Cada servidor assina apenas os seus próprios canais (broadcast, o seu grupo, o seu id e, nos proxies, o canal do proxy).

## Alternativas consideradas

- **Plugin Messaging:** precisa de um jogador conectado nas duas pontas e passa pelo proxy. Para de funcionar justamente quando um servidor está vazio.
- **Um message broker (RabbitMQ, NATS, Kafka):** entrega durável e grupos de consumidores, mas é mais uma peça de infraestrutura para operar e proteger. O Redis já é necessário para o estado compartilhado.
- **Redis Streams:** durável e reprocessável, mas mais complexo de consumir corretamente. Continua sendo uma opção por trás do `MessageTransport` se a entrega pelo menos uma vez se tornar necessária.

## Consequências

- A entrega é no máximo uma vez. Efeitos que não podem ser perdidos são gravados primeiro no PostgreSQL, e os pacotes apenas notificam. Veja [Mensageria](../architecture/messaging/README.pt-BR.md#o-que-a-entrega-garante).
- Idempotência, expiração e assinaturas são implementadas pela Muxe em cima do transporte.
- O acesso ao Redis precisa ser privado e autenticado.
- Os testes usam o transporte em memória para a lógica e um contêiner do Redis para o transporte real.
