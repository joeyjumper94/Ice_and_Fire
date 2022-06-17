package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.JumpGoal;
import net.minecraft.fluid.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class SeaSerpentAIJump extends JumpGoal {

    private static final int[] JUMP_DISTANCES = new int[] {
        0, 2, 4, 5, 6, 7
    };
    private final EntitySeaSerpent serpent;
    private final int chance;
    private boolean inWater;

    public SeaSerpentAIJump(EntitySeaSerpent dolphin, int chance) {
        this.serpent = dolphin;
        this.chance = chance;
    }

    @Override
    public boolean shouldExecute() {
        if (this.serpent.getRNG().nextInt(this.chance) != 0 || serpent.getAttackTarget() != null
            || serpent.jumpCooldown != 0) {
            return false;
        } else {
            Direction direction = this.serpent.getAdjustedHorizontalFacing();
            final int i = direction.getXOffset();
            final int j = direction.getZOffset();
            BlockPos blockpos = this.serpent.getPosition();
            for (int k : JUMP_DISTANCES) {
                if (!this.canJumpTo(blockpos, i, j, k) || !this.isAirAbove(blockpos, i, j, k)) { return false; }
            }
            return true;
        }
    }

    private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
        BlockPos blockpos = pos.add(dx * scale, 0, dz * scale);
        return this.serpent.world.getFluidState(blockpos).isTagged(FluidTags.WATER)
            && !this.serpent.world.getBlockState(blockpos).getMaterial().blocksMovement();
    }

    private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
        return this.serpent.world.getBlockState(pos.add(dx * scale, 1, dz * scale)).isAir()
            && this.serpent.world.getBlockState(pos.add(dx * scale, 2, dz * scale)).isAir();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting() {
        double d0 = this.serpent.getMotion().y;
        return serpent.jumpCooldown > 0 && (d0 * d0 >= 0.03F || this.serpent.rotationPitch == 0.0F
            || Math.abs(this.serpent.rotationPitch) >= 10.0F || !this.serpent.isInWater())
            && !this.serpent.isOnGround();
    }

    @Override
    public boolean isPreemptible() {
        return false;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void startExecuting() {
        Direction direction = this.serpent.getAdjustedHorizontalFacing();
        final float up = 1F + serpent.getRNG().nextFloat() * 0.8F;
        this.serpent
            .setMotion(this.serpent.getMotion().add(direction.getXOffset() * 0.6D, up, direction.getZOffset() * 0.6D));
        this.serpent.setJumpingOutOfWater(true);
        this.serpent.getNavigator().clearPath();
        this.serpent.jumpCooldown = serpent.getRNG().nextInt(100) + 100;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by
     * another one
     */
    @Override
    public void resetTask() {
        this.serpent.setJumpingOutOfWater(false);
        this.serpent.rotationPitch = 0.0F;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        final boolean flag = this.inWater;
        if (!flag) {
            FluidState fluidstate = this.serpent.world.getFluidState(this.serpent.getPosition());
            this.inWater = fluidstate.isTagged(FluidTags.WATER);
        }

        if (this.inWater && !flag) {
            this.serpent.playSound(SoundEvents.ENTITY_DOLPHIN_JUMP, 1.0F, 1.0F);
        }

        Vector3d vector3d = this.serpent.getMotion();
        if (vector3d.y * vector3d.y < 0.1F && this.serpent.rotationPitch != 0.0F) {
            this.serpent.rotationPitch = MathHelper.rotLerp(this.serpent.rotationPitch, 0.0F, 0.2F);
        } else {
            final double d0 = Math.sqrt(Entity.horizontalMag(vector3d));
            final double d1 = Math.signum(-vector3d.y) * Math.acos(d0 / vector3d.length()) * (180F / (float) Math.PI);
            this.serpent.rotationPitch = (float) d1;
        }

    }
}
