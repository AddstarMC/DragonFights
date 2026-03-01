//
// Created by BONNe
// Copyright - 2020
//


package lv.id.bonne.dragonfights.managers;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import lv.id.bonne.custombattle.CustomDragonBattle;
import lv.id.bonne.custombattle.DragonBattleBuilder;
import lv.id.bonne.dragonfights.DragonFightsAddon;
import lv.id.bonne.dragonfights.config.DragonCharacteristic;
import lv.id.bonne.dragonfights.database.objects.DragonFightsObject;
import lv.id.bonne.dragonfights.entity.CustomEntityAPI;
import lv.id.bonne.dragonfights.utils.Constants;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;


/**
 * This class manages data from DragonFightAddon.
 */
public class DragonFightManager
{
	/**
	 * Default constructor.
	 * @param addon Instance of DragonsFightsAddon
	 */
	public DragonFightManager(DragonFightsAddon addon)
	{
		this.addon = addon;
		this.operationWorlds = new HashSet<>(6);

		this.dragonFightsDatabase = new Database<>(addon, DragonFightsObject.class);
		this.dragonFightsCache = new HashMap<>();

		this.generatedBattles = new HashMap<>();
		this.portalCleanupState = new HashMap<>();
	}


	/**
	 * Adds given world to operation worlds where dragonFights will work.
	 *
	 * @param world List of game mode names where this addon should work.
	 */
	public void addWorld(@Nullable World world)
	{
		if (world != null)
		{
			this.operationWorlds.add(world);
		}
	}


	/**
	 * Returns if addon operates in given world.
	 * @param world World that must be checked.
	 * @return {@code true} if addon can operate in given world, {@code false} otherwise.
	 */
	public boolean operatesInWorld(World world)
	{
		return this.operationWorlds.contains(world);
	}


// ---------------------------------------------------------------------
// Section: Data related methods
// ---------------------------------------------------------------------


