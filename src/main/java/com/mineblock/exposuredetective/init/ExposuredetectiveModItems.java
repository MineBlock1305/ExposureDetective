/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.mineblock.exposuredetective.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import com.mineblock.exposuredetective.item.YellowInkItem;
import com.mineblock.exposuredetective.item.WhiteInkItem;
import com.mineblock.exposuredetective.item.RedStringItem;
import com.mineblock.exposuredetective.item.RedInkItem;
import com.mineblock.exposuredetective.item.PurpleInkItem;
import com.mineblock.exposuredetective.item.PinkInkItem;
import com.mineblock.exposuredetective.item.OrangeInkItem;
import com.mineblock.exposuredetective.item.MagnifierItem;
import com.mineblock.exposuredetective.item.MagentaInkItem;
import com.mineblock.exposuredetective.item.LimeInkItem;
import com.mineblock.exposuredetective.item.LightGrayInkItem;
import com.mineblock.exposuredetective.item.LightBlueInkItem;
import com.mineblock.exposuredetective.item.InvisibleInkItem;
import com.mineblock.exposuredetective.item.GreenInkItem;
import com.mineblock.exposuredetective.item.GrayInkItem;
import com.mineblock.exposuredetective.item.BrownInkItem;
import com.mineblock.exposuredetective.item.BlueInkItem;
import com.mineblock.exposuredetective.item.BlackInkItem;
import com.mineblock.exposuredetective.item.DetectiveHatItem;
import com.mineblock.exposuredetective.ExposuredetectiveMod;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

public class ExposuredetectiveModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(ExposuredetectiveMod.MODID);
	public static final DeferredItem<Item> BOARD = block(ExposuredetectiveModBlocks.BOARD);
	public static final DeferredItem<Item> RED_STRING = REGISTRY.register("red_string", RedStringItem::new);
	public static final DeferredItem<Item> MAGNIFIER = REGISTRY.register("magnifier", MagnifierItem::new);
	public static final DeferredItem<Item> INVISIBLE_INK = REGISTRY.register("invisible_ink", InvisibleInkItem::new);
	public static final DeferredItem<Item> RED_INK = REGISTRY.register("red_ink", RedInkItem::new);
	public static final DeferredItem<Item> ORANGE_INK = REGISTRY.register("orange_ink", OrangeInkItem::new);
	public static final DeferredItem<Item> YELLOW_INK = REGISTRY.register("yellow_ink", YellowInkItem::new);
	public static final DeferredItem<Item> LIME_INK = REGISTRY.register("lime_ink", LimeInkItem::new);
	public static final DeferredItem<Item> GREEN_INK = REGISTRY.register("green_ink", GreenInkItem::new);
	public static final DeferredItem<Item> LIGHT_BLUE_INK = REGISTRY.register("light_blue_ink", LightBlueInkItem::new);
	public static final DeferredItem<Item> BLUE_INK = REGISTRY.register("blue_ink", BlueInkItem::new);
	public static final DeferredItem<Item> PINK_INK = REGISTRY.register("pink_ink", PinkInkItem::new);
	public static final DeferredItem<Item> MAGENTA_INK = REGISTRY.register("magenta_ink", MagentaInkItem::new);
	public static final DeferredItem<Item> PURPLE_INK = REGISTRY.register("purple_ink", PurpleInkItem::new);
	public static final DeferredItem<Item> BLACK_INK = REGISTRY.register("black_ink", BlackInkItem::new);
	public static final DeferredItem<Item> GRAY_INK = REGISTRY.register("gray_ink", GrayInkItem::new);
	public static final DeferredItem<Item> LIGHT_GRAY_INK = REGISTRY.register("light_gray_ink", LightGrayInkItem::new);
	public static final DeferredItem<Item> WHITE_INK = REGISTRY.register("white_ink", WhiteInkItem::new);
	public static final DeferredItem<Item> BROWN_INK = REGISTRY.register("brown_ink", BrownInkItem::new);

	// Start of user code block custom items
	public static final Holder<ArmorMaterial> DETECTIVE_HAT_MATERIAL = ArmorMaterials.LEATHER;
	public static final DeferredItem<Item> DETECTIVE_HAT = REGISTRY.register("detective_hat",
			() -> new DetectiveHatItem(DETECTIVE_HAT_MATERIAL));
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}