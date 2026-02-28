package lv.id.bonne.dragonfights.entity;

import org.bukkit.entity.EnderDragon;

/**
 * Ender dragon entity type for BentoBox dragon fights (1.21 only).
 */
public class BentoBoxEnderDragonRoot implements EntityTypeDefinition {

	@Override
	public String getBaseKey() {
		return "ender_dragon";
	}

	@Override
	public String getKey() {
		return "bentobox_ender_dragon";
	}

	@Override
	public CreatureType getCreatureType() {
		return CreatureType.MONSTER;
	}
}
