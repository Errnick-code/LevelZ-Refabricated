package dev.errnicraft.levelz_refabricated.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PlayerSkill {

    private final int id;
    private int level;

    public PlayerSkill(int id, int level) {
        this.id = id;
        this.level = Math.max(0, level);
    }

    public PlayerSkill(CompoundTag nbt) {
        this.id = nbt.getInt("Id").orElse(0);
        this.level = nbt.getInt("Level").orElse(0);

        if (this.level < 0) {
            this.level = 0;
        }
    }

    public static PlayerSkill readDataFromView(ValueInput skillView) {
        if (skillView == null) {
            return null;
        }

        int id = skillView.getIntOr("Id", 0);
        int level = skillView.getIntOr("Level", 0);

        return new PlayerSkill(id, Math.max(0, level));
    }

    public void writeDataToNbt(ValueOutput skillView) {
        skillView.putInt("Id", this.id);
        skillView.putInt("Level", this.level);
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    public void increaseLevel(int level) {
        Skill skill = LevelManager.SKILLS.get(this.id);
        if (skill == null) {
            this.level = Math.max(0, this.level + level);
            return;
        }

        int maxLevel = skill.getMaxLevel();
        if ((this.level + level) <= maxLevel) {
            this.level += level;
        } else {
            this.level = maxLevel;
        }

        if (this.level < 0) {
            this.level = 0;
        }
    }

    public void decreaseLevel(int level) {
        if ((this.level - level) >= 0) {
            this.level -= level;
        } else {
            this.level = 0;
        }
    }
}