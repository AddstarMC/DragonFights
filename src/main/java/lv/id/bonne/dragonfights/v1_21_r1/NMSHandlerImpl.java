package lv.id.bonne.dragonfights.v1_21_r1;

import lv.id.bonne.custombattle.DragonBattleBuilder;
import lv.id.bonne.dragonfights.api.CustomRegistry;
import lv.id.bonne.dragonfights.api.NMSHandler;
import lv.id.bonne.dragonfights.v1_21_r1.battle.NMSDragonBattleBuilder;

public final class NMSHandlerImpl implements NMSHandler {

	private final CustomRegistry registry = new NMSEntityRegistry();

	@Override
	public CustomRegistry getRegistry() {
		return registry;
	}

	@Override
	public DragonBattleBuilder createDragonBattleBuilder(String battleId) {
		return new NMSDragonBattleBuilder(battleId);
	}
}
