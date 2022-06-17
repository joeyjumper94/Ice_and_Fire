package com.github.alexthe666.iceandfire.client.model.animator;

import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.iceandfire.client.model.util.DragonAnimationsLibrary;
import com.github.alexthe666.iceandfire.client.model.util.EnumDragonAnimations;
import com.github.alexthe666.iceandfire.client.model.util.EnumDragonModelTypes;
import com.github.alexthe666.iceandfire.client.model.util.EnumDragonPoses;
import com.github.alexthe666.iceandfire.entity.EntityLightningDragon;

public class LightningTabulaDragonAnimator extends DragonTabulaModelAnimator<EntityLightningDragon> {

    public LightningTabulaDragonAnimator() {
        super(EnumDragonAnimations.GROUND_POSE.lightningdragon_model);

        this.walkPoses = new TabulaModel[] {EnumDragonAnimations.WALK1.lightningdragon_model, EnumDragonAnimations.WALK2.lightningdragon_model, EnumDragonAnimations.WALK3.lightningdragon_model, EnumDragonAnimations.WALK4.lightningdragon_model};
        this.flyPoses =  new TabulaModel[] {EnumDragonAnimations.FLIGHT1.lightningdragon_model, EnumDragonAnimations.FLIGHT2.lightningdragon_model, EnumDragonAnimations.FLIGHT3.lightningdragon_model, EnumDragonAnimations.FLIGHT4.lightningdragon_model, EnumDragonAnimations.FLIGHT5.lightningdragon_model, EnumDragonAnimations.FLIGHT6.lightningdragon_model};
        this.swimPoses = new TabulaModel[] {EnumDragonAnimations.WALK1.lightningdragon_model, EnumDragonAnimations.WALK2.lightningdragon_model, EnumDragonAnimations.WALK3.lightningdragon_model, EnumDragonAnimations.WALK4.lightningdragon_model}; //TODO Proper swim animations
    }

