package com.mineblock.exposuredetective.util;

import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public final class PhotographItems {
	public static final ResourceLocation PHOTOGRAPH_ID =
			ResourceLocation.fromNamespaceAndPath("exposure", "photograph");
	public static final ResourceLocation AGED_PHOTOGRAPH_ID =
			ResourceLocation.fromNamespaceAndPath("exposure", "aged_photograph");

	private PhotographItems() {
	}

	public static boolean isPhotograph(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof PhotographItem;
	}

	public static boolean isPhotographItem(Item item) {
		return item instanceof PhotographItem;
	}

	public static Item photograph() {
		return BuiltInRegistries.ITEM.getOptional(PHOTOGRAPH_ID).orElse(null);
	}

	public static Item agedPhotograph() {
		return BuiltInRegistries.ITEM.getOptional(AGED_PHOTOGRAPH_ID).orElse(null);
	}
}
