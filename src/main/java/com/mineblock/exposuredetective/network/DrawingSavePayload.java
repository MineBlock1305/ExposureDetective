package com.mineblock.exposuredetective.network;

import java.io.IOException;
import java.util.UUID;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.drawing.DrawingCodec;
import com.mineblock.exposuredetective.drawing.DrawingItemData;
import com.mineblock.exposuredetective.drawing.DrawingRaster;
import com.mineblock.exposuredetective.drawing.DrawingStorage;

import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record DrawingSavePayload(InteractionHand hand, UUID drawingId, byte[] encodedRaster) implements CustomPacketPayload {
	public static final Type<DrawingSavePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "drawing_save"));
	public static final StreamCodec<FriendlyByteBuf, DrawingSavePayload> STREAM_CODEC = StreamCodec.of(DrawingSavePayload::encode, DrawingSavePayload::decode);
	private static final int MAX_BYTES = 256 * 1024;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void encode(FriendlyByteBuf buf, DrawingSavePayload payload) {
		buf.writeEnum(payload.hand);
		buf.writeUUID(payload.drawingId);
		buf.writeByteArray(payload.encodedRaster);
	}

	private static DrawingSavePayload decode(FriendlyByteBuf buf) {
		InteractionHand hand = buf.readEnum(InteractionHand.class);
		UUID id = buf.readUUID();
		byte[] data = buf.readByteArray(MAX_BYTES);
		return new DrawingSavePayload(hand, id, data);
	}

	public static void handle(DrawingSavePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			ItemStack photo = player.getItemInHand(payload.hand());
			if (!(photo.getItem() instanceof PhotographItem)) {
				return;
			}

			DrawingRaster raster;
			try {
				raster = DrawingCodec.decode(payload.encodedRaster());
			} catch (IOException e) {
				ExposuredetectiveMod.LOGGER.warn("Invalid drawing payload from {}", player.getGameProfile().getName(), e);
				return;
			}

			UUID previous = DrawingItemData.getDrawingId(photo).orElse(null);
			UUID id = payload.drawingId() != null ? payload.drawingId() : UUID.randomUUID();

			try {
				if (raster.isBlank()) {
					DrawingItemData.clearDrawingId(photo);
					DrawingItemData.clearLegacyPoints(photo);
					
					PacketDistributor.sendToPlayer(player, new DrawingSyncPayload(id, true, new byte[0]));
					return;
				}

				
				DrawingStorage.saveReplacing(player.server, id, previous, raster, false);
				DrawingItemData.setDrawingId(photo, id);
				DrawingItemData.clearLegacyPoints(photo);

				byte[] encoded = DrawingCodec.encode(raster);
				PacketDistributor.sendToPlayer(player, new DrawingSyncPayload(id, false, encoded));
			} catch (IOException e) {
				ExposuredetectiveMod.LOGGER.error("Failed to save drawing for {}", player.getGameProfile().getName(), e);
			}
		});
	}
}
