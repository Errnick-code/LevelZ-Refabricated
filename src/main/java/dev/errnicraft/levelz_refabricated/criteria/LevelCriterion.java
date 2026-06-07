package dev.errnicraft.levelz_refabricated.criteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import java.util.Optional;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class LevelCriterion extends SimpleCriterionTrigger<LevelCriterion.Conditions> {

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, int level) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Conditions> CODEC = RecordCodecBuilder
                .create(instance -> instance
                        .group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                                Codec.INT.fieldOf("level").forGetter(Conditions::level))
                        .apply(instance, Conditions::new));

        public boolean matches(ServerPlayer player) {
            return ((LevelManagerAccess) player).getLevelManager().getOverallLevel() == this.level;
        }

    }

}
