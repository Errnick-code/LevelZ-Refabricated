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
import net.minecraft.world.level.ChunkPos;
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
import java.util.HashMap;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements LevelManagerAccess, PlayerDropAccess {

    private final Player playerEntity = (Player) (Object) this;
    @Unique
    private final LevelManager levelManager = new LevelManager(playerEntity);

    // Long (ChunkPos.toLong) -> [killCount, lastKillTime (ms), decayStartTime (ms)]
    // decayStartTime = -1 означает что серия ещё идёт (основной таймер не запущен)
    // Статическая — счётчик общий для всех игроков в чанке, не per-player
    @Unique
    private static final Map<Long, long[]> chunkKillData = new HashMap<>();

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
        long key = chunk.getPos().toLong();
        long now = System.currentTimeMillis();
        // [killCount, lastKillTime, decayStartTime (-1 = серия ещё идёт)]
        long[] data = chunkKillData.computeIfAbsent(key, k -> new long[]{0L, now, -1L});

        // Не увеличиваем счётчик сверх лимита — 80 убийств = лимит, 81-го не существует
        int limit = ConfigInit.CONFIG.mobKillCount;
        if (limit > 0 && data[0] < limit) {
            data[0]++;
        }
        data[1] = now; // обновляем время последнего убийства
        data[2] = -1L; // серия возобновилась — останавливаем основной таймер сброса
    }

    @Override
    public void resetKilledMobStat() {
        chunkKillData.clear();
    }

    @Override
    public boolean allowMobDrop() {
        // Вызывается для текущего чанка — нужно найти чанк моба.
        // allowMobDrop проверяется из LivingEntityMixin, который передаёт chunk через increaseKilledMobStat.
        // Здесь для совместимости проверяем: есть ли хоть один чанк сверх лимита.
        // Реальная проверка по конкретному чанку — в allowMobDropInChunk.
        return true; // базовый метод не используется напрямую
    }

    @Override
    public boolean allowMobDropInChunk(ChunkAccess chunk) {
        if (ConfigInit.CONFIG.mobKillCount <= 0) return true;
        long key = chunk.getPos().toLong();
        long[] data = chunkKillData.get(key);
        if (data == null) return true;
        applyDecay(key, data);
        if (data[0] <= 0) return true;
        return data[0] < ConfigInit.CONFIG.mobKillCount;
    }

    @Unique
    private void applyDecay(long key, long[] data) {
        int seriesSeconds = ConfigInit.CONFIG.mobKillSeriesSeconds;
        int decaySeconds = ConfigInit.CONFIG.mobKillDecaySeconds;
        int decayAmount = ConfigInit.CONFIG.mobKillDecayAmount;
        if (decaySeconds <= 0 || decayAmount <= 0) return;

        long now = System.currentTimeMillis();
        long seriesMs = seriesSeconds * 1000L;
        long decayMs = decaySeconds * 1000L;

        if (data[2] == -1L) {
            // Серия ещё идёт — проверяем не истёк ли таймер серии
            long sinceLastKill = now - data[1];
            if (sinceLastKill < seriesMs) {
                return; // серия ещё активна — ничего не делаем
            }
            // Серия закончилась — запускаем основной таймер с момента окончания серии
            data[2] = data[1] + seriesMs;
        }

        // Считаем сколько циклов сброса прошло с data[2]
        long elapsed = now - data[2];
        long cycles = elapsed / decayMs;
        if (cycles > 0) {
            data[0] = Math.max(0L, data[0] - cycles * decayAmount);
            data[2] += cycles * decayMs; // сдвигаем базу
            if (data[0] <= 0) {
                chunkKillData.remove(key);
            }
        }
    }

    @Override
    public void tickChunkKillDecay() {
        if (chunkKillData.isEmpty()) return;
        // Раз в секунду (20 тиков) прогоняем decay по всем активным чанкам
        if (((Entity)(Object)this).tickCount % 20 != 0) return;
        // Копируем ключи чтобы безопасно удалять из map внутри applyDecay
        for (Long key : new java.util.ArrayList<>(chunkKillData.keySet())) {
            long[] data = chunkKillData.get(key);
            if (data != null) applyDecay(key, data);
        }
    }

    @Override
    public long[] getChunkKillStatus(ChunkAccess chunk) {
        int limit = ConfigInit.CONFIG.mobKillCount;
        long key = chunk.getPos().toLong();
        long[] data = chunkKillData.get(key);

        // Нет данных по чанку — килов не было
        if (data == null) {
            return new long[]{0L, limit, -1L, 0L};
        }

        applyDecay(key, data);
        data = chunkKillData.get(key);
        if (data == null) {
            return new long[]{0L, limit, -1L, 0L};
        }

        int decaySeconds = ConfigInit.CONFIG.mobKillDecaySeconds;
        int decayAmount = ConfigInit.CONFIG.mobKillDecayAmount;
        int seriesSeconds = ConfigInit.CONFIG.mobKillSeriesSeconds;

        long secondsUntilNextDecay = -1L;
        if (decaySeconds > 0 && decayAmount > 0) {
            long now = System.currentTimeMillis();
            long seriesMs = seriesSeconds * 1000L;
            long decayMs = decaySeconds * 1000L;

            if (data[2] == -1L) {
                // Серия ещё идёт — до старта основного таймера остаётся (seriesMs - прошедшее)
                long sinceLastKill = now - data[1];
                long remainingSeries = seriesMs - sinceLastKill;
                secondsUntilNextDecay = (remainingSeries > 0 ? remainingSeries : 0L) / 1000L + decaySeconds;
            } else {
                long elapsed = now - data[2];
                long remaining = decayMs - (elapsed % decayMs);
                secondsUntilNextDecay = remaining / 1000L;
            }
        }

        return new long[]{data[0], limit, secondsUntilNextDecay, decayAmount};
    }
}
