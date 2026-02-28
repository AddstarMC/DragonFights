package lv.id.bonne.dragonfights.v1_21_r1.battle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import lv.id.bonne.custombattle.CustomDragonBattle;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Dragon battle state for one island (Paper 1.21.11).
 */
public class BentoBoxDragonBattle implements CustomDragonBattle {

	public enum BattleStage {
		START,
		SPAWNING_TOWERS,
		PREPARING_TO_SUMMON,
		SUMMONING,
		BATTLE,
		END
	}

	private final ServerLevel world;
	private final String battleId;
	private final double range;
	private final BoundingBox boundingBox;
	private final ServerBossEvent bossBattle;
	private final BlockPos originalLocation;
	private BlockPos centerBeamLocation;
	@Nullable
	private BlockPos exitPortalLocation;
	private final boolean searchExitPortal;
	private boolean dragonKilled;
	private boolean previouslyKilled;
	private final String bossBarText;
	private final int numberOfTowers;
	private final int numberOfProtectedTowers;
	private final int distanceTillTowers;
	private final int numberPathPoints;
	private final int minTowerHeight;
	private final int maxTowerHeight;
	private final long battleSeed;
	private final Random random;
	@Nullable
	private BattleStage battleStage;
	@Nullable
	private BentoBoxEnderDragon enderDragon;
	@Nullable
	private UUID dragonUUID;
	@Nullable
	private BlockPos lastLocation;
	@Nullable
	private List<net.minecraft.world.entity.boss.enderdragon.EndCrystal> respawnCrystals;
	private int tickCounter = 0;
	private int currentTowerIndex = 0;
	private int nextTowerSpawnTick = 1;
	@Nullable
	private List<int[]> towerSpawnData;
	private final List<net.minecraft.world.entity.boss.enderdragon.EndCrystal> towerCrystals = new ArrayList<>();
	private static final Logger LOG = Logger.getLogger("DragonFights");

	public BentoBoxDragonBattle(ServerLevel world, BlockPos portalLocation, BoundingBox boundingBox, double range,
	                           boolean dragonKilled, boolean previouslyKilled, String bossBarText,
	                           net.minecraft.world.BossEvent.BossBarColor bossBarColor,
	                           net.minecraft.world.BossEvent.BossBarOverlay bossBarStyle,
	                           boolean playMusic, boolean enableFog, int distanceTillTowers, int numberOfTowers,
	                           int numberOfProtectedTowers, int numberPathPoints, int minTowerHeight, int maxTowerHeight,
	                           long battleSeed, String battleId, boolean searchExitPortal) {
		this.world = world;
		this.battleId = battleId;
		this.range = range;
		this.boundingBox = boundingBox;
		this.dragonKilled = dragonKilled;
		this.previouslyKilled = previouslyKilled;
		this.bossBarText = bossBarText;
		this.searchExitPortal = searchExitPortal;
		this.exitPortalLocation = searchExitPortal ? null : portalLocation;
		this.originalLocation = portalLocation;
		this.centerBeamLocation = originalLocation.relative(Direction.UP, 40);
		this.numberOfTowers = numberOfTowers;
		this.numberOfProtectedTowers = numberOfProtectedTowers;
		this.distanceTillTowers = distanceTillTowers;
		this.numberPathPoints = numberPathPoints;
		this.minTowerHeight = minTowerHeight;
		this.maxTowerHeight = maxTowerHeight;
		this.battleSeed = battleSeed;
		this.random = new Random(battleSeed);
		this.battleStage = BattleStage.START;
		this.bossBattle = new ServerBossEvent(
			CraftChatMessage.fromStringOrNull(bossBarText, true),
			bossBarColor,
			bossBarStyle
		);
		this.bossBattle.setPlayBossMusic(playMusic);
		this.bossBattle.setCreateWorldFog(enableFog);
	}

