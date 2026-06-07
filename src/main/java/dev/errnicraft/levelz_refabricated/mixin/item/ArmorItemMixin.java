package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(EquipmentDispenseItemBehavior.class)
public class ArmorItemMixin {

    @Inject(method = "dispenseEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEquipmentSlotForItem(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/EquipmentSlot;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void dispenseArmorMixin(BlockSource pointer, ItemStack armor, CallbackInfoReturnable<Boolean> info, BlockPos blockPos, List<LivingEntity> list, LivingEntity livingEntity) {
        if (livingEntity instanceof Player playerEntity) {
            if (playerEntity.isCreative()) {
                return;
            }
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            if (!levelManager.hasRequiredItemLevel(armor.getItem())) {
                info.setReturnValue(false);
            }
        }
    }

}
