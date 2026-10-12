package com.mineblock.exposuredetective.client.render;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.block.BoardBlock;
import com.mineblock.exposuredetective.block.entity.BoardBlockEntity;
import com.mineblock.exposuredetective.util.InvisibleInkData;
import com.mineblock.exposuredetective.util.PhotographCoordinates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.render.texture.TextureRenderer;
import io.github.mortuusars.exposure.world.item.AgedPhotographItem;
import io.github.mortuusars.exposure.world.item.PhotographItem;
public class BoardBlockEntityRenderer implements BlockEntityRenderer<BoardBlockEntity> {
	private static final int GRID_SIZE = 3;
	private static final float BOARD_MARGIN = 1f / 16f;
	private static final float BOARD_SURFACE = 1f / 16f;
	private static final float SURFACE_CLEARANCE = 0.0005f;
	private static final float SLOT_PADDING = 1f / 32f;
	private static final float PHOTO_TEXTURE_INSET = 1f / 64f;
	private static final float PHOTO_EXTRA_INSET = 1f / 128f;
	private static final float PHOTO_LAYER_OFFSET = -0.003f;
	private static final float OVERLAY_LAYER_OFFSET = 0.000f;
	private static final float INK_LAYER_OFFSET = -0.006f;
	private static final float STRING_LAYER_OFFSET = 0.01f;
	private static final float STRING_PIXEL = 1f / 32f; // Уменьшено в 2 раза
	private static final float PHOTO_SIZE_SCALE = 0.94f;
	private static final double VECTOR_EPSILON = 1.0E-6D;
	private static final ResourceLocation OVERLAY_PHOTOGRAPH = ResourceLocation.fromNamespaceAndPath("exposure", "textures/photograph/photograph.png");
	private static final ResourceLocation OVERLAY_AGED = ResourceLocation.fromNamespaceAndPath("exposure", "textures/photograph/aged_photograph.png");
	private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
	private static final ResourceLocation PIN_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MODID, "textures/block/pin");
	private static final float PIN_MODEL_SCALE = 0.025f;
	public BoardBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(BoardBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if (entity == null || entity.isRemoved()) {
			return;
		}

		Direction facing = entity.getBlockState().getValue(BoardBlock.FACING);
		poseStack.pushPose();
		orientToFacing(poseStack, facing);
		poseStack.translate(0.0D, 0.0D, BOARD_SURFACE + SURFACE_CLEARANCE);

		for (int slot = 0; slot < entity.getContainerSize(); slot++) {
			ItemStack stack = entity.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			renderSlot(entity, stack, slot, poseStack, buffer, packedLight, packedOverlay);
		}

		poseStack.popPose();
		renderConnections(entity, poseStack, buffer);
	}

	private void orientToFacing(PoseStack poseStack, Direction facing) {
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		poseStack.translate(-0.5D, -0.5D, -0.5D);
	}

	private void renderSlot(BoardBlockEntity entity, ItemStack stack, int slot, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		int row = slot / GRID_SIZE;
		int column = slot % GRID_SIZE;
		float cellSize = (1f - BOARD_MARGIN * 2f) / GRID_SIZE;
		float overlaySize = Math.max(cellSize - SLOT_PADDING, 0.01f);
		float photoSize = Math.max(overlaySize - PHOTO_TEXTURE_INSET - PHOTO_EXTRA_INSET, 0.01f) * PHOTO_SIZE_SCALE;
		float x = BOARD_MARGIN + column * cellSize;
		float y = BOARD_MARGIN + row * cellSize;
		float rotation = entity.getRotation(slot);

		poseStack.pushPose();
		poseStack.translate(x + cellSize / 2f, 1f - (y + cellSize / 2f), 0.0F);
		poseStack.mulPose(Axis.XP.rotationDegrees(180f));
		poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

		renderPhotographLayer(stack, poseStack, buffer, packedLight, photoSize);
		renderOverlayLayer(stack, poseStack, buffer, packedLight, overlaySize);
		renderInkLayer(stack, poseStack, buffer, packedLight, photoSize);

		poseStack.popPose();
	}

	private void renderPhotographLayer(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float cellSize) {
		if (!(stack.getItem() instanceof PhotographItem)) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, PHOTO_LAYER_OFFSET);
		poseStack.scale(cellSize, cellSize, 1.0f);
		poseStack.translate(-0.5f, -0.5f, 0.0f);
		ExposureClient.photographRenderer().render(stack, false, false, poseStack, buffer, packedLight);
		poseStack.popPose();
	}

	private void renderOverlayLayer(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float cellSize) {
		ResourceLocation overlay = getOverlay(stack);
		if (overlay == null) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, OVERLAY_LAYER_OFFSET);
		poseStack.scale(cellSize, cellSize, 1.0f);
		poseStack.translate(-0.5f, -0.5f, 0.0f);
		TextureRenderer.render(poseStack, buffer, overlay, packedLight, 255, 255, 255, 255);
		poseStack.popPose();
	}

	private void renderInkLayer(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float cellSize) {
		if (stack.isEmpty()) {
			return;
		}
		var marks = InvisibleInkData.getMarks(stack);
		if (marks.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, INK_LAYER_OFFSET);
		poseStack.scale(cellSize, cellSize, 1.0f);
		poseStack.translate(-0.5f, -0.5f, 0.0f);
		PoseStack.Pose pose = poseStack.last();
		Matrix4f matrix = pose.pose();
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
		float half = 0.5f / PhotographCoordinates.PHOTO_PIXELS;
		for (InvisibleInkData.InkMark mark : marks) {
			if (!mark.alwaysVisible()) {
				continue;
			}
			float u = PhotographCoordinates.toImageU(mark.uv().x);
			float v = PhotographCoordinates.toImageV(mark.uv().y);
			drawInkQuad(consumer, matrix, u - half, v - half, u + half, v + half, mark.color(), packedLight);
		}
		poseStack.popPose();
	}

	private void renderConnections(BoardBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
		Level level = entity.getLevel();
		if (level == null) {
			return;
		}
		float overlaySize = Math.max((1f - BOARD_MARGIN * 2f) / GRID_SIZE - SLOT_PADDING, 0.01f);
		BlockPos origin = entity.getBlockPos();
		poseStack.pushPose();
		poseStack.translate(-origin.getX(), -origin.getY(), -origin.getZ());
		int light = LightTexture.pack(15, 15);

		for (int slot = 0; slot < entity.getContainerSize(); slot++) {
			if (entity.getItem(slot).isEmpty()) {
				continue;
			}

			AnchorPoint start = getAnchor(entity, slot, overlaySize);
			if (start == null) {
				continue;
			}

			for (BoardBlockEntity.PhotoConnection connection : entity.getConnections(slot)) {
				if (!shouldRenderConnection(origin, slot, connection.pos(), connection.slot())) {
					continue;
				}

				BoardBlockEntity other = getBoardEntity(level, connection.pos());
				if (other == null || other.getItem(connection.slot()).isEmpty()) {
					continue;
				}

				AnchorPoint end = getAnchor(other, connection.slot(), overlaySize);
				if (end == null) {
					continue;
				}

				VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
				drawString(poseStack, consumer, start, end, light);
			}
		}
		poseStack.popPose();
	}

	private void drawString(PoseStack poseStack, VertexConsumer consumer, AnchorPoint start, AnchorPoint end, int light) {
		Vec3 startPos = start.position();
		Vec3 endPos = end.position();
		Vec3 diff = endPos.subtract(startPos);
		double length = diff.length();
		if (length < VECTOR_EPSILON) {
			return;
		}

		// Случайный цвет для нити из трех вариантов
		int[][] colors = new int[][]{{255, 65, 65}, {223, 61, 61}, {232, 50, 50}};
		int colorIndex = Math.floorMod((int)((startPos.x + startPos.y + startPos.z) * 1000), colors.length);
		int[] selectedColor = colors[colorIndex];
		int r = selectedColor[0];
		int g = selectedColor[1];
		int b = selectedColor[2];

		Vec3 tangent = diff.normalize();
		Vec3 planeNormal = start.normal().normalize();
		Vec3 side = planeNormal.cross(tangent);
		if (side.lengthSqr() < VECTOR_EPSILON) {
			side = orthogonalVector(planeNormal);
		}
		side = side.normalize();

		Vec3 step = tangent.scale(STRING_PIXEL);
		int segments = Math.max(1, (int) Math.ceil(length / STRING_PIXEL));
		Vec3 current = startPos;
		PoseStack.Pose pose = poseStack.last();
		for (int i = 0; i < segments; i++) {
			renderPixel(pose, consumer, current, tangent, side, planeNormal, light, r, g, b);
			current = current.add(step);
		}
	}

	private void renderPixel(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, Vec3 tangent, Vec3 side, Vec3 planeNormal, int light, int r, int g, int b) {
		Vec3 halfForward = tangent.scale(STRING_PIXEL / 2f);
		Vec3 halfSide = side.scale(STRING_PIXEL / 2f);

		Vec3 v0 = center.subtract(halfForward).subtract(halfSide);
		Vec3 v1 = center.add(halfForward).subtract(halfSide);
		Vec3 v2 = center.add(halfForward).add(halfSide);
		Vec3 v3 = center.subtract(halfForward).add(halfSide);

		emitQuad(consumer, pose, v0, v1, v2, v3, planeNormal, light, r, g, b);
		emitQuad(consumer, pose, v3, v2, v1, v0, planeNormal.scale(-1.0D), light, r, g, b);
	}

	private void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 normal, int light, int r, int g, int b) {
		emitVertex(consumer, pose, v0, normal, light, 0f, 0f, r, g, b);
		emitVertex(consumer, pose, v1, normal, light, 1f, 0f, r, g, b);
		emitVertex(consumer, pose, v2, normal, light, 1f, 1f, r, g, b);
		emitVertex(consumer, pose, v3, normal, light, 0f, 1f, r, g, b);
	}

	private void emitVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, Vec3 normal, int light, float u, float v, int r, int g, int b) {
		consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
				.setColor(r, g, b, 255)
				.setUv(u, v)
				.setUv1(0, 0)
				.setUv2(light & 0xFFFF, light >> 16)
				.setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
	}

	private Vec3 orthogonalVector(Vec3 direction) {
		Vec3 reference = Math.abs(direction.x) < 0.5D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 orthogonal = direction.cross(reference);
		if (orthogonal.lengthSqr() < VECTOR_EPSILON) {
			reference = new Vec3(0.0D, 0.0D, 1.0D);
			orthogonal = direction.cross(reference);
		}
		return orthogonal.normalize();
	}

	private void drawInkQuad(VertexConsumer consumer, Matrix4f poseMatrix, float left, float top, float right, float bottom, int argb, int light) {
		float adjLeft = left;
		float adjRight = right;
		float adjTop = top;
		float adjBottom = bottom;
		int a = (argb >> 24) & 255;
		int r = (argb >> 16) & 255;
		int g = (argb >> 8) & 255;
		int b = argb & 255;

		consumer.addVertex(poseMatrix, adjLeft, adjBottom, 0.0f)
				.setColor(r, g, b, a)
				.setUv(0.0f, 0.0f)
				.setUv1(0, 0)
				.setUv2(light & 0xFFFF, light >> 16)
				.setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(poseMatrix, adjRight, adjBottom, 0.0f)
				.setColor(r, g, b, a)
				.setUv(1.0f, 0.0f)
				.setUv1(0, 0)
				.setUv2(light & 0xFFFF, light >> 16)
				.setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(poseMatrix, adjRight, adjTop, 0.0f)
				.setColor(r, g, b, a)
				.setUv(1.0f, 1.0f)
				.setUv1(0, 0)
				.setUv2(light & 0xFFFF, light >> 16)
				.setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(poseMatrix, adjLeft, adjTop, 0.0f)
				.setColor(r, g, b, a)
				.setUv(0.0f, 1.0f)
				.setUv1(0, 0)
				.setUv2(light & 0xFFFF, light >> 16)
				.setNormal(0.0f, 0.0f, 1.0f);
	}

	private AnchorPoint getAnchor(BoardBlockEntity entity, int slot, float overlaySize) {
		Level level = entity.getLevel();
		if (level == null) {
			return null;
		}
		PoseStack stack = new PoseStack();
		BlockPos pos = entity.getBlockPos();
		stack.translate(pos.getX(), pos.getY(), pos.getZ());
		orientToFacing(stack, entity.getBlockState().getValue(BoardBlock.FACING));
		stack.translate(0.0D, 0.0D, BOARD_SURFACE + SURFACE_CLEARANCE);
		float cellSize = (1f - BOARD_MARGIN * 2f) / GRID_SIZE;
		float x = BOARD_MARGIN + (slot % GRID_SIZE) * cellSize;
		float y = BOARD_MARGIN + (slot / GRID_SIZE) * cellSize;
		stack.translate(x + cellSize / 2f, 1f - (y + cellSize / 2f), 0.0F);
		stack.mulPose(Axis.XP.rotationDegrees(180f));
		stack.mulPose(Axis.ZP.rotationDegrees(entity.getRotation(slot)));
		stack.translate(0.0D, 0.0D, OVERLAY_LAYER_OFFSET);
		stack.scale(overlaySize, overlaySize, 1.0f);
		stack.translate(-0.5f, -0.5f, 0.0f);
		// Local (0.5, 0.0) is top-center before the 180° X-rotation; this results in
		// a world-space anchor at the visual top edge of the photograph.
		Vector4f vector = new Vector4f(0.5f, 0.0f, 0.0f, 1.0f);
		vector.mul(stack.last().pose());
		Vec3 normal = getSurfaceNormal(entity);
		Vec3 anchor = new Vec3(vector.x(), vector.y(), vector.z()).add(normal.scale(STRING_LAYER_OFFSET));
		return new AnchorPoint(anchor, normal);
	}

	private Vec3 getSurfaceNormal(BoardBlockEntity entity) {
		BlockState state = entity.getBlockState();
		Direction facing = state != null ? state.getValue(BoardBlock.FACING) : Direction.NORTH;
		Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
		double length = normal.length();
		return length < 1.0E-6D ? Vec3.ZERO : normal.scale(1.0D / length);
	}

	private record AnchorPoint(Vec3 position, Vec3 normal) {
	}

	private boolean shouldRenderConnection(BlockPos current, int slot, BlockPos other, int otherSlot) {
		int comparison = current.compareTo(other);
		if (comparison < 0) {
			return true;
		}
		if (comparison > 0) {
			return false;
		}
		return slot < otherSlot;
	}

	private BoardBlockEntity getBoardEntity(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof BoardBlockEntity board ? board : null;
	}

	private ResourceLocation getOverlay(ItemStack stack) {
		if (stack.getItem() instanceof AgedPhotographItem) {
			return OVERLAY_AGED;
		}
		if (stack.getItem() instanceof PhotographItem) {
			return OVERLAY_PHOTOGRAPH;
		}
		return null;
	}

	private void renderPin(PoseStack poseStack, VertexConsumer consumer, AnchorPoint anchor, BoardBlockEntity entity, int slot, int light) {
		Vec3 pos = anchor.position();
		Vec3 normal = anchor.normal();
		Direction facing = entity.getBlockState().getValue(BoardBlock.FACING);
		float photoRotation = entity.getRotation(slot);

		poseStack.pushPose();
		poseStack.translate(pos.x, pos.y, pos.z);
		poseStack.translate(normal.x * 0.002f, normal.y * 0.002f, normal.z * 0.002f);
		poseStack.mulPose(createPinRotation(facing, normal, photoRotation));
		poseStack.scale(PIN_MODEL_SCALE, PIN_MODEL_SCALE, PIN_MODEL_SCALE);
		poseStack.translate(-8f, 0f, -8f);

		renderPinVertical(poseStack, consumer, 8f, 0f, 7.25f, 8f, 2f, 8.75f, light);
		renderPinHorizontal(poseStack, consumer, 7.25f, 0f, 8f, 8.75f, 2f, 8f, light);
		renderPinHead(poseStack, consumer, 7f, 2f, 7f, 9f, 2f, 9f, light);
		poseStack.popPose();
	}

	private static Quaternionf createPinRotation(Direction facing, Vec3 normal, float photoRotation) {
		Vector3f outward = new Vector3f((float) normal.x, (float) normal.y, (float) normal.z);
		if (outward.lengthSquared() < 1.0E-6f) {
			return new Quaternionf();
		}
		outward.normalize();

		Vector3f headTarget = switch (facing) {
			case NORTH -> new Vector3f(0, -1, 0);
			case SOUTH -> new Vector3f(0, 1, 0);
			case EAST -> new Vector3f(0, 0, -1);
			case WEST -> new Vector3f(0, 0, 1);
			default -> new Vector3f(0, -1, 0);
		};
		float alignment = headTarget.dot(outward);
		headTarget.sub(new Vector3f(outward).mul(alignment));
		if (headTarget.lengthSquared() < 1.0E-6f) {
			headTarget.set(1, 0, 0);
		}
		headTarget.normalize();

		Quaternionf modelFix = new Quaternionf().rotateX((float) Math.toRadians(-90));
		Quaternionf alignShaft = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), outward);
		Quaternionf combined = alignShaft.mul(modelFix);

		Vector3f currentHead = new Vector3f(0, -1, 0);
		currentHead.rotate(combined);
		combined = new Quaternionf().rotationTo(currentHead, headTarget).mul(combined);
		combined.rotateAxis((float) Math.toRadians(-photoRotation), outward);
		return combined;
	}

	private void renderPinVertical(PoseStack poseStack, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int light) {
		PoseStack.Pose pose = poseStack.last();
		Vec3 v0 = new Vec3(x1, y1, z1);
		Vec3 v1 = new Vec3(x2, y1, z1);
		Vec3 v2 = new Vec3(x2, y2, z1);
		Vec3 v3 = new Vec3(x1, y2, z1);
		Vec3 v4 = new Vec3(x1, y1, z2);
		Vec3 v5 = new Vec3(x2, y1, z2);
		Vec3 v6 = new Vec3(x2, y2, z2);
		Vec3 v7 = new Vec3(x1, y2, z2);
		emitPinQuad(consumer, pose, v0, v1, v2, v3, new Vec3(0, 0, -1), light, 0f, 0f, 0f, 2f);
		emitPinQuad(consumer, pose, v5, v4, v7, v6, new Vec3(0, 0, 1), light, 0f, 0f, 0f, 2f);
		emitPinQuad(consumer, pose, v1, v5, v6, v2, new Vec3(1, 0, 0), light, 2f, 0f, 4f, 2f);
		emitPinQuad(consumer, pose, v4, v0, v3, v7, new Vec3(-1, 0, 0), light, 2f, 0f, 4f, 2f);
		emitPinQuad(consumer, pose, v3, v2, v6, v7, new Vec3(0, 1, 0), light, 0f, 0f, 0f, 1.5f);
		emitPinQuad(consumer, pose, v1, v0, v4, v5, new Vec3(0, -1, 0), light, 0f, 0f, 0f, 1.5f);
	}

	private void renderPinHorizontal(PoseStack poseStack, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int light) {
		PoseStack.Pose pose = poseStack.last();
		Vec3 v0 = new Vec3(x1, y1, z1);
		Vec3 v1 = new Vec3(x2, y1, z1);
		Vec3 v2 = new Vec3(x2, y2, z1);
		Vec3 v3 = new Vec3(x1, y2, z1);
		Vec3 v4 = new Vec3(x1, y1, z2);
		Vec3 v5 = new Vec3(x2, y1, z2);
		Vec3 v6 = new Vec3(x2, y2, z2);
		Vec3 v7 = new Vec3(x1, y2, z2);
		emitPinQuad(consumer, pose, v0, v1, v2, v3, new Vec3(0, 0, -1), light, 2f, 0f, 4f, 2f);
		emitPinQuad(consumer, pose, v5, v4, v7, v6, new Vec3(0, 0, 1), light, 2f, 0f, 4f, 2f);
		emitPinQuad(consumer, pose, v1, v5, v6, v2, new Vec3(1, 0, 0), light, 0f, 0f, 0f, 2f);
		emitPinQuad(consumer, pose, v4, v0, v3, v7, new Vec3(-1, 0, 0), light, 0f, 0f, 0f, 2f);
		emitPinQuad(consumer, pose, v3, v2, v6, v7, new Vec3(0, 1, 0), light, 0f, 0f, 1.5f, 0f);
		emitPinQuad(consumer, pose, v1, v0, v4, v5, new Vec3(0, -1, 0), light, 0f, 0f, 1.5f, 0f);
	}

	private void renderPinHead(PoseStack poseStack, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int light) {
		PoseStack.Pose pose = poseStack.last();
		Vec3 v0 = new Vec3(x1, y1, z1);
		Vec3 v1 = new Vec3(x2, y1, z1);
		Vec3 v2 = new Vec3(x2, y1, z2);
		Vec3 v3 = new Vec3(x1, y1, z2);
		emitPinQuad(consumer, pose, v0, v1, v2, v3, new Vec3(0, 1, 0), light, 0f, 0f, 2f, 2f);
		emitPinQuad(consumer, pose, v3, v2, v1, v0, new Vec3(0, -1, 0), light, 0f, 0f, 2f, 2f);
	}

	private void emitPinQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 normal, int light, float uMin, float vMin, float uMax, float vMax) {
		float uMinNorm = uMin / 16f;
		float vMinNorm = vMin / 16f;
		float uMaxNorm = uMax / 16f;
		float vMaxNorm = vMax / 16f;
		emitPinVertex(consumer, pose, v0, normal, light, uMinNorm, vMinNorm);
		emitPinVertex(consumer, pose, v1, normal, light, uMaxNorm, vMinNorm);
		emitPinVertex(consumer, pose, v2, normal, light, uMaxNorm, vMaxNorm);
		emitPinVertex(consumer, pose, v3, normal, light, uMinNorm, vMaxNorm);
	}

	private void emitPinVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, Vec3 normal, int light, float u, float v) {
		consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setUv1(0, 0)
				.setUv2(light & 0xFFFF, light >> 16)
				.setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
	}
}

