
package com.mineblock.exposuredetective.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import com.mineblock.exposuredetective.block.BoardBlock;
import com.mineblock.exposuredetective.ExposuredetectiveMod;

public class ExposuredetectiveModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(ExposuredetectiveMod.MOD_ID);
	public static final DeferredBlock<Block> BOARD = REGISTRY.register("board", BoardBlock::new);
	
	
}