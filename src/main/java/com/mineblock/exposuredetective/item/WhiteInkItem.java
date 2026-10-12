package com.mineblock.exposuredetective.item;

import net.minecraft.world.item.DyeColor;

public class WhiteInkItem extends InkItem {
	public WhiteInkItem() {
		super(DyeColor.WHITE.getTextColor(), true);
	}
}