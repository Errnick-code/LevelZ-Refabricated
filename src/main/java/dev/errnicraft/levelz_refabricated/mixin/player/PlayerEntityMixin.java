package dev.errnicraft.levelz_refabricated.mixin.player;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.access.PlayerDropAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.SkillBonus;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements LevelManagerAccess, PlayerDropAccess {

    private final Player playerEntity = (Player) (Object) this;
    @Unique
    private final LevelManager levelManager = new LevelManager(playerEntity);

    @Unique
    private int killedMobsInChunk;
    @Unique
    @Nullable
    private ChunkAccess killedMobChunk;

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "TAIL"))
    public void readCustomData(ValueInput view, CallbackInfo info) {
        this.levelManager.readNbt(view);
    }

    @Inject(method = "addAdditionalSaveData", at = @At(value = "TAIL"))
    public void writeCustomData(ValueOutput view, CallbackInfo info) {
        this.levelManager.writeNbt(view);
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"), ordinal = 0, argsOnly = true)
    private float addExhaustionMixin(float original) {
        original *= BonusHelper.exhaustionReductionBonus(this.playerEntity);
        return original;
    }

    // work, but in test conflit with unique weapons
    @ModifyVariable(
            method = "attack",
            at = @At("STORE"), // Injeta no momento da gravação da variável
            ordinal = 2        // O mesmo endereço que o outro mod usa
    )
    private boolean player_level_skills$additionalCriticalChance(boolean original) {

        if (original) {
            return true;
        }

        return BonusHelper.meleeCriticalAttackChanceBonus((Player) (Object) this);
    }


    @ModifyVariable(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getWeaponItem()Lnet/minecraft/world/item/ItemStack;"), ordinal = 0)
    private float attackMixin(float original) {
        if (this.playerEntity.isCreative()) {
            return original;
        }
        if (!levelManager.hasRequiredItemLevel(getWeaponItem().getItem())) {
            return 0.0f;
        }
        return original;
    }

    //work
    @ModifyVariable(method = "attack", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"), ordinal = 2)
    private float attackCriticalMixin(float original) {
        boolean isCritical = this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !this.isPassenger();

        if (isCritical) {
            float bonus = BonusHelper.meleeCriticalDamageBonus((Player) (Object) this);
            return original + bonus;
        }
        return original;
    }


    @ModifyVariable(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"), ordinal = 3)
    private float attackDoubleDamageMixin(float original) {
        if (BonusHelper.meleeDoubleDamageBonus(this.playerEntity)) {
            original *= 2f;
        }
        return original;
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void player_level_skills$captureAttacker(Entity target, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player) {
            LevelManager.CURRENT_ATTACKER.set(player);
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void player_level_skills$releaseAttacker(Entity target, CallbackInfo ci) {
        LevelManager.CURRENT_ATTACKER.remove();
    }


    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"), cancellable = true)
    private void damageMixin(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        BonusHelper.damageReflectionBonus(this.playerEntity, source, amount);
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && BonusHelper.evadingDamageBonus(this.playerEntity)) {
            cir.setReturnValue(false);
        }
    }

//    @Inject(method = "eatFood", at = @At(value = "HEAD"))
//    private void eatFoodMixin(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> info) {
//        BonusHelper.foodIncreasionBonus(this.playerEntity, stack);
//    }

    @Shadow
    public abstract ItemStack getWeaponItem();


    @Override
    public LevelManager getLevelManager() {
        return this.levelManager;
    }

    @Override
    public void increaseKilledMobStat(ChunkAccess chunk) {
        if (killedMobChunk != null && killedMobChunk == chunk) {
            killedMobsInChunk++;
        } else {
            killedMobChunk = chunk;
            killedMobsInChunk = 0;
        }
    }

    @Override
    public void resetKilledMobStat() {
        killedMobsInChunk = 0;
    }

    @Override
    public boolean allowMobDrop() {
        return killedMobsInChunk < ConfigInit.CONFIG.mobKillCount;
    }



@Override
protected void dropExperience(ServerLevel serverWorld, @Nullable Entity attacker) {
    System.out.println("dropExperience chamado: " + this.getType());
    if (this.shouldDropExperience() && serverWorld.getGameRules().get(GameRules.MOB_DROPS) && ConfigInit.CONFIG.resetCurrentXp) {
            LevelExperienceOrbEntity.spawn(serverWorld, this.position(), (int) (this.levelManager.getLevelProgress() * this.levelManager.getNextLevelExperience()));
        System.out.println("orb custom spawnada");
        }
        super.dropExperience(serverWorld,attacker);
    }

}