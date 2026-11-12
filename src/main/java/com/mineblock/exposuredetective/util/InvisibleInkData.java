package com.mineblock.exposuredetective.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec2;


public final class InvisibleInkData {
	private static final String INK_TAG = ExposuredetectiveMod.MOD_ID + ":invisible_ink";
	private static final String X_KEY = "X";
	private static final String Y_KEY = "Y";
	private static final String COLOR_KEY = "Color";
	private static final String VISIBLE_KEY = "Visible";
	
	private static final int MAX_POINTS = 4096;

	private InvisibleInkData() {
	}

	public static List<InkMark> getMarks(ItemStack stack) {
		Map<Long, InkMark> map = readAsMap(stack);
		if (map.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(map.values());
	}

	public static int count(ItemStack stack) {
		return readAsMap(stack).size();
	}

	
	public static boolean putMark(ItemStack stack, float u, float v, int color, boolean alwaysVisible) {
		if (stack == null || stack.isEmpty() || !PhotographCoordinates.isOnPhoto(u, v)) {
			return false;
		}

		Vec2 snapped = PhotographCoordinates.snapToPixelCenter(u, v);
		int px = PhotographCoordinates.pixelIndex(snapped.x);
		int py = PhotographCoordinates.pixelIndex(snapped.y);
		long key = pack(px, py);

		final boolean[] changed = {false};

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			Map<Long, InkMark> map = listToMap(tag.getList(INK_TAG, Tag.TAG_COMPOUND));
			InkMark existing = map.get(key);
			if (existing != null && existing.color() == color && existing.alwaysVisible() == alwaysVisible) {
				return; 
			}

			map.put(key, new InkMark(snapped, color, alwaysVisible));

			
			while (map.size() > MAX_POINTS) {
				Long first = map.keySet().iterator().next();
				map.remove(first);
			}

			tag.put(INK_TAG, mapToList(map));
			changed[0] = true;
		});

		return changed[0];
	}

	
	public static int putMarks(ItemStack stack, List<Vec2> points, int color, boolean alwaysVisible) {
		if (stack == null || stack.isEmpty() || points == null || points.isEmpty()) {
			return 0;
		}

		final int[] changed = {0};

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			Map<Long, InkMark> map = listToMap(tag.getList(INK_TAG, Tag.TAG_COMPOUND));

			for (Vec2 point : points) {
				if (!PhotographCoordinates.isOnPhoto(point.x, point.y)) {
					continue;
				}
				Vec2 snapped = PhotographCoordinates.snapToPixelCenter(point.x, point.y);
				int px = PhotographCoordinates.pixelIndex(snapped.x);
				int py = PhotographCoordinates.pixelIndex(snapped.y);
				long key = pack(px, py);

				InkMark existing = map.get(key);
				if (existing != null && existing.color() == color && existing.alwaysVisible() == alwaysVisible) {
					continue;
				}
				map.put(key, new InkMark(snapped, color, alwaysVisible));
				changed[0]++;
			}

			while (map.size() > MAX_POINTS) {
				Long first = map.keySet().iterator().next();
				map.remove(first);
			}

			tag.put(INK_TAG, mapToList(map));
		});

		return changed[0];
	}

	

	private static Map<Long, InkMark> readAsMap(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return Collections.emptyMap();
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null || data.isEmpty()) {
			return Collections.emptyMap();
		}
		CompoundTag tag = data.copyTag();
		if (!tag.contains(INK_TAG, Tag.TAG_LIST)) {
			return Collections.emptyMap();
		}
		return listToMap(tag.getList(INK_TAG, Tag.TAG_COMPOUND));
	}

	private static Map<Long, InkMark> listToMap(ListTag list) {
		
		Map<Long, InkMark> map = new LinkedHashMap<>(Math.max(16, list.size()));
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			float x = clamp01(entry.getFloat(X_KEY));
			float y = clamp01(entry.getFloat(Y_KEY));
			if (!PhotographCoordinates.isOnPhoto(x, y)) {
				continue;
			}
			Vec2 snapped = PhotographCoordinates.snapToPixelCenter(x, y);
			int px = PhotographCoordinates.pixelIndex(snapped.x);
			int py = PhotographCoordinates.pixelIndex(snapped.y);
			int color = entry.contains(COLOR_KEY) ? entry.getInt(COLOR_KEY) : 0xFF66CCFF;
			boolean visible = entry.contains(VISIBLE_KEY) && entry.getBoolean(VISIBLE_KEY);
			
			map.put(pack(px, py), new InkMark(snapped, color, visible));
		}
		return map;
	}

	private static ListTag mapToList(Map<Long, InkMark> map) {
		ListTag list = new ListTag();
		for (InkMark mark : map.values()) {
			CompoundTag entry = new CompoundTag();
			entry.putFloat(X_KEY, mark.uv().x);
			entry.putFloat(Y_KEY, mark.uv().y);
			entry.putInt(COLOR_KEY, mark.color());
			entry.putBoolean(VISIBLE_KEY, mark.alwaysVisible());
			list.add(entry);
		}
		return list;
	}

	private static long pack(int px, int py) {
		return ((long) px << 20) | (py & 0xFFFFF);
	}

	private static float clamp01(float value) {
		if (value < 0.0f) return 0.0f;
		if (value > 1.0f) return 1.0f;
		return value;
	}

	public record InkMark(Vec2 uv, int color, boolean alwaysVisible) {
	}
}
