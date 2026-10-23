**English** | [Português](README.pt-BR.md)

# Messaging

How servers talk to each other. Every message is a **packet**: a typed Java record, encoded with an explicit binary codec, wrapped in a signed envelope and published over Redis Pub/Sub. Every step is measured.

Code: `core/common/src/main/java/br/com/muxe/core/common/messaging`.

## Why Redis Pub/Sub

| | Redis Pub/Sub | Plugin Messaging |
|---|---|---|
| Needs a player online | No | Yes. A server with no players can neither send nor receive. |
| Server to server | Direct | Only through the proxy. |
| Broadcast | One publish reaches every subscriber | One message per server. |
| Extra infrastructure | Redis | None |

Reliability decides: Plugin Messaging stops working exactly when a server is empty, which is common for game servers. See [ADR 0001](../../decisions/0001-redis-pubsub-for-packets.md).

## Building blocks

| Package | Responsibility |
|---|---|
| `packet` | `Packet`, `PacketType`, `PacketRegistry`, `DeliveryPolicy`, `IdempotencyLevel`, `IdempotentPacket`. |
| `codec` | `PacketWriter`, `PacketReader` and `PacketCodec`: the binary format of payloads. |
| `envelope` | `PacketEnvelope`, `EnvelopeCodec`, `EnvelopeKind`, `RejectionReason`. |
| `security` | `EnvelopeSigner` and `HmacEnvelopeSigner`. |
| `channel` | `PacketTarget` (broadcast, proxy, group, server) and `ChannelNames`. |
| `transport` | `MessageTransport` with the Redis and in-memory implementations. |
| `idempotency` | `IdempotencyGuard` and its stores (memory and Redis). |
| `request` | `PendingRequests` and `RequestTimeoutException`. |
| `metrics` | `PacketMetrics`. |
| `bus` | `PacketBus`, `DefaultPacketBus`, `PacketHandler`, `PacketContext`, `PacketMetadata`. |

Feature code only uses `PacketBus`, `PacketType`, `PacketTarget` and its own packet records. Redis is never reached directly.

## Defining a packet

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

