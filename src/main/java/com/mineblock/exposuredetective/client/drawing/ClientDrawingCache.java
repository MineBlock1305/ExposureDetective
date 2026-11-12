package com.mineblock.exposuredetective.client.drawing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.drawing.DrawingRaster;
import com.mineblock.exposuredetective.network.DrawingRequestPayload;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class ClientDrawingCache {
	private static final Map<UUID, DrawingRaster> RASTERS = new ConcurrentHashMap<>();
	private static final Map<UUID, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> REQUESTED_AT = new ConcurrentHashMap<>();
	private static final long REQUEST_COOLDOWN_MS = 1500L;

	private ClientDrawingCache() {
	}

	public static void put(UUID id, DrawingRaster raster) {
		RASTERS.put(id, DrawingRaster.copyOf(raster));
		uploadTexture(id, raster);
	}

	public static void remove(UUID id) {
		RASTERS.remove(id);
		REQUESTED_AT.remove(id);
		ResourceLocation loc = TEXTURES.remove(id);
		if (loc != null) {
			Minecraft.getInstance().getTextureManager().release(loc);
		}
	}

	public static DrawingRaster getRaster(UUID id) {
		return RASTERS.get(id);
	}

	public static ResourceLocation getTexture(UUID id) {
		return TEXTURES.get(id);
	}

	public static void ensureLoaded(UUID id) {
		if (id == null || RASTERS.containsKey(id)) {
			return;
		}
		long now = System.currentTimeMillis();
		Long last = REQUESTED_AT.get(id);
		if (last != null && now - last < REQUEST_COOLDOWN_MS) {
			return;
		}
		REQUESTED_AT.put(id, now);
		PacketDistributor.sendToServer(new DrawingRequestPayload(id));
	}

	public static void clear() {
		for (ResourceLocation loc : TEXTURES.values()) {
			Minecraft.getInstance().getTextureManager().release(loc);
		}
		TEXTURES.clear();
		RASTERS.clear();
		REQUESTED_AT.clear();
	}

	private static void uploadTexture(UUID id, DrawingRaster raster) {
		ResourceLocation old = TEXTURES.remove(id);
		if (old != null) {
			Minecraft.getInstance().getTextureManager().release(old);
		}

		int size = DrawingRaster.SIZE;
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				int argb = raster.getColor(px, py);
				if (argb == 0 || !raster.isAlwaysVisible(px, py)) {
					image.setPixelRGBA(px, py, 0);
					continue;
				}
				int a = (argb >>> 24) & 0xFF;
				if (a == 0) {
					a = 255;
				}
				int r = (argb >>> 16) & 0xFF;
				int g = (argb >>> 8) & 0xFF;
				int b = argb & 0xFF;
				
				image.setPixelRGBA(px, py, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}

		DynamicTexture texture = new DynamicTexture(image);
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
				ExposuredetectiveMod.MOD_ID, "drawings/" + id.toString().replace("-", ""));
		var tm = Minecraft.getInstance().getTextureManager();
		tm.register(loc, texture);
		tm.getTexture(loc).setFilter(false, false);
		TEXTURES.put(id, loc);
	}
}
