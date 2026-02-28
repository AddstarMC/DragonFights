package lv.id.bonne.custombattle;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Battle instance for a single dragon fight (per island).
 */
public interface CustomDragonBattle {

	void tickBattle();

	void onCrystalDamage(EnderCrystal crystal);

	boolean onCrystalPlacement(EnderCrystal crystal);

	@Nullable
	Vector getGeneratedPortalLocation();

	boolean isGenerated();

	boolean isFinished();

	String saveData();

	@Nullable
	UUID getLastDragonUUID();

	@Nullable
	Vector getLastDragonLocation();
}
