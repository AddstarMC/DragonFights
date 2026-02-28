package lv.id.bonne.dragonfights.v1_21_r1.entity.controllers;

import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;

import java.util.Arrays;

public class CustomControllerPhase<T extends ICustomController> {

	private static CustomControllerPhase<?>[] phases = new CustomControllerPhase[0];

	private final int index;
	private final Class<? extends ICustomController> controllerClass;
	private final String name;

	private CustomControllerPhase(int index, Class<? extends ICustomController> controllerClass, String name) {
		this.index = index;
		this.controllerClass = controllerClass;
		this.name = name;
	}

	public ICustomController createInstance(BentoBoxEnderDragon enderDragon) {
		try {
			return controllerClass.getConstructor(BentoBoxEnderDragon.class).newInstance(enderDragon);
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	public int phaseIndex() {
		return index;
	}

	public static CustomControllerPhase<?> getById(int i) {
		return (i >= 0 && i < phases.length) ? phases[i] : HOVER;
	}

	public static int numberOfPhases() {
		return phases.length;
	}

	private static <U extends ICustomController> CustomControllerPhase<U> register(Class<U> controllerClass, String name) {
		CustomControllerPhase<U> phase = new CustomControllerPhase<>(phases.length, controllerClass, name);
		phases = Arrays.copyOf(phases, phases.length + 1);
		phases[phase.phaseIndex()] = phase;
		return phase;
	}

	public static final CustomControllerPhase<CustomControllerHover> HOVER = register(CustomControllerHover.class, "Hover");
}
