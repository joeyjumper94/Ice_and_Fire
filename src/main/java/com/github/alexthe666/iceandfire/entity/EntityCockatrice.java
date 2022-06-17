package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.FoodUtils;
import com.github.alexthe666.iceandfire.entity.ai.*;
import com.github.alexthe666.iceandfire.entity.util.*;
import com.github.alexthe666.iceandfire.event.ServerEvents;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import com.google.common.base.Predicate;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;

public class EntityCockatrice extends TameableEntity implements IAnimatedEntity, IBlacklistedFromStatues, IVillagerFear, IHasCustomizableAttributes {

    public static final Animation ANIMATION_JUMPAT = Animation.create(30);
    public static final Animation ANIMATION_WATTLESHAKE = Animation.create(20);
    public static final Animation ANIMATION_BITE = Animation.create(15);
    public static final Animation ANIMATION_SPEAK = Animation.create(10);
    public static final Animation ANIMATION_EAT = Animation.create(20);
    public static final float VIEW_RADIUS = 0.6F;
    private static final DataParameter<Boolean> HEN = EntityDataManager.createKey(EntityCockatrice.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> STARING = EntityDataManager.createKey(EntityCockatrice.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> TARGET_ENTITY = EntityDataManager.createKey(EntityCockatrice.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TAMING_PLAYER = EntityDataManager.createKey(EntityCockatrice.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TAMING_LEVEL = EntityDataManager.createKey(EntityCockatrice.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.createKey(EntityCockatrice.class, DataSerializers.VARINT);
    public float sitProgress;
    public float stareProgress;
    public int ticksStaring = 0;
    public HomePosition homePos;
    public boolean hasHomePosition = false;
    private int animationTick;
    private Animation currentAnimation;
    private boolean isSitting;
    private boolean isStaring;
    private final CockatriceAIStareAttack aiStare;
    private final MeleeAttackGoal aiMelee;
    private boolean isMeleeMode = false;
    private LivingEntity targetedEntity;
    private int clientSideAttackTime;

    public EntityCockatrice(EntityType<EntityCockatrice> type, World worldIn) {
        super(type, worldIn);
        aiStare = new CockatriceAIStareAttack(this, 1.0D, 0, 15.0F);
        aiMelee = new EntityAIAttackMeleeNoCooldown(this, 1.5D, false);
        IHasCustomizableAttributes.applyAttributesForEntity(type, this);
    }

    protected int getExperiencePoints(PlayerEntity player) {
        return 10;
    }

    public boolean getCanSpawnHere() {
        return this.getRNG().nextInt(IafConfig.cockatriceSpawnCheckChance + 1) == 0;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(3, new CockatriceAIFollowOwner(this, 1.0D, 7.0F, 2.0F));
        this.goalSelector.addGoal(3, new SitGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, LivingEntity.class, 14.0F, 1.0D, 1.0D, new Predicate<LivingEntity>() {
            @Override
            public boolean apply(@Nullable LivingEntity entity) {
                if (entity instanceof PlayerEntity) {
                    return !((PlayerEntity) entity).isCreative() && !entity.isSpectator();
                } else {
                    return ServerEvents.doesScareCockatrice(entity) && !ServerEvents.isChicken(entity);
                }
            }
        }));
        this.goalSelector.addGoal(4, new CockatriceAIWander(this, 1.0D));
        this.goalSelector.addGoal(5, new CockatriceAIAggroLook(this));
        this.goalSelector.addGoal(6, new LookAtGoal(this, LivingEntity.class, 6.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new CockatriceAITargetItems(this, false));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(5, new CockatriceAITarget(this, LivingEntity.class, true, new Predicate<Entity>() {
            @Override
            public boolean apply(@Nullable Entity entity) {
                if (entity instanceof PlayerEntity) {
                    return !((PlayerEntity) entity).isCreative() && !entity.isSpectator();
                } else {
                    return ((entity instanceof IMob) && EntityCockatrice.this.isTamed() && !(entity instanceof CreeperEntity) && !(entity instanceof ZombifiedPiglinEntity) && !(entity instanceof EndermanEntity) ||
                        ServerEvents.isCockatriceTarget(entity) && !ServerEvents.isChicken(entity));
                }
            }
        }));
    }

    public boolean detachHome() {
        return this.hasHomePosition &&
            this.getCommand() == 3 &&
            getHomeDimensionName().equals(DragonUtils.getDimensionName(this.world))
            || super.detachHome();
    }

    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    public boolean canSpawn(IWorld worldIn, SpawnReason spawnReasonIn) {
        if (worldIn instanceof IServerWorld && !IafWorldRegistry.isDimensionListedForMobs((IServerWorld) world)) {
            return false;
        }
        return super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    public BlockPos getHomePosition() {
        return this.hasHomePosition && this.getCommand() == 3 && homePos != null ? homePos.getPosition() : super.getHomePosition();
    }

    @Override
    public float getMaximumHomeDistance() {
        return 30.0F;
    }

    public String getHomeDimensionName() {
        return this.homePos == null ? "" : homePos.getDimension();
    }


    public boolean isOnSameTeam(Entity entityIn) {
        return ServerEvents.isChicken(entityIn) || super.isOnSameTeam(entityIn);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        if (source.getTrueSource() != null && ServerEvents.doesScareCockatrice(source.getTrueSource())) {
            damage *= 5;
        }
        if (source == DamageSource.IN_WALL) {
            return false;
        }
        return super.attackEntityFrom(source, damage);
    }

    private boolean canUseStareOn(Entity entity) {
        return (!(entity instanceof IBlacklistedFromStatues) || ((IBlacklistedFromStatues) entity).canBeTurnedToStone()) && !ServerEvents.isCockatriceTarget(entity);
    }

    private void switchAI(boolean melee) {
        if (melee) {
            this.goalSelector.removeGoal(aiStare);
            if (aiMelee != null) {
                this.goalSelector.addGoal(2, aiMelee);
            }
            this.isMeleeMode = true;
        } else {
            this.goalSelector.removeGoal(aiMelee);
            if (aiStare != null) {
                this.goalSelector.addGoal(2, aiStare);
            }
            this.isMeleeMode = false;
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        if (this.isStaring()) {
            return false;
        }
        if (this.getRNG().nextBoolean()) {
            if (this.getAnimation() != ANIMATION_JUMPAT && this.getAnimation() != ANIMATION_BITE) {
                this.setAnimation(ANIMATION_JUMPAT);
            }
            return false;
        } else {
            if (this.getAnimation() != ANIMATION_BITE && this.getAnimation() != ANIMATION_JUMPAT) {
                this.setAnimation(ANIMATION_BITE);
            }
            return false;
        }

    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MobEntity.func_233666_p_()
            //HEALTH
            .createMutableAttribute(Attributes.MAX_HEALTH, IafConfig.cockatriceMaxHealth)
            //SPEED
            .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.4D)
            //ATTACK
            .createMutableAttribute(Attributes.ATTACK_DAMAGE, 5.0D)
            //FOLLOW RANGE
            .createMutableAttribute(Attributes.FOLLOW_RANGE, 64.0D)
            //ARMOR
            .createMutableAttribute(Attributes.ARMOR, 2.0D);
    }

    @Override
    public AttributeModifierMap.MutableAttribute getAttributes() {
        return bakeAttributes();
    }

    public boolean canMove() {
        return !this.isQueuedToSit() && !(this.getAnimation() == ANIMATION_JUMPAT && this.getAnimationTick() < 7);
    }


    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(HEN, Boolean.valueOf(false));
        this.dataManager.register(STARING, Boolean.valueOf(false));
        this.dataManager.register(TARGET_ENTITY, Integer.valueOf(0));
        this.dataManager.register(TAMING_PLAYER, Integer.valueOf(0));
        this.dataManager.register(TAMING_LEVEL, Integer.valueOf(0));
        this.dataManager.register(COMMAND, Integer.valueOf(0));
    }

    public boolean hasTargetedEntity() {
        return this.dataManager.get(TARGET_ENTITY).intValue() != 0;
    }

    public boolean hasTamingPlayer() {
        return this.dataManager.get(TAMING_PLAYER).intValue() != 0;
    }

    @Nullable
    public Entity getTamingPlayer() {
        if (!this.hasTamingPlayer()) {
            return null;
        } else if (this.world.isRemote) {
            if (this.targetedEntity != null) {
                return this.targetedEntity;
            } else {
                Entity entity = this.world.getEntityByID(this.dataManager.get(TAMING_PLAYER).intValue());
                if (entity instanceof LivingEntity) {
                    this.targetedEntity = (LivingEntity) entity;
                    return this.targetedEntity;
                } else {
                    return null;
                }
            }
        } else {
            return this.world.getEntityByID(this.dataManager.get(TAMING_PLAYER).intValue());
        }
    }

    public void setTamingPlayer(int entityId) {
        this.dataManager.set(TAMING_PLAYER, Integer.valueOf(entityId));
    }

    @Nullable
    public LivingEntity getTargetedEntity() {
        boolean blindness = this.isPotionActive(Effects.BLINDNESS) || this.getAttackTarget() != null && this.getAttackTarget().isPotionActive(Effects.BLINDNESS) || EntityGorgon.isBlindfolded(this.getAttackTarget());
        if (blindness) {
            return null;
        }
        if (!this.hasTargetedEntity()) {
            return null;
        } else if (this.world.isRemote) {
            if (this.targetedEntity != null) {
                return this.targetedEntity;
            } else {
                Entity entity = this.world.getEntityByID(this.dataManager.get(TARGET_ENTITY).intValue());
                if (entity instanceof LivingEntity) {
                    this.targetedEntity = (LivingEntity) entity;
                    return this.targetedEntity;
                } else {
                    return null;
                }
            }
        } else {
            return this.getAttackTarget();
        }
    }

    public void setTargetedEntity(int entityId) {
        this.dataManager.set(TARGET_ENTITY, Integer.valueOf(entityId));
    }

    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (TARGET_ENTITY.equals(key)) {
            this.clientSideAttackTime = 0;
            this.targetedEntity = null;
        }
    }

    @Override
    public void writeAdditional(CompoundNBT tag) {
        super.writeAdditional(tag);
        tag.putBoolean("Hen", this.isHen());
        tag.putBoolean("Staring", this.isStaring());
        tag.putInt("TamingLevel", this.getTamingLevel());
        tag.putInt("TamingPlayer", this.dataManager.get(TAMING_PLAYER).intValue());
        tag.putInt("Command", this.getCommand());
        tag.putBoolean("HasHomePosition", this.hasHomePosition);
        if (homePos != null && this.hasHomePosition) {
            homePos.write(tag);
        }
    }

    @Override
    public void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);
        this.setHen(tag.getBoolean("Hen"));
        this.setStaring(tag.getBoolean("Staring"));
        this.setTamingLevel(tag.getInt("TamingLevel"));
        this.setTamingPlayer(tag.getInt("TamingPlayer"));
        this.setCommand(tag.getInt("Command"));
        this.hasHomePosition = tag.getBoolean("HasHomePosition");
        if (hasHomePosition && tag.getInt("HomeAreaX") != 0 && tag.getInt("HomeAreaY") != 0 && tag.getInt("HomeAreaZ") != 0) {
            homePos = new HomePosition(tag);
        }
    }

    public boolean isQueuedToSit() {
        if (world.isRemote) {
            boolean isSitting = (this.dataManager.get(TAMED).byteValue() & 1) != 0;
            this.isSitting = isSitting;
            return isSitting;
        }
        return isSitting;
    }

    public void setSitting(boolean sitting) {
        super.setSwimming(sitting);
        if (!world.isRemote) {
            this.isSitting = sitting;
        }
    }

    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    @Nullable
    public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        spawnDataIn = super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.setHen(this.getRNG().nextBoolean());
        return spawnDataIn;
    }


    public boolean isHen() {
        return this.dataManager.get(HEN).booleanValue();
    }

    public void setHen(boolean hen) {
        this.dataManager.set(HEN, Boolean.valueOf(hen));
    }

    public int getTamingLevel() {
        return Integer.valueOf(this.dataManager.get(TAMING_LEVEL).intValue());
    }

    public void setTamingLevel(int level) {
        this.dataManager.set(TAMING_LEVEL, Integer.valueOf(level));
    }

    public int getCommand() {
        return Integer.valueOf(this.dataManager.get(COMMAND).intValue());
    }

    public void setCommand(int command) {
        this.dataManager.set(COMMAND, Integer.valueOf(command));
        this.setSitting(command == 1);
    }

    public boolean isStaring() {
        if (world.isRemote) {
            return this.isStaring = Boolean.valueOf(this.dataManager.get(STARING).booleanValue());
        }
        return isStaring;
    }

    public void setStaring(boolean staring) {
        this.dataManager.set(STARING, Boolean.valueOf(staring));
        if (!world.isRemote) {
            this.isStaring = staring;
        }
    }

    public void forcePreyToLook(MobEntity mob) {
        mob.getLookController().setLookPosition(this.getPosX(), this.getPosY() + (double) this.getEyeHeight(), this.getPosZ(), (float) mob.getHorizontalFaceSpeed(), (float) mob.getVerticalFaceSpeed());
    }

    @Override
    public ActionResultType getEntityInteractionResult(PlayerEntity player, Hand hand) {
        boolean flag = player.getHeldItem(hand).getItem() == Items.NAME_TAG || player.getHeldItem(hand).getItem() == Items.LEAD;
        if (flag) {
            return super.getEntityInteractionResult(player, hand);
        }
        if (player.getHeldItem(hand).getItem() == Items.POISONOUS_POTATO) {
            return super.getEntityInteractionResult(player, hand);
        }
        if (this.isTamed() && this.isOwner(player)) {
            if (FoodUtils.isSeeds(player.getHeldItem(hand)) || player.getHeldItem(hand).getItem() == Items.ROTTEN_FLESH) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(8);
                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1, 1);
                    player.getHeldItem(hand).shrink(1);
                }
                return ActionResultType.SUCCESS;
            } else if (player.getHeldItem(hand).isEmpty()) {
                if (player.isSneaking()) {
                    if (this.hasHomePosition) {
                        this.hasHomePosition = false;
                        player.sendStatusMessage(new TranslationTextComponent("cockatrice.command.remove_home"), true);
                        return ActionResultType.SUCCESS;
                    } else {
                        BlockPos pos = this.getPosition();
                        this.homePos = new HomePosition(pos, this.world);
                        this.hasHomePosition = true;
                        player.sendStatusMessage(new TranslationTextComponent("cockatrice.command.new_home", pos.getX(), pos.getY(), pos.getZ(), homePos.getDimension()), true);
                        return ActionResultType.SUCCESS;
                    }
                } else {
                    this.setCommand(this.getCommand() + 1);
                    if (this.getCommand() > 3) {
                        this.setCommand(0);
                    }
                    player.sendStatusMessage(new TranslationTextComponent("cockatrice.command." + this.getCommand()), true);
                    this.playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, 1, 1);
                    return ActionResultType.SUCCESS;
                }
            }

        }
        return ActionResultType.FAIL;
    }

    @Override
    public void livingTick() {
        super.livingTick();
        LivingEntity attackTarget = this.getAttackTarget();
        if (this.world.getDifficulty() == Difficulty.PEACEFUL && attackTarget instanceof PlayerEntity) {
            this.setAttackTarget(null);
        }
        if (this.isQueuedToSit() && this.getCommand() != 1) {
            this.setSitting(false);
        }
        if (this.isQueuedToSit() && attackTarget != null) {
            this.setAttackTarget(null);
        }
        if (attackTarget != null && this.isOnSameTeam(attackTarget)) {
            this.setAttackTarget(null);
        }
        if (!world.isRemote) {
            if (attackTarget == null || !attackTarget.isAlive()) {
                this.setTargetedEntity(0);
            } else if (this.isStaring() || this.shouldStareAttack(attackTarget)) {
                this.setTargetedEntity(attackTarget.getEntityId());
            }
        }
        if (this.getAnimation() == ANIMATION_BITE && attackTarget != null && this.getAnimationTick() == 7) {
            double dist = this.getDistanceSq(attackTarget);
            if (dist < 8) {
                attackTarget.attackEntityFrom(DamageSource.causeMobDamage(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));
            }
        }
        if (this.getAnimation() == ANIMATION_JUMPAT && attackTarget != null) {
            double dist = this.getDistanceSq(attackTarget);
            double d0 = attackTarget.getPosX() - this.getPosX();
            double d1 = attackTarget.getPosZ() - this.getPosZ();
            float leap = MathHelper.sqrt(d0 * d0 + d1 * d1);
            if (dist <= 16.0D && this.isOnGround() && this.getAnimationTick() > 7 && this.getAnimationTick() < 12) {
                Vector3d Vector3d = this.getMotion();
                Vector3d Vector3d1 = new Vector3d(attackTarget.getPosX() - this.getPosX(), 0.0D, attackTarget.getPosZ() - this.getPosZ());
                if (Vector3d1.lengthSquared() > 1.0E-7D) {
                    Vector3d1 = Vector3d1.normalize().scale(0.4D).add(Vector3d.scale(0.2D));
                }
            }
            if (dist < 4 && this.getAnimationTick() > 10) {
                attackTarget.attackEntityFrom(DamageSource.causeMobDamage(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));
                if ((double) leap >= 1.0E-4D) {
                    attackTarget.setMotion(attackTarget.getMotion().add(d0 / (double) leap * 0.800000011920929D + this.getMotion().x * 0.20000000298023224D, 0, d1 / (double) leap * 0.800000011920929D + this.getMotion().z * 0.20000000298023224D));
                }
            }
        }
        boolean sitting = isQueuedToSit();
        if (sitting && sitProgress < 20.0F) {
            sitProgress += 0.5F;
        } else if (!sitting && sitProgress > 0.0F) {
            sitProgress -= 0.5F;
        }

        boolean staring = isStaring();
        if (staring && stareProgress < 20.0F) {
            stareProgress += 0.5F;
        } else if (!staring && stareProgress > 0.0F) {
            stareProgress -= 0.5F;
        }
        if (!world.isRemote) {
            if (staring) {
                ticksStaring++;
            } else {
                ticksStaring = 0;
            }
        }
        if (!world.isRemote && staring && (attackTarget == null || this.shouldMelee())) {
            this.setStaring(false);
        }
        if (attackTarget != null) {
            this.getLookController().setLookPosition(attackTarget.getPosX(), attackTarget.getPosY() + (double) attackTarget.getEyeHeight(), attackTarget.getPosZ(), (float) this.getHorizontalFaceSpeed(), (float) this.getVerticalFaceSpeed());
            if (!shouldMelee() && attackTarget instanceof MobEntity) {
                forcePreyToLook((MobEntity) attackTarget);
            }
        }
        boolean blindness = this.isPotionActive(Effects.BLINDNESS) || attackTarget != null && attackTarget.isPotionActive(Effects.BLINDNESS);
        if (blindness) {
            this.setStaring(false);
        }
        if (!this.world.isRemote && !blindness && attackTarget != null && EntityGorgon.isEntityLookingAt(this, attackTarget, VIEW_RADIUS) && EntityGorgon.isEntityLookingAt(attackTarget, this, VIEW_RADIUS) && !EntityGorgon.isBlindfolded(attackTarget)) {
            if (!shouldMelee()) {
                if (!this.isStaring()) {
                    this.setStaring(true);
                } else {
                    int attackStrength = this.getFriendsCount(attackTarget);
                    if (this.world.getDifficulty() == Difficulty.HARD) {
                        attackStrength++;
                    }
                    attackTarget.addPotionEffect(new EffectInstance(Effects.WITHER, 10, 2 + Math.min(1, attackStrength)));
                    attackTarget.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 10, Math.min(4, attackStrength)));
                    attackTarget.addPotionEffect(new EffectInstance(Effects.NAUSEA, 200, 0));
                    if (attackStrength >= 2 && attackTarget.ticksExisted % 40 == 0) {
                        attackTarget.attackEntityFrom(DamageSource.WITHER, attackStrength - 1);
                    }
                    attackTarget.setRevengeTarget(this);
                    if (!this.isTamed() && attackTarget instanceof PlayerEntity) {
                        this.setTamingPlayer(attackTarget.getEntityId());
                        this.setTamingLevel(this.getTamingLevel() + 1);
                        if (this.getTamingLevel() % 100 == 0) {
                            this.world.setEntityState(this, (byte) 46);
                        }
                        if (this.getTamingLevel() >= 1000) {
                            this.world.setEntityState(this, (byte) 45);
                            if (this.getTamingPlayer() instanceof PlayerEntity)
                                this.setTamedBy((PlayerEntity) this.getTamingPlayer());
                            this.setAttackTarget(null);
                            this.setTamingPlayer(0);
                            this.setTargetedEntity(0);
                        }
                    }
                }
            }
        }
        if (!this.world.isRemote && attackTarget == null && this.getRNG().nextInt(300) == 0 && this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_WATTLESHAKE);
        }
        if (!this.world.isRemote) {
            if (shouldMelee() && !this.isMeleeMode) {
                switchAI(true);
            }
            if (!shouldMelee() && this.isMeleeMode) {
                switchAI(false);
            }
        }

        if (this.world.isRemote && this.getTargetedEntity() != null && EntityGorgon.isEntityLookingAt(this, this.getTargetedEntity(), VIEW_RADIUS) && EntityGorgon.isEntityLookingAt(this.getTargetedEntity(), this, VIEW_RADIUS) && this.isStaring()) {
            if (this.hasTargetedEntity()) {
                if (this.clientSideAttackTime < this.getAttackDuration()) {
                    ++this.clientSideAttackTime;
                }

                LivingEntity livingEntity = this.getTargetedEntity();

                if (livingEntity != null) {
                    this.getLookController().setLookPositionWithEntity(livingEntity, 90.0F, 90.0F);
                    this.getLookController().tick();
                    double d5 = this.getAttackAnimationScale(0.0F);
                    double d0 = livingEntity.getPosX() - this.getPosX();
                    double d1 = livingEntity.getPosY() + (double) (livingEntity.getHeight() * 0.5F) - (this.getPosY() + (double) this.getEyeHeight());
                    double d2 = livingEntity.getPosZ() - this.getPosZ();
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    d0 = d0 / d3;
                    d1 = d1 / d3;
                    d2 = d2 / d3;
                    double d4 = this.rand.nextDouble();

                    while (d4 < d3) {
                        d4 += 1.8D - d5 + this.rand.nextDouble() * (1.7D - d5);
                        this.world.addParticle(ParticleTypes.ENTITY_EFFECT, this.getPosX() + d0 * d4, this.getPosY() + d1 * d4 + (double) this.getEyeHeight(), this.getPosZ() + d2 * d4, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private int getFriendsCount(LivingEntity attackTarget) {
        if (this.getAttackTarget() == null) {
            return 0;
        }
        float dist = IafConfig.cockatriceChickenSearchLength;
        List<EntityCockatrice> list = world.getEntitiesWithinAABB(EntityCockatrice.class, this.getBoundingBox().expand(dist, dist, dist));
        int i = 0;
        for (EntityCockatrice cockatrice : list) {
            if (!cockatrice.isEntityEqual(this) && cockatrice.getAttackTarget() != null && cockatrice.getAttackTarget() == this.getAttackTarget()) {
                boolean bothLooking = EntityGorgon.isEntityLookingAt(cockatrice, cockatrice.getAttackTarget(), VIEW_RADIUS) && EntityGorgon.isEntityLookingAt(cockatrice.getAttackTarget(), cockatrice, VIEW_RADIUS);
                if (bothLooking) {
                    i++;
                }
            }
        }
        return i;
    }

    public float getAttackAnimationScale(float f) {
        return ((float) this.clientSideAttackTime + f) / (float) this.getAttackDuration();
    }

    public boolean shouldStareAttack(Entity entity) {
        return this.getDistance(entity) > 5;
    }

    public int getAttackDuration() {
        return 80;
    }

    private boolean shouldMelee() {
        boolean blindness = this.isPotionActive(Effects.BLINDNESS) || this.getAttackTarget() != null && this.getAttackTarget().isPotionActive(Effects.BLINDNESS);
        if (this.getAttackTarget() != null) {
            return this.getDistance(this.getAttackTarget()) < 4D || ServerEvents.isCockatriceTarget(this.getAttackTarget()) || blindness || !this.canUseStareOn(this.getAttackTarget());
        }
        return false;
    }

    @Override
    public void travel(Vector3d motionVec) {
        if (!this.canMove() && !this.isBeingRidden()) {
            motionVec = motionVec.mul(0, 1, 0);
        }
        super.travel(motionVec);
    }

    public void playAmbientSound() {
        if (this.getAnimation() == this.NO_ANIMATION) {
            this.setAnimation(ANIMATION_SPEAK);
        }
        super.playAmbientSound();
    }

    protected void playHurtSound(DamageSource source) {
        if (this.getAnimation() == this.NO_ANIMATION) {
            this.setAnimation(ANIMATION_SPEAK);
        }
        super.playHurtSound(source);
    }

    @Nullable
    @Override
    public AgeableEntity createChild(ServerWorld serverWorld, AgeableEntity ageable) {
        return null;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, ANIMATION_JUMPAT, ANIMATION_WATTLESHAKE, ANIMATION_BITE, ANIMATION_SPEAK, ANIMATION_EAT};
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getPosX(), this.getPosYEye(), this.getPosZ());
        return this.world.rayTraceBlocks(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() == RayTraceResult.Type.MISS;
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return IafSoundRegistry.COCKATRICE_IDLE;
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return IafSoundRegistry.COCKATRICE_HURT;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return IafSoundRegistry.COCKATRICE_DIE;
    }

    public void handleStatusUpdate(byte id) {
        if (id == 45) {
            this.playEffect(true);
        } else if (id == 46) {
            this.playEffect(false);
        } else {
            super.handleStatusUpdate(id);
        }
    }

    protected void playEffect(boolean play) {
        IParticleData enumparticletypes = ParticleTypes.HEART;

        if (!play) {
            enumparticletypes = ParticleTypes.DAMAGE_INDICATOR;
        }

        for (int i = 0; i < 7; ++i) {
            double d0 = this.rand.nextGaussian() * 0.02D;
            double d1 = this.rand.nextGaussian() * 0.02D;
            double d2 = this.rand.nextGaussian() * 0.02D;
            this.world.addParticle(enumparticletypes, this.getPosX() + (double) (this.rand.nextFloat() * this.getWidth() * 2.0F) - (double) this.getWidth(), this.getPosY() + 0.5D + (double) (this.rand.nextFloat() * this.getHeight()), this.getPosZ() + (double) (this.rand.nextFloat() * this.getWidth() * 2.0F) - (double) this.getWidth(), d0, d1, d2);
        }
    }

    @Override
    public boolean isNoDespawnRequired() {
        return true;
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }
}
