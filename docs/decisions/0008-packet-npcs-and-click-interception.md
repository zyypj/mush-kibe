**English** | [Português](0008-packet-npcs-and-click-interception.pt-BR.md)

# 0008. NPCs, holograms and boss bars live only in packets

- **Status:** Accepted
- **Date:** 2026-10-24

## Context

The lobby needs NPCs with player skins that send players to games, text above them, and a boss bar. Spigot 1.8.8 has no API for any of them: no player NPCs, no per-player holograms, no boss bar. Each player must also see only their own view (their challenge code, their boss bar text), and clicking an NPC must reach the plugin.

## Decision

- NPCs, holograms and the boss bar are entities that exist only in packets sent to each viewer: a fake player (`EntityPlayer`) with the skin in its profile, invisible armor stands with a custom name, and an invisible wither kept in front of the player. They are built by reflection on the server internals (`PacketNpcTransport`, `PacketHologramTransport`, `PacketBossBarTransport`) and never added to the world.
- Clicks on NPCs are read from the network: `PacketInterceptor` adds a Netty handler before Minecraft's `packet_handler` on every connection and reports `PacketPlayInUseEntity`, then the NPC service runs the click on the main thread. Netty is compiled against (`compileOnly`) because the server already ships it.

## Alternatives considered

- **Real NPC entities added to the world** (as Citizens does): they would be ticked, saved with the chunk, seen by everyone and need a fake network connection. Too heavy for decoration.
- **A real invisible entity on top of the fake NPC to catch Bukkit click events:** the client picks the closest hit box, which is usually the fake NPC, so clicks would be lost.
- **An NPC or hologram plugin:** another dependency, another configuration format and no per-player view.

## Consequences

- Nothing is stored in the world: restarting the server or reloading a chunk leaves no stray entity behind.
- The code depends on 1.8 internals (`v1_8_R3` names). Every transport resolves its reflection once and reports `isAvailable()`; when something is missing, it logs one warning and the feature is skipped instead of breaking the plugin.
- Viewers must be tracked: NPCs are shown within 48 blocks and hidden beyond 64, holograms are sent again periodically, the boss bar follows the player.
- Moving to another server version means updating these three transports and the interceptor, not the features.
