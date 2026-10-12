/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.mineblock.exposuredetective.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import com.mineblock.exposuredetective.block.BoardBlock;
import com.mineblock.exposuredetective.ExposuredetectiveMod;

public class ExposuredetectiveModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(ExposuredetectiveMod.MODID);
	public static final DeferredBlock<Block> BOARD = REGISTRY.register("board", BoardBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}