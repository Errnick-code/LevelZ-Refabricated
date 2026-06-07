package dev.errnicraft.levelz_refabricated.mixin.misc;

import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin extends ItemCombinerMenu {

    @Shadow
    @Mutable
    @Final
    private DataSlot cost;

    public AnvilScreenHandlerMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }


    @Inject(
            method = "createResult()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V",
                    ordinal = 3
            )
    )
    private void updateResultMixin(CallbackInfo info) {
        ItemStack secondSlot = this.inputSlots.getItem(1);
        boolean isEmpty = secondSlot.isEmpty();

        if (this.cost.get() > 1) {
            this.cost.set(BonusHelper.anvilXpDiscountBonus(this.player, this.cost.get(), isEmpty));
        }
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int modifyAnvilLevelLimit(int constant) {
        if (BonusHelper.anvilXpCapBonus(this.player)) {
            return Integer.MAX_VALUE;
        }
        return constant;
    }

    @Inject(
            method = "onTake",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V"
            ),
            require = 0
    )
    private void onTakeOutputMixin(Player playerEntity, ItemStack stack, CallbackInfo ci) {
        if (BonusHelper.anvilXpChanceBonus(playerEntity)) {
            this.cost.set(0);
        }
    }

    @Inject(method = "getCost", at = @At(value = "HEAD"), cancellable = true)
    public void getLevelCostMixin(CallbackInfoReturnable<Integer> info) {
        ItemStack secondSlot = this.inputSlots.getItem(1);
        boolean isEmpty = secondSlot.isEmpty();
        int levelCost = BonusHelper.anvilXpDiscountBonus(this.player, this.cost.get(), isEmpty);
        if (levelCost != this.cost.get()) {
            info.setReturnValue(levelCost);
        }
    }
}