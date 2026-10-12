package com.mineblock.exposuredetective.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;

@OnlyIn(Dist.CLIENT)
public final class BoardClientHelper {
	private BoardClientHelper() {
	}

	public static void openPhotographScreen(ItemStack stack) {
		if (!(stack.getItem() instanceof PhotographItem)) {
			return;
		}
		Minecraft.getInstance().setScreen(new PhotographScreen(List.of(new ItemAndStack<>(stack))));
	}
}

