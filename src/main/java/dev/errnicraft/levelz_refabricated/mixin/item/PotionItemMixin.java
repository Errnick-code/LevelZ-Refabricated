package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PotionItem.class)
public class PotionItemMixin {

//    @ModifyVariable(method = "method_57389", at = @At("HEAD"), argsOnly = true)
//    private static StatusEffectInstance finishUsingMixin(StatusEffectInstance original, PlayerEntity playerEntity, LivingEntity livingEntity, StatusEffectInstance statusEffectInstance) {
//        return BonusHelper.potionEffectChanceBonus(playerEntity, original);
//    }

}
