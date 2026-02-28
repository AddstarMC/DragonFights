package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractCustomController implements ICustomController {

	protected final BentoBoxEnderDragon enderDragon;

	protected AbstractCustomController(BentoBoxEnderDragon entity) {
		this.enderDragon = entity;
	}

	@Override
	public void movementTick() {
	}

	@Override
	public float getConstant() {
		return 0.6F;
	}

	@Override
	public Vec3 getTargetLocation() {
		return null;
	}

	@Override
	public float getRotation() {
		return 0.7F / 40.0F / 40.0F;
	}
}
