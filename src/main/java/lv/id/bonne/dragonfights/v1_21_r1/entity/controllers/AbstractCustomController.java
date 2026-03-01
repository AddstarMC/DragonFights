package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import net.minecraft.world.phys.Vec3;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;

public abstract class AbstractCustomController implements ICustomController
{
    protected final BentoBoxEnderDragon enderDragon;
    
    protected AbstractCustomController(final BentoBoxEnderDragon entity) {
        this.enderDragon = entity;
    }
    
    @Override
    public void movementTick() {
    }
    
    @Override
    public float getConstant() {
        return 0.6f;
    }
    
    @Override
    public Vec3 getTargetLocation() {
        return null;
    }
    
    @Override
    public float getRotation() {
        return 4.375E-4f;
    }
}
