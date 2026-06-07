package dev.errnicraft.levelz_refabricated.mixin.entity;

import dev.errnicraft.levelz_refabricated.access.MobEntityAccess;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin implements MobEntityAccess {

    @Unique
    private boolean spawnerMob = false;

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCustomData(ValueInput view, CallbackInfo info) {
        this.spawnerMob = view.getBooleanOr("SpawnerMob",false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCustomData(ValueOutput view, CallbackInfo info) {
        view.putBoolean("SpawnerMob", this.spawnerMob);
    }

    @Override
    public void setSpawnerMob(boolean spawnerMob) {
        this.spawnerMob = spawnerMob;
    }

    @Override
    public boolean isSpawnerMob() {
        return this.spawnerMob;
    }
}