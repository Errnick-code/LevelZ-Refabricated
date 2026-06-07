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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ToolItemMixin {

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void postHitMixin(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (attacker instanceof Player playerEntity) {
            if (playerEntity.isCreative()) {
                return;
            }
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
                ci.cancel();
            }
        }
    }

//    @Inject(method = "use", at = @At("HEAD"))
//    private void player_level_skills$capturePlayerOnUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
//        if (user instanceof ServerPlayerEntity player) {
//            // Seta o contexto do player para os Mixins de encantamento que virão a seguir
//            LevelManager.CURRENT_ATTACKER.set(player);
//            // System.out.println("[DEBUG Use] Capturado: " + player.getName().getString());
//        }
//    }
//
//    @Inject(method = "use", at = @At("TAIL"))
//    private void player_level_skills$cleanPlayerAfterUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
//        // LIMPEZA CRUCIAL para evitar o bug de "liberar tudo"
//        LevelManager.CURRENT_ATTACKER.remove();
//    }

    @Inject(method = "postHurtEnemy", at = @At("HEAD"), cancellable = true)
    private void postMineMixin(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo info) {
        if (attacker instanceof Player playerEntity) {
            if (playerEntity.isCreative()) {
                return;
            }
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
                info.cancel();
            }
        }
    }

}