package dev.errnicraft.levelz_refabricated.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.errnicraft.levelz_refabricated.item.ExperienceVialItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class SetVialXpFunction extends LootItemConditionalFunction {

    // Populated by LootInit.init() before any loot tables are loaded
    public static LootItemFunctionType<SetVialXpFunction> TYPE;

    public static final MapCodec<SetVialXpFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance).and(instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("min_xp").forGetter(f -> f.minXp),
            com.mojang.serialization.Codec.INT.fieldOf("max_xp").forGetter(f -> f.maxXp),
            com.mojang.serialization.Codec.INT.fieldOf("max_capacity").forGetter(f -> f.maxCapacity)
        )).apply(instance, SetVialXpFunction::new)
    );

    private final int minXp;
    private final int maxXp;
    private final int maxCapacity;

    // Full constructor used by codec
    public SetVialXpFunction(List<LootItemCondition> conditions, int minXp, int maxXp, int maxCapacity) {
        super(conditions);
        this.minXp = minXp;
        this.maxXp = maxXp;
        this.maxCapacity = maxCapacity;
    }

    // Convenience constructor for programmatic use (no conditions)
    public SetVialXpFunction(int minXp, int maxXp, int maxCapacity) {
        this(List.of(), minXp, maxXp, maxCapacity);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        int xp;
        if (minXp >= maxXp) {
            xp = Math.min(minXp, maxCapacity);
        } else {
            xp = context.getRandom().nextIntBetweenInclusive(minXp, maxXp);
            xp = Math.min(xp, maxCapacity);
        }
        dev.errnicraft.levelz_refabricated.LevelZRefabricated.LOGGER.info(
            "[SetVialXpFunction] run() called! min={} max={} cap={} -> xp={}", minXp, maxXp, maxCapacity, xp);
        ExperienceVialItem.setStoredXp(stack, xp);
        return stack;
    }

    @Override
    public LootItemFunctionType<SetVialXpFunction> getType() {
        return TYPE;
    }

    // Builder shorthand for LootItem.apply(...)
    public static LootItemFunction.Builder builder(int minXp, int maxXp, int maxCapacity) {
        return () -> new SetVialXpFunction(minXp, maxXp, maxCapacity);
    }
}
