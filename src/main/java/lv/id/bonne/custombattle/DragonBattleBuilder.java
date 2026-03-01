package lv.id.bonne.custombattle;

import org.jetbrains.annotations.Nullable;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BarColor;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.World;

public abstract class DragonBattleBuilder
{
    protected final String battleId;
    protected long battleSeed;
    protected World world;
    protected Vector portalLocation;
    protected BoundingBox boundingBox;
    protected double range;
    protected boolean dragonKilled;
    protected boolean previouslyKilled;
    protected String bossBarText;
    protected BarColor bossBarColor;
    protected BarStyle bossBarStyle;
    protected boolean playMusic;
    protected boolean enableFog;
    protected int distanceTillTowers;
    protected int numberOfTowers;
    protected int numberOfPathPoints;
    protected int numberOfProtectedTowers;
    protected int minTowerHeight;
    protected int maxTowerHeight;
    protected boolean searchExitPortal;
    protected boolean generated;
    protected double dragonMaxHealth;
    protected float dragonSpeedMultiplier;
    protected BarColor dragonGlowColor;
    
    public DragonBattleBuilder(final String battleId) {
        this.battleSeed = 0L;
        this.range = 196.0;
        this.bossBarText = "entity.minecraft.ender_dragon";
        this.bossBarColor = BarColor.PINK;
        this.bossBarStyle = BarStyle.SOLID;
        this.playMusic = true;
        this.enableFog = true;
        this.distanceTillTowers = 43;
        this.numberOfTowers = 10;
        this.numberOfPathPoints = 8;
        this.numberOfProtectedTowers = 2;
        this.minTowerHeight = 73;
        this.maxTowerHeight = 107;
        this.searchExitPortal = true;
        this.dragonMaxHealth = 200.0;
        this.dragonSpeedMultiplier = 1.0f;
        this.battleId = battleId;
    }
    
    @Nullable
    public abstract CustomDragonBattle build();
    
    @Nullable
    public abstract CustomDragonBattle buildFromNBT(final String p0);
    
    public DragonBattleBuilder setBattleSeed(final long battleSeed) {
        this.battleSeed = battleSeed;
        return this;
    }
    
    public DragonBattleBuilder setWorld(final World world) {
        this.world = world;
        return this;
    }
    
    public DragonBattleBuilder setPortalLocation(final Vector portalLocation) {
        this.portalLocation = portalLocation;
        return this;
    }
    
    public DragonBattleBuilder setBoundingBox(final BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
        return this;
    }
    
    public DragonBattleBuilder setRange(final double range) {
        this.range = range;
        return this;
    }
    
    public DragonBattleBuilder setDragonKilled(final boolean dragonKilled) {
        this.dragonKilled = dragonKilled;
        return this;
    }
    
    public DragonBattleBuilder setPreviouslyKilled(final boolean previouslyKilled) {
        this.previouslyKilled = previouslyKilled;
        return this;
    }
    
    public DragonBattleBuilder setBossBarText(final String bossBarText) {
        this.bossBarText = bossBarText;
        return this;
    }
    
    public DragonBattleBuilder setBossBarColor(final BarColor bossBarColor) {
        this.bossBarColor = bossBarColor;
        return this;
    }
    
    public DragonBattleBuilder setBossBarStyle(final BarStyle bossBarStyle) {
        this.bossBarStyle = bossBarStyle;
        return this;
    }
    
    public DragonBattleBuilder setPlayMusic(final boolean playMusic) {
        this.playMusic = playMusic;
        return this;
    }
    
    public DragonBattleBuilder setEnableFog(final boolean enableFog) {
        this.enableFog = enableFog;
        return this;
    }
    
    public DragonBattleBuilder setDistanceTillTowers(final int distanceTillTowers) {
        this.distanceTillTowers = distanceTillTowers;
        return this;
    }
    
    public DragonBattleBuilder setNumberOfTowers(final int numberOfTowers) {
        this.numberOfTowers = numberOfTowers;
        return this;
    }
    
    public DragonBattleBuilder setNumberOfPathPoints(final int numberOfPathPoints) {
        this.numberOfPathPoints = numberOfPathPoints;
        return this;
    }
    
    public DragonBattleBuilder setNumberOfProtectedTowers(final int numberOfProtectedTowers) {
        this.numberOfProtectedTowers = numberOfProtectedTowers;
        return this;
    }
    
    public DragonBattleBuilder setMinTowerHeight(final int minTowerHeight) {
        this.minTowerHeight = minTowerHeight;
        return this;
    }
    
    public DragonBattleBuilder setMaxTowerHeight(final int maxTowerHeight) {
        this.maxTowerHeight = maxTowerHeight;
        return this;
    }
    
    public DragonBattleBuilder setSearchExitPortal(final boolean searchExitPortal) {
        this.searchExitPortal = searchExitPortal;
        return this;
    }
    
    public DragonBattleBuilder setGenerated(final boolean generated) {
        this.generated = generated;
        return this;
    }
    
    public DragonBattleBuilder setDragonMaxHealth(final double dragonMaxHealth) {
        this.dragonMaxHealth = dragonMaxHealth;
        return this;
    }
    
    public DragonBattleBuilder setDragonSpeedMultiplier(final float dragonSpeedMultiplier) {
        this.dragonSpeedMultiplier = dragonSpeedMultiplier;
        return this;
    }
    
    public DragonBattleBuilder setDragonGlowColor(final BarColor dragonGlowColor) {
        this.dragonGlowColor = dragonGlowColor;
        return this;
    }
}
