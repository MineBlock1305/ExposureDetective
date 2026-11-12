package com.mineblock.exposuredetective.network;

import java.util.UUID;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record DrawingSyncPayload(UUID drawingId, boolean missing, byte[] encodedRaster) implements CustomPacketPayload {
	public static final Type<DrawingSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "drawing_sync"));
	public static final StreamCodec<FriendlyByteBuf, DrawingSyncPayload> STREAM_CODEC = StreamCodec.of(DrawingSyncPayload::encode, DrawingSyncPayload::decode);
	private static final int MAX_BYTES = 256 * 1024;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void encode(FriendlyByteBuf buf, DrawingSyncPayload payload) {
		buf.writeUUID(payload.drawingId);
		buf.writeBoolean(payload.missing);
		buf.writeByteArray(payload.encodedRaster == null ? new byte[0] : payload.encodedRaster);
	}

	private static DrawingSyncPayload decode(FriendlyByteBuf buf) {
		UUID id = buf.readUUID();
		boolean missing = buf.readBoolean();
		byte[] data = buf.readByteArray(MAX_BYTES);
		return new DrawingSyncPayload(id, missing, data);
	}

	public static void handle(DrawingSyncPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!FMLEnvironment.dist.isClient()) {
				return;
			}
			
			try {
				Class<?> handler = Class.forName("com.mineblock.exposuredetective.network.ClientDrawingSyncHandler");
				handler.getMethod("handle", DrawingSyncPayload.class).invoke(null, payload);
			} catch (ReflectiveOperationException e) {
				ExposuredetectiveMod.LOGGER.debug("Client drawing sync handler unavailable", e);
			}
		});
	}
}
