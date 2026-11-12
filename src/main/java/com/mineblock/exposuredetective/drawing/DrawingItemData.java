package com.mineblock.exposuredetective.drawing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.util.InvisibleInkData;
import com.mineblock.exposuredetective.util.PhotographCoordinates;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec2;


public final class DrawingItemData {
	private static final String DRAWING_ID_KEY = ExposuredetectiveMod.MOD_ID + ":drawing_id";
	private static final String LEGACY_INK_TAG = ExposuredetectiveMod.MOD_ID + ":invisible_ink";

	private DrawingItemData() {
	}

	public static Optional<UUID> getDrawingId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return Optional.empty();
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null || data.isEmpty()) {
			return Optional.empty();
		}
		CompoundTag tag = data.copyTag();
		if (!tag.contains(DRAWING_ID_KEY, Tag.TAG_STRING)) {
			return Optional.empty();
		}
		try {
			return Optional.of(UUID.fromString(tag.getString(DRAWING_ID_KEY)));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public static void setDrawingId(ItemStack stack, UUID id) {
		if (stack == null || stack.isEmpty() || id == null) {
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putString(DRAWING_ID_KEY, id.toString());
			
			tag.remove(LEGACY_INK_TAG);
		});
	}

	public static void clearDrawingId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.remove(DRAWING_ID_KEY);
		});
	}

	public static boolean hasLegacyPoints(ItemStack stack) {
		return !InvisibleInkData.getMarks(stack).isEmpty();
	}

	
	public static DrawingRaster buildEditorRaster(ItemStack stack, DrawingRaster fromAsset) {
		if (fromAsset != null && !fromAsset.isBlank()) {
			return DrawingRaster.copyOf(fromAsset);
		}
		return migrateLegacyToRaster(stack);
	}

	public static DrawingRaster migrateLegacyToRaster(ItemStack stack) {
		DrawingRaster raster = new DrawingRaster();
		List<InvisibleInkData.InkMark> marks = InvisibleInkData.getMarks(stack);
		for (InvisibleInkData.InkMark mark : marks) {
			Vec2 uv = mark.uv();
			if (!PhotographCoordinates.isOnPhoto(uv.x, uv.y)) {
				continue;
			}
			raster.setPixelUv(uv.x, uv.y, mark.color(), mark.alwaysVisible());
		}
		return raster;
	}

	
	public static void clearLegacyPoints(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LEGACY_INK_TAG));
	}
}
