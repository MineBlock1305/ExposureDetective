package com.mineblock.exposuredetective.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mineblock.exposuredetective.client.render.PhotographDrawingOverlay;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.mortuusars.exposure.client.render.PhotographFrameEntityRenderer;
import io.github.mortuusars.exposure.world.entity.PhotographFrameEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;


@Mixin(value = PhotographFrameEntityRenderer.class, remap = true)
public class PhotographFrameEntityRendererMixin {

    @Inject(method = "renderPhotograph", at = @At("HEAD"), remap = true)
    private void exposuredetective$frameHead(PhotographFrameEntity entity, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight, ItemStack item, int size,
                                             CallbackInfoReturnable<Boolean> cir) {
        PhotographDrawingOverlay.enterPhotographFrame();
    }

    @Inject(
            method = "renderPhotograph",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"),
            remap = true
    )
    private void exposuredetective$frameBeforePop(PhotographFrameEntity entity, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight, ItemStack item, int size,
                                                  CallbackInfoReturnable<Boolean> cir) {
        
        PhotographDrawingOverlay.render(item, poseStack, bufferSource, packedLight, false);
    }

    @Inject(method = "renderPhotograph", at = @At("RETURN"), remap = true)
    private void exposuredetective$frameReturn(PhotographFrameEntity entity, PoseStack poseStack,
                                               MultiBufferSource bufferSource, int packedLight, ItemStack item, int size,
                                               CallbackInfoReturnable<Boolean> cir) {
        PhotographDrawingOverlay.leavePhotographFrame();
    }
}
