package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.level.LevelManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private void player_level_skills$captureFishingUser(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (user instanceof ServerPlayer player) {
            LevelManager.CURRENT_ATTACKER.set(player);
        }
    }

    @Inject(method = "use", at = @At("TAIL"))
    private void player_level_skills$releaseFishingUser(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        LevelManager.CURRENT_ATTACKER.remove();
    }

}