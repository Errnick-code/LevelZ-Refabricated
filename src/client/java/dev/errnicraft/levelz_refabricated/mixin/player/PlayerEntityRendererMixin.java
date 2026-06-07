package dev.errnicraft.levelz_refabricated.mixin.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {

//    @WrapOperation(method = "renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V"))
//    private void renderLabelIfPresentMixin(PlayerEntityRenderer instance, AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, Operation<Void> original) {
//        if (!((ClientPlayerAccess) abstractClientPlayerEntity).shouldRenderClientName()) {
//            return;
//        }
//        if (ConfigInit.CONFIG.showLevel) {
//            original.call(instance, abstractClientPlayerEntity, Team.decorateName(abstractClientPlayerEntity.getScoreboardTeam(),
//                    Text.translatable("text.levelz.scoreboard", ((ClientPlayerListAccess) abstractClientPlayerEntity).getLevel(), abstractClientPlayerEntity.getName())), matrixStack, vertexConsumerProvider, i, f);
//        } else {
//            original.call(instance, abstractClientPlayerEntity, text, matrixStack, vertexConsumerProvider, i, f);
//        }
//    }

}
