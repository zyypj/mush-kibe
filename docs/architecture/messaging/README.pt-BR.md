[English](README.md) | **Português**

# Mensageria

Como os servidores conversam entre si. Toda mensagem é um **pacote**: um record Java tipado, codificado com um codec binário explícito, embrulhado em um envelope assinado e publicado via Redis Pub/Sub. Cada etapa é medida.

Código: `core/common/src/main/java/br/com/muxe/core/common/messaging`.

## Por que Redis Pub/Sub

| | Redis Pub/Sub | Plugin Messaging |
|---|---|---|
| Precisa de jogador online | Não | Sim. Um servidor sem jogadores não envia nem recebe. |
| Servidor para servidor | Direto | Só pelo proxy. |
| Broadcast | Uma publicação chega a todos os assinantes | Uma mensagem por servidor. |
| Infraestrutura extra | Redis | Nenhuma |

A confiabilidade decide: o Plugin Messaging para de funcionar justamente quando um servidor está vazio, o que é comum em servidores de jogo. Veja a [ADR 0001](../../decisions/0001-redis-pubsub-for-packets.pt-BR.md).

## Blocos de construção

| Pacote | Responsabilidade |
|---|---|
| `packet` | `Packet`, `PacketType`, `PacketRegistry`, `DeliveryPolicy`, `IdempotencyLevel`, `IdempotentPacket`. |
| `codec` | `PacketWriter`, `PacketReader` e `PacketCodec`: o formato binário dos payloads. |
| `envelope` | `PacketEnvelope`, `EnvelopeCodec`, `EnvelopeKind`, `RejectionReason`. |
| `security` | `EnvelopeSigner` e `HmacEnvelopeSigner`. |
| `channel` | `PacketTarget` (broadcast, proxy, group, server) e `ChannelNames`. |
| `transport` | `MessageTransport` com as implementações Redis e em memória. |
| `idempotency` | `IdempotencyGuard` e seus stores (memória e Redis). |
| `request` | `PendingRequests` e `RequestTimeoutException`. |
| `metrics` | `PacketMetrics`. |
| `bus` | `PacketBus`, `DefaultPacketBus`, `PacketHandler`, `PacketContext`, `PacketMetadata`. |

O código das features só usa `PacketBus`, `PacketType`, `PacketTarget` e seus próprios records de pacote. O Redis nunca é acessado diretamente.

## Definindo um pacote

```java
public record ConnectPlayerPacket(UUID playerId, DestinationType type, String destination) implements Packet {

    public static final PacketType<ConnectPlayerPacket> TYPE = PacketType.of(
        "player.connect",
        ConnectPlayerPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeUuid(packet.playerId()).writeEnum(packet.type()).writeString(packet.destination()),
            in -> new ConnectPlayerPacket(in.readUuid(), in.readEnum(DestinationType.class), in.readString())
        )
    );
}
```

