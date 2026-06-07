package dev.errnicraft.levelz_refabricated.criteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import java.util.Optional;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class SkillCriterion extends SimpleCriterionTrigger<SkillCriterion.Conditions> {

    @Override
    public Codec<SkillCriterion.Conditions> codec() {
        return SkillCriterion.Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, String skillName, int skillLevel) {
        this.trigger(player, conditions -> conditions.matches(player, skillName, skillLevel));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, String skillName, int skillLevel) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<SkillCriterion.Conditions> CODEC = RecordCodecBuilder
                .create(instance -> instance
                        .group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SkillCriterion.Conditions::player),
                                Codec.STRING.fieldOf("skill_name").forGetter(SkillCriterion.Conditions::skillName), Codec.INT.fieldOf("skill_level").forGetter(SkillCriterion.Conditions::skillLevel))
                        .apply(instance, SkillCriterion.Conditions::new));

        public boolean matches(ServerPlayer player, String skillName, int skillLevel) {
            if (!skillName.equals(this.skillName)) {
                return false;
            }
            return skillLevel == this.skillLevel;
        }
    }

}