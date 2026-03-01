package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;

public class CustomControllerManager
{
    private final BentoBoxEnderDragon enderDragon;
    private final ICustomController[] dragonControllers;
    private ICustomController currentDragonController;
    
    public CustomControllerManager(final BentoBoxEnderDragon enderDragon) {
        this.enderDragon = enderDragon;
        this.dragonControllers = new ICustomController[CustomControllerPhase.numberOfPhases()];
        this.setControllerPhase(CustomControllerPhase.HOVER);
    }
    
    public void setControllerPhase(final CustomControllerPhase<?> controllerPhase) {
        if (this.currentDragonController == null || controllerPhase != this.currentDragonController.getControllerPhase()) {
            if (this.currentDragonController != null) {
                this.currentDragonController.stop();
            }
            this.currentDragonController = this.getPhase(controllerPhase);
            this.enderDragon.getEntityData().set(EnderDragon.DATA_PHASE, controllerPhase.phaseIndex());
            this.currentDragonController.init();
        }
    }
    
    public ICustomController getCurrentPhase() {
        return this.currentDragonController;
    }
    
    public <U extends ICustomController> U getPhase(final CustomControllerPhase<U> phase) {
        final int i = phase.phaseIndex();
        if (this.dragonControllers[i] == null) {
            this.dragonControllers[i] = phase.createInstance(this.enderDragon);
        }
        return (U)this.dragonControllers[i];
    }
}
