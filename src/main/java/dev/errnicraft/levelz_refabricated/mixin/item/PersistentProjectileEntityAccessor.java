package dev.errnicraft.levelz_refabricated.mixin.item;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface PersistentProjectileEntityAccessor {
    @Accessor("baseDamage")
    double getDamage();
}

