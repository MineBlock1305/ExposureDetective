package com.mineblock.exposuredetective.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class InvisibleInkItem extends InkItem {
	private static final int DEFAULT_COLOR = 0xFF66CCFF;

	public InvisibleInkItem() {
		super(DEFAULT_COLOR, false, new Item.Properties().rarity(Rarity.RARE));
	}
}