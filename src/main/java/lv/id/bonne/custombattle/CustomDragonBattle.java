package lv.id.bonne.custombattle;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.util.Vector;
import org.bukkit.entity.EnderCrystal;

public interface CustomDragonBattle
{
    void tickBattle();
    
    void onCrystalDamage(final EnderCrystal p0);
    
    boolean onCrystalPlacement(final EnderCrystal p0);
    
    @Nullable
    Vector getGeneratedPortalLocation();
    
    boolean isGenerated();
    
    boolean isFinished();
    
    String saveData();
    
    @Nullable
    UUID getLastDragonUUID();
    
    @Nullable
    Vector getLastDragonLocation();

    default void removeBossBarPlayer(Player player) {}
}
