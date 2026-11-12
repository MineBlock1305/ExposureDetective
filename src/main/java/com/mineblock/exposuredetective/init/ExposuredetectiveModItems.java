package com.mineblock.exposuredetective.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.item.ColoredStringItem;
import com.mineblock.exposuredetective.item.RgbThreadSpoolItem;
import com.mineblock.exposuredetective.item.DetectiveHatItem;
import com.mineblock.exposuredetective.item.InkItem;
import com.mineblock.exposuredetective.item.MagnifierItem;

public class ExposuredetectiveModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(ExposuredetectiveMod.MOD_ID);

    public static final DeferredItem<Item> BOARD = block(ExposuredetectiveModBlocks.BOARD);

    
    public static final DeferredItem<Item> WHITE_THREAD_SPOOL = REGISTRY.register("white_thread_spool",
            () -> new ColoredStringItem(0xFFFFFF, "white"));
    public static final DeferredItem<Item> LIGHT_GRAY_THREAD_SPOOL = REGISTRY.register("light_gray_thread_spool",
            () -> new ColoredStringItem(0xAAAAAA, "light_gray"));
    public static final DeferredItem<Item> GRAY_THREAD_SPOOL = REGISTRY.register("gray_thread_spool",
            () -> new ColoredStringItem(0x555555, "gray"));
    public static final DeferredItem<Item> BLACK_THREAD_SPOOL = REGISTRY.register("black_thread_spool",
            () -> new ColoredStringItem(0x1A1A1A, "black"));
    public static final DeferredItem<Item> BROWN_THREAD_SPOOL = REGISTRY.register("brown_thread_spool",
            () -> new ColoredStringItem(0x8B4513, "brown"));
    public static final DeferredItem<Item> RED_THREAD_SPOOL = REGISTRY.register("red_thread_spool",
            () -> new ColoredStringItem(0xFF5555, "red"));
    public static final DeferredItem<Item> ORANGE_THREAD_SPOOL = REGISTRY.register("orange_thread_spool",
            () -> new ColoredStringItem(0xFF8C00, "orange"));
    public static final DeferredItem<Item> YELLOW_THREAD_SPOOL = REGISTRY.register("yellow_thread_spool",
            () -> new ColoredStringItem(0xFFFF55, "yellow"));
    public static final DeferredItem<Item> LIME_THREAD_SPOOL = REGISTRY.register("lime_thread_spool",
            () -> new ColoredStringItem(0x55FF55, "lime"));
    public static final DeferredItem<Item> GREEN_THREAD_SPOOL = REGISTRY.register("green_thread_spool",
            () -> new ColoredStringItem(0x00AA00, "green"));
    public static final DeferredItem<Item> CYAN_THREAD_SPOOL = REGISTRY.register("cyan_thread_spool",
            () -> new ColoredStringItem(0x00AAAA, "cyan"));
    public static final DeferredItem<Item> LIGHT_BLUE_THREAD_SPOOL = REGISTRY.register("light_blue_thread_spool",
            () -> new ColoredStringItem(0x55FFFF, "light_blue"));
    public static final DeferredItem<Item> BLUE_THREAD_SPOOL = REGISTRY.register("blue_thread_spool",
            () -> new ColoredStringItem(0x5555FF, "blue"));
    public static final DeferredItem<Item> PURPLE_THREAD_SPOOL = REGISTRY.register("purple_thread_spool",
            () -> new ColoredStringItem(0xAA00AA, "purple"));
    public static final DeferredItem<Item> MAGENTA_THREAD_SPOOL = REGISTRY.register("magenta_thread_spool",
            () -> new ColoredStringItem(0xFF00FF, "magenta"));
    public static final DeferredItem<Item> PINK_THREAD_SPOOL = REGISTRY.register("pink_thread_spool",
            () -> new ColoredStringItem(0xFF69B4, "pink"));
    public static final DeferredItem<Item> RGB_THREAD_SPOOL = REGISTRY.register("rgb_thread_spool",
            RgbThreadSpoolItem::new);

    
    public static final DeferredItem<Item> INVISIBLE_INK = REGISTRY.register("invisible_ink",
            () -> new InkItem(0xFFFFFF, false));
    public static final DeferredItem<Item> WHITE_INK = REGISTRY.register("white_ink",
            () -> new InkItem(DyeColor.WHITE.getTextColor(), true));
    public static final DeferredItem<Item> LIGHT_GRAY_INK = REGISTRY.register("light_gray_ink",
            () -> new InkItem(DyeColor.LIGHT_GRAY.getTextColor(), true));
    public static final DeferredItem<Item> GRAY_INK = REGISTRY.register("gray_ink",
            () -> new InkItem(DyeColor.GRAY.getTextColor(), true));
    public static final DeferredItem<Item> BLACK_INK = REGISTRY.register("black_ink",
            () -> new InkItem(DyeColor.BLACK.getTextColor(), true));
    public static final DeferredItem<Item> BROWN_INK = REGISTRY.register("brown_ink",
            () -> new InkItem(DyeColor.BROWN.getTextColor(), true));
    public static final DeferredItem<Item> RED_INK = REGISTRY.register("red_ink",
            () -> new InkItem(DyeColor.RED.getTextColor(), true));
    public static final DeferredItem<Item> ORANGE_INK = REGISTRY.register("orange_ink",
            () -> new InkItem(DyeColor.ORANGE.getTextColor(), true));
    public static final DeferredItem<Item> YELLOW_INK = REGISTRY.register("yellow_ink",
            () -> new InkItem(DyeColor.YELLOW.getTextColor(), true));
    public static final DeferredItem<Item> LIME_INK = REGISTRY.register("lime_ink",
            () -> new InkItem(DyeColor.LIME.getTextColor(), true));
    public static final DeferredItem<Item> GREEN_INK = REGISTRY.register("green_ink",
            () -> new InkItem(DyeColor.GREEN.getTextColor(), true));
    public static final DeferredItem<Item> CYAN_INK = REGISTRY.register("cyan_ink",
            () -> new InkItem(DyeColor.CYAN.getTextColor(), true));
    public static final DeferredItem<Item> LIGHT_BLUE_INK = REGISTRY.register("light_blue_ink",
            () -> new InkItem(DyeColor.LIGHT_BLUE.getTextColor(), true));
    public static final DeferredItem<Item> BLUE_INK = REGISTRY.register("blue_ink",
            () -> new InkItem(DyeColor.BLUE.getTextColor(), true));
    public static final DeferredItem<Item> PURPLE_INK = REGISTRY.register("purple_ink",
            () -> new InkItem(DyeColor.PURPLE.getTextColor(), true));
    public static final DeferredItem<Item> MAGENTA_INK = REGISTRY.register("magenta_ink",
            () -> new InkItem(DyeColor.MAGENTA.getTextColor(), true));
    public static final DeferredItem<Item> PINK_INK = REGISTRY.register("pink_ink",
            () -> new InkItem(DyeColor.PINK.getTextColor(), true));

    public static final DeferredItem<Item> MAGNIFIER = REGISTRY.register("magnifier", MagnifierItem::new);
    public static final Holder<ArmorMaterial> DETECTIVE_HAT_MATERIAL = ArmorMaterials.LEATHER;
    public static final DeferredItem<Item> DETECTIVE_HAT = REGISTRY.register("detective_hat",
            () -> new DetectiveHatItem(DETECTIVE_HAT_MATERIAL));

    
    private static DeferredItem<Item> block(net.neoforged.neoforge.registries.DeferredHolder<Block, ? extends Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    
    public static Item[] threadSpools() {
        return new Item[] {
                WHITE_THREAD_SPOOL.get(),
                LIGHT_GRAY_THREAD_SPOOL.get(),
                GRAY_THREAD_SPOOL.get(),
                BLACK_THREAD_SPOOL.get(),
                BROWN_THREAD_SPOOL.get(),
                RED_THREAD_SPOOL.get(),
                ORANGE_THREAD_SPOOL.get(),
                YELLOW_THREAD_SPOOL.get(),
                LIME_THREAD_SPOOL.get(),
                GREEN_THREAD_SPOOL.get(),
                CYAN_THREAD_SPOOL.get(),
                LIGHT_BLUE_THREAD_SPOOL.get(),
                BLUE_THREAD_SPOOL.get(),
                PURPLE_THREAD_SPOOL.get(),
                MAGENTA_THREAD_SPOOL.get(),
                PINK_THREAD_SPOOL.get(),
                RGB_THREAD_SPOOL.get(),
        };
    }

    
    public static Item[] visibleInks() {
        return new Item[] {
                WHITE_INK.get(),
                LIGHT_GRAY_INK.get(),
                GRAY_INK.get(),
                BLACK_INK.get(),
                BROWN_INK.get(),
                RED_INK.get(),
                ORANGE_INK.get(),
                YELLOW_INK.get(),
                LIME_INK.get(),
                GREEN_INK.get(),
                CYAN_INK.get(),
                LIGHT_BLUE_INK.get(),
                BLUE_INK.get(),
                PURPLE_INK.get(),
                MAGENTA_INK.get(),
                PINK_INK.get(),
        };
    }
}
