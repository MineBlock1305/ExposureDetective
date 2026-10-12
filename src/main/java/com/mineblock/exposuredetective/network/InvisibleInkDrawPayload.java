package com.mineblock.exposuredetective.network;

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
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InvisibleInkDrawPayload(InteractionHand hand, float u, float v) implements CustomPacketPayload {
    public static final Type<InvisibleInkDrawPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MODID, "ink_draw"));
    public static final StreamCodec<FriendlyByteBuf, InvisibleInkDrawPayload> STREAM_CODEC = StreamCodec.of(InvisibleInkDrawPayload::encode, InvisibleInkDrawPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, InvisibleInkDrawPayload payload) {
        buf.writeEnum(payload.hand);
        buf.writeFloat(payload.u);
        buf.writeFloat(payload.v);
    }

    private static InvisibleInkDrawPayload decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        float u = buf.readFloat();
        float v = buf.readFloat();
        return new InvisibleInkDrawPayload(hand, u, v);
    }

    public static void handle(InvisibleInkDrawPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
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
            if (!PhotographCoordinates.isOnPhoto(payload.u(), payload.v())) {
                return;
            }
            InvisibleInkData.putMark(photo, payload.u(), payload.v(), inkItem.getColor(), inkItem.isAlwaysVisible());
        });
    }
}
