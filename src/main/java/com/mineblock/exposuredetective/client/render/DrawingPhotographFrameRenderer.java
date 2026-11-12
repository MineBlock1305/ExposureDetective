package com.mineblock.exposuredetective.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.mortuusars.exposure.client.render.GlassPhotographFrameEntityRenderer;
import io.github.mortuusars.exposure.client.render.PhotographFrameEntityRenderer;
import io.github.mortuusars.exposure.world.entity.GlassPhotographFrameEntity;
import io.github.mortuusars.exposure.world.entity.PhotographFrameEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public final class DrawingPhotographFrameRenderer {
    private DrawingPhotographFrameRenderer() {
    }

    public static PhotographFrameEntityRenderer<PhotographFrameEntity> regular(EntityRendererProvider.Context ctx) {
        return new PhotographFrameEntityRenderer<>(ctx);
    }

    public static GlassPhotographFrameEntityRenderer glass(EntityRendererProvider.Context ctx) {
        return new GlassPhotographFrameEntityRenderer(ctx);
    }
}
