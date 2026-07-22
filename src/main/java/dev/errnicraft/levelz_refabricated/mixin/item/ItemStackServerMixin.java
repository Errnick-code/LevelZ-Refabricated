package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

@Mixin(ItemStack.class)
public class ItemStackServerMixin {

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At(value = "HEAD"), cancellable = true)
    private void damageMixin(int amount, ServerLevel world, @Nullable ServerPlayer player, Consumer<Item> breakCallback, CallbackInfo info) {
        if (BonusHelper.itemDamageChanceBonus(player)) {
            info.cancel();
        }
    }

    //para unbreaking no geral funciona 100%
    @Redirect(
            method = "processDurabilityChange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I")
    )
    private int player_level_skills$cancelUnbreakingBenefit(ServerLevel world, ItemStack stack, int amount, int originalAmount, ServerLevel worldParam, @Nullable ServerPlayer player) {

        if (player == null || player.isCreative()) {
            return EnchantmentHelper.processDurabilityChange(world, stack, amount);
        }

        Holder<Enchantment> unbreakingEntry = world.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.UNBREAKING.identifier())
                .orElse(null);

        if (unbreakingEntry != null) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(unbreakingEntry, stack);
            //System.out.println("[DEBUG ItemStack] Bloqueando bônus de atributo de: " + unbreakingEntry.getIdAsString());
            if (level > 0) {
                LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                if (!levelManager.hasRequiredEnchantmentLevel(unbreakingEntry, level)) {
                    return amount;
                }
            }
        }
        return EnchantmentHelper.processDurabilityChange(world, stack, amount);
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void player_level_skills$captureUser(Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (user instanceof ServerPlayer player) {
            // Se o item que está sendo finalizado (comido) tem componente de comida
            if (((ItemStack)(Object)this).has(DataComponents.FOOD)) {
                LevelManager.CURRENT_ATTACKER.set(player);
            }
        }
    }

    @Inject(method = "finishUsingItem", at = @At("TAIL"))
    private void player_level_skills$clearUser(Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        LevelManager.CURRENT_ATTACKER.remove();
    }

        @Inject(method = "use", at = @At("HEAD"), cancellable = true)
        private void player_level_skills$restrictPotionUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
            ItemStack stack = user.getItemInHand(hand);
            if (user.isCreative()) {
                return;
            }
            if (user instanceof ServerPlayer player) {
                LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

                if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
                    player.displayClientMessage(Component.translatable("text.levelz.restriction", Component.translatable(stack.getItemName().getString())).withStyle(ChatFormatting.RED), true);
                    cir.setReturnValue(InteractionResult.FAIL);
                    return;
                }

                if (!levelManager.hasRequiredPotionLevel(stack)) {
                    player.displayClientMessage(Component.translatable("text.levelz.restriction", Component.translatable(stack.getItemName().getString())).withStyle(ChatFormatting.RED), true);
                    cir.setReturnValue(InteractionResult.FAIL);
                }
            }
        }

}