	/**
	 * This method saves every active battle.
	 * During shutdown, Bukkit's scheduler is already stopped so the normal async
	 * database writes will never execute. We queue the writes via the normal API
	 * (which serializes the JSON on the calling thread) and then drain the
	 * handler's internal processQueue synchronously to flush them to disk.
	 */
	public void save()
	{
		this.stopTickTask();

		this.addon.log("save() called. Active battles: " + this.generatedBattles.size() +
			", cached objects: " + this.dragonFightsCache.size());

		this.generatedBattles.forEach((s, battle) -> {
			try
			{
				DragonFightsObject data = this.getIslandData(s);

				if (data != null)
				{
					Optional<Island> islandById = this.addon.getIslands().getIslandById(data.getUniqueId());

					if (islandById.isEmpty())
					{
						this.addon.logWarning("save(): Island not found for id=" + data.getUniqueId());
						return;
					}

					World world = islandById.get().getWorld();

					String battleData = battle.saveData();
					data.setLatestBattleData(battleData);
					data.setPortalLocation(battle.getGeneratedPortalLocation());
					data.setWorld(this.addon.getPlugin().getIWM().getEndWorld(world));

					this.addon.log("save(): Battle '" + s + "' stage=" +
						(battle.isFinished() ? "END" : "active") +
						" dragonUUID=" + battle.getLastDragonUUID() +
						" portal=" + battle.getGeneratedPortalLocation() +
						" world=" + data.getWorld() +
						" dataLength=" + (battleData != null ? battleData.length() : 0));
				}
				else
				{
					this.addon.logWarning("save(): No DragonFightsObject found for battle key=" + s);
				}
			}
			catch (Exception ex)
			{
				this.addon.logError("save(): Failed to serialize battle key=" + s + ": " + ex.getMessage());
				ex.printStackTrace();
			}
		});

		// Only queue saves for objects with active battles (these are the only ones modified).
		this.generatedBattles.keySet().forEach(id ->
		{
			DragonFightsObject obj = this.dragonFightsCache.get(id);

			if (obj != null)
			{
				try
				{
					this.addon.log("save(): Queueing write for id=" + id +
						" hasBattleData=" + (obj.getLatestBattleData() != null &&
							!obj.getLatestBattleData().isEmpty()));
					this.dragonFightsDatabase.saveObjectAsync(obj);
				}
				catch (Exception ex)
				{
					this.addon.logError("save(): Failed to queue object id=" + id + ": " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		});

		// During shutdown, Bukkit's scheduler is already dead so the handler's
		// async queue processor will never fire. Drain it manually on the main thread.
		this.flushDatabaseQueue();

		this.addon.log("save() complete.");
	}


	/**
	 * Drains the BentoBox JSON database handler's internal processQueue,
	 * running all pending writes synchronously on the calling thread.
	 * This is necessary during server shutdown when the Bukkit scheduler
	 * that normally processes the queue has already been cancelled.
	 */
	@SuppressWarnings("unchecked")
	private void flushDatabaseQueue()
	{
		try
		{
			java.lang.reflect.Field handlerField =
				Database.class.getDeclaredField("handler");
			handlerField.setAccessible(true);
			Object handler = handlerField.get(this.dragonFightsDatabase);

			java.lang.reflect.Field queueField = null;

			for (Class<?> c = handler.getClass(); c != null && c != Object.class; c = c.getSuperclass())
			{
				try
				{
					queueField = c.getDeclaredField("processQueue");
					break;
				}
				catch (NoSuchFieldException ignored)
				{
				}
			}

			if (queueField == null)
			{
				this.addon.logWarning("save(): Could not find processQueue field — database writes may be lost");
				return;
			}

			queueField.setAccessible(true);
			Queue<Runnable> queue = (Queue<Runnable>) queueField.get(handler);

			int flushed = 0;
			Runnable task;

			while ((task = queue.poll()) != null)
			{
				task.run();
				flushed++;
			}

			this.addon.log("save(): Flushed " + flushed + " pending database writes to disk.");
		}
		catch (Exception ex)
		{
			this.addon.logError("save(): Failed to flush database queue: " + ex.getMessage());
			ex.printStackTrace();
		}
	}


	/**
	 * This method loads everything from the database into local cache.
	 */
	public void load()
	{
		this.addon.log("load() called. operationWorlds: " + this.operationWorlds.size());

		List<DragonFightsObject> objects = this.dragonFightsDatabase.loadObjects();
		this.addon.log("load(): Loaded " + objects.size() + " objects from database.");

		objects.forEach(dragonFightsObject -> {
			this.dragonFightsCache.put(dragonFightsObject.getUniqueId(), dragonFightsObject);

			if (dragonFightsObject.getLatestBattleData() != null &&
				!dragonFightsObject.getLatestBattleData().isEmpty())
			{
				DragonBattleBuilder builder =
					CustomEntityAPI.getAPI().createDragonBattleBuilder(dragonFightsObject.getUniqueId());

				if (this.addon.getAddonManager().operatesInWorld(dragonFightsObject.getWorld()))
				{
					builder.setWorld(dragonFightsObject.getWorld());

					CustomDragonBattle customDragonBattle =
						builder.buildFromNBT(dragonFightsObject.getLatestBattleData());

					this.addon.log("load(): buildFromNBT result=" +
						(customDragonBattle != null ? "OK" : "NULL") +
						" for id=" + dragonFightsObject.getUniqueId());

					if (customDragonBattle != null)
					{
						this.generatedBattles.put(dragonFightsObject.getUniqueId(), customDragonBattle);

						if (dragonFightsObject.getDragonsKilled() == 0)
						{
							this.portalCleanupState.put(dragonFightsObject.getUniqueId(), new int[]{0});
						}

						this.addon.log("load(): Restored battle for id=" +
							dragonFightsObject.getUniqueId() +
							" dragonUUID=" + customDragonBattle.getLastDragonUUID() +
							" dragonLoc=" + customDragonBattle.getLastDragonLocation());
					}
				}
				else
				{
					this.addon.logWarning("load(): operatesInWorld=false for world=" +
						dragonFightsObject.getWorld() + " id=" + dragonFightsObject.getUniqueId());
					this.dragonFightsCache.remove(dragonFightsObject.getUniqueId());
				}
			}
		});

		this.addon.log("load() complete. Active battles: " + this.generatedBattles.size());

		if (!this.generatedBattles.isEmpty())
		{
			this.checkTickTaskNeeded();
		}
	}


	/**
	 * This method checks every island in stored worlds for user and loads them in cache.
	 *
	 * @param uniqueId User unique id.
	 */
	public void loadUserIslands(UUID uniqueId)
	{
		this.operationWorlds.stream().
			map(world -> this.addon.getIslands().getIsland(world, uniqueId)).
			filter(Objects::nonNull).
			forEach(this::addIslandData);
	}


	/**
	 * This method checks if island can be added to the cache.
	 *
	 * @param island Island object.
	 */
	public void loadIslandData(@NotNull Island island)
	{
		if (this.operationWorlds.contains(island.getWorld()))
		{
			this.addIslandData(island);
		}
	}


	/**
	 * This method allows to store single dragonFightsObject object.
	 *
	 * @param dragonFightsObject object that must be saved in database.
	 */
	public void saveDragonFightsData(DragonFightsObject dragonFightsObject)
	{
		this.dragonFightsDatabase.saveObjectAsync(dragonFightsObject);
	}


	/**
	 * This method returns data object for given island.
	 * @param island Island which data must be returned.
	 * @return instance of DragonFightsObject that stores data for given island.
	 */
	public DragonFightsObject getIslandData(@NotNull Island island)
	{
		this.addIslandData(island);
		return this.dragonFightsCache.get(island.getUniqueId());
	}


	/**
	 * This method tries to get addon data for island based on given string.
	 * @param islandUUID Island UUID value.
	 * @return instance of DragonFightsObject that stores data for given island or null.
	 */
	public @Nullable DragonFightsObject getIslandData(@NotNull String islandUUID)
	{
		// This is very very bad code, but bukkit Namespace generator does not support upper letters.

		return this.addon.getIslands().getIslandById(islandUUID).map(this::getIslandData).orElse(
			this.addon.getIslands().getIslandCache().getIslands().stream().
				filter(island -> island.getUniqueId().equalsIgnoreCase(islandUUID)).
				findFirst().
				map(this::getIslandData).
				orElse(null));
	}


	/**
	 * Load island from database into the cache or create new island data
	 *
	 * @param island - island that must be loaded
	 */
	private void addIslandData(@NotNull Island island)
	{
		final String uniqueID = island.getUniqueId();

		if (this.dragonFightsCache.containsKey(uniqueID))
		{
			return;
		}

		// The island is not in the cache
		// Check if the island exists in the database

		if (this.dragonFightsDatabase.objectExists(uniqueID))
		{
			// Load player from database
			DragonFightsObject data = this.dragonFightsDatabase.loadObject(uniqueID);
			// Store in cache

			if (data != null)
			{
				this.dragonFightsCache.put(uniqueID, data);
			}
			else
			{
				this.addon.logError("Could not load NULL generator data object.");
			}
		}
		else
		{
			// Create the island data
			DragonFightsObject pd = new DragonFightsObject();
			pd.setUniqueId(uniqueID);

			// Save data.
			this.saveDragonFightsData(pd);

			// Add to cache
			this.dragonFightsCache.put(uniqueID, pd);
		}
	}


	/**
	 * This method removes island data from cache and database.
	 * @param island Island data that must be removed.
	 */
	public void removeIslandData(@NotNull Island island)
	{
		this.addIslandData(island);

		if (this.dragonFightsCache.containsKey(island.getUniqueId()))
		{
			this.dragonFightsCache.remove(island.getUniqueId());
			this.dragonFightsDatabase.deleteID(island.getUniqueId());
		}
	}


// ---------------------------------------------------------------------
// Section: Dragon Battle Generation
// ---------------------------------------------------------------------


	/**
	 * This method returns Optional with generated dragon battle with given UniqueId.
	 * @param uniqueId Battle which must be returned.
	 * @return Optional with custom dragon battle.
	 */
	public Optional<CustomDragonBattle> getDragonBattle(String uniqueId)
	{
		return Optional.ofNullable(this.generatedBattles.get(uniqueId));
	}


	/**
	 * Returns whether a dragon is currently alive (battle active with a spawned dragon) for the given island.
	 * @param islandUniqueId The island unique id.
	 * @return true if a dragon battle is active and the dragon has been spawned.
	 */
	public boolean isDragonAlive(String islandUniqueId)
	{
		CustomDragonBattle battle = this.generatedBattles.get(islandUniqueId);
		return battle != null && battle.getLastDragonUUID() != null && !battle.isFinished();
	}


	/**
	 * This method generates dragon name based on island owner localization.
	 * @param island Island which dragon name must be generated.
	 * @return String of dragon name for the island.
	 */
	public String generateDragonName(Island island)
	{
		User owner = User.getInstance(island.getOwner());

		if (owner == null)
		{
			return island.getName();
		}
		else
		{
			return owner.getTranslation(Constants.DRAGON_NAME,
				Constants.PARAMETER_ISLAND, island.getName() == null ? "" : island.getName(),
				Constants.PARAMETER_OWNER, owner.getName());
		}
	}


	/**
	 * This method creates a new dragon battle instance using CustomEntityAPI.
	 * @param world World where battle can be generated.
	 * @param island Island on which battle is starting.
	 * @return Instance of CustomDragonBattle that is started.
	 */
	public CustomDragonBattle createDragonBattle(World world, Island island)
	{
		DragonFightsObject dragonFightsObject = this.getIslandData(island);
		dragonFightsObject.setWorld(world);

		DragonBattleBuilder dragonBattleBuilder = CustomEntityAPI.getAPI().createDragonBattleBuilder(island.getUniqueId());
		dragonBattleBuilder.setDragonKilled(dragonFightsObject.getDragonsKilled() > 0);
		dragonBattleBuilder.setPreviouslyKilled(dragonFightsObject.getDragonsKilled() > 0);
		dragonBattleBuilder.setWorld(world);

		if (dragonFightsObject.getPortalLocation() != null)
		{
			dragonBattleBuilder.setPortalLocation(dragonFightsObject.getPortalLocation());
		}
		else
		{
			dragonBattleBuilder.setPortalLocation(island.getCenter().toVector());
		}

		dragonBattleBuilder.setBoundingBox(island.getBoundingBox());
		dragonBattleBuilder.setRange(island.getRange());

		dragonBattleBuilder.setBattleSeed(this.addon.getSettings().getBattleSeed());
		dragonBattleBuilder.setNumberOfTowers(this.addon.getSettings().getTowerCount());
		dragonBattleBuilder.setNumberOfProtectedTowers(this.addon.getSettings().getNumberOfProtectedTowers());
		dragonBattleBuilder.setNumberOfPathPoints(this.addon.getSettings().getNumberOfPathPoints());
		dragonBattleBuilder.setMaxTowerHeight(this.addon.getSettings().getMaxTowerHeight());
		dragonBattleBuilder.setMinTowerHeight(this.addon.getSettings().getMinTowerHeight());
		dragonBattleBuilder.setDistanceTillTowers(this.addon.getSettings().getTowerDistance());

		dragonBattleBuilder.setPlayMusic(this.addon.getSettings().isPlayMusic());
		dragonBattleBuilder.setEnableFog(this.addon.getSettings().isEnableFog());

		dragonBattleBuilder.setBossBarStyle(this.addon.getSettings().getBossBarStyle());
		dragonBattleBuilder.setBossBarColor(this.addon.getSettings().getBossBarColour());
		dragonBattleBuilder.setBossBarText(this.generateDragonName(island));

		List<String> characteristicStrings = this.addon.getSettings().getDragonCharacteristics();

		if (characteristicStrings != null && !characteristicStrings.isEmpty())
		{
			List<DragonCharacteristic> validCharacteristics = characteristicStrings.stream()
				.map(DragonCharacteristic::parse)
				.filter(Objects::nonNull)
				.toList();

			if (!validCharacteristics.isEmpty())
			{
				DragonCharacteristic selected = validCharacteristics.get(
					ThreadLocalRandom.current().nextInt(validCharacteristics.size()));

				dragonBattleBuilder.setDragonMaxHealth(selected.getHealth());
				dragonBattleBuilder.setDragonSpeedMultiplier(selected.getSpeed());
				dragonBattleBuilder.setDragonGlowColor(selected.getColour());
				dragonBattleBuilder.setBossBarColor(selected.getColour());

				dragonFightsObject.setActiveCharacteristic(selected.serialize());
			}
		}

		CustomDragonBattle battle = dragonBattleBuilder.build();

		// start the battle

		if (battle != null)
		{
			this.generatedBattles.put(island.getUniqueId(), battle);
			this.startBattleTask(dragonFightsObject, battle, 0);
		}

		return battle;
	}


	/**
	 * Registers a battle for ticking. If no active battles existed before,
	 * this starts the single consolidated tick task.
	 * @param databaseObject The database object.
	 * @param battle Battle that must be started.
	 * @param delay Unused, kept for API compatibility.
	 */
	public void startBattleTask(DragonFightsObject databaseObject, CustomDragonBattle battle, long delay)
	{
		if (databaseObject.getDragonsKilled() == 0)
		{
			this.portalCleanupState.put(databaseObject.getUniqueId(), new int[]{0});
		}

		this.ensureTickTaskRunning();
	}


	/**
	 * Starts the single consolidated tick task if it is not already running.
	 */
	private void ensureTickTaskRunning()
	{
		if (this.tickTask == null || this.tickTask.isCancelled())
		{
			this.tickTask = Bukkit.getScheduler().runTaskTimer(
				BentoBox.getInstance(),
				this::tickAllBattles,
				1,
				1);
		}
	}


	/**
	 * Stops the consolidated tick task if it is running.
	 * Called when there are no active battles or no players in end worlds.
	 */
	private void stopTickTask()
	{
		if (this.tickTask != null && !this.tickTask.isCancelled())
		{
			this.tickTask.cancel();
			this.tickTask = null;
		}
	}


	/**
	 * Removes the given player from all active battle boss bars.
	 * Called when a player leaves an End world or disconnects to ensure the
	 * boss bar is cleaned up immediately rather than waiting for the next tick.
	 * @param player The player to remove.
	 */
	public void removePlayerFromAllBossBars(Player player)
	{
		for (CustomDragonBattle battle : this.generatedBattles.values())
		{
			battle.removeBossBarPlayer(player);
		}
	}


	/**
	 * Called by player enter/leave events to start or stop the tick task
	 * based on whether any players are in managed end worlds.
	 */
	public void checkTickTaskNeeded()
	{
		if (this.generatedBattles.isEmpty())
		{
			this.stopTickTask();
			return;
		}

		boolean anyPlayerInEnd = this.operationWorlds.stream()
			.filter(w -> World.Environment.THE_END.equals(w.getEnvironment()))
			.anyMatch(w -> !w.getPlayers().isEmpty());

		if (anyPlayerInEnd)
		{
			this.ensureTickTaskRunning();
		}
		else
		{
			this.stopTickTask();
		}
	}


	/**
	 * Single consolidated tick that processes all active battles.
	 */
	private void tickAllBattles()
	{
		if (this.generatedBattles.isEmpty())
		{
			this.stopTickTask();
			return;
		}

		List<String> finished = new ArrayList<>();

		for (Map.Entry<String, CustomDragonBattle> entry : this.generatedBattles.entrySet())
		{
			String id = entry.getKey();
			CustomDragonBattle battle = entry.getValue();
			DragonFightsObject data = this.dragonFightsCache.get(id);

			if (data == null || battle == null)
			{
				continue;
			}

			boolean loadedChunks;

			if (battle.getLastDragonUUID() != null &&
				battle.getLastDragonLocation() != null)
			{
				int chunkX = battle.getLastDragonLocation().getBlockX() >> 4;
				int chunkZ = battle.getLastDragonLocation().getBlockZ() >> 4;

				World world = data.getWorld();

				loadedChunks = world != null && world.isChunkLoaded(chunkX, chunkZ);
			}
			else
			{
				loadedChunks = true;
			}

			if (loadedChunks)
			{
				battle.tickBattle();

				int[] portalState = this.portalCleanupState.get(id);

				if (portalState != null)
				{
					this.checkAndRemovePortalBlocks(data, battle, portalState);
				}

				if (battle.isFinished())
				{
					finished.add(id);
				}
			}
		}

		for (String id : finished)
		{
			DragonFightsObject data = this.dragonFightsCache.get(id);
			CustomDragonBattle battle = this.generatedBattles.get(id);

			if (data != null && battle != null)
			{
				this.finishTheBattle(data, battle);
			}

			this.portalCleanupState.remove(id);
		}

		if (this.generatedBattles.isEmpty())
		{
			this.stopTickTask();
		}
	}


	/**
	 * This method process battle finishing data.
	 * @param databaseObject The database object.
	 * @param battle that must be finished.
	 */
	public void finishTheBattle(DragonFightsObject databaseObject, CustomDragonBattle battle)
	{
		boolean firstKill = databaseObject.getDragonsKilled() == 0;

		this.openExitPortal(databaseObject, battle);

		// Reset data to the null value.
		databaseObject.setLatestBattleData("");
		databaseObject.setDragonsKilled(databaseObject.getDragonsKilled() + 1);
		databaseObject.setPortalLocation(battle.getGeneratedPortalLocation());

		this.handleDragonEggReward(databaseObject, battle, firstKill);

		// Save data.
		this.saveDragonFightsData(databaseObject);

		// Remove battle from cache.
		this.generatedBattles.remove(databaseObject.getUniqueId());
	}


	/**
	 * Determines whether a dragon egg should be rewarded and places it if so.
	 * @param databaseObject The database object.
	 * @param battle The battle that just finished.
	 * @param firstKill Whether this was the first dragon kill on this island.
	 */
	private void handleDragonEggReward(DragonFightsObject databaseObject,
		CustomDragonBattle battle,
		boolean firstKill)
	{
		String islandId = databaseObject.getUniqueId();

		this.addon.log("Dragon killed on island " + islandId +
			" (kill #" + databaseObject.getDragonsKilled() + ").");

		if (firstKill)
		{
			if (this.addon.getSettings().isDragonEggDropOnFirstKill())
			{
				boolean placed = this.placeDragonEgg(databaseObject, battle);

				this.addon.log("First kill on island " + islandId +
					": dragon egg " + (placed ? "placed successfully." : "could not be placed (no valid air-above-bedrock location)."));
			}
			else
			{
				this.addon.log("First kill on island " + islandId +
					": dragon egg not rewarded (drop-on-first-kill is disabled).");
			}
		}
		else
		{
			double chance = this.addon.getSettings().getDragonEggDropChance();
			double roll = ThreadLocalRandom.current().nextDouble();

			if (roll < chance)
			{
				boolean placed = this.placeDragonEgg(databaseObject, battle);

				this.addon.log("Subsequent kill on island " + islandId +
					": dragon egg " + (placed ? "placed successfully" : "could not be placed (no valid air-above-bedrock location)") +
					" (roll=" + String.format("%.4f", roll) + ", chance=" + chance + ").");
			}
			else
			{
				this.addon.log("Subsequent kill on island " + islandId +
					": dragon egg not rewarded (roll=" + String.format("%.4f", roll) + ", chance=" + chance + ").");
			}
		}
	}


	/**
	 * Places a dragon egg on top of bedrock in a 5x5x5 area centred on the exit portal pillar.
	 * Only replaces AIR blocks that sit directly on top of BEDROCK.
	 * @param databaseObject The database object.
	 * @param battle The battle that just finished.
	 * @return true if the egg was placed, false if no valid location was found.
	 */
	private boolean placeDragonEgg(DragonFightsObject databaseObject, CustomDragonBattle battle)
	{
		Vector portalLoc = battle.getGeneratedPortalLocation();
		World world = databaseObject.getWorld();

		if (world == null || portalLoc == null)
		{
			return false;
		}

		int cx = portalLoc.getBlockX();
		int cy = portalLoc.getBlockY();
		int cz = portalLoc.getBlockZ();

		// Search a 5x5x5 volume centred above the portal for the highest bedrock with air above.
		// Start from the top so the egg ends up at the highest valid spot (top of the pillar).
		for (int dy = 4; dy >= 0; dy--)
		{
			for (int dx = -2; dx <= 2; dx++)
			{
				for (int dz = -2; dz <= 2; dz++)
				{
					Block candidate = world.getBlockAt(cx + dx, cy + dy, cz + dz);
					Block below = world.getBlockAt(cx + dx, cy + dy - 1, cz + dz);

					if (candidate.getType() == Material.AIR && below.getType() == Material.BEDROCK)
					{
						candidate.setType(Material.DRAGON_EGG);
						return true;
					}
				}
			}
		}

		return false;
	}


	/**
	 * Places END_PORTAL blocks in the exit portal after the dragon is killed.
	 * The portal inner ring is the circular area (radius < sqrt(7)) at the portal base level,
	 * excluding the center bedrock pillar.
	 * @param databaseObject The database object.
	 * @param battle The battle that just finished.
	 */
	private void openExitPortal(DragonFightsObject databaseObject, CustomDragonBattle battle)
	{
		Vector portalLoc = battle.getGeneratedPortalLocation();
		World world = databaseObject.getWorld();

		if (world == null || portalLoc == null)
		{
			return;
		}

		int px = portalLoc.getBlockX();
		int py = portalLoc.getBlockY() - 1;
		int pz = portalLoc.getBlockZ();

		for (int dx = -2; dx <= 2; dx++)
		{
			for (int dz = -2; dz <= 2; dz++)
			{
				if (dx == 0 && dz == 0)
				{
					continue;
				}

				if (dx * dx + dz * dz < 7)
				{
					world.getBlockAt(px + dx, py, pz + dz).setType(Material.END_PORTAL);
				}
			}
		}
	}


	/**
	 * This method grants the given list of advancements to all players on the given island.
	 * @param island island which players receives advancements.
	 * @param advancementList The list of advancements.
	 */
	public void grantAdvancements(Island island, Map<String, String> advancementList)
	{
		if (advancementList.isEmpty())
		{
			// No advancements in this category.
			return;
		}

		island.getPlayersOnIsland().forEach(player -> {
			// only to players in end dimension.
			if (World.Environment.THE_END.equals(player.getWorld().getEnvironment()))
			{
				this.grantAdvancements(player, advancementList);
			}
		});
	}


	/**
	 * This method grants the given list of advancements to the player.
	 * @param player Player who receives advancements.
	 * @param advancementList The list of advancements.
	 */
	public void grantAdvancements(Player player, Map<String, String> advancementList)
	{
		if (player == null || advancementList.isEmpty())
		{
			return;
		}

		advancementList.forEach((advancementID, criteria) ->
		{
			NamespacedKey namespacedKey = NamespacedKey.fromString(advancementID);

			if (namespacedKey != null)
			{
				Advancement advancement = Bukkit.getAdvancement(namespacedKey);

				if (advancement != null)
				{
					AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);

					// Only for players that does not have it.

					if (!advancementProgress.isDone())
					{
						advancementProgress.awardCriteria(criteria);
					}
				}
			}
		});
	}


	/**
	 * Checks if the battle has generated END_PORTAL blocks and removes them.
	 * This keeps the exit portal closed until the dragon is killed for the first time.
	 * @param data The database object.
	 * @param battle The battle.
	 * @param portalState Single-element array tracking cleanup tick count.
	 *                    Removed from the map when cleanup is done.
	 */
	private void checkAndRemovePortalBlocks(DragonFightsObject data,
		CustomDragonBattle battle, int[] portalState)
	{
		Vector portalLoc = battle.getGeneratedPortalLocation();
		World world = data.getWorld();

		if (world == null || portalLoc == null)
		{
			return;
		}

		int px = portalLoc.getBlockX();
		int py = portalLoc.getBlockY() - 1;
		int pz = portalLoc.getBlockZ();

		if (world.getBlockAt(px + 1, py, pz).getType() == Material.END_PORTAL)
		{
			for (int dx = -2; dx <= 2; dx++)
			{
				for (int dz = -2; dz <= 2; dz++)
				{
					if (dx == 0 && dz == 0)
					{
						continue;
					}

					if (dx * dx + dz * dz < 7)
					{
						Block block = world.getBlockAt(px + dx, py, pz + dz);

						if (block.getType() == Material.END_PORTAL)
						{
							block.setType(Material.AIR);
						}
					}
				}
			}

			this.portalCleanupState.remove(data.getUniqueId());
		}
		else if (++portalState[0] > 100)
		{
			this.portalCleanupState.remove(data.getUniqueId());
		}
	}


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


	/**
	 * Instance of dragon fights addon
	 */
	private final DragonFightsAddon addon;

	/**
	 * This variable holds worlds where addon should work.
	 */
	private final Set<World> operationWorlds;

	/**
	 * Database storage object.
	 */
	private final Database<DragonFightsObject> dragonFightsDatabase;

	/**
	 * Database cache.
	 */
	private final Map<String, DragonFightsObject> dragonFightsCache;

	/**
	 * Stores portal generator cache.
	 */
	private final Map<String, CustomDragonBattle> generatedBattles;

	/**
	 * Tracks portal cleanup tick count per island (first fight only).
	 * Key = island unique id, Value = single-element int array for mutable tick counter.
	 */
	private final Map<String, int[]> portalCleanupState;

	/**
	 * The single consolidated tick task for all battles.
	 * Null when no task is running (no battles or no players in end worlds).
	 */
	private BukkitTask tickTask;
}
