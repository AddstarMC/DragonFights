package lv.id.bonne.dragonfights.entity;

import org.bukkit.Bukkit;

import lv.id.bonne.dragonfights.api.NMSHandler;
import lv.id.bonne.dragonfights.v1_21_r1.NMSHandlerImpl;

/**
 * API for custom entity and dragon battle (Paper 1.21.11 only).
 */
public final class CustomEntityAPI {

	private static NMSHandler api;

	public static NMSHandler getAPI() {
		if (api == null) {
			Bukkit.getLogger().info("[DragonFights] Loading NMS handler for Paper 1.21.11");
			api = new NMSHandlerImpl();
		}
		return api;
	}

	private CustomEntityAPI() {
	}
}
