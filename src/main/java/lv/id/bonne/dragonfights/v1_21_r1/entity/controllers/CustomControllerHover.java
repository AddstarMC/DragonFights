package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;
import net.minecraft.world.phys.Vec3;

public class CustomControllerHover extends AbstractCustomController
{
    private Vec3 targetLocation;
    
    public CustomControllerHover(final BentoBoxEnderDragon enderDragon) {
        super(enderDragon);
    }
    
    @Override
    public void init() {
        this.targetLocation = null;
    }
    
    @Override
    public void movementTick() {
        if (this.targetLocation == null) {
            this.targetLocation = this.enderDragon.position();
        }
    }
    
    @Override
    public boolean isLanded() {
        return true;
    }
    
    @Override
    public float getConstant() {
        return 1.0f;
    }
    
    @Override
    public Vec3 getTargetLocation() {
        return this.targetLocation;
    }
    
    @Override
    public CustomControllerPhase<CustomControllerHover> getControllerPhase() {
        return CustomControllerPhase.HOVER;
    }
}
