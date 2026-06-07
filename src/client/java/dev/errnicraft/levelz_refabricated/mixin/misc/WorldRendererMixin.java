package dev.errnicraft.levelz_refabricated.mixin.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow
    @Mutable
    @Final
    private Minecraft minecraft;

    @Redirect(
            method = "renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/LevelRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V"
            )
    )
    private void redirectDrawBlockOutline(LevelRenderer instance,
                                          PoseStack matrices,
                                          VertexConsumer vertexConsumer,
                                          double x,
                                          double y,
                                          double z,
                                          BlockOutlineRenderState state,
                                          int color,
                                          float lineWidth) {

        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null || !ConfigInit.CONFIG.highlightLocked) {
            ((WorldRendererInvoker) instance).invokeDrawBlockOutline(matrices, vertexConsumer, x, y, z, state, color, lineWidth);
            return;
        }

        BlockPos blockPos = ((OutlineRenderStateAccessor) (Object) state).getPos();
        BlockState blockState = this.minecraft.level.getBlockState(blockPos);

        boolean locked = !((LevelManagerAccess) this.minecraft.player)
                .getLevelManager()
                .hasRequiredMiningLevel(blockState.getBlock());

        int finalColor = locked ? 0x80FF0000 : color;

        ((WorldRendererInvoker) instance).invokeDrawBlockOutline(matrices, vertexConsumer, x, y, z, state, finalColor, lineWidth);
    }
}