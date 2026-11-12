package com.mineblock.exposuredetective.client.drawing;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;


@OnlyIn(Dist.CLIENT)
public final class InkTextureRenderer {
    private InkTextureRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                              int packedLight, float minX, float minY, float maxX, float maxY) {
        render(poseStack, buffer, texture, packedLight, minX, minY, maxX, maxY, false);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                              int packedLight, float minX, float minY, float maxX, float maxY, boolean doubleSided) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
        int fullBright = LightTexture.FULL_BRIGHT;
        int lightU = fullBright & 0xFFFF;
        int lightV = (fullBright >> 16) & 0xFFFF;
        int overlay = OverlayTexture.NO_OVERLAY;
        int ou = overlay & 0xFFFF;
        int ov = (overlay >> 16) & 0xFFFF;

        emitQuad(consumer, matrix, normalMatrix, minX, minY, maxX, maxY, false, lightU, lightV, ou, ov);
        if (doubleSided) {
            emitQuad(consumer, matrix, normalMatrix, minX, minY, maxX, maxY, true, lightU, lightV, ou, ov);
        }
    }

    
    public static void renderBoardDecal(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture) {
        Matrix4f matrix = poseStack.last().pose();
        int light = LightTexture.FULL_BRIGHT;

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        VertexConsumer consumer = buffer.getBuffer(RenderType.text(texture));
        
        consumer.addVertex(matrix, 0f, 1f, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setLight(light);
        consumer.addVertex(matrix, 1f, 1f, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setLight(light);
        consumer.addVertex(matrix, 1f, 0f, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setLight(light);
        consumer.addVertex(matrix, 0f, 0f, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setLight(light);

        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RenderType.text(texture));
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void emitQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                 float minX, float minY, float maxX, float maxY, boolean backFace,
                                 int lightU, int lightV, int ou, int ov) {
        Vector3f n = new Vector3f(0f, 0f, backFace ? -1f : 1f);
        normalMatrix.transform(n);
        n.normalize();
        float nx = n.x(), ny = n.y(), nz = n.z();

        int r = 255, g = 255, b = 255, a = 255;

        if (!backFace) {
            vert(consumer, matrix, minX, maxY, 0f, 0f, 1f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
            vert(consumer, matrix, maxX, maxY, 0f, 1f, 1f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
            vert(consumer, matrix, maxX, minY, 0f, 1f, 0f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
            vert(consumer, matrix, minX, minY, 0f, 0f, 0f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
        } else {
            vert(consumer, matrix, minX, minY, 0f, 0f, 0f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
            vert(consumer, matrix, maxX, minY, 0f, 1f, 0f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
            vert(consumer, matrix, maxX, maxY, 0f, 1f, 1f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
            vert(consumer, matrix, minX, maxY, 0f, 0f, 1f, r, g, b, a, ou, ov, lightU, lightV, nx, ny, nz);
        }
    }

    private static void vert(VertexConsumer consumer, Matrix4f matrix,
                             float x, float y, float z, float u, float v,
                             int r, int g, int b, int a,
                             int ou, int ov, int lightU, int lightV,
                             float nx, float ny, float nz) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setUv1(ou, ov)
                .setUv2(lightU, lightV)
                .setNormal(nx, ny, nz);
    }

    public static void renderUnit(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, int packedLight) {
        renderUnit(poseStack, buffer, texture, packedLight, false);
    }

    public static void renderUnit(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                                  int packedLight, boolean doubleSided) {
        render(poseStack, buffer, texture, packedLight, 0f, 0f, 1f, 1f, doubleSided);
    }
}
