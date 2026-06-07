package dev.errnicraft.levelz_refabricated.mixin.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

@Mixin(ProjectileWeaponItem.class)
public abstract class RangedWeaponItemMixin {

    // para multishot
    @Inject(method = "shoot", at = @At("HEAD"))
    private void player_level_skills$blockMultishot(
            ServerLevel world,
            LivingEntity shooter,
            net.minecraft.world.InteractionHand hand,
            ItemStack stack,
            List<ItemStack> projectiles,
            float speed,
            float divergence,
            boolean critical,
            @Nullable LivingEntity target,
            CallbackInfo ci
    ) {

        // SE FOR CRIATIVO, NÃO BLOQUEIA
        if (shooter instanceof Player player && player.isCreative()) return;

        if (!(shooter instanceof Player player)) return;

        // Na 1.21.1, o multishot faz com que a lista 'projectiles' tenha 3 itens.
        // Se o jogador não tiver nível, limpamos as flechas extras da lista ANTES do disparo.
        if (projectiles.size() > 1) {
            Holder<Enchantment> multishotEntry = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.MULTISHOT.identifier())
                    .orElse(null);

            if (multishotEntry != null) {
                LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                int level = EnchantmentHelper.getItemEnchantmentLevel(multishotEntry, stack);

                if (!levelManager.hasRequiredEnchantmentLevel(multishotEntry, level)) {
                    // Mantemos apenas a primeira flecha (a central)
                    ItemStack mainProjectile = projectiles.getFirst();
                    projectiles.clear();
                    projectiles.add(mainProjectile);

                    System.out.println("[DEBUG] Multishot bloqueado: disparando apenas 1 flecha.");
                }
            }
        }

    }
    //para infinity
        @Inject(method = "shoot", at = @At("TAIL"))
        private void player_level_skills$forceConsumeIfInfinityBlocked(
                ServerLevel world,
                LivingEntity shooter,
                net.minecraft.world.InteractionHand hand,
                ItemStack stack,
                List<ItemStack> projectiles,
                float speed,
                float divergence,
                boolean critical,
                net.minecraft.world.entity.LivingEntity target,
                CallbackInfo ci
        ) {
            if (!(shooter instanceof Player player) || player.isCreative()) return;

            // 1. Pegamos o registro do Infinity
            Holder<Enchantment> infinityEntry = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.INFINITY.identifier())
                    .orElse(null);

            if (infinityEntry != null) {
                int level = EnchantmentHelper.getItemEnchantmentLevel(infinityEntry, stack);

                // 2. Se o arco TEM Infinity, mas o jogador NÃO tem nível:
                if (level > 0) {
                    LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

                    if (!levelManager.hasRequiredEnchantmentLevel(infinityEntry, level)) {

                        // 3. Consumimos a flecha manualmente do inventário
                        // O metodo 'removeItem' procura o item de munição e diminui 1
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack invStack = player.getInventory().getItem(i);

                            // Verifica se é uma flecha comum (ajuste conforme necessário para flechas de efeitos)
                            if (!invStack.isEmpty() && invStack.getItem().equals(net.minecraft.world.item.Items.ARROW)) {
                                invStack.shrink(1);

                                // Se a pilha acabar, removemos o item
                                if (invStack.isEmpty()) {
                                    player.getInventory().removeItemNoUpdate(i);
                                }
                                break; // Removemos apenas UMA flecha por disparo
                            }
                        }
                    }
                }
            }
        }
    }
