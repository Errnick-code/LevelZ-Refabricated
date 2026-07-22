package dev.errnicraft.levelz_refabricated.mixin.compat;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "artifacts.equipment.EquipmentSlotManager", remap = false)
public class ArtifactsEquipMixin {

    @Inject(method = "tryEquipFromUse", at = @At("HEAD"), cancellable = true, remap = false)
    private static void levelz$blockRestrictedArtifactEquip(
            LivingEntity entity,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (!(entity instanceof Player player)) return;
        if (player.isCreative()) return;

        var stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) return;

        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
        if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("restriction.levelz.locked.tooltip")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
