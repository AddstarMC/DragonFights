//
// Created by BONNe
// Copyright - 2020
//


package lv.id.bonne.dragonfights.listeners;


import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
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
	 * Only crystals placed on specific exit portal positions are accepted:
	 * - Initial spawn (no previous kills, no active battle): crystal on top of center pillar
	 * - Respawn (previous kills, or battle already in progress): crystal on one of 4 edge positions
	 * @param event Entity Spawn event that must be monitored.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCrystalPlacement(EntitySpawnEvent event)
	{
		if (event.getEntityType() != EntityType.END_CRYSTAL)
		{
			// Not an ender crystal.
			return;
		}

		World world = event.getLocation().getWorld();

		if (world == null || !this.addon.getPlugin().getIWM().isIslandEnd(world))
		{
			// Not a bentobox end island.
			return;
		}

		if (!this.addon.getAddonManager().operatesInWorld(world))
		{
			// Not operating in given gamemode.
			return;
		}

		if (event.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.BEDROCK)
		{
			// Not on the bedrock.
			return;
		}

		Location bedrockLocation = event.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
		Optional<Island> optionalIsland = this.addon.getPlugin().getIslands().getIslandAt(bedrockLocation);

		if (!optionalIsland.isPresent())
		{
			// Not on the island
			return;
		}

		Island island = optionalIsland.get();

		if (island.getCenter().getBlockX() == 0 && island.getCenter().getBlockZ() == 0)
		{
			// Dragon should not operate for 0, 0 island because that spot is reserved for
			// vanilla ender dragon.
			return;
		}

		DragonFightsObject islandData = this.addonManager.getIslandData(island);

		Vector portalLocation = islandData.getPortalLocation() != null ?
			islandData.getPortalLocation() : island.getCenter().toVector();

		int portalX = portalLocation.getBlockX();
		int portalY = portalLocation.getBlockY();
		int portalZ = portalLocation.getBlockZ();

		int bedrockX = bedrockLocation.getBlockX();
		int bedrockY = bedrockLocation.getBlockY();
		int bedrockZ = bedrockLocation.getBlockZ();

		Optional<CustomDragonBattle> optionalBattle =
			this.addonManager.getDragonBattle(island.getUniqueId());

		if (!optionalBattle.isPresent() && islandData.getDragonsKilled() == 0)
		{
			// Initial spawn: crystal must be on top of the center bedrock pillar (±1 Y tolerance)
			if (bedrockX != portalX || bedrockZ != portalZ || bedrockY < portalY - 1)
			{
				return;
			}

			CustomDragonBattle battle = this.addonManager.createDragonBattle(world, island);

			if (battle == null)
			{
				return;
			}

			Bukkit.getScheduler().runTask(this.addon.getPlugin(),
				tick -> battle.onCrystalPlacement((EnderCrystal) event.getEntity()));
		}
		else
		{
			// Respawn or battle in progress: crystal must be on bedrock near the
			// exit portal, but NOT on the center pillar itself.
			if (bedrockX == portalX && bedrockZ == portalZ)
			{
				// On the center pillar -- not a respawn crystal.
				return;
			}

			if (Math.abs(bedrockX - portalX) > 5 || Math.abs(bedrockZ - portalZ) > 5)
			{
				// Too far from the portal to be a respawn crystal.
				return;
			}

			// Count end crystals near the portal (excluding center pillar) -- need 4.
			// Note that this does not include the crystal that is being placed (so it's N-1 crystals total)
			List<EnderCrystal> edgeCrystals =
				this.findPortalEdgeCrystals(world, portalX, portalY, portalZ);

			if ((edgeCrystals.size()+1) < 4)
			{
				this.addon.log("Only " + (edgeCrystals.size()+1) + " crystals near the portal, but we need 4");
				return;
			}

			CustomDragonBattle battle = optionalBattle.orElseGet(() ->
				this.addonManager.createDragonBattle(bedrockLocation.getWorld(), island));

			if (battle == null)
			{
				return;
			}

			if (battle.isFinished())
			{
				this.addon.log("Battle is finished, starting a new one");
				this.addonManager.startBattleTask(this.addonManager.getIslandData(island), battle, 0);
			}

			Bukkit.getScheduler().runTask(this.addon.getPlugin(), tick ->
			{
				for (EnderCrystal crystal : edgeCrystals)
				{
					battle.onCrystalPlacement(crystal);
				}
			});
		}
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
// Section: Helper Methods
// ---------------------------------------------------------------------


	/**
	 * Finds all end crystal entities near the exit portal, excluding any on the center pillar.
	 * Searches within a 5-block XZ radius and 4-block Y radius of the portal base.
	 * @param world The world to search.
	 * @param portalX Portal center X.
	 * @param portalY Portal center Y.
	 * @param portalZ Portal center Z.
	 * @return List of end crystals found near the portal edge.
	 */
	private List<EnderCrystal> findPortalEdgeCrystals(World world, int portalX, int portalY, int portalZ)
	{
		List<EnderCrystal> crystals = new ArrayList<>();
		Location portalCenter = new Location(world,
			portalX + 0.5, portalY + 1.5, portalZ + 0.5);

		for (Entity entity : world.getNearbyEntities(portalCenter, 5, 4, 5))
		{
			if (entity.getType() == EntityType.END_CRYSTAL)
			{
				int entityBlockX = entity.getLocation().getBlockX();
				int entityBlockZ = entity.getLocation().getBlockZ();

				if (entityBlockX != portalX || entityBlockZ != portalZ)
				{
					crystals.add((EnderCrystal) entity);
				}
			}
		}

		return crystals;
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
