package com.mineblock.exposuredetective.item;

import net.minecraft.util.Mth;


public class RgbThreadSpoolItem extends ColoredStringItem {
	
	public static final int SENTINEL = -1;

	
	public static final float CYCLE_SECONDS = 4.0f;

	public RgbThreadSpoolItem() {
		
		super(0xFF0000, "rgb");
	}

	@Override
	public int getColorRgb() {
		return SENTINEL;
	}

	
	public static int rainbowRgb(double timeSeconds) {
		float hue = (float) ((timeSeconds / CYCLE_SECONDS) % 1.0);
		if (hue < 0) {
			hue += 1.0f;
		}
		return hsvToRgb(hue, 1.0f, 1.0f);
	}

	
	public static int rainbowRgbMillis(long millis) {
		return rainbowRgb(millis / 1000.0);
	}

	
	public static int hsvToRgb(float h, float s, float v) {
		h = h - Mth.floor(h);
		int i = Mth.floor(h * 6.0f);
		float f = h * 6.0f - i;
		float p = v * (1.0f - s);
		float q = v * (1.0f - f * s);
		float t = v * (1.0f - (1.0f - f) * s);
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		int ri = Mth.clamp((int) (r * 255.0f + 0.5f), 0, 255);
		int gi = Mth.clamp((int) (g * 255.0f + 0.5f), 0, 255);
		int bi = Mth.clamp((int) (b * 255.0f + 0.5f), 0, 255);
		return (ri << 16) | (gi << 8) | bi;
	}

	public static boolean isRgbColor(int colorRgb) {
		return colorRgb == SENTINEL;
	}
}
