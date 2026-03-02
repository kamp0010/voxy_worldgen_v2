package com.ethan.voxyworldgenv2.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface ChunkAccessUnsavedMixin {
    @Accessor("unsaved")
    void voxyworldgen$setUnsaved(boolean unsaved);
}
