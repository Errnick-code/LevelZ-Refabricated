package dev.errnicraft.levelz_refabricated.level.restriction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerRestriction {

    private final int id;
    private final Map<Integer, Integer> skillLevelRestrictions; // skillid, lvl
    private final int requiredOverallLevel; // 0 = no overall level requirement

    public PlayerRestriction(int id, Map<Integer, Integer> skillLevelRestrictions) {
        this(id, skillLevelRestrictions, 0);
    }

    public PlayerRestriction(int id, Map<Integer, Integer> skillLevelRestrictions, int requiredOverallLevel) {
        this.id = id;
        this.skillLevelRestrictions = skillLevelRestrictions;
        this.requiredOverallLevel = requiredOverallLevel;
    }

    public int getId() {
        return id;
    }

    public Map<Integer, Integer> getSkillLevelRestrictions() {
        return skillLevelRestrictions;
    }

    public int getRequiredOverallLevel() {
        return requiredOverallLevel;
    }
}
