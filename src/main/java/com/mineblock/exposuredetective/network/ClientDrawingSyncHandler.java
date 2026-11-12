package com.mineblock.exposuredetective.network;

import java.io.IOException;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.client.drawing.ClientDrawingCache;
import com.mineblock.exposuredetective.client.ink.PhotographInkClient;
import com.mineblock.exposuredetective.drawing.DrawingCodec;
import com.mineblock.exposuredetective.drawing.DrawingItemData;
import com.mineblock.exposuredetective.drawing.DrawingRaster;

import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class ClientDrawingSyncHandler {
	private ClientDrawingSyncHandler() {
	}

	public static void handle(DrawingSyncPayload payload) {
		handleClient(payload);
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(DrawingSyncPayload payload) {
		if (payload.missing()) {
			ClientDrawingCache.remove(payload.drawingId());
			return;
		}
		try {
			DrawingRaster raster = DrawingCodec.decode(payload.encodedRaster());
			ClientDrawingCache.put(payload.drawingId(), raster);
			PhotographInkClient.onRasterArrived(payload.drawingId(), raster);
			
			
			Player player = Minecraft.getInstance().player;
			if (player != null) {
				attachIdIfPhotograph(player.getMainHandItem(), payload.drawingId());
				attachIdIfPhotograph(player.getOffhandItem(), payload.drawingId());
			}
		} catch (IOException e) {
			ExposuredetectiveMod.LOGGER.warn("Failed to decode synced drawing {}", payload.drawingId(), e);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static void attachIdIfPhotograph(ItemStack stack, java.util.UUID id) {
		if (stack != null && !stack.isEmpty() && stack.getItem() instanceof PhotographItem) {
			
			var existing = DrawingItemData.getDrawingId(stack);
			if (existing.isEmpty()) {
				DrawingItemData.setDrawingId(stack, id);
			}
		}
	}
}
