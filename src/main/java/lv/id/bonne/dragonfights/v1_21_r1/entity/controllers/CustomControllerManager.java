package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public class CustomControllerManager {

	private final BentoBoxEnderDragon enderDragon;
	private final ICustomController[] dragonControllers;
	private ICustomController currentDragonController;

	public CustomControllerManager(BentoBoxEnderDragon enderDragon) {
		this.enderDragon = enderDragon;
		this.dragonControllers = new ICustomController[CustomControllerPhase.numberOfPhases()];
		setControllerPhase(CustomControllerPhase.HOVER);
	}

	public void setControllerPhase(CustomControllerPhase<?> controllerPhase) {
		if (currentDragonController == null || controllerPhase != currentDragonController.getControllerPhase()) {
			if (currentDragonController != null) {
				currentDragonController.stop();
			}
			currentDragonController = getPhase(controllerPhase);
			enderDragon.getEntityData().set(EnderDragon.DATA_PHASE, controllerPhase.phaseIndex());
			currentDragonController.init();
		}
	}

	public ICustomController getCurrentPhase() {
		return currentDragonController;
	}

	@SuppressWarnings("unchecked")
	public <U extends ICustomController> U getPhase(CustomControllerPhase<U> phase) {
		int i = phase.phaseIndex();
		if (dragonControllers[i] == null) {
			dragonControllers[i] = phase.createInstance(enderDragon);
		}
		return (U) dragonControllers[i];
	}
}
