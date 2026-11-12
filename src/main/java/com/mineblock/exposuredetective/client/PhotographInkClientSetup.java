package com.mineblock.exposuredetective.client;


import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.item.RgbThreadSpoolItem;
import com.mineblock.exposuredetective.init.ExposuredetectiveModItems;
import com.mineblock.exposuredetective.client.render.PhotographInkBewlr;
import com.mineblock.exposuredetective.util.PhotographItems;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class PhotographInkClientSetup {
    private PhotographInkClientSetup() {
    }

    @EventBusSubscriber(modid = ExposuredetectiveMod.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
            
            event.register((stack, tintIndex) -> {
                if (tintIndex == 1 && stack.getItem() instanceof RgbThreadSpoolItem) {
                    return 0xFF000000 | RgbThreadSpoolItem.rainbowRgbMillis(System.currentTimeMillis());
                }
                return 0xFFFFFFFF;
            }, ExposuredetectiveModItems.RGB_THREAD_SPOOL.get());
        }

        
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            Item photo = PhotographItems.photograph();
            Item aged = PhotographItems.agedPhotograph();
            if (photo == null && aged == null) {
                return;
            }

            BlockEntityWithoutLevelRenderer photoDelegate = photo != null
                    ? IClientItemExtensions.of(new ItemStack(photo)).getCustomRenderer()
                    : null;
            BlockEntityWithoutLevelRenderer agedDelegate = aged != null
                    ? IClientItemExtensions.of(new ItemStack(aged)).getCustomRenderer()
                    : photoDelegate;

            
            PhotographInkBewlr photoBewlr = new PhotographInkBewlr(photoDelegate);
            PhotographInkBewlr agedBewlr = (agedDelegate == photoDelegate)
                    ? photoBewlr
                    : new PhotographInkBewlr(agedDelegate);

            if (photo != null) {
                event.registerItem(new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return photoBewlr;
                    }
                }, photo);
            }
            if (aged != null) {
                event.registerItem(new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return agedBewlr;
                    }
                }, aged);
            }
        }
    }

}
