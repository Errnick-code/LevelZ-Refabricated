package dev.errnicraft.levelz_refabricated.util;

import dev.errnicraft.levelz_refabricated.mixin.item.PersistentProjectileEntityAccessor;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.SkillBonus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BonusHelper {

    public static void bowBonus(LivingEntity shooter, Projectile projectile) {
        if (shooter instanceof Player playerEntity && projectile instanceof AbstractArrow arrow) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

            // 1. Pegamos o dano base inicial (Ex: 2.0)
            double damageToApply = ((PersistentProjectileEntityAccessor) arrow).getDamage();
            System.out.println("[DEBUG] Dano Base Inicial: " + damageToApply);

            // 2. Aplicamos primeiro o bônus fixo de nível
            if (LevelManager.BONUSES.containsKey("bowDamage")) {
                SkillBonus skillBonus = LevelManager.BONUSES.get("bowDamage");
                int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
                if (level >= skillBonus.getLevel()) {
                    double fixBonus = ConfigInit.CONFIG.bowDamageBonus * level;
                    damageToApply += fixBonus; // SOMAMOS ao valor atual
                    System.out.println("[DEBUG] Dano + Bônus Fixo: " + damageToApply);
                }
            }

            // 3. Agora, sobre o valor já aumentado, tentamos dobrar
            if (LevelManager.BONUSES.containsKey("bowDoubleDamageChance")) {
                SkillBonus skillBonus = LevelManager.BONUSES.get("bowDoubleDamageChance");
                int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
                if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= (ConfigInit.CONFIG.bowDoubleDamageChanceBonus * level)) {
                    damageToApply *= 2.0D; // DOBRAMOS o valor que já tem o bônus fixo
                    System.out.println("[DEBUG] SORTE! Dano Final Dobrado: " + damageToApply);
                }
            }

            // 4. APLICAMOS UMA ÚNICA VEZ NO FINAL
            arrow.setBaseDamage(damageToApply);
        }
    }



    public static void crossbowBonus(LivingEntity shooter, Projectile projectile) {
        if (shooter instanceof Player playerEntity && projectile instanceof AbstractArrow persistentProjectileEntity) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            // 1. Pegamos o dano base inicial (Ex: 2.0)
            double damageToApply = ((PersistentProjectileEntityAccessor) persistentProjectileEntity).getDamage();
            System.out.println("[DEBUG] Dano Base Inicial: " + damageToApply);

            // 2. Aplicamos primeiro o bônus fixo de nível
            if (LevelManager.BONUSES.containsKey("crossbowDamage")) {
                SkillBonus skillBonus = LevelManager.BONUSES.get("crossbowDamage");
                int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
                if (level >= skillBonus.getLevel()) {
                    double fixBonus = ConfigInit.CONFIG.crossbowDamageBonus * level;
                    damageToApply += fixBonus; // SOMAMOS ao valor atual
                    System.out.println("[DEBUG] Dano + Bônus Fixo: " + damageToApply);
                }
            }

            // 3. Agora, sobre o valor já aumentado, tentamos dobrar
            if (LevelManager.BONUSES.containsKey("crossbowDoubleDamageChance")) {
                SkillBonus skillBonus = LevelManager.BONUSES.get("crossbowDoubleDamageChance");
                int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
                if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= (ConfigInit.CONFIG.crossbowDoubleDamageChanceBonus * level)) {
                    damageToApply *= 2.0D; // DOBRAMOS o valor que já tem o bônus fixo
                    System.out.println("[DEBUG] SORTE! Dano Final Dobrado: " + damageToApply);
                }
            }
            persistentProjectileEntity.setBaseDamage(damageToApply);
        }
    }

    public static boolean itemDamageChanceBonus(@Nullable Player playerEntity) {
        if (playerEntity != null && LevelManager.BONUSES.containsKey("itemDamageChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("itemDamageChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= ConfigInit.CONFIG.itemDamageChanceBonus * level) {
                return true;
            }
        }
        return false;
    }

    public static MobEffectInstance potionEffectChanceBonus(@Nullable Player playerEntity, MobEffectInstance statusEffectInstance) {
        if (playerEntity != null && LevelManager.BONUSES.containsKey("potionEffectChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("potionEffectChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= (level * ConfigInit.CONFIG.potionEffectChanceBonus)) {
                float amplifier = level * ConfigInit.CONFIG.potionEffectAmplifier;
                return new MobEffectInstance(statusEffectInstance.getEffect(), statusEffectInstance.getDuration(),
                        (int) (statusEffectInstance.getAmplifier() + amplifier), statusEffectInstance.isAmbient(),
                        statusEffectInstance.isVisible(), statusEffectInstance.showIcon());
            }
        }
        return statusEffectInstance;
    }

    public static void breedTwinChanceBonus(ServerLevel world, Player playerEntity, AgeableMob animalEntity, AgeableMob otherAnimalEntity) {
        if (LevelManager.BONUSES.containsKey("breedTwinChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("breedTwinChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= ConfigInit.CONFIG.twinBreedChanceBonus) {
                AgeableMob extraPassiveEntity = animalEntity.getBreedOffspring(world, otherAnimalEntity);
                extraPassiveEntity.setBaby(true);
                extraPassiveEntity.snapTo(animalEntity.getX(), animalEntity.getY(), animalEntity.getZ(), playerEntity.getRandom().nextFloat() * 360F, 0.0F);
                world.addFreshEntityWithPassengers(extraPassiveEntity);
            }
        }
    }

    public static float fallDamageReductionBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("fallDamageReduction")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("fallDamageReduction");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return level * ConfigInit.CONFIG.fallDamageReductionBonus;
            }
        }
        return 0.0f;
    }

    public static boolean deathGraceChanceBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("deathGraceChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("deathGraceChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.deathGraceChanceBonus) {
                playerEntity.setHealth(1.0F);
                playerEntity.removeAllEffects();
                playerEntity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                playerEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
                return true;
            }
        }

        return false;
    }

    public static float tntStrengthBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("tntStrength")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("tntStrength");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return ConfigInit.CONFIG.tntStrengthBonus;
            }
        }
        return 0.0f;
    }

    public static float priceDiscountBonus(Player playerEntity) {
        if (playerEntity.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            return 1.0f;
        }
        if (LevelManager.BONUSES.containsKey("priceDiscount")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("priceDiscount");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return 1.0f - (level * ConfigInit.CONFIG.priceDiscountBonus);
            }
        }
        return 1.0f;
    }

    public static void tradeXpBonus(ServerLevel serverWorld, @Nullable Player playerEntity, AbstractVillager merchantEntity, int amount) {
        amount = (int) (amount * ConfigInit.CONFIG.tradingXPMultiplier);
        if (amount > 0) {
            if (playerEntity != null) {
                if (LevelManager.BONUSES.containsKey("tradeXp")) {
                    LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
                    SkillBonus skillBonus = LevelManager.BONUSES.get("tradeXp");
                    int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
                    if (level >= skillBonus.getLevel()) {
                        amount = (int) (amount * level * ConfigInit.CONFIG.tradeXpBonus);
                    }
                }
            }
            LevelExperienceOrbEntity.spawn(serverWorld, merchantEntity.position().add(0.0D, 0.5D, 0.0D), amount);
            // Todo: HERE
            // ? 1.0F + ConfigInit.CONFIG.basedOnMultiplier * ((PlayerStatsManagerAccess) lastCustomer).getPlayerStatsManager().getOverallLevel()
        }
    }

    public static boolean merchantImmuneBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("merchantImmune")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("merchantImmune");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return true;
            }
        }
        return false;
    }

