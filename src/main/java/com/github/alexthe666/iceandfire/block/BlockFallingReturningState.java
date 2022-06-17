package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;

import java.util.Random;

public class BlockFallingReturningState extends FallingBlock {
    public static final BooleanProperty REVERTS = BooleanProperty.create("revert");
    public Item itemBlock;
    private final BlockState returnState;

    public BlockFallingReturningState(Material materialIn, String name, String toolUsed, int toolStrength, float hardness, float resistance, SoundType sound, BlockState revertState) {
        super(
            AbstractBlock.Properties
                .create(materialIn)
                .sound(sound)
                .hardnessAndResistance(hardness, resistance)
                .harvestTool(ToolType.get(toolUsed))
                .harvestLevel(toolStrength)
                .tickRandomly()
        );

        setRegistryName(IceAndFire.MODID, name);
        this.returnState = revertState;
        this.setDefaultState(this.stateContainer.getBaseState().with(REVERTS, Boolean.valueOf(false)));
    }

    @SuppressWarnings("deprecation")
    public BlockFallingReturningState(Material materialIn, String name, String toolUsed, int toolStrength, float hardness, float resistance, SoundType sound, boolean slippery, BlockState revertState) {
        super(
            AbstractBlock.Properties
                .create(materialIn)
                .sound(sound)
                .hardnessAndResistance(hardness, resistance)
                .harvestTool(ToolType.get(toolUsed))
                .harvestLevel(toolStrength)
                .tickRandomly()
        );

        setRegistryName(IceAndFire.MODID, name);
        this.returnState = revertState;
        this.setDefaultState(this.stateContainer.getBaseState().with(REVERTS, Boolean.valueOf(false)));
    }

    @Override
    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
        super.tick(state, worldIn, pos, rand);
        if (!worldIn.isRemote) {
            if (!worldIn.isAreaLoaded(pos, 3))
                return;
            if (state.get(REVERTS) && rand.nextInt(3) == 0) {
                worldIn.setBlockState(pos, returnState);
            }
        }
    }


    public int getDustColor(BlockState blkst) {
        return -8356741;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(REVERTS);
    }
}
