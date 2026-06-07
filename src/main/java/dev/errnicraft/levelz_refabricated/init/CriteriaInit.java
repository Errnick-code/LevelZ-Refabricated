package dev.errnicraft.levelz_refabricated.init;

import dev.errnicraft.levelz_refabricated.criteria.LevelCriterion;
import dev.errnicraft.levelz_refabricated.criteria.SkillCriterion;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class CriteriaInit {

    public static final ObjectiveCriteria LEVELZ = ObjectiveCriteria.registerCustom("levelz");

    public static final LevelCriterion LEVEL_UP = CriteriaTriggers.register("levelz:level", new LevelCriterion());
    public static final SkillCriterion SKILL_UP = CriteriaTriggers.register("levelz:skill",new SkillCriterion());

    public static void init() {
    }

}
