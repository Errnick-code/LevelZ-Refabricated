package dev.errnicraft.levelz_refabricated.mixin.misc;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.RestrictionHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {
    @Shadow
    private ItemStack carried;

    @Shadow
    @Final
    @Mutable
    public NonNullList<Slot> slots = NonNullList.create();


//    @Unique
//    private PlayerEntity player_level_skills$currentPlayer;
//
//    @Inject(method = "onSlotClick", at = @At("HEAD"))
//    private void player_level_skills$capturePlayer(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
//        this.player_level_skills$currentPlayer = player;
//    }
//
//    @Inject(method = "canInsertIntoSlot(Lnet/minecraft/item/ItemStack;Lnet/minecraft/screen/slot/Slot;)Z", at = @At("HEAD"), cancellable = true)
//    private void player_level_skills$canInsertIntoSlot(ItemStack stack, Slot slot, CallbackInfoReturnable<Boolean> cir) {
//        if (stack.isEmpty() || slot == null) {
//            return;
//        }
//
//        if (!(slot.getClass().getName().startsWith("net.minecraft.screen.BrewingStandScreenHandler$"))) {
//            return;
//        }
//
//        PlayerEntity player = this.player_level_skills$currentPlayer;
//        if (player == null || player.isCreative()) {
//            return;
//        }
//
//        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
//        if (!levelManager.hasRequiredCraftingLevel(stack.getItem())) {
//            cir.setReturnValue(false);
//        }
//    }

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void internalOnSlotClickMixin(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo info) {
        if (player.isCreative()) {
            return;
        }
        if (slotIndex >= 0 && slotIndex != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE && RestrictionHelper.restrictSlotClick(player, actionType, this.carried, this.slots.get(slotIndex), (AbstractContainerMenu) (Object) this)) {
            info.cancel();
        }
    }

}
