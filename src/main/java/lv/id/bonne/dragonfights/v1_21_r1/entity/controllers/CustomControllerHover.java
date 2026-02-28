package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;
import net.minecraft.world.phys.Vec3;

public class CustomControllerHover extends AbstractCustomController {

	private Vec3 targetLocation;

	public CustomControllerHover(BentoBoxEnderDragon enderDragon) {
		super(enderDragon);
	}

	@Override
	public void init() {
		targetLocation = null;
	}

	@Override
	public void movementTick() {
		if (targetLocation == null) {
			targetLocation = enderDragon.position();
		}
	}

	@Override
	public boolean isLanded() {
		return true;
	}

	@Override
	public float getConstant() {
		return 1.0F;
	}

	@Override
	public Vec3 getTargetLocation() {
		return targetLocation;
	}

	@Override
	public CustomControllerPhase<CustomControllerHover> getControllerPhase() {
		return CustomControllerPhase.HOVER;
	}
}
