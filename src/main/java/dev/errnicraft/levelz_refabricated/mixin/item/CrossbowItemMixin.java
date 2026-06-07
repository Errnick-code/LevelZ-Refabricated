package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.Level;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "shootProjectile", at = @At("TAIL"))
    private void shootMixin(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, LivingEntity target, CallbackInfo info) {
        BonusHelper.crossbowBonus(shooter, projectile);
    }

    @Inject(method = "use", at = @At("HEAD"))
    private void player_level_skills$captureExtraProjectilesBeforeUse(
            Level world,
            Player user,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (user instanceof ServerPlayer player) {
            LevelManager.CURRENT_ATTACKER.set(player);
        }
    }

    @Inject(method = "use", at = @At("TAIL"))
    private void player_level_skills$releaseProjectilesBeforeUse(
            Level world,
            Player user,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (user instanceof ServerPlayer player) {
            LevelManager.CURRENT_ATTACKER.remove();

        }
    }
}
