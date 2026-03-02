package com.ethan.voxyworldgenv2.core;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks chunk positions that were loaded exclusively for LOD generation
 * and have not been claimed by player view distance. Session-only; not persisted.
 */
public final class LodChunkTracker {
    private static final LodChunkTracker INSTANCE = new LodChunkTracker();

    // per-dimension set of packed ChunkPos longs that are LOD-only this session
    private final Map<ResourceKey<Level>, LongSet> lodChunks = new ConcurrentHashMap<>();
    private final AtomicLong savedSkipCount = new AtomicLong(0);

    private LodChunkTracker() {}

    public static LodChunkTracker getInstance() {
        return INSTANCE;
    }

    /** Mark a chunk as having been generated only for LOD purposes. */
    public void markLod(ResourceKey<Level> dim, long packedPos) {
        lodChunks.computeIfAbsent(dim, k -> LongSets.synchronize(new LongOpenHashSet())).add(packedPos);
    }

    /** Remove a chunk from the LOD-only set (player claimed it). */
    public void unmark(ResourceKey<Level> dim, long packedPos) {
        LongSet set = lodChunks.get(dim);
        if (set != null) set.remove(packedPos);
    }

    /** Returns true if the chunk is tracked as LOD-only for this dimension. */
    public boolean isLodOnly(ResourceKey<Level> dim, long packedPos) {
        LongSet set = lodChunks.get(dim);
        return set != null && set.contains(packedPos);
    }

    public void incrementSkipped() {
        savedSkipCount.incrementAndGet();
    }

    public long getSkippedSaveCount() {
        return savedSkipCount.get();
    }

    /** Call on server shutdown to release all memory. */
    public void clearAll() {
        lodChunks.clear();
        savedSkipCount.set(0);
    }
}
