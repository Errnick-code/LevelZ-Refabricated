package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Inject(method = "shootProjectile", at = @At("TAIL"))
    private void shootMixin(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, LivingEntity target, CallbackInfo info) {
        BonusHelper.bowBonus(shooter, projectile);
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void player_level_skills$captureShooter(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (user instanceof ServerPlayer player) {
            LevelManager.CURRENT_ATTACKER.set(player);
        }
    }

    @Inject(method = "releaseUsing", at = @At("TAIL"))
    private void player_level_skills$releaseShooter(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        LevelManager.CURRENT_ATTACKER.remove();
    }
}
