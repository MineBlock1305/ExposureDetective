package com.mineblock.exposuredetective.client;

import com.mineblock.exposuredetective.client.model.DetectiveHatModel;
import com.mineblock.exposuredetective.init.ExposuredetectiveModItems;
import com.mineblock.exposuredetective.item.DetectiveHatItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class DetectiveHatClientExtensions {
	private DetectiveHatClientExtensions() {
	}

	public static void register(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			@Override
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
				if (slot != EquipmentSlot.HEAD || !(stack.getItem() instanceof DetectiveHatItem)) {
					return original;
				}

				DetectiveHatModel model = new DetectiveHatModel(
						Minecraft.getInstance().getEntityModels().bakeLayer(DetectiveHatModel.LAYER_LOCATION));
				if (original instanceof HumanoidModel<?> humanoid) {
					model.head.copyFrom(humanoid.head);
					model.hat.copyFrom(humanoid.head);
				}
				model.prepareForRender();
				return model;
			}
		}, ExposuredetectiveModItems.DETECTIVE_HAT.get());
	}
}
