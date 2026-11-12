package com.mineblock.exposuredetective.client.ink;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.client.drawing.ClientDrawingCache;
import com.mineblock.exposuredetective.client.drawing.InkTextureRenderer;
import com.mineblock.exposuredetective.drawing.DrawingCodec;
import com.mineblock.exposuredetective.drawing.DrawingItemData;
import com.mineblock.exposuredetective.drawing.DrawingRaster;
import com.mineblock.exposuredetective.init.ExposuredetectiveModItems;
import com.mineblock.exposuredetective.item.InkItem;
import com.mineblock.exposuredetective.network.DrawingRequestPayload;
import com.mineblock.exposuredetective.network.DrawingSavePayload;
import com.mineblock.exposuredetective.util.PhotographCoordinates;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;


@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ExposuredetectiveMod.MOD_ID, value = Dist.CLIENT)
public final class PhotographInkClient {

	private static final ResourceLocation MAGNIFIER_ICON = ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "textures/gui/magnifier_overlay.png");
	private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
	private static final int BASE_ICON_SIZE = 64;
	private static final double MAGNIFIER_BASE_SCALE = 1.5d;
	private static final double VECTOR_EPSILON = 1.0E-5D;
	private static final float MIN_STROKE_UV = PhotographCoordinates.PIXEL_UV * 0.35f;
	private static final int GHOST_BRUSH_ALPHA = 0x66;
	private static final int MIN_BRUSH_SIZE = 1;
	private static final int MAX_BRUSH_SIZE = 8;

	private static boolean drawing;
	private static boolean draggingMagnifier;
	private static boolean sessionActive;
	private static boolean sessionDirty;
	private static double dragOffsetX;
	private static double dragOffsetY;
	private static double magnifierX = Double.NaN;
	private static double magnifierY = Double.NaN;
	private static Vec2 lastDrawPoint;
	private static int brushSize = 1;

	
	private static DrawingRaster sessionRaster = new DrawingRaster();
	private static UUID sessionDrawingId;
	
	private static boolean sessionForked;
	private static boolean sessionTextureDirty = true;
	private static DynamicTexture sessionVisibleTexture;
	private static DynamicTexture sessionInvisibleTexture;
	private static ResourceLocation sessionVisibleLoc;
	private static ResourceLocation sessionInvisibleLoc;

	private PhotographInkClient() {
	}

	

	@SubscribeEvent
	public static void onScreenOpening(ScreenEvent.Opening event) {
		if (!(event.getNewScreen() instanceof PhotographScreen screen)) {
			return;
		}
		
		ItemStack stack = screen.getCurrentPhotograph().getItemStack();
		if (stack.isEmpty() || !(stack.getItem() instanceof PhotographItem)) {
			return;
		}
		beginSession(stack);
	}

	@SubscribeEvent
	public static void onScreenClosing(ScreenEvent.Closing event) {
		if (!PhotographScreenAccess.isPhotographScreen(event.getScreen())) {
			return;
		}
		endSessionAndSave();
	}

	private static void beginSession(ItemStack stack) {
		sessionActive = true;
		sessionDirty = false;
		sessionForked = false;
		sessionTextureDirty = true;
		drawing = false;
		draggingMagnifier = false;
		lastDrawPoint = null;
		magnifierX = Double.NaN;
		magnifierY = Double.NaN;

		sessionDrawingId = DrawingItemData.getDrawingId(stack).orElse(null);
		DrawingRaster fromAsset = null;
		if (sessionDrawingId != null) {
			fromAsset = ClientDrawingCache.getRaster(sessionDrawingId);
			if (fromAsset == null) {
				PacketDistributor.sendToServer(new DrawingRequestPayload(sessionDrawingId));
			}
		}
		sessionRaster = DrawingItemData.buildEditorRaster(stack, fromAsset);
	}

	private static void endSessionAndSave() {
		if (!sessionActive) {
			return;
		}
		sessionActive = false;
		drawing = false;
		draggingMagnifier = false;
		lastDrawPoint = null;

		if (!sessionDirty) {
			sessionRaster = new DrawingRaster();
			sessionDrawingId = null;
			return;
		}

		try {
			byte[] encoded = DrawingCodec.encode(sessionRaster);
			UUID id = sessionDrawingId != null ? sessionDrawingId : UUID.randomUUID();
			PacketDistributor.sendToServer(new DrawingSavePayload(InteractionHand.MAIN_HAND, id, encoded));
		} catch (IOException e) {
			ExposuredetectiveMod.LOGGER.error("Failed to encode drawing on close", e);
		}
		releaseSessionTextures();
		sessionRaster = new DrawingRaster();
		sessionDrawingId = null;
		sessionDirty = false;
		sessionForked = false;
	}

	
	public static void onRasterArrived(UUID id, DrawingRaster raster) {
		if (!sessionActive || sessionDirty || sessionForked) {
			return;
		}
		if (sessionDrawingId != null && sessionDrawingId.equals(id)) {
			sessionRaster = DrawingRaster.copyOf(raster);
			sessionTextureDirty = true;
		}
	}

	

	@SubscribeEvent
	public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
		if (!(event.getScreen() instanceof PhotographScreen screen) || event.getButton() != 1) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (hasMagnifier(player) && isOverMagnifier(event.getMouseX(), event.getMouseY(), screen)) {
			startDraggingMagnifier(event.getMouseX(), event.getMouseY());
			event.setCanceled(true);
			return;
		}
		if (!canDraw(player, screen)) {
			return;
		}
		InkProperties ink = getInkProperties(player);
		if (!ink.isValid()) {
			return;
		}
		Optional<Vec2> uv = mouseToPhoto(screen, event.getMouseX(), event.getMouseY());
		if (uv.isEmpty()) {
			return;
		}
		beginDrawing(uv.get(), ink);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		if (!(event.getScreen() instanceof PhotographScreen) || event.getButton() != 1) {
			return;
		}
		drawing = false;
		draggingMagnifier = false;
		lastDrawPoint = null;
	}

	@SubscribeEvent
	public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (!(event.getScreen() instanceof PhotographScreen screen)) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (draggingMagnifier) {
			moveMagnifier(event.getMouseX(), event.getMouseY(), screen);
			event.setCanceled(true);
			return;
		}
		if (!drawing || !canDraw(player, screen)) {
			return;
		}
		InkProperties ink = getInkProperties(player);
		if (!ink.isValid()) {
			return;
		}
		Optional<Vec2> uv = mouseToPhoto(screen, event.getMouseX(), event.getMouseY());
		if (uv.isEmpty()) {
			return;
		}
		if (lastDrawPoint != null && distanceSq(lastDrawPoint, uv.get()) < MIN_STROKE_UV * MIN_STROKE_UV) {
			return;
		}
		continueStroke(uv.get(), ink);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (!(event.getScreen() instanceof PhotographScreen)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.options == null) {
			return;
		}
		int keyCode = event.getKeyCode();
		if (keyCode == mc.options.keyUp.getKey().getValue()) {
			brushSize = Math.min(MAX_BRUSH_SIZE, brushSize + 1);
			event.setCanceled(true);
		} else if (keyCode == mc.options.keyDown.getKey().getValue()) {
			brushSize = Math.max(MIN_BRUSH_SIZE, brushSize - 1);
			event.setCanceled(true);
		}
	}

	

	private static void beginDrawing(Vec2 uv, InkProperties ink) {
		drawing = true;
		lastDrawPoint = null;
		continueStroke(uv, ink);
	}

	private static void continueStroke(Vec2 uv, InkProperties ink) {
		if (lastDrawPoint == null) {
			applyBrush(uv, ink);
			lastDrawPoint = PhotographCoordinates.snapToPixelCenter(uv.x, uv.y);
			return;
		}
		float distance = (float) Math.sqrt(distanceSq(lastDrawPoint, uv));
		int steps = Math.max(1, Mth.ceil(distance / PhotographCoordinates.PIXEL_UV));
		for (int i = 1; i <= steps; i++) {
			float t = i / (float) steps;
			Vec2 point = new Vec2(
					Mth.lerp(t, lastDrawPoint.x, uv.x),
					Mth.lerp(t, lastDrawPoint.y, uv.y));
			applyBrush(point, ink);
		}
		lastDrawPoint = PhotographCoordinates.snapToPixelCenter(uv.x, uv.y);
	}

	private static void applyBrush(Vec2 uv, InkProperties ink) {
		Vec2 center = PhotographCoordinates.snapToPixelCenter(uv.x, uv.y);
		int centerPx = PhotographCoordinates.pixelIndex(center.x);
		int centerPy = PhotographCoordinates.pixelIndex(center.y);
		int half = brushSize / 2;
		for (int dx = -half; dx <= half; dx++) {
			for (int dy = -half; dy <= half; dy++) {
				int px = Mth.clamp(centerPx + dx, 0, DrawingRaster.SIZE - 1);
				int py = Mth.clamp(centerPy + dy, 0, DrawingRaster.SIZE - 1);
				sessionRaster.setPixel(px, py, ink.color(), ink.alwaysVisible());
			}
		}
		if (!sessionForked) {
			
			sessionDrawingId = UUID.randomUUID();
			sessionForked = true;
		}
		sessionTextureDirty = true;
		sessionDirty = true;
	}

	

	
	

	@SubscribeEvent
	public static void onScreenRender(ScreenEvent.Render.Post event) {
		if (!(event.getScreen() instanceof PhotographScreen screen)) {
			return;
		}
		
		if (!sessionActive) {
			ItemStack stack = screen.getCurrentPhotograph().getItemStack();
			if (!stack.isEmpty() && stack.getItem() instanceof PhotographItem) {
				beginSession(stack);
			}
		}
		
		if (sessionActive && !sessionDirty && sessionDrawingId != null) {
			DrawingRaster cached = ClientDrawingCache.getRaster(sessionDrawingId);
			if (cached != null && sessionRaster.isBlank()) {
				sessionRaster = DrawingRaster.copyOf(cached);
			}
		}

		GuiGraphics graphics = event.getGuiGraphics();
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		double scale = PhotographScreenAccess.displayScale(screen);
		ensureSessionTextures();
		renderSessionTexture(graphics, screen, scale, true, null);
		if (hasMagnifier(player)) {
			ensureMagnifierPosition(screen);
			int iconSize = currentIconSize(screen);
			int[] scissor = new int[]{
					(int) Math.round(magnifierX),
					(int) Math.round(magnifierY),
					(int) Math.round(magnifierX) + iconSize,
					(int) Math.round(magnifierY) + iconSize
			};
			renderSessionTexture(graphics, screen, scale, false, scissor);
			renderMagnifierIcon(graphics, screen);
		}
		renderBrushGhost(graphics, screen, player, scale, event.getMouseX(), event.getMouseY());
		renderInkCursor(graphics, event.getMouseX(), event.getMouseY());
	}



	private static void ensureSessionTextures() {
		if (!sessionTextureDirty && sessionVisibleTexture != null && sessionInvisibleTexture != null) {
			return;
		}
		int size = DrawingRaster.SIZE;
		if (sessionVisibleTexture == null) {
			sessionVisibleTexture = new DynamicTexture(size, size, true);
			sessionVisibleLoc = ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "session_ink_visible");
			Minecraft.getInstance().getTextureManager().register(sessionVisibleLoc, sessionVisibleTexture);
		}
		if (sessionInvisibleTexture == null) {
			sessionInvisibleTexture = new DynamicTexture(size, size, true);
			sessionInvisibleLoc = ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "session_ink_invisible");
			Minecraft.getInstance().getTextureManager().register(sessionInvisibleLoc, sessionInvisibleTexture);
		}
		NativeImage vis = sessionVisibleTexture.getPixels();
		NativeImage inv = sessionInvisibleTexture.getPixels();
		if (vis == null || inv == null) {
			return;
		}
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				int color = sessionRaster.getColor(px, py);
				if (color == 0) {
					vis.setPixelRGBA(px, py, 0);
					inv.setPixelRGBA(px, py, 0);
					continue;
				}
				int a = (color >> 24) & 255;
				int r = (color >> 16) & 255;
				int g = (color >> 8) & 255;
				int b = color & 255;
				
				int abgr = (a << 24) | (b << 16) | (g << 8) | r;
				if (sessionRaster.isAlwaysVisible(px, py)) {
					vis.setPixelRGBA(px, py, abgr);
					inv.setPixelRGBA(px, py, 0);
				} else {
					vis.setPixelRGBA(px, py, 0);
					
					int invAbgr = (0xAA << 24) | (b << 16) | (g << 8) | r;
					inv.setPixelRGBA(px, py, invAbgr);
				}
			}
		}
		sessionVisibleTexture.upload();
		sessionInvisibleTexture.upload();
		var tm = Minecraft.getInstance().getTextureManager();
		if (sessionVisibleLoc != null) {
			tm.getTexture(sessionVisibleLoc).setFilter(false, false);
		}
		if (sessionInvisibleLoc != null) {
			tm.getTexture(sessionInvisibleLoc).setFilter(false, false);
		}
		sessionTextureDirty = false;
	}

	
	private static void renderSessionTexture(GuiGraphics graphics, PhotographScreen screen, double scale,
											 boolean visibleLayer, int[] scissorOrNull) {
		if (scale < VECTOR_EPSILON) {
			return;
		}
		ResourceLocation loc = visibleLayer ? sessionVisibleLoc : sessionInvisibleLoc;
		if (loc == null) {
			return;
		}
		double centerX = screen.width / 2.0 + PhotographScreenAccess.offsetX(screen);
		double centerY = screen.height / 2.0 + PhotographScreenAccess.offsetY(screen);
		float u0 = PhotographCoordinates.PHOTO_UV_MIN;
		float u1 = PhotographCoordinates.PHOTO_UV_MAX;
		float v0 = PhotographCoordinates.PHOTO_UV_MIN;
		float v1 = PhotographCoordinates.PHOTO_UV_MAX;
		int left = (int) Math.floor((u0 - 0.5f) * scale + centerX);
		int top = (int) Math.floor((v0 - 0.5f) * scale + centerY);
		int right = (int) Math.ceil((u1 - 0.5f) * scale + centerX);
		int bottom = (int) Math.ceil((v1 - 0.5f) * scale + centerY);
		int w = Math.max(1, right - left);
		int h = Math.max(1, bottom - top);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, visibleLayer ? 480.0F : 500.0F);
		if (scissorOrNull != null) {
			graphics.enableScissor(scissorOrNull[0], scissorOrNull[1], scissorOrNull[2], scissorOrNull[3]);
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		
		graphics.blit(loc, left, top, w, h, 0, 0, DrawingRaster.SIZE, DrawingRaster.SIZE, DrawingRaster.SIZE, DrawingRaster.SIZE);
		if (scissorOrNull != null) {
			graphics.disableScissor();
		}
		RenderSystem.disableBlend();
		graphics.pose().popPose();
	}

	private static void releaseSessionTextures() {
		var tm = Minecraft.getInstance().getTextureManager();
		if (sessionVisibleLoc != null) {
			tm.release(sessionVisibleLoc);
		}
		if (sessionInvisibleLoc != null) {
			tm.release(sessionInvisibleLoc);
		}
		sessionVisibleTexture = null;
		sessionInvisibleTexture = null;
		sessionVisibleLoc = null;
		sessionInvisibleLoc = null;
		sessionTextureDirty = true;
	}

	private static void renderBrushGhost(GuiGraphics graphics, PhotographScreen screen, Player player,
										 double scale, double mouseX, double mouseY) {
		if (!canDraw(player, screen)) {
			return;
		}
		InkProperties ink = getInkProperties(player);
		if (!ink.isValid()) {
			return;
		}
		Optional<Vec2> center = mouseToPhoto(screen, mouseX, mouseY);
		if (center.isEmpty()) {
			return;
		}
		int centerPx = PhotographCoordinates.pixelIndex(center.get().x);
		int centerPy = PhotographCoordinates.pixelIndex(center.get().y);
		int half = brushSize / 2;
		int ghostColor = (GHOST_BRUSH_ALPHA << 24) | (ink.color() & 0x00FFFFFF);
		double halfScreen = scale * PhotographCoordinates.PIXEL_UV * 0.5;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 520.0F);
		for (int dx = -half; dx <= half; dx++) {
			for (int dy = -half; dy <= half; dy++) {
				int px = Mth.clamp(centerPx + dx, 0, DrawingRaster.SIZE - 1);
				int py = Mth.clamp(centerPy + dy, 0, DrawingRaster.SIZE - 1);
				Vec2 uv = new Vec2(PhotographCoordinates.pixelToU(px), PhotographCoordinates.pixelToV(py));
				drawPixel(graphics, photoToScreen(screen, uv), halfScreen, ghostColor);
			}
		}
		double frameHalf = halfScreen * brushSize + 1.0;
		Vec2 centerScreen = photoToScreen(screen, center.get());
		int left = (int) Math.round(centerScreen.x - frameHalf);
		int top = (int) Math.round(centerScreen.y - frameHalf);
		int right = (int) Math.round(centerScreen.x + frameHalf);
		int bottom = (int) Math.round(centerScreen.y + frameHalf);
		int frameColor = 0xAAFFFFFF;
		graphics.fill(left, top, right, top + 1, frameColor);
		graphics.fill(left, bottom - 1, right, bottom, frameColor);
		graphics.fill(left, top, left + 1, bottom, frameColor);
		graphics.fill(right - 1, top, right, bottom, frameColor);
		graphics.pose().popPose();
	}

	private static void renderMagnifierIcon(GuiGraphics graphics, PhotographScreen screen) {
		int size = currentIconSize(screen);
		double scale = size / (double) BASE_ICON_SIZE;
		int x = (int) Math.round(magnifierX);
		int y = (int) Math.round(magnifierY);
		graphics.pose().pushPose();
		graphics.pose().translate((float) x, (float) y, 600.0F);
		graphics.pose().scale((float) scale, (float) scale, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.blit(MAGNIFIER_ICON, 0, 0, 0, 0, BASE_ICON_SIZE, BASE_ICON_SIZE, BASE_ICON_SIZE, BASE_ICON_SIZE);
		RenderSystem.disableBlend();
		graphics.pose().popPose();
	}

	private static void renderInkCursor(GuiGraphics graphics, double mouseX, double mouseY) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		ItemStack ink = findInkStack(player);
		if (ink.isEmpty()) {
			return;
		}
		float s = 1.5f;
		float size = 16f * s;
		graphics.pose().pushPose();
		graphics.pose().translate((float) mouseX + 8, (float) mouseY - size - 4, 700f);
		graphics.pose().scale(s, s, 1.0f);
		graphics.renderItem(ink, 0, 0);
		graphics.pose().popPose();
	}

	

	private static Optional<Vec2> mouseToPhoto(PhotographScreen screen, double mouseX, double mouseY) {
		double scale = PhotographScreenAccess.displayScale(screen);
		if (scale < VECTOR_EPSILON) {
			return Optional.empty();
		}
		double centerX = screen.width / 2.0 + PhotographScreenAccess.offsetX(screen);
		double centerY = screen.height / 2.0 + PhotographScreenAccess.offsetY(screen);
		float u = (float) (((mouseX - centerX) / scale) + 0.5);
		float v = (float) (((mouseY - centerY) / scale) + 0.5);
		if (!PhotographCoordinates.isOnPhoto(u, v)) {
			return Optional.empty();
		}
		return Optional.of(PhotographCoordinates.snapToPixelCenter(u, v));
	}

	private static Vec2 photoToScreen(PhotographScreen screen, Vec2 uv) {
		double scale = PhotographScreenAccess.displayScale(screen);
		double centerX = screen.width / 2.0 + PhotographScreenAccess.offsetX(screen);
		double centerY = screen.height / 2.0 + PhotographScreenAccess.offsetY(screen);
		return new Vec2(
				(float) ((uv.x - 0.5f) * scale + centerX),
				(float) ((uv.y - 0.5f) * scale + centerY));
	}

	private static void ensureMagnifierPosition(PhotographScreen screen) {
		int size = currentIconSize(screen);
		if (Double.isNaN(magnifierX) || Double.isNaN(magnifierY)) {
			magnifierX = screen.width - size - 12.0;
			magnifierY = (screen.height - size) / 2.0;
		}
		magnifierX = clamp(magnifierX, 4.0, screen.width - size - 4.0);
		magnifierY = clamp(magnifierY, 4.0, screen.height - size - 4.0);
	}

	private static void moveMagnifier(double mouseX, double mouseY, PhotographScreen screen) {
		int size = currentIconSize(screen);
		magnifierX = clamp(mouseX + dragOffsetX, 4.0, screen.width - size - 4.0);
		magnifierY = clamp(mouseY + dragOffsetY, 4.0, screen.height - size - 4.0);
	}

	private static void startDraggingMagnifier(double mouseX, double mouseY) {
		draggingMagnifier = true;
		dragOffsetX = magnifierX - mouseX;
		dragOffsetY = magnifierY - mouseY;
	}

	private static boolean isOverMagnifier(double mouseX, double mouseY, PhotographScreen screen) {
		int size = currentIconSize(screen);
		return mouseX >= magnifierX && mouseX <= magnifierX + size
				&& mouseY >= magnifierY && mouseY <= magnifierY + size;
	}

	private static int currentIconSize(PhotographScreen screen) {
		return (int) Math.max(32.0d, BASE_ICON_SIZE * MAGNIFIER_BASE_SCALE * PhotographScreenAccess.zoom(screen));
	}

	private static boolean canDraw(Player player, PhotographScreen screen) {
		if (!(player.getMainHandItem().getItem() instanceof PhotographItem)) {
			return false;
		}
		if (!getInkProperties(player).isValid()) {
			return false;
		}
		ItemStack current = screen.getCurrentPhotograph().getItemStack();
		return ItemStack.isSameItemSameComponents(player.getMainHandItem(), current);
	}

	private static boolean hasMagnifier(Player player) {
		return player.getMainHandItem().is(ExposuredetectiveModItems.MAGNIFIER.get())
				|| player.getOffhandItem().is(ExposuredetectiveModItems.MAGNIFIER.get());
	}

	private static InkProperties getInkProperties(Player player) {
		ItemStack stack = player.getOffhandItem();
		if (stack.getItem() instanceof InkItem ink) {
			return new InkProperties(stack, ink.getColor(), ink.isAlwaysVisible());
		}
		return InkProperties.EMPTY;
	}

	private static ItemStack findInkStack(Player player) {
		ItemStack offhand = player.getOffhandItem();
		return offhand.getItem() instanceof InkItem ? offhand : ItemStack.EMPTY;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float distanceSq(Vec2 first, Vec2 second) {
		float dx = first.x - second.x;
		float dy = first.y - second.y;
		return dx * dx + dy * dy;
	}

	private static void drawPixel(GuiGraphics graphics, Vec2 screenPos, double halfSize, int color) {
		int left = (int) Math.round(screenPos.x - halfSize);
		int top = (int) Math.round(screenPos.y - halfSize);
		int right = (int) Math.round(screenPos.x + halfSize);
		int bottom = (int) Math.round(screenPos.y + halfSize);
		if (right > left && bottom > top) {
			graphics.fill(left, top, right, bottom, color);
		}
	}

	private static void drawInkQuadOnHand(VertexConsumer consumer, Matrix4f poseMatrix,
										  float left, float top, float right, float bottom,
										  int argb, int light) {
		int a = (argb >> 24) & 255;
		int r = (argb >> 16) & 255;
		int g = (argb >> 8) & 255;
		int b = argb & 255;
		consumer.addVertex(poseMatrix, left, bottom, 0.0f).setColor(r, g, b, a).setUv(0.0f, 0.0f).setUv1(0, 0).setUv2(light & 0xFFFF, light >> 16).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(poseMatrix, right, bottom, 0.0f).setColor(r, g, b, a).setUv(1.0f, 0.0f).setUv1(0, 0).setUv2(light & 0xFFFF, light >> 16).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(poseMatrix, right, top, 0.0f).setColor(r, g, b, a).setUv(1.0f, 1.0f).setUv1(0, 0).setUv2(light & 0xFFFF, light >> 16).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(poseMatrix, left, top, 0.0f).setColor(r, g, b, a).setUv(0.0f, 1.0f).setUv1(0, 0).setUv2(light & 0xFFFF, light >> 16).setNormal(0.0f, 0.0f, 1.0f);
	}

	private record InkProperties(ItemStack stack, int color, boolean alwaysVisible) {
		private static final InkProperties EMPTY = new InkProperties(ItemStack.EMPTY, 0, false);

		boolean isValid() {
			return !stack.isEmpty();
		}
	}
}
