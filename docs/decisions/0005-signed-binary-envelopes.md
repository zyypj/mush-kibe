**English** | [Português](0005-signed-binary-envelopes.pt-BR.md)

# 0005. Binary envelopes with explicit codecs, signed with HMAC-SHA256

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

Packets need a stable format that supports rolling updates, a fixed set of metadata (ids, origin, timestamps, correlation) and protection against forged or tampered messages from anyone who reaches Redis.

## Decision

- Every packet is wrapped in a binary envelope with a magic number, a protocol version, metadata and the payload.
- Payloads are encoded by an explicit `PacketCodec` per packet type, with a versioned type id.
- The whole envelope is signed with HMAC-SHA256 using a shared secret, with support for previous secrets during rotation.

## Alternatives considered

- **JSON with Gson:** readable, but Spigot 1.8 ships an old Gson, reflection-based mapping hides layout changes, and messages are larger.
- **Java serialization:** unsafe to deserialize from the network.
- **Protocol Buffers:** schema evolution for free, but a code generator and another runtime library to shade.
- **No signature, relying only on Redis ACLs:** a single leaked Redis password would allow injecting any packet.

## Consequences

- Codecs are a few lines per packet and must be kept symmetric; tests cover round trips.
- Layout changes are new type versions, registered side by side during updates.
- Every server must share the messaging secret; a wrong secret shows up as `bad_signature` rejections.
