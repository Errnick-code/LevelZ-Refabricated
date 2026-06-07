package dev.errnicraft.levelz_refabricated.util;

//import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.loader.api.FabricLoader;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class RestrictionHelper {

    private static final boolean isAccessoriesLoaded = FabricLoader.getInstance().isModLoaded("accessories");

    private static boolean hasRestrictedEnchantment(LevelManager levelManager, ItemStack stack) {
        ItemEnchantments itemEnchantmentsComponent = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantmentsComponent.entrySet()) {
            if (!levelManager.hasRequiredEnchantmentLevel(entry.getKey(), entry.getIntValue())) {
                return true;
            }
        }

        return false;
    }

    public static boolean restrictSlotClick(Player playerEntity, ClickType actionType, ItemStack cursorStack, Slot slot, AbstractContainerMenu screenHandler) {
        if (!playerEntity.isCreative()) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            if (actionType.equals(ClickType.QUICK_MOVE)) {

                //Brewing - Quick
                if (screenHandler instanceof BrewingStandMenu) {
                    return !slot.getItem().isEmpty() && !levelManager.hasRequiredCraftingLevel(slot.getItem().getItem()) || !levelManager.hasRequiredPotionLevel(slot.getItem());
                }

                //Crafting - Quick
                if (screenHandler instanceof CraftingMenu craftingScreenHandler) {
                    Slot outputSlot = craftingScreenHandler.getResultSlot();

                    // Bloqueia só a retirada do resultado, mas não o preview.
                    if (slot == outputSlot) {
                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty()) {
                            return !levelManager.hasRequiredCraftingLevel(stack.getItem());
                        }
                    }
                }
                //Anvil - Quick
                if (screenHandler instanceof AnvilMenu anvilScreenHandler) {
                    Slot outputSlot = anvilScreenHandler.getSlot(2);

                    if (slot == outputSlot) {
                        return !levelManager.hasRequiredCraftingLevel(slot.getItem().getItem()) || hasRestrictedEnchantment(levelManager, slot.getItem());
                    }
                    return false;
                }
                //Merchant - Quick
                if (screenHandler instanceof MerchantMenu merchantScreenHandler) {
                    Slot outputSlot = merchantScreenHandler.getSlot(2);

                    if (slot == outputSlot) {
                        return hasRestrictedEnchantment(levelManager, slot.getItem());
                    }

                    return false;
                }
                //Smithing - Quick
                if (screenHandler instanceof SmithingMenu smithingScreenHandler) {
                    Slot outputSlot = smithingScreenHandler.getSlot(3);
                    if (slot == outputSlot) {
                        return !levelManager.hasRequiredCraftingLevel(slot.getItem().getItem());
                    }
                }

                return !slot.getItem().isEmpty() && !levelManager.hasRequiredItemLevel(slot.getItem().getItem());


            }
            // Bloqueio específico para Brewing Stand (Mouse)
            if (screenHandler instanceof BrewingStandMenu) {
                // Se estiver tentando COLOCAR algo (cursorStack) ou TIRAR algo (slot.getStack)
                ItemStack targetStack = cursorStack.isEmpty() ? slot.getItem() : cursorStack;
                if (!targetStack.isEmpty()) {
                    return !levelManager.hasRequiredCraftingLevel(targetStack.getItem()) ||
                            !levelManager.hasRequiredPotionLevel(targetStack);
                }
            }
            //Crafting mouse
            if (screenHandler instanceof CraftingMenu craftingScreenHandler) {
                Slot outputSlot = craftingScreenHandler.getResultSlot();

                // Bloqueia só a retirada do resultado, mas não o preview.
                if (slot == outputSlot) {
                    ItemStack stack = slot.getItem();
                    if (!stack.isEmpty()) {
                        return !levelManager.hasRequiredCraftingLevel(stack.getItem());
                    }
                }
            }
            //Smithing mouse
            if (screenHandler instanceof SmithingMenu smithingScreenHandler) {
                Slot outputSlot = smithingScreenHandler.getSlot(3);

                if (slot == outputSlot && !slot.getItem().isEmpty()) {
                    return !levelManager.hasRequiredCraftingLevel(slot.getItem().getItem());
                }
            }
            //Anvil mouse
            if (screenHandler instanceof AnvilMenu anvilScreenHandler) {
                Slot outputSlot = anvilScreenHandler.getSlot(2);

                if (slot == outputSlot) {
                    return !levelManager.hasRequiredCraftingLevel(slot.getItem().getItem()) || hasRestrictedEnchantment(levelManager, slot.getItem());
                }

                return false;
            }
            //Merchant - mouse
            if (screenHandler instanceof MerchantMenu merchantScreenHandler) {
                Slot outputSlot = merchantScreenHandler.getSlot(2);

                if (slot == outputSlot) {
                    return hasRestrictedEnchantment(levelManager, slot.getItem());
                }

                return false;
            }


            else if (!cursorStack.isEmpty()) {
                boolean isNonNormalSlot = !slot.getClass().equals(Slot.class);

                if (!levelManager.hasRequiredItemLevel(cursorStack.getItem())) {
                    if (screenHandler instanceof InventoryMenu) {
                        if (isNonNormalSlot) {
                            return true;
                        }
//                    } else if (isAccessoriesLoaded && screenHandler instanceof AccessoriesMenuBase) {
//                        if (isNonNormalSlot) {
//                            return true;
//                        }
                    }
                }
                if (!levelManager.hasRequiredCraftingLevel(cursorStack.getItem()) && isNonNormalSlot) {
                    return true;
                }
            }
        }

        return false;
    }
}
