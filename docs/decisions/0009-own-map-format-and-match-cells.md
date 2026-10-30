**English** | [Português](0009-own-map-format-and-match-cells.pt-BR.md)

# 0009. Maps use our own format and matches share one world in cells

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

A game server runs many matches at once, often on the same map: a popular map may have dozens of simultaneous matches across the network. Every match needs a fresh copy of the map, and the copy must disappear when the match ends.

On 1.8, creating a world per match is slow on the main thread. Our older code measured about 67 ms per world with SlimeWorldManager, and each extra world adds its own ticking, chunk map and entity tracker. Copying region folders on disk costs even more and leaves files behind after a crash.

Maps are prepared by staff during the arena setup and copied to every game server as files.

## Decision

- **Maps are stored in our own format, `.mxw`.** Each chunk is kept compressed (Deflate) with its blocks, light, biomes, height map and tile entities. Entities are not stored. A whole map stays in memory compressed, and a chunk is only decompressed when the server loads it.
  - Format: `MUXEMAP`, version byte, chunk count, then `x`, `z`, length and compressed bytes for each chunk (`MapFiles`).
  - A world folder (Anvil `.mca` regions) is imported with `AnvilImporter`, without a running server.
- **Worlds get their chunks from memory, never from disk.** `MuxeWorlds` creates a void world and, during `WorldInitEvent`, replaces its chunk loader with `SourceChunkLoader`. That loader builds server chunks straight from `ChunkData` and gives saved chunks back to a `ChunkSource`. The world never writes region files, and its folder is deleted when it is destroyed.
- **All matches of a server share one world, split into cells** (`MatchCells`).
  - A cell is a square of `sizeChunks` (64 by default, 1024 blocks) in a grid.
  - Each match gets a free cell, and the map is served around the cell centre.
  - Chunks a match changes are kept compressed per cell when the server unloads them, and are served again while the match lasts.
  - When the match ends, the cell's chunks are unloaded without saving and the cell is released, so the next match sees the original map.
- **The arena setup edits a separate world** backed by an `EditableMap`. Saving the world writes every chunk back into it, and the setup writes the `.mxw` file from it.
- The server classes are used directly (`muxe.server-internals`), not through reflection, because this code runs for every chunk.

## Alternatives considered

- **SlimeWorldManager:** a third-party plugin that has to be installed on every server and creates one world per match, which is the cost we want to avoid.
- **One world per match, loaded from our format:** simpler isolation, but each world still costs its creation time and its own ticking.
- **Pasting the map block by block into a shared world:** thousands of block changes and light updates on the main thread for each match.
- **Keeping maps in PostgreSQL:** the staff chose files that are copied to each server.

## Consequences

Measured on PandaSpigot 226 with a 107-chunk four-team map:

| Measure | Result |
|---|---|
| Map file | 101 KB, against 952 KB of region files |
| Creating a world | 22 ms |
| Twenty matches | 2,140 chunks loaded in 409 ms, about 0.19 ms per chunk |
| Resetting a match | 2 ms |
| Accuracy | 0 mismatches over 199,826 sampled blocks, and tile entities kept |

- Matches must route every event by cell. A player, block or entity belongs to the match whose cell contains it.
- Cells must be wider than a map plus the view distance. `MatchCells.fits` refuses a map that is too wide for the configured cell and margin.
- World settings are shared by every match on the server, such as time and weather.
- The loader depends on 1.8 internals: `Chunk`, `ChunkSection`, `TileEntity`, NBT, and the private fields `ChunkProviderServer.chunkLoader` and `Chunk.q` (modified). A new server version needs these updated.
- Entities placed in the map (armor stands, item frames, paintings) are not kept.
