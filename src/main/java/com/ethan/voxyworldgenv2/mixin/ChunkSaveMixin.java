package com.ethan.voxyworldgenv2.mixin;

import com.ethan.voxyworldgenv2.core.Config;
import com.ethan.voxyworldgenv2.core.LodChunkTracker;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    @Shadow @Final ServerLevel level;

    /**
     * Intercepts chunk saves. When saveNormalChunks is false, chunks that were only
     * loaded for LOD generation are skipped unless a player is within view distance.
     * Runs on the main thread — no synchronization concerns with level.players().
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void voxyworldgen$onSave(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (Config.DATA.saveNormalChunks) return;

        ChunkPos pos = chunk.getPos();
        LodChunkTracker tracker = LodChunkTracker.getInstance();

        if (!tracker.isLodOnly(this.level.dimension(), pos.toLong())) return;

        // allow save if any player is within vanilla view distance (+ 2 chunk buffer)
        int viewDist = this.level.getServer().getPlayerList().getViewDistance() + 2;
        for (ServerPlayer player : this.level.players()) {
            ChunkPos pc = player.chunkPosition();
            if (Math.abs(pc.x - pos.x) <= viewDist && Math.abs(pc.z - pos.z) <= viewDist) {
                // player is nearby — unmark and save normally so the chunk is persisted
                tracker.unmark(this.level.dimension(), pos.toLong());
                return;
            }
        }

        // no player nearby — suppress save, report false (nothing was written)
        tracker.incrementSkipped();
        cir.setReturnValue(false);
    }
}
