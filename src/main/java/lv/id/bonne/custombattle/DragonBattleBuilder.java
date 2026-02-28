package lv.id.bonne.custombattle;

import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for creating and restoring dragon battles (1.21 only).
 */
public abstract class DragonBattleBuilder {

	protected final String battleId;
	protected long battleSeed = 0L;
	protected World world;
	protected Vector portalLocation;
	protected BoundingBox boundingBox;
	protected double range = 196.0D;
	protected boolean dragonKilled;
	protected boolean previouslyKilled;
	protected String bossBarText = "entity.minecraft.ender_dragon";
	protected BarColor bossBarColor = BarColor.PINK;
	protected BarStyle bossBarStyle = BarStyle.SOLID;
	protected boolean playMusic = true;
	protected boolean enableFog = true;
	protected int distanceTillTowers = 43;
	protected int numberOfTowers = 10;
	protected int numberOfPathPoints = 8;
	protected int numberOfProtectedTowers = 2;
	protected int minTowerHeight = 73;
	protected int maxTowerHeight = 107;
	protected boolean searchExitPortal = true;
	protected boolean generated;

	public DragonBattleBuilder(String battleId) {
		this.battleId = battleId;
	}

	@Nullable
	public abstract CustomDragonBattle build();

	@Nullable
	public abstract CustomDragonBattle buildFromNBT(String nbtData);

	public DragonBattleBuilder setBattleSeed(long battleSeed) {
		this.battleSeed = battleSeed;
		return this;
	}

	public DragonBattleBuilder setWorld(World world) {
		this.world = world;
		return this;
	}

	public DragonBattleBuilder setPortalLocation(Vector portalLocation) {
		this.portalLocation = portalLocation;
		return this;
	}

	public DragonBattleBuilder setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
		return this;
	}

	public DragonBattleBuilder setRange(double range) {
		this.range = range;
		return this;
	}

	public DragonBattleBuilder setDragonKilled(boolean dragonKilled) {
		this.dragonKilled = dragonKilled;
		return this;
	}

	public DragonBattleBuilder setPreviouslyKilled(boolean previouslyKilled) {
		this.previouslyKilled = previouslyKilled;
		return this;
	}

	public DragonBattleBuilder setBossBarText(String bossBarText) {
		this.bossBarText = bossBarText;
		return this;
	}

	public DragonBattleBuilder setBossBarColor(BarColor bossBarColor) {
		this.bossBarColor = bossBarColor;
		return this;
	}

	public DragonBattleBuilder setBossBarStyle(BarStyle bossBarStyle) {
		this.bossBarStyle = bossBarStyle;
		return this;
	}

	public DragonBattleBuilder setPlayMusic(boolean playMusic) {
		this.playMusic = playMusic;
		return this;
	}

	public DragonBattleBuilder setEnableFog(boolean enableFog) {
		this.enableFog = enableFog;
		return this;
	}

	public DragonBattleBuilder setDistanceTillTowers(int distanceTillTowers) {
		this.distanceTillTowers = distanceTillTowers;
		return this;
	}

	public DragonBattleBuilder setNumberOfTowers(int numberOfTowers) {
		this.numberOfTowers = numberOfTowers;
		return this;
	}

	public DragonBattleBuilder setNumberOfPathPoints(int numberOfPathPoints) {
		this.numberOfPathPoints = numberOfPathPoints;
		return this;
	}

	public DragonBattleBuilder setNumberOfProtectedTowers(int numberOfProtectedTowers) {
		this.numberOfProtectedTowers = numberOfProtectedTowers;
		return this;
	}

	public DragonBattleBuilder setMinTowerHeight(int minTowerHeight) {
		this.minTowerHeight = minTowerHeight;
		return this;
	}

	public DragonBattleBuilder setMaxTowerHeight(int maxTowerHeight) {
		this.maxTowerHeight = maxTowerHeight;
		return this;
	}

	public DragonBattleBuilder setSearchExitPortal(boolean searchExitPortal) {
		this.searchExitPortal = searchExitPortal;
		return this;
	}

	public DragonBattleBuilder setGenerated(boolean generated) {
		this.generated = generated;
		return this;
	}
}
