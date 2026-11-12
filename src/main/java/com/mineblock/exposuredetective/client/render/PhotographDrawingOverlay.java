package com.mineblock.exposuredetective.client.render;

import java.util.Optional;
import java.util.UUID;

import com.mineblock.exposuredetective.client.drawing.ClientDrawingCache;
import com.mineblock.exposuredetective.client.drawing.InkTextureRenderer;
import com.mineblock.exposuredetective.drawing.DrawingItemData;
import com.mineblock.exposuredetective.util.PhotographCoordinates;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public final class PhotographDrawingOverlay {
    private static final ThreadLocal<Boolean> IN_PHOTOGRAPH_FRAME = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IN_BOARD = ThreadLocal.withInitial(() -> false);

    private static final float Z_FRONT_PAPER = 0.003f;
    private static final float Z_FRONT_FRAME = -0.003f;
    private static final float Z_FRONT_BOARD = -0.003f;

    private PhotographDrawingOverlay() {
    }

    public static void enterPhotographFrame() {
        IN_PHOTOGRAPH_FRAME.set(true);
    }

    public static void leavePhotographFrame() {
        IN_PHOTOGRAPH_FRAME.set(false);
    }

    public static boolean isInPhotographFrame() {
        return Boolean.TRUE.equals(IN_PHOTOGRAPH_FRAME.get());
    }

    public static void enterBoard() {
        IN_BOARD.set(true);
    }

    public static void leaveBoard() {
        IN_BOARD.set(false);
    }

    public static boolean isInBoard() {
        return Boolean.TRUE.equals(IN_BOARD.get());
    }

    
    public static void render(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, boolean withPaperInset) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Optional<UUID> id = DrawingItemData.getDrawingId(stack);
        if (id.isEmpty()) {
            return;
        }
        ClientDrawingCache.ensureLoaded(id.get());
        ResourceLocation tex = ClientDrawingCache.getTexture(id.get());
        if (tex == null) {
            return;
        }

        
        boolean doubleSided = isInBoard();

        poseStack.pushPose();
        if (withPaperInset) {
            float offset = PhotographCoordinates.PAPER_INSET;
            poseStack.translate(offset, offset, Z_FRONT_PAPER);
            poseStack.scale(PhotographCoordinates.PHOTO_UV_SIZE, PhotographCoordinates.PHOTO_UV_SIZE, 1f);
            InkTextureRenderer.renderUnit(poseStack, buffer, tex, packedLight, doubleSided);
        } else {
            float z = Z_FRONT_PAPER;
            if (isInPhotographFrame()) {
                z = Z_FRONT_FRAME;
            } else if (isInBoard()) {
                z = Z_FRONT_BOARD;
            }
            poseStack.translate(0f, 0f, z);
            InkTextureRenderer.renderUnit(poseStack, buffer, tex, packedLight, doubleSided);
        }
        poseStack.popPose();
    }
}
