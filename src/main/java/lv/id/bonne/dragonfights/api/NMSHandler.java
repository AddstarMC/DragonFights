package lv.id.bonne.dragonfights.api;

import lv.id.bonne.custombattle.DragonBattleBuilder;

public interface NMSHandler
{
    CustomRegistry getRegistry();
    
    DragonBattleBuilder createDragonBattleBuilder(final String p0);
}
