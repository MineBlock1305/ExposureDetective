package com.mineblock.exposuredetective.client.model;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class DetectiveHatModel extends HumanoidModel<LivingEntity> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID, "detective_hat"), "main");
	
	public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExposuredetectiveMod.MOD_ID,
			"textures/entity/detective_hat.png");

	public DetectiveHatModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0), 0);
		
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("hat",
				CubeListBuilder.create()
						.texOffs(32, 39).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.75F))
						.texOffs(32, 50).addBox(-7.0F, -5.35F, -7.0F, 14.0F, 0.0F, 14.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		hat.copyFrom(head);
	}

	public void prepareForRender() {
		setAllVisible(false);
		hat.visible = true;
	}
}
