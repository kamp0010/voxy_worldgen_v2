package com.ethan.voxyworldgenv2.mixin;

import com.ethan.voxyworldgenv2.core.ChunkGenerationManager;
import com.ethan.voxyworldgenv2.core.Config;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkSaveMixin {

    @Shadow @Final private ServerLevel level;

    @Inject(
        method = "save",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onChunkSave(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.DATA.saveNormalChunks) {
            long chunkPos = chunk.getPos().toLong();
            ChunkGenerationManager manager = ChunkGenerationManager.getInstance();
            if (manager.isVoxyOnlyChunk(level.dimension(), chunkPos)) {
                manager.incrementSaveSkipped();
                cir.setReturnValue(true);
            }
        }
    }
}
