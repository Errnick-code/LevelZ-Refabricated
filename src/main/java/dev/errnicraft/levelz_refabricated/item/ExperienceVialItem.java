package dev.errnicraft.levelz_refabricated.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.access.ServerPlayerSyncAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class ExperienceVialItem extends Item {

    public static final String STORED_XP_KEY = "StoredExperience";

    // CustomModelData flag index 0: true = filled texture, false/absent = empty texture
    private static final CustomModelData FILLED_MODEL   = new CustomModelData(List.of(), List.of(true),  List.of(), List.of());
    private static final CustomModelData EMPTY_MODEL    = new CustomModelData(List.of(), List.of(false), List.of(), List.of());

    public ExperienceVialItem(Properties properties) {
        super(properties);
    }

    // ── NBT helpers ────────────────────────────────────────────────────────────

    public static int getStoredXp(ItemStack stack) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return nbt.getIntOr(STORED_XP_KEY, 0);
    }

    public static void setStoredXp(ItemStack stack, int xp) {
        xp = Math.max(0, xp);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(STORED_XP_KEY, xp);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, xp > 0 ? FILLED_MODEL : EMPTY_MODEL);
    }

    public static boolean isFull(ItemStack stack) {
        return getStoredXp(stack) >= ConfigInit.CONFIG.vialMaxCapacity;
    }

    public static boolean isFilled(ItemStack stack) {
        return getStoredXp(stack) > 0;
    }

    // ── Use logic ──────────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        boolean sneaking = player.isShiftKeyDown();
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
        int storedXp  = getStoredXp(stack);
        int maxCap    = ConfigInit.CONFIG.vialMaxCapacity;
        int fillAmt   = ConfigInit.CONFIG.vialFillAmount;

        if (sneaking) {
            // Shift + RMB → fill vial from player XP
            int minLevel = ConfigInit.CONFIG.vialMinLevelToFill;
            if (levelManager.getOverallLevel() < minLevel) {
                player.displayClientMessage(
                    Component.translatable("item.levelz.experience_vial.need_level", minLevel)
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }

            if (isFull(stack)) {
                player.displayClientMessage(
                    Component.translatable("item.levelz.experience_vial.full")
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }

            int canStore = maxCap - storedXp;
            int toFill   = Math.min(fillAmt, canStore);
            if (toFill <= 0) {
                player.displayClientMessage(
                    Component.translatable("item.levelz.experience_vial.full")
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }

            // Cap by XP on current level — never let the level go down
            int nextLevelXp = levelManager.getNextLevelExperience();
            int xpOnCurrentLevel = (int)(levelManager.getLevelProgress() * nextLevelXp);
            toFill = Math.min(toFill, xpOnCurrentLevel);

            if (toFill <= 0) {
                player.displayClientMessage(
                    Component.translatable("item.levelz.experience_vial.no_xp")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }

            // addLevelExperience ignores negative values (Math.max(...,0) inside)
            // so we subtract directly from progress and totalXP
            float newProgress = levelManager.getLevelProgress() - (float) toFill / nextLevelXp;
            levelManager.setLevelProgress(Math.max(0.0f, newProgress));
            levelManager.setTotalLevelExperience(Math.max(0, levelManager.getTotalLevelExperience() - toFill));
            dev.errnicraft.levelz_refabricated.util.PacketHelper.updateLevels(player);

            if (stack.getCount() > 1) {
                // Split: one vial gets filled, rest stay empty
                ItemStack filled = stack.copyWithCount(1);
                setStoredXp(filled, storedXp + toFill);
                stack.shrink(1);
                if (!player.getInventory().add(filled)) {
                    player.drop(filled, false);
                }
            } else {
                setStoredXp(stack, storedXp + toFill);
                if (isFull(stack)) {
                    player.displayClientMessage(
                        Component.translatable("item.levelz.experience_vial.full")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
            }

        } else {
            // RMB → apply stored XP from ONE vial to player
            if (storedXp <= 0) {
                player.displayClientMessage(
                    Component.translatable("item.levelz.experience_vial.empty")
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }

            if (stack.getCount() > 1) {
                // Split stack: consume one filled vial, give its XP, leave rest unchanged
                ItemStack consumed = stack.copyWithCount(1);
                setStoredXp(consumed, 0);
                stack.shrink(1);
                // Give back the now-empty vial
                if (!player.getInventory().add(consumed)) {
                    player.drop(consumed, false);
                }
            } else {
                setStoredXp(stack, 0);
            }
            ((ServerPlayerSyncAccess) player).addLevelExperience(storedXp);
        }

        // Cooldown 6 ticks (0.3 sec) for all vial actions
        player.getCooldowns().addCooldown(stack, 6);
        return InteractionResult.SUCCESS;
    }

    // ── Tooltip ────────────────────────────────────────────────────────────────

    @Override
    public boolean isFoil(ItemStack stack) {
        return isFilled(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        int stored = getStoredXp(stack);
        int max    = ConfigInit.CONFIG.vialMaxCapacity;
        tooltip.accept(Component.literal(stored + " / " + max + " XP").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
