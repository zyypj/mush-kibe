package br.com.muxe.minigame.engine.world.server;

import br.com.muxe.minigame.engine.world.map.ChunkSource;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import net.minecraft.server.v1_8_R3.World;

final class SourceChunkLoader implements IChunkLoader {

    private final ChunkSource source;
    private final boolean saveUnchanged;

    SourceChunkLoader(ChunkSource source, boolean saveUnchanged) {
        this.source = source;
        this.saveUnchanged = saveUnchanged;
    }

    @Override
    public Chunk a(World world, int x, int z) {
        return source.load(x, z).map(data -> NmsChunks.toServer(world, data)).orElse(null);
    }

    @Override
    public void a(World world, Chunk chunk) {
        if (saveUnchanged || NmsChunks.isModified(chunk)) {
            source.save(NmsChunks.fromServer(chunk));
        }
    }

    @Override
    public void b(World world, Chunk chunk) {
    }

    @Override
    public void a() {
    }

    @Override
    public void b() {
    }
}
