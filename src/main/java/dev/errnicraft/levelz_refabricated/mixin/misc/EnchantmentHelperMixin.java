package dev.errnicraft.levelz_refabricated.mixin.misc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    //para frostwalker, fireAspect,
    @WrapOperation(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"
            )
    )
    private static void player_level_skills$filterEnchantmentEffects(
            EnchantmentHelper.EnchantmentInSlotVisitor instance,
            Holder<Enchantment> enchantment,
            int level,
            EnchantedItemInUse context,
            Operation<Void> original
    ) {
        LivingEntity owner = context.owner();

        if (owner instanceof Player playerEntity && (!playerEntity.isCreative())) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

            if (!levelManager.hasRequiredEnchantmentLevel(enchantment, level)) {
                return;
            }
        }

        original.call(instance, enchantment, level, context);
    }




    // para sharpness, knockback, bane of A., Unbreaking -besta
//    @WrapOperation(
//            method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/enchantment/EnchantmentHelper$Consumer;)V",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper$Consumer;accept(Lnet/minecraft/registry/entry/RegistryEntry;I)V")
//    )
//    private static void player_level_skills$filterEnchantmentEffect(
//            EnchantmentHelper.Consumer instance, RegistryEntry<Enchantment> enchantment, int level, Operation<Void> original
//    ) {
//        // Tenta pegar o player da thread atual (definido no Mixin do attack ou tick)
//        ServerPlayerEntity player = LevelManager.CURRENT_ATTACKER.get();
//
//        if (player != null) {
//            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
//            if (!levelManager.hasRequiredEnchantmentLevel(enchantment, level)) {
//                System.out.println("[DEBUG forEachEnchantment] Enchantment " + enchantment.getKey().toString());
//                // Ignora o encantamento se o player não tem nível
//                return;
//            }
//        }
//
//        original.call(instance, enchantment, level);
//    }

    @WrapOperation(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V")
    )
    private static void player_level_skills$filterEnchantmentEffect(
            EnchantmentHelper.EnchantmentVisitor instance,
            Holder<Enchantment> enchantment,
            int level,
            Operation<Void> original
    ) {
        ServerPlayer player = LevelManager.CURRENT_ATTACKER.get();

        if (player != null && (!player.isCreative())) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            if (!levelManager.hasRequiredEnchantmentLevel(enchantment, level)) {
                return;
            }
        }

        original.call(instance, enchantment, level);
    }


    // funcionando para encantamentos de atributos
    @Inject(method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
    private static void player_level_skills$applyAttributeModifiers(
            ItemStack stack, EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> attributeModifierConsumer, CallbackInfo ci
    ) {
        // 1. Pega o player da Thread (Setado no Tick ou no ponto de atualização)
        ServerPlayer player = LevelManager.CURRENT_MINER.get();
        if (player == null) return;
        if (player.isCreative()) return;

        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

        // 2. Verifica os encantamentos do item
        for (var entry : stack.getEnchantments().entrySet()) {
            Holder<Enchantment> enchant = entry.getKey();
            int level = entry.getIntValue();

            // 3. Aplica a sua restrição de nível
            if (!levelManager.hasRequiredEnchantmentLevel(enchant, level)) {
                //System.out.println("[DEBUG] Bloqueando bônus de: " + enchant.getIdAsString());
                ci.cancel();
                return;
            }
        }
    }

//    @Inject(method = "getFishingTimeReduction", at = @At("HEAD"), cancellable = true)
//    private static void player_level_skills$fishingTimeReduction(
//            ServerWorld world, ItemStack stack, Entity user, CallbackInfoReturnable<Float> cir
//    ) {
//        if (user instanceof ServerPlayerEntity player) {
//            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
//            RegistryEntry<Enchantment> lureEntry = player.getEntityWorld().getRegistryManager()
//                    .getOrThrow(RegistryKeys.ENCHANTMENT)
//                    .getEntry(Enchantments.LURE.getValue())
//                    .orElse(null);
//
//            // Buscamos o nível de LURE (Isca) na vara de pesca
//            int level = EnchantmentHelper.getLevel(
//                    world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT)
//                            .getEntry(Enchantments.LURE.getValue()).orElse(null),
//                    stack
//            );
//
//            if (level > 0) {
//                // Se o player não tem nível, retornamos 0.0f (sem redução de tempo)
//                if (!levelManager.hasRequiredEnchantmentLevel(lureEntry, level)) {
//                    System.out.println("[DEBUG getFishingTimeReduction] Bloqueando bônus de: " + lureEntry);
//                    cir.setReturnValue(0.0f);
//                }
//            }
//        }
//    }


    @Inject(method = "modifyDamage", at = @At("RETURN"), cancellable = true)
    private static void player_level_skills$filterSpearDamage(
            ServerLevel world,
            ItemStack stack,
            Entity target,
            DamageSource damageSource,
            float baseDamage,
            CallbackInfoReturnable<Float> cir
    ) {
        // 1. Pegamos o player que causou o dano
        if (damageSource.getEntity() instanceof ServerPlayer player && (!player.isCreative())) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            float extraDamage = cir.getReturnValue() - baseDamage;

            if (extraDamage > 0) {
                // 2. Verificamos os encantamentos do item (Lança ou Espada)
                for (var entry : stack.getEnchantments().entrySet()) {
                    Holder<Enchantment> enchant = entry.getKey();
                    int level = entry.getIntValue();

                    // 3. Se o player não tem nível para o encantamento no item
                    if (!levelManager.hasRequiredEnchantmentLevel(enchant, level)) {
                        // System.out.println("[DEBUG Lança] Bloqueando dano extra de: " + enchant.getIdAsString());

                        // Retornamos apenas o dano base, ignorando o Sharpness/Afiação
                        cir.setReturnValue(baseDamage);
                        return;
                    }
                }
            }
        }
    }


    // para Power
