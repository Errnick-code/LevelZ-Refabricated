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
    private static final boolean isTrinketsLoaded = FabricLoader.getInstance().isModLoaded("trinkets");
    private static final boolean isTravelersBackpackLoaded = FabricLoader.getInstance().isModLoaded("travelersbackpack");

    /**
     * Слоты инвентаря рюкзака (Traveler's Backpack) — это просто хранилище,
     * item-ограничение на использование не должно блокировать туда вставку.
     */
    private static boolean isBackpackStorageSlot(Slot slot) {
        if (!isTravelersBackpackLoaded) return false;
        String className = slot.getClass().getName();
        return className.equals("com.tiviacz.travelersbackpack.inventory.menu.slot.BackpackSlotItemHandler")
                || className.equals("com.tiviacz.travelersbackpack.inventory.menu.slot.ToolSlotItemHandler")
                || className.equals("com.tiviacz.travelersbackpack.inventory.menu.slot.SlotItemHandler");
    }

    private static boolean hasRestrictedEnchantment(LevelManager levelManager, ItemStack stack) {
        ItemEnchantments itemEnchantmentsComponent = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantmentsComponent.entrySet()) {
            if (!levelManager.hasRequiredEnchantmentLevel(entry.getKey(), entry.getIntValue())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Проверяет, является ли слот тринкет-слотом (SurvivalTrinketSlot).
     * Используем проверку по имени класса, так как Trinkets — необязательная зависимость
     * и недоступен при компиляции.
     */
    private static boolean isTrinketSlot(Slot slot) {
        if (!isTrinketsLoaded) return false;
        String className = slot.getClass().getName();
        return className.equals("eu.pb4.trinkets.impl.SurvivalTrinketSlot")
                || className.startsWith("eu.pb4.trinkets.");
    }

    public static boolean restrictSlotClick(Player playerEntity, ClickType actionType, ItemStack cursorStack, Slot slot, AbstractContainerMenu screenHandler) {
        if (!playerEntity.isCreative()) {
            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            if (actionType.equals(ClickType.QUICK_MOVE)) {

                //Brewing - Quick
                if (screenHandler instanceof BrewingStandMenu) {
                    return !slot.getItem().isEmpty() && !levelManager.hasRequiredCraftingLevel(slot.getItem().getItem()) || !levelManager.hasRequiredPotionLevel(slot.getItem());
                }

                //Crafting - Quick (верстак 3x3)
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
                //Inventory crafting - Quick (сетка 2x2 в инвентаре)
                if (screenHandler instanceof InventoryMenu inventoryMenu) {
                    Slot outputSlot = inventoryMenu.getResultSlot();

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

                // Shift+клик из инвентаря в слот брони / тринкет-слот:
                // блокируем только если есть item-ограничение, но НЕ если только crafting-ограничение
                if (!slot.getItem().isEmpty()) {
                    ItemStack slotStack = slot.getItem();
                    boolean isNonNormalSlot = !slot.getClass().equals(Slot.class);
                    boolean isTrinket = isTrinketSlot(slot);
                    boolean isEquipSlot = screenHandler instanceof InventoryMenu && isNonNormalSlot;

                    // Слоты рюкзака — просто хранилище, item-блокировка не применяется
                    if (isBackpackStorageSlot(slot)) {
                        return false;
                    }

                    if (isEquipSlot || isTrinket) {
                        // Слот экипировки/тринкета — проверяем только item-блокировку
                        return !levelManager.hasRequiredItemLevel(slotStack.getItem());
                    }
                    // Обычный слот инвентаря — старая логика
                    return !levelManager.hasRequiredItemLevel(slotStack.getItem());
                }
                return false;


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
            //Crafting mouse (верстак 3x3)
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
            //Inventory crafting mouse (сетка 2x2 в инвентаре)
            if (screenHandler instanceof InventoryMenu inventoryMenu) {
                Slot outputSlot = inventoryMenu.getResultSlot();

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

                // Слоты рюкзака — просто хранилище, ни item- ни crafting-блокировка не применяется
                if (isBackpackStorageSlot(slot)) {
                    return false;
                }

                if (!levelManager.hasRequiredItemLevel(cursorStack.getItem())) {
                    if (screenHandler instanceof InventoryMenu) {
                        if (isNonNormalSlot) {
                            return true;
                        }
                    }
                    // Блокировка тринкет-слотов: проверяем по имени класса,
                    // чтобы не зависеть от Trinkets при компиляции
                    if (isTrinketSlot(slot)) {
                        return true;
                    }
                }
                // crafting restriction НЕ должна блокировать надевание в слоты брони/тринкетов —
                // только item restriction это контролирует
                if (!levelManager.hasRequiredCraftingLevel(cursorStack.getItem())) {
                    boolean isNonEquipSlot = !(screenHandler instanceof InventoryMenu && isNonNormalSlot)
                            && !isTrinketSlot(slot);
                    if (isNonEquipSlot && isNonNormalSlot) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}