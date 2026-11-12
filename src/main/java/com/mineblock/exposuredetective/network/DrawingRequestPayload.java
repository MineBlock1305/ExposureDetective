package com.mineblock.exposuredetective.network;

import java.io.IOException;
import java.util.UUID;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.drawing.DrawingCodec;
import com.mineblock.exposuredetective.drawing.DrawingRaster;
import com.mineblock.exposuredetective.drawing.DrawingStorage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record DrawingRequestPayload(UUID drawingId) implements CustomPacketPayload {
	public static final Type<DrawingRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "drawing_request"));
	public static final StreamCodec<FriendlyByteBuf, DrawingRequestPayload> STREAM_CODEC = StreamCodec.of(DrawingRequestPayload::encode, DrawingRequestPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void encode(FriendlyByteBuf buf, DrawingRequestPayload payload) {
		buf.writeUUID(payload.drawingId);
	}

	private static DrawingRequestPayload decode(FriendlyByteBuf buf) {
		return new DrawingRequestPayload(buf.readUUID());
	}

	public static void handle(DrawingRequestPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			DrawingRaster raster = DrawingStorage.load(player.server, payload.drawingId());
			if (raster == null) {
				PacketDistributor.sendToPlayer(player, new DrawingSyncPayload(payload.drawingId(), true, new byte[0]));
				return;
			}
			try {
				byte[] encoded = DrawingCodec.encode(raster);
				PacketDistributor.sendToPlayer(player, new DrawingSyncPayload(payload.drawingId(), false, encoded));
			} catch (IOException e) {
				ExposuredetectiveMod.LOGGER.warn("Failed to encode drawing {}", payload.drawingId(), e);
			}
		});
	}
}
