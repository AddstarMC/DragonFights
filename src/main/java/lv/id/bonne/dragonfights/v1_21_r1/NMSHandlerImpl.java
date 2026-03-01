package lv.id.bonne.dragonfights.v1_21_r1;

import lv.id.bonne.dragonfights.v1_21_r1.battle.NMSDragonBattleBuilder;
import lv.id.bonne.custombattle.DragonBattleBuilder;
import lv.id.bonne.dragonfights.api.CustomRegistry;
import lv.id.bonne.dragonfights.api.NMSHandler;

public final class NMSHandlerImpl implements NMSHandler
{
    private final CustomRegistry registry;
    
    public NMSHandlerImpl() {
        this.registry = new NMSEntityRegistry();
    }
    
    @Override
    public CustomRegistry getRegistry() {
        return this.registry;
    }
    
    @Override
    public DragonBattleBuilder createDragonBattleBuilder(final String battleId) {
        return new NMSDragonBattleBuilder(battleId);
    }
}
