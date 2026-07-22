package dev.errnicraft.levelz_refabricated.mixin.misc;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingTableMixin extends ItemCombinerMenu {

    public SmithingTableMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    @Inject(method = "createResult()V", at = @At("TAIL"))
    private void blockRestrictedSmithingResultMixin(CallbackInfo info) {
        ItemStack result = this.resultSlots.getItem(0);
        if (result.isEmpty()) {
            return;
        }
        if (!((LevelManagerAccess) this.player).getLevelManager().hasRequiredCraftingLevel(result.getItem())) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        }
    }
}
