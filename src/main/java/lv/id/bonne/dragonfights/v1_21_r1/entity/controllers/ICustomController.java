package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import net.minecraft.world.phys.Vec3;

public interface ICustomController
{
    default void init() {
    }
    
    void movementTick();
    
    default void stop() {
    }
    
    default boolean isLanded() {
        return false;
    }
    
    float getConstant();
    
    float getRotation();
    
    Vec3 getTargetLocation();
    
    CustomControllerPhase<? extends ICustomController> getControllerPhase();
}
