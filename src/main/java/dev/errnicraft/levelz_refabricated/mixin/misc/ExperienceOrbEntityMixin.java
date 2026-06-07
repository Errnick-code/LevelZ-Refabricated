package dev.errnicraft.levelz_refabricated.mixin.misc;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbEntityMixin {

    @Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true)
    private void player_level_skills$blockMendingIfNeeded(ServerPlayer player, int amount, CallbackInfoReturnable<Integer> cir) {
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

        Holder<Enchantment> mendingEntry = player.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.MENDING.identifier())
                .orElse(null);

        if (mendingEntry == null) {
            return;
        }

        boolean hasMending = false;

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(mendingEntry, stack) > 0) {
                hasMending = true;
                break;
            }
        }

        if (!hasMending) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(mendingEntry, stack) > 0) {
                    hasMending = true;
                    break;
                }
            }
        }

        if (!hasMending) {
            return;
        }

        if (!levelManager.hasRequiredEnchantmentLevel(mendingEntry, 1)) {
            cir.setReturnValue(0);
        }
    }
}