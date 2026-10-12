package com.mineblock.exposuredetective.item;

import net.minecraft.world.item.Item;

public class InkItem extends Item {
	private final int color;
	private final boolean alwaysVisible;

	public InkItem(int color, boolean alwaysVisible) {
		this(color, alwaysVisible, new Item.Properties());
	}

	public InkItem(int color, boolean alwaysVisible, Properties properties) {
		super(properties.stacksTo(1));
		this.color = 0xFF000000 | color;
		this.alwaysVisible = alwaysVisible;
	}

	public int getColor() {
		return color;
	}

	public boolean isAlwaysVisible() {
		return alwaysVisible;
	}
}

