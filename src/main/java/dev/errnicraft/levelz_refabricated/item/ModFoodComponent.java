package dev.errnicraft.levelz_refabricated.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

public class ModFoodComponent {

    public static final FoodProperties RARE_CANDY = new FoodProperties.Builder().nutrition(2).saturationModifier(1.0f).build();

    public static final Consumable RARE_CANDY_EFFECT = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 200), 0.95f)).build();

    public static final FoodProperties STRANGE_POTION = new FoodProperties.Builder().nutrition(0).saturationModifier(0.0f).alwaysEdible().build();

    public static final Consumable STRANGE_POTION_EFFECT = Consumables.defaultDrink()
            .consumeSeconds(4.0f)
            .animation(ItemUseAnimation.DRINK)
            .sound(SoundEvents.GENERIC_DRINK)
            .hasConsumeParticles(false)
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200,0), 1.0f)).build();
}
