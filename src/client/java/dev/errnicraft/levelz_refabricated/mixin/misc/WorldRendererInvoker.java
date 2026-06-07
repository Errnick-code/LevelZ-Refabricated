package dev.errnicraft.levelz_refabricated.mixin.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface WorldRendererInvoker {

    /**
     * Invoca o método private drawBlockOutline de forma segura.
     * Assinatura exata do Yarn 1.21.11+build.4.
     */
    @Invoker("renderHitOutline")
    void invokeDrawBlockOutline(PoseStack matrices,
                                VertexConsumer vertexConsumer,
                                double x,
                                double y,
                                double z,
                                BlockOutlineRenderState state,
                                int color,
                                float lineWidth);
}