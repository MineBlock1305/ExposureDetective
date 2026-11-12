package com.mineblock.exposuredetective.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mineblock.exposuredetective.client.render.PhotographDrawingOverlay;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.mortuusars.exposure.client.render.photograph.PhotographRenderer;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;


@Mixin(value = PhotographRenderer.class, remap = true)
public class PhotographRendererMixin {

    @Inject(method = "renderPhotograph", at = @At("RETURN"), remap = true)
    private void exposuredetective$afterPhotograph(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            PhotographItem photographItem,
            ItemStack photographStack,
            boolean renderPaper,
            boolean renderBackside,
            int packedLight,
            int r, int g, int b, int a,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == null || !cir.getReturnValue()) {
            return;
        }
        
        if (PhotographDrawingOverlay.isInPhotographFrame() || PhotographDrawingOverlay.isInBoard()) {
            return;
        }
        PhotographDrawingOverlay.render(photographStack, poseStack, bufferSource, packedLight, renderPaper);
    }
}
