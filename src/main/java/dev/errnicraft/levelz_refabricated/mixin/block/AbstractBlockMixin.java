package dev.errnicraft.levelz_refabricated.mixin.block;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockBehaviour.class)
public class AbstractBlockMixin {

    @Inject(method = "onExplosionHit", at = @At("HEAD"), cancellable = true)
    private void onExplodedMixin(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger, CallbackInfo ci) {
        if (explosion.getIndirectSourceEntity() instanceof Player playerEntity
                && !playerEntity.isCreative()
                && !((LevelManagerAccess) playerEntity).getLevelManager().hasRequiredMiningLevel(state.getBlock())) {
            ci.cancel();
        }
    }
}