    //old setRotationAngles
    /*
    @Override
    public void setRotationAngles(TabulaModel model, EntityLightningDragon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float rotationYaw, float rotationPitch, float scale) {
        model.resetToDefaultPose();
        if (neckParts == null) {
            init(model);
        }
        animate(model, entity, limbSwing, limbSwingAmount, ageInTicks, rotationYaw, rotationPitch, scale);
        boolean walking = !entity.isHovering() && !entity.isFlying() && entity.hoverProgress <= 0 && entity.flyProgress <= 0;
        int currentIndex = walking ? (entity.walkCycle / 10) : (entity.flightCycle / 10);
        int prevIndex = currentIndex - 1;
        float dive = (10 - entity.diveProgress) * 0.1F;
        if (prevIndex < 0) {
            prevIndex = walking ? 3 : 5;
        }
        TabulaModel currentPosition = walking ? walkPoses[currentIndex] : flyPoses[currentIndex];
        TabulaModel prevPosition = walking ? walkPoses[prevIndex] : flyPoses[prevIndex];
        float delta = ((walking ? entity.walkCycle : entity.flightCycle) / 10.0F) % 1.0F;
        float partialTick = Minecraft.getInstance().getRenderPartialTicks();
        float deltaTicks = delta + (partialTick / 10.0F);
        if (delta == 0) {
            deltaTicks = 0;
        }




        for (AdvancedModelBox cube : model.getCubes().values()) {
            this.genderMob(entity, cube);
            if (walking && entity.flyProgress <= 0.0F && entity.hoverProgress <= 0.0F && entity.modelDeadProgress <= 0.0F) {
                AdvancedModelBox walkPart = EnumDragonAnimations.GROUND_POSE.lightningdragon_model.getCube(cube.boxName);
                AdvancedModelBox prevPositionCube =  prevPosition.getCube(cube.boxName);
                AdvancedModelBox currPositionCube =  currentPosition.getCube(cube.boxName);
                float prevX = prevPositionCube.rotateAngleX;
                float prevY = prevPositionCube.rotateAngleY;
                float prevZ = prevPositionCube.rotateAngleZ;
                float x = currPositionCube.rotateAngleX;
                float y = currPositionCube.rotateAngleY;
                float z = currPositionCube.rotateAngleZ;
                if (isWing(model, cube) && (entity.getAnimation() == EntityDragonBase.ANIMATION_WINGBLAST || entity.getAnimation() == EntityDragonBase.ANIMATION_EPIC_ROAR)) {
                    this.addToRotateAngle(cube, limbSwingAmount, walkPart.rotateAngleX, walkPart.rotateAngleY, walkPart.rotateAngleZ);
                } else {
                    this.addToRotateAngle(cube, limbSwingAmount, prevX + deltaTicks * distance(prevX, x), prevY + deltaTicks * distance(prevY, y), prevZ + deltaTicks * distance(prevZ, z));
                }
            }
            if (entity.modelDeadProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.DEAD.lightningdragon_model.getCube(cube.boxName))) {
                    transitionTo(cube, EnumDragonAnimations.DEAD.lightningdragon_model.getCube(cube.boxName), entity.prevModelDeadProgress + (entity.modelDeadProgress - entity.prevModelDeadProgress) * Minecraft.getInstance().getRenderPartialTicks(), 20, cube.boxName.equals("ThighR") || cube.boxName.equals("ThighL"));
                }
            }
            if (entity.sleepProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.SLEEPING_POSE.lightningdragon_model.getCube(cube.boxName))) {
                    transitionTo(cube, EnumDragonAnimations.SLEEPING_POSE.lightningdragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevAnimationProgresses[1], entity.sleepProgress), 20, false);
                }
            }
            if (entity.hoverProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.HOVERING_POSE.lightningdragon_model.getCube(cube.boxName)) && !isWing(model, cube) && !cube.boxName.contains("Tail") && !cube.boxName.contains("Head")) {
                    transitionTo(cube, EnumDragonAnimations.HOVERING_POSE.lightningdragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevAnimationProgresses[2], entity.hoverProgress), 20, false);
                }
            }
            if (entity.flyProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.FLYING_POSE.lightningdragon_model.getCube(cube.boxName))) {
                    transitionTo(cube, EnumDragonAnimations.FLYING_POSE.lightningdragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevAnimationProgresses[3], entity.flyProgress) - (MathHelper.lerp(partialTick, entity.prevDiveProgress, entity.diveProgress)) * 2, 20, false);
                }
            }
            if (entity.ridingProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.SIT_ON_PLAYER_POSE.lightningdragon_model.getCube(cube.boxName))) {
                    transitionTo(cube, EnumDragonAnimations.SIT_ON_PLAYER_POSE.lightningdragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevAnimationProgresses[5], entity.ridingProgress), 20, false);
                    if (cube.boxName.equals("BodyUpper")) {
                        cube.rotationPointZ += ((-12F - cube.rotationPointZ) / 20) * MathHelper.lerp(partialTick, entity.prevAnimationProgresses[5], entity.ridingProgress);
                    }

                }
            }
            if (entity.tackleProgress > 0.0F) {
                if (!isPartEqual(EnumDragonAnimations.TACKLE.lightningdragon_model.getCube(cube.boxName), EnumDragonAnimations.FLYING_POSE.lightningdragon_model.getCube(cube.boxName)) && !isWing(model, cube)) {
                    transitionTo(cube, EnumDragonAnimations.TACKLE.lightningdragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevAnimationProgresses[6], entity.tackleProgress), 5, false);
                }
            }
            if (entity.diveProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.DIVING_POSE.lightningdragon_model.getCube(cube.boxName))) {
                    transitionTo(cube, EnumDragonAnimations.DIVING_POSE.lightningdragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevDiveProgress, entity.diveProgress), 10, false);
                }
            }
            if (entity.fireBreathProgress > 0.0F) {
                if (!isPartEqual(cube, EnumDragonAnimations.STREAM_BREATH.lightningdragon_model.getCube(cube.boxName)) && !isWing(model, cube) && !cube.boxName.contains("Finger")) {
                    if (entity.prevFireBreathProgress <= entity.fireBreathProgress) {
                        transitionTo(cube, EnumDragonAnimations.BLAST_CHARGE3.lightningdragon_model.getCube(cube.boxName), MathHelper.clamp(MathHelper.lerp(partialTick, entity.prevFireBreathProgress, entity.fireBreathProgress), 0, 5), 5, false);
                    }
                    transitionTo(cube, EnumDragonAnimations.STREAM_BREATH.lightningdragon_model.getCube(cube.boxName), MathHelper.clamp(MathHelper.lerp(partialTick, entity.prevFireBreathProgress, entity.fireBreathProgress) - 5, 0, 5), 5, false);

                }
            }
            if (entity.sitProgress > 0.0F) {
                if (!entity.isPassenger()) {
                    if (!isPartEqual(cube, EnumDragonAnimations.SITTING_POSE.lightningdragon_model.getCube(cube.boxName))) {
                        transitionTo(cube, EnumDragonAnimations.SITTING_POSE.icedragon_model.getCube(cube.boxName), MathHelper.lerp(partialTick, entity.prevAnimationProgresses[0], entity.sitProgress), 20, false);
                    }
                }
                if (cube.boxName.equals("Head")) {
                    if (entity.isBreathingFire()) {
                        cube.rotateAngleX -= Math.toRadians(20F * MathHelper.lerp(partialTick, entity.prevFireBreathProgress, entity.fireBreathProgress) / 10F);
                    }
                }
            }
            if (!walking) {
                AdvancedModelBox flightPart = EnumDragonAnimations.FLYING_POSE.lightningdragon_model.getCube(cube.boxName);
                AdvancedModelBox prevPositionCube =  prevPosition.getCube(cube.boxName);
                AdvancedModelBox currPositionCube =  currentPosition.getCube(cube.boxName);
                float prevX = prevPositionCube.rotateAngleX;
                float prevY = prevPositionCube.rotateAngleY;
                float prevZ = prevPositionCube.rotateAngleZ;
                float x = currPositionCube.rotateAngleX;
                float y = currPositionCube.rotateAngleY;
                float z = currPositionCube.rotateAngleZ;
                if (x != flightPart.rotateAngleX || y != flightPart.rotateAngleY || z != flightPart.rotateAngleZ) {
                    this.setRotateAngle(cube, 1F, prevX + deltaTicks * distance(prevX, x), prevY + deltaTicks * distance(prevY, y), prevZ + deltaTicks * distance(prevZ, z));
                }
            }
        }
        float speed_walk = 0.2F;
        float speed_idle = entity.isSleeping() ? 0.025F : 0.05F;
        float speed_fly = 0.2F;
        float degree_walk = 0.5F;
        float degree_idle = entity.isSleeping() ? 0.25F : 0.5F;
        float degree_fly = 0.5F;
        if (!entity.isAIDisabled()) {
            if (!walking) {
                model.bob(model.getCube("BodyUpper"), -speed_fly, degree_fly * 5, false, ageInTicks, 1);
                model.walk(model.getCube("BodyUpper"), -speed_fly, degree_fly * 0.1F, false, 0, 0, ageInTicks, 1);
                model.chainWave(tailPartsWBody, speed_fly, degree_fly * -0.1F, 0, ageInTicks, 1);
                model.chainWave(neckParts, speed_fly, degree_fly * 0.2F, -4, ageInTicks, 1);
                model.chainWave(toesPartsL, speed_fly, degree_fly * 0.2F, -2, ageInTicks, 1);
                model.chainWave(toesPartsR, speed_fly, degree_fly * 0.2F, -2, ageInTicks, 1);
                model.walk(model.getCube("ThighR"), -speed_fly, degree_fly * 0.1F, false, 0, 0, ageInTicks, 1);
                model.walk(model.getCube("ThighL"), -speed_fly, degree_fly * 0.1F, true, 0, 0, ageInTicks, 1);
            } else {
                model.bob(model.getCube("BodyUpper"), speed_walk * 2, degree_walk * 1.7F, false, limbSwing, limbSwingAmount);
                model.bob(model.getCube("ThighR"), speed_walk, degree_walk * 1.7F, false, limbSwing, limbSwingAmount);
                model.bob(model.getCube("ThighL"), speed_walk, degree_walk * 1.7F, false, limbSwing, limbSwingAmount);
                model.chainSwing(tailParts, speed_walk, degree_walk * 0.25F, -2, limbSwing, limbSwingAmount);
                model.chainWave(tailParts, speed_walk, degree_walk * 0.15F, 2, limbSwing, limbSwingAmount);
                model.chainSwing(neckParts, speed_walk, degree_walk * 0.15F, 2, limbSwing, limbSwingAmount);
                model.chainWave(neckParts, speed_walk, degree_walk * 0.05F, -2, limbSwing, limbSwingAmount);
                model.chainSwing(tailParts, speed_idle, degree_idle * 0.25F, -2, ageInTicks, 1);
                float sitMod = entity.isQueuedToSit() ? 0.15F : 1F;
                model.chainWave(tailParts, speed_idle, degree_idle * 0.15F * sitMod, -2, ageInTicks, 1);
                model.chainWave(neckParts, speed_idle, degree_idle * -0.15F, -3, ageInTicks, 1);
                model.walk(model.getCube("Neck1"), speed_idle, degree_idle * 0.05F, false, 0, 0, ageInTicks, 1);
            }
            model.bob(model.getCube("BodyUpper"), speed_idle, degree_idle * 1.3F, false, ageInTicks, 1);
            model.bob(model.getCube("ThighR"), speed_idle, -degree_idle * 1.3F, false, ageInTicks, 1);
            model.bob(model.getCube("ThighL"), speed_idle, -degree_idle * 1.3F, false, ageInTicks, 1);
            model.bob(model.getCube("armR1"), speed_idle, -degree_idle * 1.3F, false, ageInTicks, 1);
            model.bob(model.getCube("armL1"), speed_idle, -degree_idle * 1.3F, false, ageInTicks, 1);
            if (entity.getAnimation() != EntityDragonBase.ANIMATION_SHAKEPREY || entity.getAnimation() != EntityDragonBase.ANIMATION_ROAR) {
                model.faceTarget(rotationYaw, rotationPitch, 4, neckParts);
            }
            if (entity.isActuallyBreathingFire()) {
                float speed_shake = 0.7F;
                float degree_shake = 0.03F;
                model.chainFlap(neckParts, speed_shake, degree_shake, 2, ageInTicks, 1);
                model.chainSwing(neckParts, speed_shake * 0.65F, degree_shake * 0.1F, 1, ageInTicks, 1);
            }
        }
        if (!entity.isModelDead()) {
            if (entity.turn_buffer != null && !entity.isBeingRidden() && !entity.isPassenger() && !entity.isBreathingFire()) {
                entity.turn_buffer.applyChainSwingBuffer(neckParts);
            }
            if (entity.tail_buffer != null && !entity.isPassenger()) {
                entity.tail_buffer.applyChainSwingBuffer(tailPartsWBody);
            }
            if (entity.roll_buffer != null && entity.pitch_buffer_body != null && entity.pitch_buffer != null) {
                if (entity.flyProgress > 0 || entity.hoverProgress > 0) {
                    entity.roll_buffer.applyChainFlapBuffer(model.getCube("BodyUpper"));
                    entity.pitch_buffer_body.applyChainWaveBuffer(model.getCube("BodyUpper"));
                    entity.pitch_buffer.applyChainWaveBufferReverse(tailPartsWBody);
                }
            }

        }
        if (entity.getWidth() >= 2 && entity.flyProgress == 0 && entity.hoverProgress == 0) {
            LegArticulator.articulateQuadruped(entity, entity.legSolver, model.getCube("BodyUpper"), model.getCube("BodyLower"), model.getCube("Neck1"),
                    model.getCube("ThighL"), model.getCube("LegL"), toesPartsL,
                    model.getCube("ThighR"), model.getCube("LegR"), toesPartsR,
                    model.getCube("armL1"), model.getCube("armL2"), clawL,
                    model.getCube("armR1"), model.getCube("armR2"), clawR,
                    1.0F, 0.5F, 0.5F, -0.15F, -0.15F, 0F,
                    Minecraft.getInstance().getRenderPartialTicks()
            );
        }
    }
*/

    @Override
    protected TabulaModel getModel(EnumDragonPoses pose) {
        return DragonAnimationsLibrary.getModel(pose, EnumDragonModelTypes.LIGHTNING_DRAGON_MODEL);
    }

}
