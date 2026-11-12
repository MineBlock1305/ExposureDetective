package com.mineblock.exposuredetective.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public class ColoredStringItem extends Item {
	public static final int MAX_USES = 64;

	private final int colorRgb;
	private final String colorName;

	public ColoredStringItem(int colorRgb, String colorName) {
		super(new Item.Properties().durability(MAX_USES));
		this.colorRgb = colorRgb & 0x00FFFFFF;
		this.colorName = colorName;
	}

	public int getColorRgb() {
		return colorRgb;
	}

	public int getColorArgb() {
		return 0xFF000000 | colorRgb;
	}

	public String getColorName() {
		return colorName;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}
}
