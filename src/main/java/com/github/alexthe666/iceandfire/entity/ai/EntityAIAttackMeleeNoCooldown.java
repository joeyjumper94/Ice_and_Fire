package com.github.alexthe666.iceandfire.entity.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;

public class EntityAIAttackMeleeNoCooldown extends MeleeAttackGoal {
    public EntityAIAttackMeleeNoCooldown(CreatureEntity creature, double speed, boolean memory) {
        super(creature, speed, memory);
    }

    public void tick() {
        // TODO: investigate why the goal is even running when the attack target is null
        // Probably has something to do with the goal switching
        if (this.attacker.getAttackTarget() != null)
            super.tick();
    }
}
