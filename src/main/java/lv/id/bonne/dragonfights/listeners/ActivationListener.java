//
// Created by BONNe
// Copyright - 2020
//


package lv.id.bonne.dragonfights.listeners;


import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.util.Vector;

import java.util.Optional;

import lv.id.bonne.custombattle.CustomDragonBattle;
import lv.id.bonne.dragonfights.DragonFightsAddon;
import lv.id.bonne.dragonfights.database.objects.DragonFightsObject;
import lv.id.bonne.dragonfights.managers.DragonFightManager;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;


/**
 * This method activates ender dragon battle.
 */
public class ActivationListener implements Listener
{
	/**
	 * Main class Constructor.
	 * @param addon DragonFightsAddon instance.
	 */
	public ActivationListener(DragonFightsAddon addon)
	{
		this.addon = addon;
		this.addonManager = addon.getAddonManager();
	}


	/**
	 * This event checks if player joins the end world for the first time and enables battle sequence, if it is.
	 * @param event PlayerChangedWorldEvent that must be monitored.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFirstEndJoin(PlayerChangedWorldEvent event)
	{
		if (!this.addon.getPlugin().getIWM().isIslandEnd(event.getPlayer().getWorld()))
		{
			// Not end islands or end world. Do not operate here.
			return;
		}

		if (!this.addon.getAddonManager().operatesInWorld(event.getPlayer().getWorld()))
		{
			// Not operating in given gamemode.
			return;
		}

		if (!this.addon.getSettings().isStartOnFirstJoin())
		{
			// Battle should not start on first enabling.
			return;
		}

		Optional<Island> islandOptional = this.addon.getIslands().getIslandAt(event.getPlayer().getLocation());

		if (!islandOptional.isPresent())
		{
			// There are no island at the given location.
			return;
		}

		Island island = islandOptional.get();

		if (island.getCenter().getBlockX() == 0 && island.getCenter().getBlockZ() == 0)
		{
			// Dragon should not operate for 0, 0 island because that spot is reserved for
			// vanilla ender dragon.
			return;
		}

		if (island.getRank(User.getInstance(event.getPlayer())) < RanksManager.MEMBER_RANK)
		{
			// Only island members should activate the battle.
			return;
		}

		DragonFightsObject islandData = this.addonManager.getIslandData(island);

		if (islandData.getDragonsKilled() > 0)
		{
			// Dragon was already killed.
			return;
		}

		Optional<CustomDragonBattle> optionalBattle =
			this.addonManager.getDragonBattle(islandData.getUniqueId());

		if (optionalBattle.isPresent())
		{
			// Well, battle already in progress.
			return;
		}

		if (island.getCenter() == null)
		{
			// Emm... wth?
			return;
		}

		// Ok, it has been scrapped all situations when battle should not be started. Now I should hack in some battle.

		CustomDragonBattle dragonBattle = this.addonManager.createDragonBattle(event.getPlayer().getWorld(),
			island);

		// Tick battle once.
		dragonBattle.tickBattle();

		// Generate crystals based on portal location.
		Vector generatedPortalLocation = dragonBattle.getGeneratedPortalLocation();

		if (generatedPortalLocation == null)
		{
			System.out.println("Missing Portal Location.");
			return;
		}

		// Summon crystals.
		World world = event.getPlayer().getWorld();

		world.spawnEntity(generatedPortalLocation.toLocation(world).add(3.5, 1, 0.5), EntityType.END_CRYSTAL);
		world.spawnEntity(generatedPortalLocation.toLocation(world).add(-2.5, 1, 0.5), EntityType.END_CRYSTAL);
		world.spawnEntity(generatedPortalLocation.toLocation(world).add(0.5, 1, 3.5), EntityType.END_CRYSTAL);
		world.spawnEntity(generatedPortalLocation.toLocation(world).add(0.5, 1, -2.5), EntityType.END_CRYSTAL);
		// The battle should start.
	}


	/**
	 * This event passes crystal placement event to the correct ender dragon battle instance.
	 * @param event Entity Spawn event that must be monitored.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCrystalPlacement(EntitySpawnEvent event)
	{
		if (event.getEntityType() != EntityType.END_CRYSTAL)
		{
			return;
		}

		World world = event.getLocation().getWorld();
		Bukkit.getLogger().info("END_CRYSTAL spawned at " + event.getLocation());

		if (world == null || !this.addon.getPlugin().getIWM().isIslandEnd(world))
		{
			Bukkit.getLogger().info("Crystal: not in BentoBox end island world (world=" +
				(world != null ? world.getName() : "null") + ")");
			return;
		}

		if (!this.addon.getAddonManager().operatesInWorld(world))
		{
			Bukkit.getLogger().info("Crystal: addon not operating in world " + world.getName());
			return;
		}

		if (event.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.BEDROCK)
		{
			Bukkit.getLogger().info("Crystal: block below is " +
				event.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() + ", not BEDROCK");
			return;
		}

		Location location = event.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
		Optional<Island> optionalIsland = this.addon.getPlugin().getIslands().getIslandAt(location);

		if (!optionalIsland.isPresent())
		{
			Bukkit.getLogger().info("Crystal: no island at location " + location);
			return;
		}

		Island island = optionalIsland.get();
		Bukkit.getLogger().info("Crystal: island=" + island.getUniqueId() +
			" center=" + island.getCenter() + " range=" + island.getRange());

		if (island.getCenter().getBlockX() == 0 && island.getCenter().getBlockZ() == 0)
		{
			Bukkit.getLogger().info("Crystal: skipping 0,0 island (reserved for vanilla)");
			return;
		}

		Optional<CustomDragonBattle> optionalBattle =
			this.addonManager.getDragonBattle(island.getUniqueId());

		Bukkit.getLogger().info("Crystal: existing battle present=" + optionalBattle.isPresent());

		CustomDragonBattle battle = optionalBattle.orElseGet(() ->
			this.addonManager.createDragonBattle(location.getWorld(), island));

		if (battle == null)
		{
			Bukkit.getLogger().severe("Crystal: FAILED to create/get battle! createDragonBattle returned null");
			return;
		}

		Bukkit.getLogger().info("Crystal: battle.isFinished()=" + battle.isFinished() +
			" battle.isGenerated()=" + battle.isGenerated());

		if (battle.isFinished())
		{
			Bukkit.getLogger().info("Crystal: restarting finished battle tick task");
			this.addonManager.startBattleTask(this.addonManager.getIslandData(island), battle, 0);
		}

		Bukkit.getScheduler().runTask(this.addon.getPlugin(),
			tick -> battle.onCrystalPlacement((EnderCrystal) event.getEntity()));

		Bukkit.getLogger().info("Crystal: onCrystalPlacement callback scheduled for next tick");
	}


	/**
	 * This event passes crystal damage event to the correct ender dragon battle instance.
	 * @param event Entity Damage event that must be monitored.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCrystalDamage(EntityDamageEvent event)
	{
		if (event.getEntityType() != EntityType.END_CRYSTAL)
		{
			// Not an ender crystal.
			return;
		}

		World world = event.getEntity().getWorld();

		if (!this.addon.getPlugin().getIWM().isIslandEnd(world))
		{
			// Not a bentobox end island.
			return;
		}

		if (!this.addon.getAddonManager().operatesInWorld(world))
		{
			// Not operating in given gamemode.
			return;
		}

		Location location = event.getEntity().getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
		Optional<Island> optionalIsland = this.addon.getPlugin().getIslands().getIslandAt(location);

		if (!optionalIsland.isPresent())
		{
			// Not on the island
			return;
		}

		Optional<CustomDragonBattle> customDragonBattle =
			this.addonManager.getDragonBattle(optionalIsland.get().getUniqueId());

		// Pass damage event to the dragon battle.
		customDragonBattle.ifPresent(battle ->
			Bukkit.getScheduler().runTask(this.addon.getPlugin(),
				tick -> battle.onCrystalDamage((EnderCrystal) event.getEntity())));
	}


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


	/**
	 * DragonFightsAddon instance.
	 */
	private final DragonFightsAddon addon;

	/**
	 * Addon Manager Instance.
	 */
	private final DragonFightManager addonManager;
}
