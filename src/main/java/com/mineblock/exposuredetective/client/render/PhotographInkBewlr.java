package com.mineblock.exposuredetective.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public final class PhotographInkBewlr extends BlockEntityWithoutLevelRenderer {
    private final BlockEntityWithoutLevelRenderer delegate;

    public PhotographInkBewlr(BlockEntityWithoutLevelRenderer delegate) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.delegate = delegate;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (delegate != null) {
            delegate.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
        }
        
        boolean paper = displayContext != ItemDisplayContext.FIXED;
        
        
        PhotographDrawingOverlay.render(stack, poseStack, buffer, packedLight, true);
    }
}