//    public static void miningDropChanceBonus(PlayerEntity playerEntity, BlockState state, BlockPos pos, LootWorldContext.Builder builder) {
//        if (state.isIn(ConventionalBlockTags.ORES) && EnchantmentHelper.getEquipmentLevel(playerEntity.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), playerEntity) <= 0) {
//            if (LevelManager.BONUSES.containsKey("miningDropChance")) {
//                LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
//                SkillBonus skillBonus = LevelManager.BONUSES.get("miningDropChance");
//                int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
//                if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.miningDropChanceBonus) {
//                    List<ItemStack> list = state.getDroppedStacks(builder);
//                    if (!list.isEmpty()) {
//                        Block.dropStack(playerEntity.getEntityWorld(), pos, state.getDroppedStacks(builder).getFirst().split(1));
//                    }
//                }
//            }
//        }
//    }

    public static void miningDropChanceBonus(Player playerEntity, BlockState state, BlockPos pos, LootParams.Builder builder) {
        if (state.is(ConventionalBlockTags.ORES)) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

            Holder<Enchantment> silkEntry = playerEntity.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.SILK_TOUCH.identifier()).orElse(null);

            int silkLevel = EnchantmentHelper.getItemEnchantmentLevel(silkEntry, playerEntity.getMainHandItem());

            // Verificamos se o Silk Touch está ATIVO (tem o encanto E tem nível pra usar)
            boolean silkAtivoEReal = silkLevel > 0 && levelManager.hasRequiredEnchantmentLevel(silkEntry, silkLevel);

            // Se o Silk Touch NÃO estiver ativo (ou porque não tem, ou porque o nível é baixo), permitimos o bônus
            if (!silkAtivoEReal) {
                if (LevelManager.BONUSES.containsKey("miningDropChance")) {
                    SkillBonus skillBonus = LevelManager.BONUSES.get("miningDropChance");
                    int currentLevel = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();

                    // 2. LÓGICA DE CHANCE ESCALÁVEL (Nível 10+)
                    if (currentLevel >= skillBonus.getLevel()) { // skillBonus.getLevel() é 10

                        // Cálculo solicitado: Nível atual * Valor da Configuração
                        // Ex: 10 * 0.02 = 0.2 (20%) | 25 * 0.02 = 0.5 (50%)
                        float chance = (float) currentLevel * ConfigInit.CONFIG.miningDropChanceBonus;

                        // Garante que a chance não ultrapasse 100% (1.0)
                        if (playerEntity.getRandom().nextFloat() <= Math.min(1.0f, chance)) {
                            List<ItemStack> list = state.getDrops(builder);
                            if (!list.isEmpty()) {
                                // Dropa 1 unidade extra do que o bloco soltaria
                                Block.popResource(playerEntity.level(), pos, list.getFirst().copy().split(1));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void plantDropChanceBonus(Player playerEntity, BlockState state, BlockPos pos) {
        if (EnchantmentHelper.getEnchantmentLevel(playerEntity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), playerEntity) <= 0) {
            if (LevelManager.BONUSES.containsKey("plantDropChance")) {
                LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
                SkillBonus skillBonus = LevelManager.BONUSES.get("plantDropChance");
                int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
                if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.plantDropChanceBonus) {
                    List<ItemStack> list = Block.getDrops(state, (ServerLevel) playerEntity.level(), pos, null);
                    for (ItemStack itemStack : list) {
                        if (itemStack.is(ConventionalItemTags.CROPS)) {
                            Block.popResource(playerEntity.level(), pos, itemStack);
                            break;
                        }
                    }
                }
            }
        }
    }

//    public static void foodIncreasionBonus(PlayerEntity playerEntity, ItemStack itemStack) {
//        if (LevelManager.BONUSES.containsKey("foodIncreasion") && itemStack.get(DataComponentTypes.FOOD) != null) {
//            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
//            SkillBonus skillBonus = LevelManager.BONUSES.get("foodIncreasion");
//            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
//            if (level >= skillBonus.getLevel()) {
//                FoodComponent foodComponent = itemStack.get(DataComponentTypes.FOOD);
//                float multiplier = level * ConfigInit.CONFIG.foodIncreasionBonus;
//                playerEntity.getHungerManager().eat(new FoodComponent((int) (foodComponent.nutrition() * multiplier), (int) (foodComponent.saturation() * multiplier), true));
//            }
//        }
//    }




    public static boolean anvilXpCapBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("anvilXpCap")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("anvilXpCap");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return true;
            }
        }
        return false;
    }


    public static int anvilXpDiscountBonus(Player playerEntity, int levelCost, boolean keepSecondSlot) {
    //        if (levelCost > ConfigInit.CONFIG.anvilXpCap && anvilXpCapBonus(playerEntity)) {
    //            return ConfigInit.CONFIG.anvilXpCap;
    //        }
        int custoFinal = levelCost;
        if (LevelManager.BONUSES.containsKey("anvilXpDiscount")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("anvilXpDiscount");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                custoFinal = Math.round(levelCost * (1.0f - level * ConfigInit.CONFIG.anvilXpDiscountBonus));
            }

        }

        if (keepSecondSlot && custoFinal >= 40) {
            if (anvilXpCapBonus(playerEntity)) {
                return 39; // Trava em 39 para permitir renomear sem o erro "Muito Caro"
            }
        }
        return custoFinal ;
    }


    public static boolean anvilXpChanceBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("anvilXpChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("anvilXpChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.anvilXpChanceBonus) {
                return true;
            }
        }
        return false;
    }

    public static void healthRegenBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("healthRegen")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("healthRegen");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                playerEntity.heal(level * ConfigInit.CONFIG.healthRegenBonus);
            }
        }
    }

    public static void healthAbsorptionBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("healthAbsorption")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("healthAbsorption");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                playerEntity.addEffect(new MobEffectInstance(
                        MobEffects.ABSORPTION,
                        400, 10, false, false, true));
                playerEntity.setAbsorptionAmount(level * ConfigInit.CONFIG.healthAbsorptionBonus);
            }
        }
    }

    public static float exhaustionReductionBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("exhaustionReduction")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("exhaustionReduction");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return 1.0f - (level * ConfigInit.CONFIG.exhaustionReductionBonus);
            }
        }
        return 0.0f;
    }

    public static boolean meleeKnockbackAttackChanceBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("meleeKockbackAttackChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("meleeKockbackAttackChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.meleeKnockbackAttackChanceBonus) {
                return true;
            }
        }
        return false;
    }

    public static boolean meleeCriticalAttackChanceBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("meleeCriticalAttackChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("meleeCriticalAttackChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.meleeCriticalAttackChanceBonus) {
                return true;
            }
        }
        return false;
    }

    public static float meleeCriticalDamageBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("meleeCriticalAttackDamage")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("meleeCriticalAttackDamage");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel()) {
                return level * ConfigInit.CONFIG.meleeCriticalAttackDamageBonus;
            }
        }
        return 0.0f;
    }

    public static boolean meleeDoubleDamageBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("meleeDoubleAttackDamageChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("meleeDoubleAttackDamageChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= ConfigInit.CONFIG.meleeDoubleAttackDamageChanceBonus) {
                return true;
            }
        }
        return false;
    }


    public static void damageReflectionBonus(Player playerEntity, DamageSource source, float amount) {
        if (source.getEntity() != null
                && LevelManager.BONUSES.containsKey("damageReflection")
                && LevelManager.BONUSES.containsKey("damageReflectionChance")) {

            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

            SkillBonus skillBonus = LevelManager.BONUSES.get("damageReflectionChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();

            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= level * ConfigInit.CONFIG.damageReflectionChanceBonus) {

                skillBonus = LevelManager.BONUSES.get("damageReflection");
                level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();

                if (level >= skillBonus.getLevel()) {
                    float reflectedDamage = amount * level * ConfigInit.CONFIG.damageReflectionBonus;

                    if (playerEntity.level() instanceof net.minecraft.server.level.ServerLevel serverWorld) {
                        source.getEntity().hurtServer(serverWorld, source, reflectedDamage);
                    }
                }
            }
        }
    }

    public static boolean evadingDamageBonus(Player playerEntity) {
        if (LevelManager.BONUSES.containsKey("evadingDamageChance")) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("evadingDamageChance");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();
            if (level >= skillBonus.getLevel() && playerEntity.getRandom().nextFloat() <= (level * ConfigInit.CONFIG.evadingDamageChanceBonus)) {
                return true;
            }
        }
        return false;
    }


}

