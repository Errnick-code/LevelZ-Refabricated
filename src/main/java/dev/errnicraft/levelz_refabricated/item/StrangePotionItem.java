package dev.errnicraft.levelz_refabricated.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class StrangePotionItem extends Item {

    public StrangePotionItem(Properties settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!world.isClientSide() && user instanceof ServerPlayer playerEntity) {
            CriteriaTriggers.CONSUME_ITEM.trigger(playerEntity, stack);

            LevelManager levelManager = ((LevelManagerAccess) playerEntity).getLevelManager();
            List<Integer> list = new ArrayList<>(levelManager.getPlayerSkills().keySet());
            Collections.shuffle(list);

            for (int skillId : list) {
                if (levelManager.resetSkill(skillId) && !ConfigInit.CONFIG.opStrangePotion) {
                    break;
                }
            }
            PacketHelper.updatePlayerSkills(playerEntity, null);

            if (!playerEntity.isCreative()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
                playerEntity.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }

            user.gameEvent(GameEvent.DRINK);
            user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
    }

}