package com.github.alexthe666.iceandfire.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import com.github.alexthe666.iceandfire.entity.EntityMyrmexBase;
import com.github.alexthe666.iceandfire.entity.EntityMyrmexEgg;
import com.github.alexthe666.iceandfire.entity.EntityMyrmexWorker;

import com.github.alexthe666.iceandfire.util.IAFMath;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.AxisAlignedBB;

public class MyrmexAIPickupBabies<T extends ItemEntity> extends TargetGoal {
    protected final DragonAITargetItems.Sorter theNearestAttackableTargetSorter;
    protected final Predicate<? super LivingEntity> targetEntitySelector;
    public EntityMyrmexWorker myrmex;
    protected LivingEntity targetEntity;

    private List<LivingEntity> listBabies = IAFMath.emptyLivingEntityList;

    public MyrmexAIPickupBabies(EntityMyrmexWorker myrmex) {
        super(myrmex, false, false);
        this.theNearestAttackableTargetSorter = new DragonAITargetItems.Sorter(myrmex);
        this.targetEntitySelector = new Predicate<LivingEntity>() {
            @Override
            public boolean test(LivingEntity myrmex) {
                return (myrmex instanceof EntityMyrmexBase && ((EntityMyrmexBase) myrmex).getGrowthStage() < 2
                    && !((EntityMyrmexBase) myrmex).isInNursery()
                    || myrmex instanceof EntityMyrmexEgg && !((EntityMyrmexEgg) myrmex).isInNursery());
            }
        };
        this.myrmex = myrmex;
        this.setMutexFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean shouldExecute() {
        if (!this.myrmex.canMove() || this.myrmex.holdingSomething() || !this.myrmex.getNavigator().noPath() || this.myrmex.shouldEnterHive() || !this.myrmex.keepSearching || this.myrmex.holdingBaby()) {
            listBabies = IAFMath.emptyLivingEntityList;
            return false;
        }

        if (this.myrmex.world.getGameTime() % 4 == 0) // only update the list every 4 ticks
            listBabies = this.goalOwner.world.getLoadedEntitiesWithinAABB(LivingEntity.class, this.getTargetableArea(20), this.targetEntitySelector);

        if (listBabies.isEmpty())
            return false;

        listBabies.sort(this.theNearestAttackableTargetSorter);
        this.targetEntity = listBabies.get(0);
        return true;
    }

    protected AxisAlignedBB getTargetableArea(double targetDistance) {
        return this.goalOwner.getBoundingBox().grow(targetDistance, 4.0D, targetDistance);
    }

    @Override
    public void startExecuting() {
        this.goalOwner.getNavigator().tryMoveToXYZ(this.targetEntity.getPosX(), this.targetEntity.getPosY(), this.targetEntity.getPosZ(), 1);
        super.startExecuting();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetEntity != null && this.targetEntity.isAlive()
            && this.goalOwner.getDistanceSq(this.targetEntity) < 2) {
            this.targetEntity.startRiding(this.myrmex);
        }
        resetTask();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.goalOwner.getNavigator().noPath();
    }

    public static class Sorter implements Comparator<Entity> {
        private final Entity theEntity;

        public Sorter(EntityMyrmexBase theEntityIn) {
            this.theEntity = theEntityIn;
        }

        @Override
        public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            final double d0 = this.theEntity.getDistanceSq(p_compare_1_);
            final double d1 = this.theEntity.getDistanceSq(p_compare_2_);
            return Double.compare(d0, d1);
        }
    }
}