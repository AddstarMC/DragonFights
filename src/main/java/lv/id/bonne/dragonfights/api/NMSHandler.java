package lv.id.bonne.dragonfights.api;

import lv.id.bonne.custombattle.DragonBattleBuilder;

/**
 * NMS handler for 1.21: registry and battle builder (single version).
 */
public interface NMSHandler {

	CustomRegistry getRegistry();

	DragonBattleBuilder createDragonBattleBuilder(String battleId);
}
