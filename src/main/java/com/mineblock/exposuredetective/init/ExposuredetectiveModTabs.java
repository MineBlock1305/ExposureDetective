/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.mineblock.exposuredetective.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

@EventBusSubscriber()
public class ExposuredetectiveModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExposuredetectiveMod.MODID);

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			tabData.accept(ExposuredetectiveModBlocks.BOARD.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			tabData.accept(ExposuredetectiveModItems.RED_STRING.get());
			tabData.accept(ExposuredetectiveModItems.INVISIBLE_INK.get());
			tabData.accept(ExposuredetectiveModItems.RED_INK.get());
			tabData.accept(ExposuredetectiveModItems.ORANGE_INK.get());
			tabData.accept(ExposuredetectiveModItems.YELLOW_INK.get());
			tabData.accept(ExposuredetectiveModItems.LIME_INK.get());
			tabData.accept(ExposuredetectiveModItems.GREEN_INK.get());
			tabData.accept(ExposuredetectiveModItems.LIGHT_BLUE_INK.get());
			tabData.accept(ExposuredetectiveModItems.BLUE_INK.get());
			tabData.accept(ExposuredetectiveModItems.PINK_INK.get());
			tabData.accept(ExposuredetectiveModItems.MAGENTA_INK.get());
			tabData.accept(ExposuredetectiveModItems.PURPLE_INK.get());
			tabData.accept(ExposuredetectiveModItems.BLACK_INK.get());
			tabData.accept(ExposuredetectiveModItems.GRAY_INK.get());
			tabData.accept(ExposuredetectiveModItems.LIGHT_GRAY_INK.get());
			tabData.accept(ExposuredetectiveModItems.WHITE_INK.get());
			tabData.accept(ExposuredetectiveModItems.BROWN_INK.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			tabData.accept(ExposuredetectiveModItems.MAGNIFIER.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(ExposuredetectiveModItems.DETECTIVE_HAT.get());
		}
	}
}