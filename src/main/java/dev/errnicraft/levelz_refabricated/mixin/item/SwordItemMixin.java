package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public class SwordItemMixin {

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void postHitMixin(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (!(attacker instanceof Player playerEntity)) {
            return;
        }

        if (playerEntity.isCreative()) {
            return;
        }

        LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

        if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
            ci.cancel();
        }
    }

    @Inject(method = "postHurtEnemy", at = @At("HEAD"), cancellable = true)
    private void postDamageEntityMixin(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (!(attacker instanceof Player playerEntity)) {
            return;
        }

        if (playerEntity.isCreative()) {
            return;
        }

        LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

        if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
            ci.cancel();
        }
    }
}