//    @Inject(method = "getDamage", at = @At("HEAD"), cancellable = true)
//    private static void player_level_skills$filterEnchantmentDamage(ServerWorld world, ItemStack stack, Entity target, DamageSource damageSource, float baseDamage, CallbackInfoReturnable<Float> cir) {
//        // 1. Pegamos o atacante diretamente da fonte do dano (funciona para flechas e espadas)
//        if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {
//            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
//
//            RegistryEntry<Enchantment> powerEntry = player.getEntityWorld().getRegistryManager()
//                    .getOrThrow(RegistryKeys.ENCHANTMENT)
//                    .getEntry(Enchantments.POWER.getValue())
//                    .orElse(null);
//
//            if (powerEntry != null) {
//                int level = EnchantmentHelper.getLevel(powerEntry, stack);
//                if (level > 0) {
//                    if (!levelManager.hasRequiredEnchantmentLevel(powerEntry, level)) {
//                        cir.setReturnValue(baseDamage);
//                    }
//
//                }
//            }
//
//        }
//    }

    @Inject(method = "modifyKnockback", at = @At("RETURN"), cancellable = true)
    private static void player_level_skills$cancelKnockback(ServerLevel world, ItemStack stack, Entity target, DamageSource damageSource, float baseKnockback, CallbackInfoReturnable<Float> cir) {
        if (damageSource.getEntity() instanceof ServerPlayer player && (!player.isCreative())) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            var knockbackEntry = player.registryAccess() .lookupOrThrow(Registries.ENCHANTMENT) .get(Enchantments.PUNCH.identifier()) .orElse(null);
            int level = EnchantmentHelper.getItemEnchantmentLevel(knockbackEntry, stack);
            if (level > 0 && !levelManager.hasRequiredEnchantmentLevel(knockbackEntry, level)) {
                // Retorna o knockback base (sem o bônus do encantamento)
                cir.setReturnValue(baseKnockback);
            }
        }
    }


    @Inject(
            method = "doPostAttackEffectsWithItemSource(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void player_level_skills$blockRestrictedWeaponUse(
            ServerLevel world,
            Entity target,
            DamageSource damageSource,
            ItemStack weapon,
            CallbackInfo ci
    ) {
        if (weapon == null || weapon.isEmpty()) {
            return;
        }

        if (!(damageSource.getEntity() instanceof Player playerEntity)) {
            return;
        }
        if (playerEntity.isCreative()) {
            return;
        }

        LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

        if (!levelManager.hasRequiredItemLevel(weapon.getItem())) {
            ci.cancel();
        }
    }


    @Inject(method = "getTridentSpinAttackStrength", at = @At("HEAD"), cancellable = true)
    private static void player_level_skills$filterRiptideImpulse(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Float> ci) {

        if (user instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) {
            return;
        }

            LevelManager levelManager = ((LevelManagerAccess) user).getLevelManager();

            Holder<Enchantment> riptideEntry = user.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.RIPTIDE.identifier()).orElse(null);

            int level = EnchantmentHelper.getItemEnchantmentLevel(riptideEntry, stack);

                if (!levelManager.hasRequiredEnchantmentLevel(riptideEntry, level)) {
                    ci.cancel();
                }

    }
}
