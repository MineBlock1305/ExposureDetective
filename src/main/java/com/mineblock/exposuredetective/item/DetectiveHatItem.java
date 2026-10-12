package com.mineblock.exposuredetective.item;

import com.mineblock.exposuredetective.ExposuredetectiveMod;
import com.mineblock.exposuredetective.client.model.DetectiveHatModel;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DetectiveHatItem extends ArmorItem {
	public DetectiveHatItem(Holder<ArmorMaterial> material) {
		super(material, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));
	}

	@Override
	public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
			ArmorMaterial.Layer layer, boolean innerModel) {
		if (innerModel) {
			return null;
		}
		return DetectiveHatModel.TEXTURE;
	}
}
