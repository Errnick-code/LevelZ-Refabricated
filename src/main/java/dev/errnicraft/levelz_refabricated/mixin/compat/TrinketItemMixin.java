package dev.errnicraft.levelz_refabricated.mixin.compat;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Совместимость с Trinkets — блокирует надевание предмета в тринкет-слот
 * через ПКМ, если у игрока нет нужного уровня для этого предмета.
 *
 * Проблема: Trinkets перехватывает Item#use и напрямую кладёт предмет
 * в инвентарь тринкета, минуя ScreenHandlerMixin. Этот миксин с более
 * низким приоритетом (900 < 1000) выполняется первым и отменяет use(),
 * до того как Trinkets успевает сделать надевание.
 */
@Mixin(value = Item.class, priority = 900)
public class TrinketItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void levelz$blockRestrictedTrinketEquip(
            Level world,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (player.isCreative()) {
            return;
        }

        // Проверка только на сервере, чтобы не дублировать логику на клиенте
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }

        LevelManager levelManager = ((LevelManagerAccess) serverPlayer).getLevelManager();

        if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "text.levelz.restriction",
                            Component.translatable(stack.getItem().getDescriptionId())
                    ).withStyle(ChatFormatting.RED),
                    true
            );
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
