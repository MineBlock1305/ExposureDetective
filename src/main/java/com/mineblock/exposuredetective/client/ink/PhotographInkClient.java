package com.mineblock.exposuredetective.client.ink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.init.ExposuredetectiveModItems;
import com.mineblock.exposuredetective.item.InkItem;
import com.mineblock.exposuredetective.network.InvisibleInkDrawBatchPayload;
import com.mineblock.exposuredetective.util.InvisibleInkData;
import com.mineblock.exposuredetective.util.PhotographCoordinates;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.Minecraft;
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

/**
 * Клиентская логика рисования чернилами на фотографии (PhotographScreen).
 * <ul>
 *   <li>ПКМ — рисовать</li>
 *   <li>Up/Down — размер кисти</li>
 *   <li>Лупа в руке — показывает невидимые чернила в своей области</li>
 *   <li>Призрак кисти под курсором</li>
 *   <li>Повторная запись в пиксель заменяет цвет</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ExposuredetectiveMod.MODID, value = Dist.CLIENT)
public final class PhotographInkClient {

    private static final ResourceLocation MAGNIFIER_ICON = ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MODID, "textures/gui/magnifier_overlay.png");
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final int BASE_ICON_SIZE = 64;
    private static final double MAGNIFIER_BASE_SCALE = 1.5d;
    private static final double VECTOR_EPSILON = 1.0E-5D;
    private static final float MIN_STROKE_UV = PhotographCoordinates.PIXEL_UV * 0.35f;
    private static final int GHOST_BRUSH_ALPHA = 0x66;
    private static final int MIN_BRUSH_SIZE = 1;
    private static final int MAX_BRUSH_SIZE = 8;

    // --- состояние сессии рисования ---
    private static boolean drawing;
    private static boolean draggingMagnifier;
    private static double dragOffsetX;
    private static double dragOffsetY;
    private static double magnifierX = Double.NaN;
    private static double magnifierY = Double.NaN;
    private static Vec2 lastDrawPoint;
    private static int brushSize = 1;

    /** Локальный предикт: pixelKey → mark. Перезаписывается при рисовании. */
    private static final Map<Long, ClientInkMark> pendingMarks = new HashMap<>();
    /** Пиксели текущего штриха (чтобы не слать дубликаты в одном жесте). */
    private static final Set<Long> strokePixels = new HashSet<>();

    private static ItemStack trackedStack = ItemStack.EMPTY;
    private static int lastKnownServerCount;

    private PhotographInkClient() {
    }

    // =========================================================================
    // Events
    // =========================================================================

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (PhotographScreenAccess.isPhotographScreen(event.getNewScreen())) {
            resetState();
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (PhotographScreenAccess.isPhotographScreen(event.getScreen())) {
            resetState();
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

        beginDrawing(screen, uv.get(), ink);
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
        strokePixels.clear();
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

        continueStroke(screen, uv.get(), ink);
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

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof PhotographItem)) {
            return;
        }
        List<InvisibleInkData.InkMark> marks = InvisibleInkData.getMarks(stack);
        if (marks.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));

        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.translate(0.0f, 0.0f, 0.001f);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        float half = PhotographCoordinates.PIXEL_UV * 0.5f;

        for (InvisibleInkData.InkMark mark : marks) {
            if (!mark.alwaysVisible()) {
                continue;
            }
            float imageU = PhotographCoordinates.toImageU(mark.uv().x);
            float imageV = PhotographCoordinates.toImageV(mark.uv().y);
            float left = imageU - half;
            float right = imageU + half;
            float top = imageV - half;
            float bottom = imageV + half;
            drawInkQuadOnHand(consumer, matrix, left, top, right, bottom, mark.color(), packedLight);
        }

        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof PhotographScreen screen)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack photoStack = screen.getCurrentPhotograph().getItemStack();
        updateTracking(photoStack);
        double scale = PhotographScreenAccess.displayScale(screen);

        // Постоянные (alwaysVisible) метки
        renderInkMarks(graphics, screen, photoStack, scale, 480.0F, true);
        if (hasMagnifier(player)) {
            ensureMagnifierPosition(screen);
            renderRevealedInk(graphics, screen, photoStack, scale);
            renderMagnifierIcon(graphics, screen);
        }

        // Призрак кисти под курсором + иконка чернил
        renderBrushGhost(graphics, screen, player, scale, event.getMouseX(), event.getMouseY());
        renderInkCursor(graphics, event.getMouseX(), event.getMouseY());
    }

    // =========================================================================
    // Drawing
    // =========================================================================

    private static void beginDrawing(PhotographScreen screen, Vec2 uv, InkProperties ink) {
        drawing = true;
        strokePixels.clear();
        lastDrawPoint = null;
        continueStroke(screen, uv, ink);
    }

    private static void continueStroke(PhotographScreen screen, Vec2 uv, InkProperties ink) {
        if (lastDrawPoint == null) {
            applyBrush(screen, uv, ink);
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
            applyBrush(screen, point, ink);
        }
        lastDrawPoint = PhotographCoordinates.snapToPixelCenter(uv.x, uv.y);
    }

    private static void applyBrush(PhotographScreen screen, Vec2 uv, InkProperties ink) {
        Vec2 center = PhotographCoordinates.snapToPixelCenter(uv.x, uv.y);
        int centerPx = PhotographCoordinates.pixelIndex(center.x);
        int centerPy = PhotographCoordinates.pixelIndex(center.y);
        int half = brushSize / 2;

        List<Vec2> newPixels = new ArrayList<>(brushSize * brushSize);

        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                int px = Mth.clamp(centerPx + dx, 0, PhotographCoordinates.PHOTO_PIXELS - 1);
                int py = Mth.clamp(centerPy + dy, 0, PhotographCoordinates.PHOTO_PIXELS - 1);
                long key = packPixel(px, py);

                // В рамках одного штриха не дублируем
                if (!strokePixels.add(key)) {
                    continue;
                }

                float u = PhotographCoordinates.pixelToU(px);
                float v = PhotographCoordinates.pixelToV(py);
                Vec2 pixelUv = new Vec2(u, v);

                // Предикт: всегда перезаписываем
                pendingMarks.put(key, new ClientInkMark(pixelUv, ink.color(), ink.alwaysVisible()));
                newPixels.add(pixelUv);
            }
        }

        if (!newPixels.isEmpty()) {
            PacketDistributor.sendToServer(
                    new InvisibleInkDrawBatchPayload(InteractionHand.MAIN_HAND, newPixels));
        }
    }

    // =========================================================================
    // Rendering helpers
    // =========================================================================

    private static void renderInkMarks(GuiGraphics graphics, PhotographScreen screen, ItemStack stack,
                                       double scale, float z, boolean alwaysVisibleOnly) {
        if (stack.isEmpty() || scale < VECTOR_EPSILON) {
            return;
        }
        List<InkRenderMark> marks = collectMarks(stack);
        if (marks.isEmpty()) {
            return;
        }
        double halfPixel = scale * PhotographCoordinates.PIXEL_UV * 0.5;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, z);
        for (InkRenderMark mark : marks) {
            if (alwaysVisibleOnly && !mark.alwaysVisible()) {
                continue;
            }
            if (!alwaysVisibleOnly && mark.alwaysVisible()) {
                continue;
            }
            Vec2 screenPos = photoToScreen(screen, mark.uv());
            drawPixel(graphics, screenPos, halfPixel, mark.color());
        }
        graphics.pose().popPose();
    }


    private static void renderRevealedInk(GuiGraphics graphics, PhotographScreen screen, ItemStack stack, double scale) {
        List<InkRenderMark> marks = collectMarks(stack);
        if (marks.isEmpty() || scale < VECTOR_EPSILON) {
            return;
        }
        int iconSize = currentIconSize(screen);
        int scissorX = (int) Math.round(magnifierX);
        int scissorY = (int) Math.round(magnifierY);
        double centerX = magnifierX + iconSize / 2.0;
        double centerY = magnifierY + iconSize / 2.0;
        Optional<Vec2> centerUv = mouseToPhoto(screen, centerX, centerY);
        if (centerUv.isEmpty()) {
            return;
        }
        double radius = (iconSize / 2.0) / scale;
        double halfPixel = scale * PhotographCoordinates.PIXEL_UV * 0.5;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);
        graphics.enableScissor(scissorX, scissorY, scissorX + iconSize, scissorY + iconSize);
        for (InkRenderMark mark : marks) {
            if (mark.alwaysVisible()) {
                continue;
            }
            Vec2 uv = mark.uv();
            if (Math.abs(uv.x - centerUv.get().x) > radius || Math.abs(uv.y - centerUv.get().y) > radius) {
                continue;
            }
            drawPixel(graphics, photoToScreen(screen, uv), halfPixel, (mark.color() & 0x00FFFFFF) | 0xAA000000);
        }
        graphics.disableScissor();
        graphics.pose().popPose();
    }

    /**
     * Призрак кисти под курсором: полупрозрачный квадрат размера текущей кисти.
     */
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
                int px = Mth.clamp(centerPx + dx, 0, PhotographCoordinates.PHOTO_PIXELS - 1);
                int py = Mth.clamp(centerPy + dy, 0, PhotographCoordinates.PHOTO_PIXELS - 1);
                Vec2 uv = new Vec2(PhotographCoordinates.pixelToU(px), PhotographCoordinates.pixelToV(py));
                drawPixel(graphics, photoToScreen(screen, uv), halfScreen, ghostColor);
            }
        }
        // Рамка вокруг кисти для лучшей читаемости
        int frameColor = 0xAAFFFFFF;
        double frameHalf = halfScreen * (brushSize) + 1.0;
        Vec2 centerScreen = photoToScreen(screen, center.get());
        int left = (int) Math.round(centerScreen.x - frameHalf);
        int top = (int) Math.round(centerScreen.y - frameHalf);
        int right = (int) Math.round(centerScreen.x + frameHalf);
        int bottom = (int) Math.round(centerScreen.y + frameHalf);
        // тонкая рамка
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
        float scale = 1.5f;
        float size = 16f * scale;
        graphics.pose().pushPose();
        graphics.pose().translate((float) mouseX + 8, (float) mouseY - size - 4, 700f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(ink, 0, 0);
        graphics.pose().popPose();
    }

    // =========================================================================
    // Coordinate / tracking
    // =========================================================================

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

    private static void updateTracking(ItemStack stack) {
        if (stack.isEmpty()) {
            trackedStack = ItemStack.EMPTY;
            lastKnownServerCount = 0;
            pendingMarks.clear();
            return;
        }
        boolean same = !trackedStack.isEmpty() && ItemStack.isSameItemSameComponents(trackedStack, stack);
        int serverCount = InvisibleInkData.count(stack);
        if (!same) {
            trackedStack = stack.copy();
            lastKnownServerCount = serverCount;
            pendingMarks.clear();
            return;
        }
        // Сервер подтвердил изменения — можно сбросить предикт
        if (serverCount != lastKnownServerCount) {
            lastKnownServerCount = serverCount;
            trackedStack = stack.copy();
            pendingMarks.clear();
        }
    }

    private static List<InkRenderMark> collectMarks(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        // Сначала серверные, потом предикт поверх (замена по ключу)
        Map<Long, InkRenderMark> merged = new HashMap<>();
        for (InvisibleInkData.InkMark mark : InvisibleInkData.getMarks(stack)) {
            int px = PhotographCoordinates.pixelIndex(mark.uv().x);
            int py = PhotographCoordinates.pixelIndex(mark.uv().y);
            merged.put(packPixel(px, py), new InkRenderMark(mark.uv(), mark.color(), mark.alwaysVisible()));
        }
        if (!trackedStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, trackedStack)) {
            for (Map.Entry<Long, ClientInkMark> e : pendingMarks.entrySet()) {
                ClientInkMark p = e.getValue();
                merged.put(e.getKey(), new InkRenderMark(p.uv(), p.color(), p.alwaysVisible()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    // =========================================================================
    // Magnifier
    // =========================================================================

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

    // =========================================================================
    // Utils
    // =========================================================================

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


    private static void resetState() {
        drawing = false;
        draggingMagnifier = false;
        lastDrawPoint = null;
        trackedStack = ItemStack.EMPTY;
        pendingMarks.clear();
        strokePixels.clear();
        lastKnownServerCount = 0;
        magnifierX = Double.NaN;
        magnifierY = Double.NaN;
        // brushSize не сбрасываем — удобнее оставлять выбранный размер
    }

    private static long packPixel(int px, int py) {
        return ((long) px << 20) | (py & 0xFFFFF);
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

    // =========================================================================
    // Records
    // =========================================================================

    private record InkRenderMark(Vec2 uv, int color, boolean alwaysVisible) {}
    private record ClientInkMark(Vec2 uv, int color, boolean alwaysVisible) {}

    private record InkProperties(ItemStack stack, int color, boolean alwaysVisible) {
        private static final InkProperties EMPTY = new InkProperties(ItemStack.EMPTY, 0, false);

        boolean isValid() {
            return !stack.isEmpty();
        }
    }
}