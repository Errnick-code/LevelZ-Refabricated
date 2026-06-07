package dev.errnicraft.levelz_refabricated.mixin.entity;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public class FishingBobberEntityMixin {

    @Inject(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", ordinal = 0))
    private void useMixin(ItemStack usedItem, CallbackInfoReturnable<Integer> info) {
        if (ConfigInit.CONFIG.fishingXPMultiplier > 0.0F) {
            LevelExperienceOrbEntity.spawn((ServerLevel) getPlayerOwner().level(), getPlayerOwner().position().add(0.0D, 0.5D, 0.0D),
                    (int) ((getPlayerOwner().level().getRandom().nextInt(6) + 1) * ConfigInit.CONFIG.fishingXPMultiplier
                            * (ConfigInit.CONFIG.dropXPbasedOnLvl && getPlayerOwner() != null
                            ? 1.0F + ConfigInit.CONFIG.basedOnMultiplier * ((LevelManagerAccess) getPlayerOwner()).getLevelManager().getOverallLevel()
                            : 1.0F)));
        }
    }

    @Shadow
    @Nullable
    public Player getPlayerOwner() {
        return null;
    }
}
