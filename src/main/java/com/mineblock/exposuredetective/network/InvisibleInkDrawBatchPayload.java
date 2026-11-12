package com.mineblock.exposuredetective.network;

import java.util.ArrayList;
import java.util.List;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.item.InkItem;
import com.mineblock.exposuredetective.util.InvisibleInkData;
import com.mineblock.exposuredetective.util.PhotographCoordinates;

import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InvisibleInkDrawBatchPayload(InteractionHand hand, List<Vec2> points) implements CustomPacketPayload {
	public static final Type<InvisibleInkDrawBatchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "ink_draw_batch"));
	public static final StreamCodec<FriendlyByteBuf, InvisibleInkDrawBatchPayload> STREAM_CODEC = StreamCodec.of(InvisibleInkDrawBatchPayload::encode, InvisibleInkDrawBatchPayload::decode);
	private static final int MAX_POINTS_PER_PACKET = 512;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void encode(FriendlyByteBuf buf, InvisibleInkDrawBatchPayload payload) {
		buf.writeEnum(payload.hand);
		int count = Math.min(payload.points.size(), MAX_POINTS_PER_PACKET);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			Vec2 point = payload.points.get(i);
			buf.writeFloat(point.x);
			buf.writeFloat(point.y);
		}
	}

	private static InvisibleInkDrawBatchPayload decode(FriendlyByteBuf buf) {
		InteractionHand hand = buf.readEnum(InteractionHand.class);
		int count = buf.readVarInt();
		
		count = Math.min(count, MAX_POINTS_PER_PACKET);
		List<Vec2> points = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			points.add(new Vec2(buf.readFloat(), buf.readFloat()));
		}
		return new InvisibleInkDrawBatchPayload(hand, points);
	}

	public static void handle(InvisibleInkDrawBatchPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			if (player == null || payload.points().isEmpty()) {
				return;
			}

			ItemStack inkStack = player.getOffhandItem();
			if (!(inkStack.getItem() instanceof InkItem inkItem)) {
				return;
			}

			ItemStack photo = player.getItemInHand(payload.hand());
			if (!(photo.getItem() instanceof PhotographItem)) {
				return;
			}

			
			List<Vec2> valid = new ArrayList<>(payload.points().size());
			for (Vec2 point : payload.points()) {
				if (PhotographCoordinates.isOnPhoto(point.x, point.y)) {
					valid.add(PhotographCoordinates.snapToPixelCenter(point.x, point.y));
				}
			}
			if (valid.isEmpty()) {
				return;
			}

			InvisibleInkData.putMarks(photo, valid, inkItem.getColor(), inkItem.isAlwaysVisible());
		});
	}
}
