package dev.errnicraft.levelz_refabricated.mixin.misc;

import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockOutlineRenderState.class)
public interface OutlineRenderStateAccessor {
    @Accessor("pos")
    BlockPos getPos();
}