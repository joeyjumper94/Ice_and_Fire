package com.github.alexthe666.iceandfire.entity.ai;

import java.util.EnumSet;

import com.github.alexthe666.iceandfire.entity.EntityGorgon;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

public class GorgonAIStareAttack extends Goal {
    private final EntityGorgon entity;
    private final double moveSpeedAmp;
    private final float maxAttackDistance;
    private int attackCooldown;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public GorgonAIStareAttack(EntityGorgon gorgon, double speedAmplifier, int delay, float maxDistance) {
        this.entity = gorgon;
        this.moveSpeedAmp = speedAmplifier;
        this.attackCooldown = delay;
        this.maxAttackDistance = maxDistance * maxDistance;
        this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void setAttackCooldown(int cooldown) {
        this.attackCooldown = cooldown;
    }

    @Override
    public boolean shouldExecute() {
        return this.entity.getAttackTarget() != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return (this.shouldExecute() || !this.entity.getNavigator().noPath());
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.seeTime = 0;
        this.attackTime = -1;
        this.entity.resetActiveHand();
    }

    @Override
    public void tick() {
        LivingEntity LivingEntity = this.entity.getAttackTarget();

        if (LivingEntity != null) {
            if (EntityGorgon.isStoneMob(LivingEntity)) {
                entity.setAttackTarget(null);
                resetTask();
                return;
            }
            this.entity.getLookController().setLookPosition(LivingEntity.getPosX(),
                LivingEntity.getPosY() + LivingEntity.getEyeHeight(), LivingEntity.getPosZ(),
                this.entity.getHorizontalFaceSpeed(), this.entity.getVerticalFaceSpeed());

            final double d0 = this.entity.getDistanceSq(LivingEntity.getPosX(), LivingEntity.getBoundingBox().minY, LivingEntity.getPosZ());
            final boolean flag = this.entity.getEntitySenses().canSee(LivingEntity);
            final boolean flag1 = this.seeTime > 0;

            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if (d0 <= this.maxAttackDistance && this.seeTime >= 20) {
                this.entity.getNavigator().clearPath();
                ++this.strafingTime;
            } else {
                this.entity.getNavigator().tryMoveToEntityLiving(LivingEntity, this.moveSpeedAmp);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if (this.entity.getRNG().nextFloat() < 0.3D) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if (this.entity.getRNG().nextFloat() < 0.3D) {
                    this.strafingBackwards = !this.strafingBackwards;
                }

                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (d0 > this.maxAttackDistance * 0.75F) {
                    this.strafingBackwards = false;
                } else if (d0 < this.maxAttackDistance * 0.25F) {
                    this.strafingBackwards = true;
                }

                this.entity.getMoveHelper().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                this.entity.getLookController().setLookPosition(LivingEntity.getPosX(),
                    LivingEntity.getPosY() + LivingEntity.getEyeHeight(), LivingEntity.getPosZ(),
                    this.entity.getHorizontalFaceSpeed(), this.entity.getVerticalFaceSpeed());
                this.entity.forcePreyToLook(LivingEntity);
            } else {
                this.entity.getLookController().setLookPositionWithEntity(LivingEntity, 30.0F, 30.0F);
                this.entity.forcePreyToLook(LivingEntity);
            }

        }
    }
}