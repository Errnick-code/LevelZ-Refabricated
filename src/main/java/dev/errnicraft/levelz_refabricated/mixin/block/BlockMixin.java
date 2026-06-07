package dev.errnicraft.levelz_refabricated.mixin.block;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.init.EntityInit;
import dev.errnicraft.levelz_refabricated.init.TagInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Shadow
    private BlockState defaultBlockState;

    @Unique
    @Nullable
    private ServerPlayer serverPlayerEntity = null;

    @Inject(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"
            ),
            cancellable = true
    )
    private static void dropStacksMixin(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity blockEntity, Entity entity, ItemStack stack, CallbackInfo info) {
        if (entity instanceof Player playerEntity) {
            if (playerEntity.isCreative()) {
                return;
            }
            if (EntityInit.isRedstoneBitsLoaded && entity.getClass().getName().contains("RedstoneBitsFakePlayer")) {
                // allow fake player through
            } else {
                LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
                if (!levelManager.hasRequiredMiningLevel(state.getBlock())) {
                    info.cancel();
                } else if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
                    info.cancel();
                }
            }
        }
    }

    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void getDroppedStacksMixin(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> info, LootParams.Builder builder) {
        if (entity instanceof Player playerEntity) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();

            boolean hasForbiddenEnchantment = false;
            for (var entry : stack.getEnchantments().entrySet()) {
                if (!levelManager.hasRequiredEnchantmentLevel(entry.getKey(), entry.getIntValue())) {
                    hasForbiddenEnchantment = true;
                    break;
                }
            }

            if (hasForbiddenEnchantment) {
                builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY);
            }

            BonusHelper.miningDropChanceBonus(playerEntity, state, pos, builder);
        }
    }

    @Inject(
            method = "popExperience",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"
            )
    )
    protected void dropExperienceMixin(ServerLevel world, BlockPos pos, int size, CallbackInfo info) {
        if (ConfigInit.CONFIG.oreXPMultiplier > 0.0F && !this.defaultBlockState.is(TagInit.RESTRICTED_ORE_EXPERIENCE_BLOCKS)) {
            LevelExperienceOrbEntity.spawn(world, Vec3.atCenterOf(pos),
                    (int) (size * ConfigInit.CONFIG.oreXPMultiplier
                            * (ConfigInit.CONFIG.dropXPbasedOnLvl && this.serverPlayerEntity != null
                            ? 1.0F + ConfigInit.CONFIG.basedOnMultiplier * ((LevelManagerAccess) this.serverPlayerEntity).getLevelManager().getOverallLevel()
                            : 1.0F)));
        }
    }

    @Inject(
            method = "playerWillDestroy",
            at = @At(value = "HEAD")
    )
    private void onBreakMixin(Level world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> info) {
        if (!world.isClientSide()) {
            this.serverPlayerEntity = (ServerPlayer) player;
        }
    }
}