package dev.errnicraft.levelz_refabricated.mixin.compat;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "eu.pb4.trinkets.impl.TrinketSlot", remap = false)
public class TrinketItemMixin {

    // canInsert — это instance-метод в TrinketSlot, поэтому убираем static.
    // Сигнатура: boolean canInsert(ItemStack stack, SlotReference slotRef, LivingEntity entity)
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true, remap = false)
    private void levelz$blockRestrictedTrinketEquip(
            ItemStack stack,
            Object slotRef,
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir) {

        if (!(entity instanceof Player player)) return;
        if (player.isCreative()) return;
        if (stack.isEmpty()) return;

        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
        if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
            // Показываем сообщение над хотбаром (actionbar)
            player.displayClientMessage(
                    Component.translatable("restriction.levelz.locked.tooltip")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            cir.setReturnValue(false);
            if (entity instanceof ServerPlayer serverPlayer) {
                LevelZRefabricated.LOGGER.info("[LevelZ] Blocked trinket equip (server) for {} item={}",
                        serverPlayer.getName().getString(), stack.getItem().getDescriptionId());
            }
        }
    }
}