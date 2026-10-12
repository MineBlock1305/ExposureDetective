package com.mineblock.exposuredetective.client;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.client.model.DetectiveHatModel;
import com.mineblock.exposuredetective.client.render.BoardBlockEntityRenderer;
import com.mineblock.exposuredetective.init.ExposuredetectiveModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = ExposuredetectiveMod.MODID, value = Dist.CLIENT)
public final class ExposuredetectiveModClient {
	private ExposuredetectiveModClient() {
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ExposuredetectiveModBlockEntities.BOARD.get(), BoardBlockEntityRenderer::new);
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(DetectiveHatModel.LAYER_LOCATION, DetectiveHatModel::createBodyLayer);
	}

	@SubscribeEvent
	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		DetectiveHatClientExtensions.register(event);
	}
}