	public BentoBoxDragonBattle(ServerLevel world, CompoundTag storedData) {
		this.world = world;
		this.range = storedData.getDoubleOr("Range", 196);
		this.originalLocation = readBlockPos(storedData, "OriginalLocation");
		BlockPos minCorner = readBlockPos(storedData, "BoundingBoxMin");
		BlockPos maxCorner = readBlockPos(storedData, "BoundingBoxMax");
		this.boundingBox = new BoundingBox(minCorner.getX(), minCorner.getY(), minCorner.getZ(), maxCorner.getX(), maxCorner.getY(), maxCorner.getZ());
		this.exitPortalLocation = storedData.contains("PortalLocation") ? readBlockPos(storedData, "PortalLocation") : null;
		this.bossBarText = storedData.getStringOr("BarTitle", "entity.minecraft.ender_dragon");
		net.minecraft.world.BossEvent.BossBarColor barColor = net.minecraft.world.BossEvent.BossBarColor.valueOf(storedData.getStringOr("BarColor", "pink").toUpperCase(Locale.ROOT));
		net.minecraft.world.BossEvent.BossBarOverlay barStyle = net.minecraft.world.BossEvent.BossBarOverlay.valueOf(storedData.getStringOr("BarStyle", "progress").toUpperCase(Locale.ROOT));
		this.bossBattle = new ServerBossEvent(CraftChatMessage.fromStringOrNull(this.bossBarText, true), barColor, barStyle);
		this.bossBattle.setPlayBossMusic(storedData.getBooleanOr("BarMusic", true));
		this.searchExitPortal = (this.exitPortalLocation == null);
		this.dragonKilled = storedData.getBooleanOr("DragonKilled", false);
		this.previouslyKilled = storedData.getBooleanOr("PreviouslyKilled", false);
		if (storedData.contains("DragonId")) {
			this.dragonUUID = UUID.fromString(storedData.getStringOr("DragonId", ""));
			this.lastLocation = storedData.contains("LastLocation") ? readBlockPos(storedData, "LastLocation") : null;
		}
		this.battleId = storedData.getStringOr("BattleId", "");
		String stageStr = storedData.getStringOr("BattleStage", "null");
		this.battleStage = stageStr == null || "null".equals(stageStr) ? null : BattleStage.valueOf(stageStr);
		this.centerBeamLocation = this.exitPortalLocation != null ? this.exitPortalLocation.relative(Direction.UP, 40) : this.originalLocation.relative(Direction.UP, 40);
		this.numberOfTowers = storedData.getIntOr("TowerCount", 10);
		this.numberOfProtectedTowers = storedData.getIntOr("ProtectedTowerCount", 2);
		this.distanceTillTowers = storedData.getIntOr("TowerDistance", 43);
		this.numberPathPoints = storedData.getIntOr("PathCount", 8);
		this.minTowerHeight = storedData.getIntOr("MinHeight", 73);
		this.maxTowerHeight = storedData.getIntOr("MaxHeight", 107);
		this.battleSeed = storedData.getLongOr("BattleSeed", 0L);
		this.random = new Random(this.battleSeed);
		this.currentTowerIndex = storedData.getIntOr("CurrentTowerIndex", 0);
		if (this.battleStage == BattleStage.SPAWNING_TOWERS && this.exitPortalLocation != null) {
			precomputeTowers();
		}
		if (storedData.contains("RespawnCrystals")) {
			ListTag list = storedData.getListOrEmpty("RespawnCrystals");
			this.respawnCrystals = new ArrayList<>(list.size());
			for (int i = 0; i < list.size(); i++) {
				CompoundTag tag = list.getCompoundOrEmpty(i);
				String id = tag.getStringOr("CrystalID", "");
				if (!id.isEmpty()) {
					try {
						UUID uuid = UUID.fromString(id);
						net.minecraft.world.entity.Entity e = world.getEntity(uuid);
					if (e instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal ec) {
						this.respawnCrystals.add(ec);
					}
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
		}
	}

	private static BlockPos readBlockPos(CompoundTag tag, String key) {
		CompoundTag c = tag.getCompoundOrEmpty(key);
		if (c.isEmpty()) {
			return BlockPos.ZERO;
		}
		return new BlockPos(c.getIntOr("X", 0), c.getIntOr("Y", 0), c.getIntOr("Z", 0));
	}

	private static void writeBlockPos(CompoundTag tag, String key, BlockPos pos) {
		CompoundTag c = new CompoundTag();
		c.putInt("X", pos.getX());
		c.putInt("Y", pos.getY());
		c.putInt("Z", pos.getZ());
		tag.put(key, c);
	}

	@Override
	public void tickBattle() {
		if (battleStage == null) {
			return;
		}

		tickCounter++;

		switch (battleStage) {
			case START:
				LOG.info("Battle '" + battleId + "' stage=START tick=" + tickCounter);
				if (exitPortalLocation == null) {
					exitPortalLocation = originalLocation;
					LOG.info("exitPortalLocation defaulted to originalLocation: " +
						exitPortalLocation.getX() + "," + exitPortalLocation.getY() + "," + exitPortalLocation.getZ());
				}
				LOG.info("Generating exit portal at " +
					exitPortalLocation.getX() + "," + exitPortalLocation.getY() + "," + exitPortalLocation.getZ());
				generateExitPortal();
				centerBeamLocation = exitPortalLocation.relative(Direction.UP, 40);
				precomputeTowers();
				currentTowerIndex = 0;
				towerCrystals.clear();
				battleStage = BattleStage.SPAWNING_TOWERS;
				tickCounter = 0;
				LOG.info("Battle '" + battleId + "' -> SPAWNING_TOWERS (" + numberOfTowers + " towers)");
				break;

			case SPAWNING_TOWERS:
				if (towerSpawnData == null) {
					precomputeTowers();
				}
				if (currentTowerIndex < numberOfTowers && tickCounter >= nextTowerSpawnTick) {
					spawnSingleTower(currentTowerIndex);
					currentTowerIndex++;
					if (currentTowerIndex >= numberOfTowers) {
						battleStage = BattleStage.PREPARING_TO_SUMMON;
						tickCounter = 0;
						LOG.info("Battle '" + battleId + "' all towers placed -> PREPARING_TO_SUMMON");
					} else {
						nextTowerSpawnTick = tickCounter + 30 + world.random.nextInt(11);
					}
				}
				break;

			case PREPARING_TO_SUMMON:
				if (tickCounter == 1) {
					LOG.info("Battle '" + battleId + "' PREPARING_TO_SUMMON, waiting 20 ticks...");
				}
				if (tickCounter >= 20) {
					battleStage = BattleStage.SUMMONING;
					tickCounter = 0;
					LOG.info("Battle '" + battleId + "' -> SUMMONING");
				}
				break;

			case SUMMONING:
				LOG.info("Battle '" + battleId + "' SUMMONING at " +
					centerBeamLocation.getX() + "," + centerBeamLocation.getY() + "," + centerBeamLocation.getZ());
				clearCrystalBeamTargets();
				findOrCreateDragon();
				if (enderDragon != null) {
					dragonKilled = false;
					battleStage = BattleStage.BATTLE;
					tickCounter = 0;
					world.playSound(null, exitPortalLocation.getX() + 0.5, exitPortalLocation.getY(), exitPortalLocation.getZ() + 0.5,
						SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE, 5.0f, 1.0f);
					LOG.info("Battle '" + battleId + "' dragon spawned UUID=" + dragonUUID + " -> BATTLE");
				} else {
					LOG.warning("Battle '" + battleId + "' failed to spawn dragon (attempt " + tickCounter + ")");
					if (tickCounter >= 100) {
						LOG.severe("Battle '" + battleId + "' giving up dragon spawn after " + tickCounter + " attempts");
						battleStage = BattleStage.END;
					}
				}
				break;

			case BATTLE:
				if (enderDragon != null && !enderDragon.isAlive()) {
					LOG.info("Battle '" + battleId + "' dragon died! -> END");
					dragonKilled = true;
					if (bossBattle != null) {
						bossBattle.removeAllPlayers();
						bossBattle.setVisible(false);
					}
					battleStage = BattleStage.END;
					tickCounter = 0;
					break;
				}
				if (enderDragon == null) {
					findOrCreateDragon();
					if (enderDragon == null && tickCounter % 200 == 0) {
						LOG.warning("Battle '" + battleId + "' no dragon found (BATTLE tick=" + tickCounter + ")");
					}
				}
				if (enderDragon != null && bossBattle != null) {
					bossBattle.setProgress(enderDragon.getHealth() / enderDragon.getMaxHealth());
					world.players().stream()
						.filter(p -> p.distanceToSqr(centerBeamLocation.getX(), centerBeamLocation.getY(), centerBeamLocation.getZ()) < range * range)
						.forEach(bossBattle::addPlayer);
				}
				break;

			case END:
				LOG.info("Battle '" + battleId + "' END - cleaning up boss bar");
				if (bossBattle != null) {
					bossBattle.removeAllPlayers();
					bossBattle.setVisible(false);
				}
				break;
		}
	}

	private void findOrCreateDragon() {
		LOG.info("findOrCreateDragon: searching world for existing BentoBoxEnderDragon...");
		List<? extends EnderDragon> allDragons = world.getDragons();
		LOG.info("Total dragons in world: " + allDragons.size());
		for (EnderDragon d : allDragons) {
			LOG.info("  dragon: " + d.getClass().getSimpleName() + " UUID=" + d.getUUID() +
				" isBentoBox=" + (d instanceof BentoBoxEnderDragon));
		}
		Optional<BentoBoxEnderDragon> existing = allDragons.stream()
			.filter(e -> e instanceof BentoBoxEnderDragon && Objects.equals(((BentoBoxEnderDragon) e).getDragonBattle(), this))
			.map(e -> (BentoBoxEnderDragon) e)
			.findFirst();
		if (existing.isPresent()) {
			enderDragon = existing.get();
			dragonUUID = enderDragon.getUUID();
			enderDragon.setDragonBattle(this);
			LOG.info("Found existing dragon UUID=" + dragonUUID);
		} else {
			LOG.info("No matching dragon found, creating new one...");
			createNewDragon();
		}
	}

	@Nullable
	private BentoBoxEnderDragon createNewDragon() {
		EntityType<BentoBoxEnderDragon> type = lv.id.bonne.dragonfights.v1_21_r1.NMSEntityRegistry.getRegisteredEntityType();
		LOG.info("createNewDragon: registeredEntityType=" + (type != null ? type : "NULL"));
		if (type == null) {
			LOG.severe("Cannot create dragon: registered entity type is NULL! Entity was not registered.");
			return null;
		}
		BentoBoxEnderDragon dragon = type.create(world, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (dragon == null) {
			LOG.severe("type.create() returned null! Dragon entity could not be instantiated.");
			return null;
		}
		dragon.setPos(centerBeamLocation.getX(), centerBeamLocation.getY(), centerBeamLocation.getZ());
		dragon.setDragonBattle(this);
		LOG.info("Spawning dragon at " +
			centerBeamLocation.getX() + "," + centerBeamLocation.getY() + "," + centerBeamLocation.getZ() +
			" UUID=" + dragon.getUUID());
		boolean added = world.addFreshEntity(dragon, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
		LOG.info("addFreshEntity result=" + added);
		if (added) {
			this.enderDragon = dragon;
			this.dragonUUID = dragon.getUUID();
			dragon.initIslandCenter(exitPortalLocation != null ? exitPortalLocation : originalLocation);
			LOG.info("Dragon island center initialized at portal location");
		} else {
			LOG.severe("addFreshEntity FAILED - dragon was not added to world!");
		}
		return added ? dragon : null;
	}

	@Override
	public void onCrystalDamage(EnderCrystal crystal) {
		LOG.info("onCrystalDamage: stage=" + battleStage +
			" crystalAt=" + crystal.getLocation().getBlockX() + "," + crystal.getLocation().getBlockY() + "," + crystal.getLocation().getBlockZ());
		if (battleStage != null && boundingBox.contains(crystal.getLocation().toVector())) {
			if (respawnCrystals != null) {
				respawnCrystals.removeIf(ec -> ec.getUUID().equals(crystal.getUniqueId()));
				LOG.info("Crystal removed from tracking, remaining=" + respawnCrystals.size());
			}
		}
	}

	private void generateExitPortal() {
		BlockPos center = exitPortalLocation;
		LOG.info("generateExitPortal at " +
			center.getX() + "," + center.getY() + "," + center.getZ() +
			" world=" + world.dimension());

		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;

				// Foundation layer (Y-1): solid bedrock
				world.setBlock(center.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState(), 3);

				// Surface layer (Y+0): outer ring = bedrock, inner 3x3 = end_portal (center = bedrock pillar base)
				BlockPos surfacePos = center.offset(x, 0, z);
				if (Math.abs(x) >= 2 || Math.abs(z) >= 2 || (x == 0 && z == 0)) {
					world.setBlock(surfacePos, Blocks.BEDROCK.defaultBlockState(), 3);
				} else {
					world.setBlock(surfacePos, Blocks.END_PORTAL.defaultBlockState(), 3);
				}
			}
		}

		// Center pillar (Y+1 to Y+3) and torch on top (Y+4)
		for (int y = 1; y <= 3; y++) {
			world.setBlock(center.offset(0, y, 0), Blocks.BEDROCK.defaultBlockState(), 3);
		}
		world.setBlock(center.offset(0, 4, 0), Blocks.TORCH.defaultBlockState(), 3);

		LOG.info("Exit portal generated: 8 END_PORTAL blocks + bedrock frame at Y=" + center.getY());
	}

	/**
	 * Pre-computes tower positions, heights, and protection status from the battle seed.
	 * Must be called before spawning towers individually.
	 */
	private void precomputeTowers() {
		towerSpawnData = new ArrayList<>(numberOfTowers);
		int centerX = exitPortalLocation.getX();
		int centerZ = exitPortalLocation.getZ();
		Random towerRandom = new Random(battleSeed);

		for (int i = 0; i < numberOfTowers; i++) {
			double angle = 2.0 * Math.PI * i / numberOfTowers;
			int towerX = centerX + (int) (distanceTillTowers * Math.cos(angle));
			int towerZ = centerZ + (int) (distanceTillTowers * Math.sin(angle));
			int towerHeight = minTowerHeight + towerRandom.nextInt(Math.max(1, maxTowerHeight - minTowerHeight + 1));
			boolean isProtected = i < numberOfProtectedTowers;
			towerSpawnData.add(new int[]{towerX, towerZ, towerHeight, isProtected ? 1 : 0});
		}
	}

	/**
	 * Spawns a single tower at the pre-computed position, plays an explosion sound at the crystal,
	 * and sets the crystal's beam target to the exit portal center.
	 */
	private void spawnSingleTower(int index) {
		int[] data = towerSpawnData.get(index);
		int towerX = data[0];
		int towerZ = data[1];
		int towerHeight = data[2];
		boolean isProtected = data[3] == 1;
		int baseY = exitPortalLocation.getY();
		int radius = 2;

		LOG.info("Tower " + index + ": pos=" + towerX + "," + towerZ +
			" height=" + towerHeight + " protected=" + isProtected);

		for (int y = baseY; y <= towerHeight; y++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dz * dz <= radius * radius) {
						world.setBlock(
							new BlockPos(towerX + dx, y, towerZ + dz),
							Blocks.OBSIDIAN.defaultBlockState(), 3);
					}
				}
			}
		}

		world.setBlock(new BlockPos(towerX, towerHeight + 1, towerZ),
			Blocks.BEDROCK.defaultBlockState(), 3);

		if (isProtected) {
			for (int dy = -1; dy <= 3; dy++) {
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						boolean isEdge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
						boolean isCorner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
						if (isEdge && !isCorner) {
							world.setBlock(
								new BlockPos(towerX + dx, towerHeight + 2 + dy, towerZ + dz),
								Blocks.IRON_BARS.defaultBlockState(), 3);
						}
					}
				}
			}
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean isCorner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
					if (!isCorner) {
						world.setBlock(
							new BlockPos(towerX + dx, towerHeight + 5, towerZ + dz),
							Blocks.IRON_BARS.defaultBlockState(), 3);
					}
				}
			}
		}

		net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal =
			new net.minecraft.world.entity.boss.enderdragon.EndCrystal(world,
				towerX + 0.5, towerHeight + 2, towerZ + 0.5);
		crystal.setShowBottom(false);
		crystal.setInvulnerable(false);
		crystal.setBeamTarget(exitPortalLocation);
		world.addFreshEntity(crystal);
		towerCrystals.add(crystal);

		world.playSound(null, exitPortalLocation.getX() + 0.5, exitPortalLocation.getY(), exitPortalLocation.getZ() + 0.5,
			SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 3.2f, 1.0f);

		LOG.info("Tower " + index + " spawned with beam target and explosion sound");
	}

	/**
	 * Clears beam targets from all end crystals in the battle area before the dragon spawns.
	 */
	private void clearCrystalBeamTargets() {
		AABB searchBox = new AABB(
			boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
			boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
		List<net.minecraft.world.entity.boss.enderdragon.EndCrystal> crystals =
			world.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, searchBox);
		for (net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal : crystals) {
			crystal.setBeamTarget(null);
		}
		towerCrystals.clear();
	}

	@Override
	public boolean onCrystalPlacement(EnderCrystal crystal) {
		boolean inBounds = boundingBox.contains(crystal.getLocation().toVector());
		LOG.info("onCrystalPlacement: stage=" + battleStage +
			" crystalAt=" + crystal.getLocation().getBlockX() + "," + crystal.getLocation().getBlockY() + "," + crystal.getLocation().getBlockZ() +
			" inBounds=" + inBounds);

		if (!inBounds) {
			LOG.info("Crystal outside bounding box " + boundingBox + ", ignoring");
			return false;
		}

		if (respawnCrystals == null) {
			respawnCrystals = new ArrayList<>();
		}
		net.minecraft.world.entity.Entity nmsEntity =
			((org.bukkit.craftbukkit.entity.CraftEntity) crystal).getHandle();
		if (nmsEntity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal ec) {
			respawnCrystals.add(ec);
			LOG.info("Tracked crystal, total count=" + respawnCrystals.size());
		}

		if (battleStage == null || battleStage == BattleStage.START || battleStage == BattleStage.END) {
			battleStage = BattleStage.START;
			tickCounter = 0;
			LOG.info("Crystal placement triggered battle -> START");
			return true;
		}

		LOG.info("Crystal placed but battle already in stage=" + battleStage + ", no state change");
		return false;
	}

	@Override
	@Nullable
	public Vector getGeneratedPortalLocation() {
		return exitPortalLocation != null ? new Vector(exitPortalLocation.getX(), exitPortalLocation.getY(), exitPortalLocation.getZ()) : null;
	}

	@Override
	public boolean isGenerated() {
		return exitPortalLocation != null;
	}

	@Override
	public boolean isFinished() {
		return battleStage == BattleStage.END;
	}

	@Override
	public String saveData() {
		CompoundTag compound = new CompoundTag();
		saveData(compound);
		return compound.toString();
	}

	private void saveData(CompoundTag nbt) {
		CompoundTag battleData = new CompoundTag();
		battleData.putString("BattleId", battleId);
		battleData.putString("BattleStage", battleStage != null ? battleStage.name() : "null");
		battleData.putLong("BattleSeed", battleSeed);
		battleData.putString("BarTitle", bossBarText);
		battleData.putString("BarColor", bossBattle.getColor().name().toLowerCase(Locale.ROOT));
		battleData.putString("BarStyle", bossBattle.getOverlay().name().toLowerCase(Locale.ROOT));
		battleData.putBoolean("BarMusic", bossBattle.shouldPlayBossMusic());
		battleData.putInt("TowerCount", numberOfTowers);
		battleData.putInt("ProtectedTowerCount", numberOfProtectedTowers);
		battleData.putInt("PathCount", numberPathPoints);
		battleData.putInt("TowerDistance", distanceTillTowers);
		battleData.putInt("MinHeight", minTowerHeight);
		battleData.putInt("MaxHeight", maxTowerHeight);
		battleData.putInt("CurrentTowerIndex", currentTowerIndex);
		if (exitPortalLocation != null) {
			writeBlockPos(battleData, "PortalLocation", exitPortalLocation);
		}
		writeBlockPos(battleData, "OriginalLocation", originalLocation);
		battleData.putDouble("Range", range);
		battleData.putBoolean("PreviouslyKilled", previouslyKilled);
		battleData.putBoolean("DragonKilled", dragonKilled);
		if (dragonUUID != null && enderDragon != null) {
			battleData.putString("DragonId", dragonUUID.toString());
			writeBlockPos(battleData, "LastLocation", enderDragon.blockPosition());
		}
		writeBlockPos(battleData, "BoundingBoxMin", new BlockPos(
			(int) Math.floor(boundingBox.getMinX()), (int) Math.floor(boundingBox.getMinY()), (int) Math.floor(boundingBox.getMinZ())));
		writeBlockPos(battleData, "BoundingBoxMax", new BlockPos(
			(int) Math.floor(boundingBox.getMaxX()), (int) Math.floor(boundingBox.getMaxY()), (int) Math.floor(boundingBox.getMaxZ())));
		if (respawnCrystals != null) {
			ListTag listTag = new ListTag();
			for (net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal : respawnCrystals) {
				if (crystal.isAlive()) {
					CompoundTag tag = new CompoundTag();
					tag.putString("CrystalID", crystal.getUUID().toString());
					listTag.add(tag);
				}
			}
			battleData.put("RespawnCrystals", listTag);
		}
		nbt.put("BattleData", battleData);
	}

	@Override
	@Nullable
	public UUID getLastDragonUUID() {
		return dragonUUID;
	}

	@Override
	@Nullable
	public Vector getLastDragonLocation() {
		if (lastLocation != null) {
			return new Vector(lastLocation.getX(), lastLocation.getY(), lastLocation.getZ());
		}
		return enderDragon != null ? new Vector(enderDragon.getX(), enderDragon.getY(), enderDragon.getZ()) : null;
	}
}
