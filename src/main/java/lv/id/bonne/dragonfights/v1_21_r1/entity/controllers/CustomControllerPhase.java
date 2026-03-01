package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import java.util.Arrays;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;

public class CustomControllerPhase<T extends ICustomController>
{
    private static CustomControllerPhase<?>[] phases;
    private final int index;
    private final Class<? extends ICustomController> controllerClass;
    private final String name;
    public static final CustomControllerPhase<CustomControllerHover> HOVER;
    
    private CustomControllerPhase(final int index, final Class<? extends ICustomController> controllerClass, final String name) {
        this.index = index;
        this.controllerClass = controllerClass;
        this.name = name;
    }
    
    public ICustomController createInstance(final BentoBoxEnderDragon enderDragon) {
        try {
            return (ICustomController)this.controllerClass.getConstructor(BentoBoxEnderDragon.class).newInstance(enderDragon);
        }
        catch (final Exception e) {
            throw new Error(e);
        }
    }
    
    public int phaseIndex() {
        return this.index;
    }
    
    public static CustomControllerPhase<?> getById(final int i) {
        return (i >= 0 && i < CustomControllerPhase.phases.length) ? CustomControllerPhase.phases[i] : CustomControllerPhase.HOVER;
    }
    
    public static int numberOfPhases() {
        return CustomControllerPhase.phases.length;
    }
    
    private static <U extends ICustomController> CustomControllerPhase<U> register(final Class<U> controllerClass, final String name) {
        final CustomControllerPhase<U> phase = new CustomControllerPhase<U>(CustomControllerPhase.phases.length, controllerClass, name);
        CustomControllerPhase.phases = Arrays.copyOf(CustomControllerPhase.phases, CustomControllerPhase.phases.length + 1);
        return (CustomControllerPhase<U>)(CustomControllerPhase.phases[phase.phaseIndex()] = phase);
    }
    
    static {
        CustomControllerPhase.phases = new CustomControllerPhase[0];
        HOVER = register(CustomControllerHover.class, "Hover");
    }
}
