package com.mineblock.exposuredetective.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;


public final class PhotographCoordinates {
	public static final int PHOTO_PIXELS = 64;
	public static final float PAPER_INSET = 1f / 16f;
	public static final float PHOTO_UV_SIZE = 0.875f;
	public static final float PIXEL_UV = PHOTO_UV_SIZE / PHOTO_PIXELS;
	public static final float PHOTO_UV_MIN = PAPER_INSET;
	public static final float PHOTO_UV_MAX = PAPER_INSET + PHOTO_UV_SIZE;

	private PhotographCoordinates() {
	}

	public static boolean isOnPhoto(float u, float v) {
		return u >= PHOTO_UV_MIN && u <= PHOTO_UV_MAX && v >= PHOTO_UV_MIN && v <= PHOTO_UV_MAX;
	}

	public static Vec2 snapToPixelCenter(float u, float v) {
		int px = pixelIndex(u);
		int py = pixelIndex(v);
		return new Vec2(pixelToU(px), pixelToV(py));
	}

	public static int pixelIndex(float uv) {
		float relative = (uv - PHOTO_UV_MIN) / PIXEL_UV;
		return Mth.clamp((int) Math.floor(relative), 0, PHOTO_PIXELS - 1);
	}

	public static float pixelToU(int pixel) {
		return PHOTO_UV_MIN + (pixel + 0.5f) * PIXEL_UV;
	}

	public static float pixelToV(int pixel) {
		return pixelToU(pixel);
	}

	
	public static float toImageU(float photographU) {
		return (photographU - PHOTO_UV_MIN) / PHOTO_UV_SIZE;
	}

	public static float toImageV(float photographV) {
		return toImageU(photographV);
	}

	public static Vec2 clampToPhoto(Vec2 uv) {
		return new Vec2(
				Mth.clamp(uv.x, PHOTO_UV_MIN, PHOTO_UV_MAX),
				Mth.clamp(uv.y, PHOTO_UV_MIN, PHOTO_UV_MAX));
	}
}
