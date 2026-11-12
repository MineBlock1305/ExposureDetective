package com.mineblock.exposuredetective.init;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = ExposuredetectiveMod.MOD_ID)
public class ExposuredetectiveModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExposuredetectiveMod.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXPOSURE_DETECTIVE = REGISTRY.register(
			"exposure_detective",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.exposuredetective.exposure_detective"))
					.icon(() -> new ItemStack(ExposuredetectiveModItems.MAGNIFIER.get()))
					.displayItems((params, output) -> {
						output.accept(ExposuredetectiveModItems.BOARD.get());
						output.accept(ExposuredetectiveModItems.MAGNIFIER.get());
						output.accept(ExposuredetectiveModItems.DETECTIVE_HAT.get());
						
						for (var item : ExposuredetectiveModItems.threadSpools()) {
							output.accept(item);
						}
						
						output.accept(ExposuredetectiveModItems.INVISIBLE_INK.get());
						for (var item : ExposuredetectiveModItems.visibleInks()) {
							output.accept(item);
						}
					})
					.build()
	);

	
	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		
	}
}