- **Id:** `area.name`, em minúsculas. Nunca muda depois de lançado.
- **Versão:** começa em 1. Uma mudança de layout é uma nova versão, registrada lado a lado com a antiga durante uma atualização gradual (o registro é indexado por id e versão).
- **Codec:** explícito e simétrico. O leitor precisa consumir todos os bytes, senão o pacote é rejeitado.
- **Política:** veja [Política de entrega](#política-de-entrega).
- Pacotes carregam ids e valores pequenos, nunca objetos inteiros. Se o receptor precisar de mais, ele lê do banco de dados.

Primitivas de codificação: byte, boolean, short, int, long, VarInt, string UTF-8 (até 32.767 bytes), string anulável, UUID, UUID anulável, array de bytes, enum (pelo nome), lista (até 65.536 itens).

## Enviando e recebendo

```java
core.packets().publish(PacketTarget.proxy(), ConnectPlayerPacket.toGroup(player.getUniqueId(), "skywars"));

core.packets().subscribe(ConnectPlayerPacket.TYPE, context -> {
    ConnectPlayerPacket packet = context.packet();
    String origin = context.metadata().origin();
});
```

- `subscribe` também registra o tipo, então um servidor pode receber um pacote assim que o assina.
- Handlers rodam em uma **virtual thread**, nunca na thread principal. Tudo o que mexe com o Bukkit passa por `MuxeCore.mainThread()`.
- Um handler que lança exceção é registrado em log e contado; os outros handlers do mesmo pacote continuam rodando.

## Requisições e respostas

```java
core.packets()
    .request(PacketTarget.proxy(), new PlayerLocateRequest("Steve"), PlayerLocateResponse.class, Duration.ofSeconds(3))
    .whenComplete((response, error) -> core.mainThread().execute(() -> ...));

bus.subscribe(PlayerLocateRequest.TYPE, context -> context.reply(new PlayerLocateResponse(true, "Steve", "lobby-1")));
```

- O envelope da requisição carrega um **id de correlação** aleatório. A resposta carrega o mesmo id e é publicada no canal próprio de quem pediu (`server:<origin>`).
- O future falha com `RequestTimeoutException` quando nenhuma resposta chega a tempo. Cada chamador decide o que um timeout significa para o jogador.
- Uma resposta que chega depois do timeout é descartada e contada como `late_response`.

## Alvos e canais

Cada servidor só assina os canais que lhe dizem respeito.

| Alvo | Canal | Quem recebe |
|---|---|---|
| `PacketTarget.broadcast()` | `muxe:packets:broadcast` | Todo servidor e proxy. |
| `PacketTarget.proxy()` | `muxe:packets:proxy` | Todo proxy. |
| `PacketTarget.group("skywars")` | `muxe:packets:group:skywars` | Todo servidor do grupo. |
| `PacketTarget.server("lobby-2")` | `muxe:packets:server:lobby-2` | Um servidor (também usado para respostas). |

Um servidor ignora pacotes cuja origem é ele mesmo, então broadcasts nunca voltam para quem enviou.

## Envelope

Todo pacote viaja dentro de um envelope binário:

| Campo | Tipo | Finalidade |
|---|---|---|
| Magic | 2 bytes `0x4D58` | Identifica um envelope da Muxe. |
| Versão do protocolo | 1 byte | Versão do formato do envelope (atualmente 1). Versões desconhecidas são rejeitadas. |
| Tipo do envelope | 1 byte | `MESSAGE`, `REQUEST` ou `RESPONSE`. |
| Id do pacote | UUID | Único por publicação. A chave de idempotência padrão. |
| Id e versão do tipo | string, VarInt | Qual `PacketType` decodifica o payload. |
| Origem | string | O `server.id` de quem enviou. |
| Id de correlação | UUID anulável | Liga uma resposta à sua requisição. |
| Enviado em | long (epoch ms) | Usado para expiração e latência. |
| Payload | bytes | O pacote codificado pelo seu codec. |
| Assinatura | 32 bytes | HMAC-SHA256 de tudo o que vem acima. |

- O tamanho máximo do envelope é 256 KiB por padrão (`messaging.max-envelope-bytes`). Um pacote maior falha no envio e nunca é publicado.
- A assinatura é verificada antes de qualquer parsing, em tempo constante.

## Assinaturas

Envelopes são assinados com HMAC-SHA256 usando `messaging.secret`, compartilhado por todos os servidores e proxies, com pelo menos 32 bytes.

- Um envelope com assinatura errada é rejeitado (`bad_signature`). Um processo que consegue publicar no Redis mas não conhece o segredo não consegue injetar pacotes.
- **Rotação sem downtime:** coloque o novo segredo em `messaging.secret` e mantenha o antigo em `messaging.previous-secrets` em todos os servidores; envelopes assinados com qualquer um são aceitos, e os novos são assinados com o novo segredo. Quando todos os servidores estiverem com o novo segredo, remova o antigo.
- As assinaturas autenticam a rede, não um servidor específico: `origin` não é prova de identidade. Handlers que confiam em um remetente também conferem o papel dele no diretório de servidores, como o proxy faz para `auth.authenticated`.

## Política de entrega

Cada `PacketType` tem uma `DeliveryPolicy`:

| Campo | Significado |
|---|---|
| `idempotency` | Como duplicatas são detectadas, veja abaixo. |
| `deduplicationWindow` | Por quanto tempo a chave de um pacote é lembrada. |
| `maxAge` | Pacotes mais velhos do que isso quando chegam são descartados como `expired`. |

Fábricas: `DeliveryPolicy.standard()` (local, 2 minutos, 30 segundos), `volatileState(maxAge)`, `durable(window)`, `exclusive(window)` e `withMaxAge(...)`.

### Níveis de idempotência

A chave é `IdempotentPacket.idempotencyKey()` quando o pacote a implementa (por exemplo, o id de uma compra); caso contrário, o id do pacote no envelope. Ela sempre tem o tipo como escopo.

| Nível | Onde a chave é lembrada | Garantia | Use para |
|---|---|---|---|
| `NONE` | Em lugar nenhum | Uma repetição é tratada de novo. | Estado de alta frequência em que vale o último valor, como `server.status`. |
| `LOCAL` | Na memória do receptor (limitada) | Um tratamento por receptor enquanto ele roda. | A maioria dos pacotes. |
| `DURABLE` | Memória, depois Redis por receptor | Um tratamento por receptor, mesmo entre reinícios. Se o Redis cair, volta a usar só a memória. | Pacotes com efeitos no receptor, como `auth.authenticated`. |
| `EXCLUSIVE` | Memória, depois Redis compartilhado pela rede | Um tratamento **na rede inteira**, pelo primeiro receptor que reservar. Se o Redis cair, o pacote é descartado (falha fechada). | Trabalho que um servidor de um grupo precisa fazer uma única vez, como entregar uma compra. |

A idempotência só é verificada quando o receptor tem um handler para o tipo, então um pacote exclusivo nunca é reservado por um servidor que não consegue tratá-lo.

### O que a entrega garante

O Redis Pub/Sub entrega **no máximo uma vez**: um servidor que está offline ou reconectando quando um pacote é publicado nunca o recebe. Por isso:

- Pacotes que podem se perder sem dano são simplesmente publicados (status, chat, pedidos de roteamento).
- Efeitos que não podem se perder são gravados primeiro no PostgreSQL; o pacote só avisa os servidores para agirem agora. Um pacote perdido atrasa o efeito, e o receptor reconcilia a partir do banco (ao entrar ou periodicamente).
- Um handler que falha depois da sua reserva de idempotência não é repetido pelo bus; trabalho durável precisa ser recuperável a partir do banco.

## Pipeline de processamento

```text
Redis ─► thread de assinatura do transporte ─► virtual thread:
  1. decodifica e verifica o envelope       → too_large, malformed, bad_magic, unsupported_protocol, bad_signature
  2. ignora pacotes deste servidor
  3. encontra o tipo pelo id e versão       → unknown_type
  4. conta bytes recebidos e latência
  5. descarta se mais velho que maxAge      → expired
  6. respostas completam a requisição       → late_response
  7. para se ninguém trata este tipo        → muxe_packets_unhandled_total
  8. decodifica o payload                   → malformed_payload
  9. reserva a idempotência                 → duplicates, idempotency_unavailable
 10. roda cada handler, medindo cada um     → muxe_packets_handler_failures_total
```

## Perda de conexão

O transporte Redis mantém uma conexão dedicada para a assinatura:

- Se ela cair, reconecta com backoff exponencial de 250 ms até 10 s, e conta `muxe_transport_reconnects_total`.
- A cada 10 s envia um ping ao Redis pela assinatura; um ping que falha força a reconexão, então uma conexão TCP morta silenciosamente é detectada.
- Depois de cada (re)assinatura o servidor republica seu status imediatamente, então o diretório se recupera rápido.
- Enquanto desconectado, as publicações falham rápido e o jogo continua rodando.

## Catálogo de pacotes

Pacotes definidos em `core:common` (`br.com.muxe.core.common.network`). Pacotes de features são documentados na sua feature.

| Id | Record | Alvo | Política | Finalidade |
|---|---|---|---|---|
| `server.status` | `ServerStatusPacket` | broadcast | `NONE`, idade máxima 10 s | Heartbeat para o diretório de servidores. |
| `server.shutdown` | `ServerShutdownPacket` | broadcast | standard | Um servidor está parando de forma limpa. |
| `player.connect` | `ConnectPlayerPacket` | proxy | standard | Envia um jogador a um servidor ou ao servidor menos cheio de um grupo. |
| `player.locate_request` | `PlayerLocateRequest` | proxy | standard, idade máxima 5 s | Pergunta onde um jogador está. |
| `player.locate_response` | `PlayerLocateResponse` | quem pediu | standard, idade máxima 5 s | Resposta à anterior. |
| `auth.authenticated` | `PlayerAuthenticatedPacket` | proxy | `DURABLE` 5 min, idade máxima 15 s | Um jogador se registrou ou fez login em um servidor de auth. |

## Métricas

Todo tipo de pacote é medido: publicados, recebidos, bytes, tempo de publicação, latência ponta a ponta, tempo de tratamento, duplicatas, não tratados, falhas de handler, rejeições por motivo e requisições por resultado. A lista completa está em [Observabilidade](../observability/README.pt-BR.md#mensageria).

## Testes

- `TestNetwork` (test fixtures) monta vários buses sobre um broker em memória com executor direto e um `MutableClock`, então cenários com vários servidores são testes unitários determinísticos.
- `RecordingTransport` captura os bytes publicados para que os testes possam reenviá-los (duplicatas, expiração, adulteração).
- `RedisMessagingIntegrationTest` roda o transporte Redis real contra um container do Redis.
