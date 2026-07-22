package dev.errnicraft.levelz_refabricated.access;

import net.minecraft.world.level.chunk.ChunkAccess;

public interface PlayerDropAccess {

    void increaseKilledMobStat(ChunkAccess chunk);

    boolean allowMobDrop();

    boolean allowMobDropInChunk(ChunkAccess chunk);

    void resetKilledMobStat();

    void tickChunkKillDecay();

    long[] getChunkKillStatus(ChunkAccess chunk);
}
