package com.mineblock.exposuredetective.drawing;

import java.util.Arrays;

import com.mineblock.exposuredetective.util.PhotographCoordinates;

import net.minecraft.world.phys.Vec2;


public final class DrawingRaster {
	public static final int SIZE = PhotographCoordinates.PHOTO_PIXELS;
	public static final int PIXEL_COUNT = SIZE * SIZE;

	private final int[] colors; 
	private final boolean[] alwaysVisible;

	public DrawingRaster() {
		this.colors = new int[PIXEL_COUNT];
		this.alwaysVisible = new boolean[PIXEL_COUNT];
	}

	public DrawingRaster(int[] colors, boolean[] alwaysVisible) {
		if (colors.length != PIXEL_COUNT || alwaysVisible.length != PIXEL_COUNT) {
			throw new IllegalArgumentException("Invalid raster size");
		}
		this.colors = colors;
		this.alwaysVisible = alwaysVisible;
	}

	public static DrawingRaster copyOf(DrawingRaster other) {
		return new DrawingRaster(Arrays.copyOf(other.colors, PIXEL_COUNT), Arrays.copyOf(other.alwaysVisible, PIXEL_COUNT));
	}

	public int index(int px, int py) {
		return py * SIZE + px;
	}

	public boolean inBounds(int px, int py) {
		return px >= 0 && py >= 0 && px < SIZE && py < SIZE;
	}

	public int getColor(int px, int py) {
		return inBounds(px, py) ? colors[index(px, py)] : 0;
	}

	public boolean isAlwaysVisible(int px, int py) {
		return inBounds(px, py) && alwaysVisible[index(px, py)];
	}

	public boolean isEmpty(int px, int py) {
		return getColor(px, py) == 0;
	}

	
	public void setPixel(int px, int py, int color, boolean visible) {
		if (!inBounds(px, py)) {
			return;
		}
		int i = index(px, py);
		if (color == 0) {
			colors[i] = 0;
			alwaysVisible[i] = false;
		} else {
			colors[i] = color;
			alwaysVisible[i] = visible;
		}
	}

	public void setPixelUv(float u, float v, int color, boolean visible) {
		if (!PhotographCoordinates.isOnPhoto(u, v)) {
			return;
		}
		Vec2 snapped = PhotographCoordinates.snapToPixelCenter(u, v);
		int px = PhotographCoordinates.pixelIndex(snapped.x);
		int py = PhotographCoordinates.pixelIndex(snapped.y);
		setPixel(px, py, color, visible);
	}

	public int[] colors() {
		return colors;
	}

	public boolean[] alwaysVisible() {
		return alwaysVisible;
	}

	public int countFilled() {
		int n = 0;
		for (int c : colors) {
			if (c != 0) n++;
		}
		return n;
	}

	public boolean isBlank() {
		for (int c : colors) {
			if (c != 0) return false;
		}
		return true;
	}

	
	public byte[] packVisibility() {
		byte[] bits = new byte[(PIXEL_COUNT + 7) / 8];
		for (int i = 0; i < PIXEL_COUNT; i++) {
			if (alwaysVisible[i]) {
				bits[i >>> 3] |= (byte) (1 << (i & 7));
			}
		}
		return bits;
	}

	public static boolean[] unpackVisibility(byte[] bits) {
		boolean[] out = new boolean[PIXEL_COUNT];
		if (bits == null) return out;
		for (int i = 0; i < PIXEL_COUNT && (i >>> 3) < bits.length; i++) {
			out[i] = (bits[i >>> 3] & (1 << (i & 7))) != 0;
		}
		return out;
	}
}
