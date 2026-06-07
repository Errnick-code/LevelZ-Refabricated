package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.level.LevelManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class TridentUsageMixin {

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void player_level_skills$captureTridentUser(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (user instanceof ServerPlayer player) {
            LevelManager.CURRENT_ATTACKER.set(player);
        }
    }

    @Inject(method = "releaseUsing", at = @At("TAIL"))
    private void player_level_skills$releaseTridentUser(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        LevelManager.CURRENT_ATTACKER.remove();
    }

}