- **Id:** `area.name`, lowercase. It never changes once released.
- **Version:** starts at 1. A layout change is a new version, registered side by side with the old one during a rolling update (the registry is keyed by id and version).
- **Codec:** explicit and symmetric. The reader must consume every byte, or the packet is rejected.
- **Policy:** see [Delivery policy](#delivery-policy).
- Packets carry ids and small values, never whole objects. If the receiver needs more, it reads from the database.

Encoding primitives: byte, boolean, short, int, long, VarInt, UTF-8 string (up to 32,767 bytes), nullable string, UUID, nullable UUID, byte array, enum (by name), list (up to 65,536 items).

## Sending and receiving

```java
core.packets().publish(PacketTarget.proxy(), ConnectPlayerPacket.toGroup(player.getUniqueId(), "skywars"));

core.packets().subscribe(ConnectPlayerPacket.TYPE, context -> {
    ConnectPlayerPacket packet = context.packet();
    String origin = context.metadata().origin();
});
```

- `subscribe` also registers the type, so a server can receive a packet as soon as it subscribes to it.
- Handlers run on a **virtual thread**, never on the main thread. Anything that touches Bukkit goes through `MuxeCore.mainThread()`.
- A handler that throws is logged and counted; the other handlers of the same packet still run.

## Requests and responses

```java
core.packets()
    .request(PacketTarget.proxy(), new PlayerLocateRequest("Steve"), PlayerLocateResponse.class, Duration.ofSeconds(3))
    .whenComplete((response, error) -> core.mainThread().execute(() -> ...));

bus.subscribe(PlayerLocateRequest.TYPE, context -> context.reply(new PlayerLocateResponse(true, "Steve", "lobby-1")));
```

- The request envelope carries a random **correlation id**. The response carries the same id and is published to the requester's own channel (`server:<origin>`).
- The future fails with `RequestTimeoutException` when no response arrives in time. Every caller decides what a timeout means for the player.
- A response that arrives after the timeout is dropped and counted as `late_response`.

## Targets and channels

Each server only subscribes to the channels that concern it.

| Target | Channel | Who receives |
|---|---|---|
| `PacketTarget.broadcast()` | `muxe:packets:broadcast` | Every server and proxy. |
| `PacketTarget.proxy()` | `muxe:packets:proxy` | Every proxy. |
| `PacketTarget.group("skywars")` | `muxe:packets:group:skywars` | Every server of the group. |
| `PacketTarget.server("lobby-2")` | `muxe:packets:server:lobby-2` | One server (also used for responses). |

A server ignores packets whose origin is itself, so broadcasts never loop back.

## Envelope

Every packet travels inside a binary envelope:

| Field | Type | Purpose |
|---|---|---|
| Magic | 2 bytes `0x4D58` | Identifies a Muxe envelope. |
| Protocol version | 1 byte | Envelope format version (currently 1). Unknown versions are rejected. |
| Kind | 1 byte | `MESSAGE`, `REQUEST` or `RESPONSE`. |
| Packet id | UUID | Unique per publish. The default idempotency key. |
| Type id and version | string, VarInt | Which `PacketType` decodes the payload. |
| Origin | string | The `server.id` of the sender. |
| Correlation id | nullable UUID | Links a response to its request. |
| Sent at | long (epoch ms) | Used for expiration and latency. |
| Payload | bytes | The packet encoded by its codec. |
| Signature | 32 bytes | HMAC-SHA256 of everything above. |

- The maximum envelope size is 256 KiB by default (`messaging.max-envelope-bytes`). A larger packet fails when it is sent and is never published.
- The signature is checked before anything is parsed, in constant time.

## Signatures

Envelopes are signed with HMAC-SHA256 using `messaging.secret`, shared by every server and proxy, with at least 32 bytes.

- An envelope with a wrong signature is rejected (`bad_signature`). A process that can publish to Redis but does not know the secret cannot inject packets.
- **Rotation without downtime:** add the new secret as `messaging.secret` and keep the old one in `messaging.previous-secrets` on every server; envelopes signed with either are accepted, new ones are signed with the new secret. Once every server runs the new secret, remove the old one.
- Signatures authenticate the network, not a specific server: `origin` is not proof of identity. Handlers that trust a sender also check its role in the server directory, as the proxy does for `auth.authenticated`.

## Delivery policy

Each `PacketType` has a `DeliveryPolicy`:

| Field | Meaning |
|---|---|
| `idempotency` | How duplicates are detected, see below. |
| `deduplicationWindow` | How long a packet key is remembered. |
| `maxAge` | Packets older than this when they arrive are dropped as `expired`. |

Factories: `DeliveryPolicy.standard()` (local, 2 minutes, 30 seconds), `volatileState(maxAge)`, `durable(window)`, `exclusive(window)`, and `withMaxAge(...)`.

### Idempotency levels

The key is `IdempotentPacket.idempotencyKey()` when the packet implements it (for example a purchase id), otherwise the envelope's packet id. It is always scoped by type.

| Level | Where the key is remembered | Guarantee | Use for |
|---|---|---|---|
| `NONE` | Nowhere | A replay is handled again. | High-rate state where the last value wins, such as `server.status`. |
| `LOCAL` | The receiver's memory (bounded) | One handling per receiver while it runs. | Most packets. |
| `DURABLE` | Memory, then Redis per receiver | One handling per receiver, even across restarts. If Redis is down it falls back to memory. | Packets with effects on the receiver, such as `auth.authenticated`. |
| `EXCLUSIVE` | Memory, then Redis shared by the network | One handling **in the whole network**, by the first receiver that claims it. If Redis is down the packet is dropped (fail closed). | Work that one server of a group must do once, such as delivering a purchase. |

Idempotency is checked only when the receiver has a handler for the type, so an exclusive packet is never claimed by a server that cannot handle it.

### What delivery guarantees

Redis Pub/Sub delivers **at most once**: a server that is offline or reconnecting when a packet is published never receives it. Therefore:

- Packets that can be lost without harm are just published (status, chat, routing requests).
- Effects that must not be lost are written to PostgreSQL first; the packet only tells servers to act now. A lost packet delays the effect, and the receiver reconciles from the database (on join or periodically).
- A handler that fails after its idempotency claim is not retried by the bus; durable work must be recoverable from the database.

## Processing pipeline

```text
Redis ─► transport subscriber thread ─► virtual thread:
  1. decode and verify the envelope        → too_large, malformed, bad_magic, unsupported_protocol, bad_signature
  2. ignore packets from this server
  3. find the type by id and version       → unknown_type
  4. count received bytes and latency
  5. drop if older than maxAge             → expired
  6. responses complete the pending request→ late_response
  7. stop if nobody handles this type      → muxe_packets_unhandled_total
  8. decode the payload                    → malformed_payload
  9. claim idempotency                     → duplicates, idempotency_unavailable
 10. run every handler, timing each one    → muxe_packets_handler_failures_total
```

## Connection loss

The Redis transport keeps one dedicated subscription connection:

- If it drops, it reconnects with exponential backoff from 250 ms up to 10 s, and counts `muxe_transport_reconnects_total`.
- Every 10 s it pings Redis through the subscription; a failed ping forces a reconnect, so a silently dead TCP connection is detected.
- After every (re)subscription the server immediately republishes its status, so the directory recovers quickly.
- While disconnected, publishes fail fast and the game keeps running.

## Packet catalog

Packets defined in `core:common` (`br.com.muxe.core.common.network`). Feature packets are documented in their feature.

| Id | Record | Target | Policy | Purpose |
|---|---|---|---|---|
| `server.status` | `ServerStatusPacket` | broadcast | `NONE`, max age 10 s | Heartbeat for the server directory. |
| `server.shutdown` | `ServerShutdownPacket` | broadcast | standard | A server is stopping cleanly. |
| `player.connect` | `ConnectPlayerPacket` | proxy | standard | Send a player to a server or to the least loaded server of a group. |
| `player.locate_request` | `PlayerLocateRequest` | proxy | standard, max age 5 s | Ask where a player is. |
| `player.locate_response` | `PlayerLocateResponse` | requester | standard, max age 5 s | Answer to the above. |
| `auth.authenticated` | `PlayerAuthenticatedPacket` | proxy | `DURABLE` 5 min, max age 15 s | A player registered or logged in on an auth server. |

## Metrics

Every packet type is measured: published, received, bytes, publish time, end-to-end latency, handling time, duplicates, unhandled, handler failures, rejections by reason, and requests by outcome. The full list is in [Observability](../observability/README.md#messaging).

## Testing

- `TestNetwork` (test fixtures) builds several buses on an in-memory broker with a direct executor and a `MutableClock`, so multi-server scenarios are deterministic unit tests.
- `RecordingTransport` captures published bytes so tests can replay them (duplicates, expiration, tampering).
- `RedisMessagingIntegrationTest` runs the real Redis transport against a Redis container.
