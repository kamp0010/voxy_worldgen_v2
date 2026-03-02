package com.ethan.voxyworldgenv2.mixin;

import com.ethan.voxyworldgenv2.core.Config;
import com.ethan.voxyworldgenv2.core.ChunkGenerationManager;
import com.ethan.voxyworldgenv2.core.LodChunkTracker;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkSaveMixin {

    // reading a @Final field is safe from any thread — it is set once in the constructor
    @Shadow @Final ServerLevel level;

    /**
     * Intercepts chunk saves. When saveNormalChunks is false, chunks that were only
     * loaded for LOD generation are suppressed unless a player is within view distance.
     *
     * THREAD SAFETY: ChunkMap.save() is called from C2ME storage threads, not just
     * the main server thread. This method must not touch any main-thread-only APIs.
     * All player proximity data is read from ChunkGenerationManager's thread-safe
     * cached maps (ConcurrentHashMap) which are updated on the main thread each tick.
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void voxyworldgen$onSave(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (Config.DATA.saveNormalChunks) return;

        ChunkPos pos = chunk.getPos();
        LodChunkTracker tracker = LodChunkTracker.getInstance();

        if (!tracker.isLodOnly(this.level.dimension(), pos.toLong())) return;

        // isAnyPlayerNear reads only from ConcurrentHashMaps updated each server tick —
        // safe to call from C2ME storage threads
        if (ChunkGenerationManager.getInstance().isAnyPlayerNear(this.level.dimension(), pos)) {
            // player is nearby: unmark and let the save proceed so the chunk persists normally
            tracker.unmark(this.level.dimension(), pos.toLong());
            return;
        }

        // no player nearby — suppress this save and record the chunk (once) in the counter
        tracker.incrementSkipped(this.level.dimension(), pos.toLong());
        cir.setReturnValue(false);
    }
}
