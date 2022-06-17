package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.enums.EnumDragonEgg;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;

public class BlockDragonScales extends Block implements IDragonProof {
    EnumDragonEgg type;

    public BlockDragonScales(String name, EnumDragonEgg type) {
        super(
            Properties
                .create(Material.ROCK)
                .variableOpacity()
                .hardnessAndResistance(30F, 500)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(2)
                .sound(SoundType.STONE)
                .setRequiresTool()
        );

        this.setRegistryName(IceAndFire.MODID, name);
        this.type = type;
    }


    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new TranslationTextComponent("dragon." + type.toString().toLowerCase()).mergeStyle(type.color));
    }
}
