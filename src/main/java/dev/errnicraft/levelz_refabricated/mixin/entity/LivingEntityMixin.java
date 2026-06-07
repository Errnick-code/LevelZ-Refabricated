package dev.errnicraft.levelz_refabricated.mixin.entity;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.access.MobEntityAccess;
import dev.errnicraft.levelz_refabricated.access.PlayerDropAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.SkillBonus;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    @Shadow
    public abstract int getExperienceReward(ServerLevel world, @org.jspecify.annotations.Nullable Entity attacker);

    @Shadow
    public abstract @org.jspecify.annotations.Nullable Player getLastHurtByPlayer();

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyVariable(
            method = "getDamageAfterMagicAbsorb",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F",
                    shift = At.Shift.AFTER
            ),
            ordinal = 1
    )
    private float modifyAppliedDamageMixin(float original, DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL) && (Object) this instanceof Player playerEntity) {
            return original + BonusHelper.fallDamageReductionBonus(playerEntity);
        } else {
            return original;
        }
    }

    @ModifyVariable(
            method = "checkTotemDeathProtection",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
                    ordinal = 0
            )
    )
    private ItemStack tryUseTotemMixin(ItemStack original) {
        if ((Object) this instanceof Player playerEntity && original.is(Items.TOTEM_OF_UNDYING)) {
            if (playerEntity.isCreative()) {
                return original;
            }

            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            if (!levelManager.hasRequiredItemLevel(original.getItem())) {
                return ItemStack.EMPTY;
            }
        }

        return original;
    }

    @Inject(
            method = "checkTotemDeathProtection",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;"
            ),
            cancellable = true
    )
    private void tryUseTotemMixin(DamageSource source, CallbackInfoReturnable<Boolean> info) {
        if ((Object) this instanceof Player playerEntity && BonusHelper.deathGraceChanceBonus(playerEntity)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    protected void dropMixin(ServerLevel world, DamageSource damageSource, CallbackInfo info) {
        if (!((Object) this instanceof Player) && this.lastHurtByPlayerMemoryTime > 0 && ConfigInit.CONFIG.disableMobFarms) {
            Player attacker = this.getLastHurtByPlayer();
            if (attacker != null && !((PlayerDropAccess) attacker).allowMobDrop()) {
                info.cancel();
            }
        }
    }

    @Inject(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"
            )
    )
    private void onDeathMixin(DamageSource source, CallbackInfo info) {
        Player attacker = this.getLastHurtByPlayer();
        if (attacker != null && this.lastHurtByPlayerMemoryTime > 0 && ConfigInit.CONFIG.disableMobFarms) {
            ((PlayerDropAccess) attacker).increaseKilledMobStat(this.level().getChunk(this.blockPosition()));
        }
    }

    @Inject(method = "dropExperience", at = @At("TAIL"))
    protected void dropXpMixin(ServerLevel world, @Nullable Entity attacker, CallbackInfo info) {
        if (ConfigInit.CONFIG.mobXPMultiplier <= 0.0F) {
            return;
        }

        if ((Object) this instanceof Mob mobEntity && !ConfigInit.CONFIG.spawnerMobXP && ((MobEntityAccess) mobEntity).isSpawnerMob()) {
            return;
        }

        int baseXp = this.getExperienceReward(world, attacker);

        float customXp = baseXp * ConfigInit.CONFIG.mobXPMultiplier;

        if (ConfigInit.CONFIG.dropXPbasedOnLvl && attacker instanceof Player playerEntity) {
            customXp *= 1.0F + ConfigInit.CONFIG.basedOnMultiplier
                    * ((LevelManagerAccess) playerEntity).getLevelManager().getOverallLevel();
        }

        int finalXp = Math.max(1, Math.round(customXp));

        LevelExperienceOrbEntity.spawn((ServerLevel) this.level(), this.position(), finalXp);
    }

    @ModifyVariable(
            method = "knockback",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private double player_level_skills$increaseKnockbackStrength(double strength) {
        ServerPlayer player = LevelManager.CURRENT_ATTACKER.get();

        if (player != null) {
            if (BonusHelper.meleeKnockbackAttackChanceBonus(player)) {
                return strength + 0.5;
            }
        }
        return strength;
    }

    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private MobEffectInstance player_level_skills$upgradePotionEffect(MobEffectInstance statusEffectInstance) {
        // 'this' é a entidade recebendo o efeito
        if ((Object)this instanceof ServerPlayer playerEntity) {
            return BonusHelper.potionEffectChanceBonus(playerEntity, statusEffectInstance);
        }
        return statusEffectInstance;
    }


}