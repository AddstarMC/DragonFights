//
// Created by BONNe
// Copyright - 2020
//


package lv.id.bonne.dragonfights.listeners;


import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


import lv.id.bonne.dragonfights.DragonFightsAddon;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;


/**
 * This listener loads player islands in cache when they login.
 */
public class JoinLeaveListener implements Listener
{
	/**
	 * @param addon - addon
	 */
	public JoinLeaveListener(DragonFightsAddon addon)
	{
		this.addon = addon;
	}


	/**
	 * This method handles player join event. When player joins it loads all its islands
	 * into local cache.
	 * @param event PlayerJoinEvent instance.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		this.addon.getAddonManager().loadUserIslands(event.getPlayer().getUniqueId());

		if (World.Environment.THE_END.equals(event.getPlayer().getWorld().getEnvironment()))
		{
			this.addon.getAddonManager().checkTickTaskNeeded();
		}
	}


	/**
	 * Pauses or resumes the battle tick task when a player leaves the server.
	 * @param event PlayerQuitEvent instance.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		if (World.Environment.THE_END.equals(event.getPlayer().getWorld().getEnvironment()))
		{
			this.addon.getAddonManager().checkTickTaskNeeded();
		}
	}


	/**
	 * Pauses or resumes the battle tick task when a player changes worlds.
	 * @param event PlayerChangedWorldEvent instance.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldChange(PlayerChangedWorldEvent event)
	{
		boolean wasEnd = World.Environment.THE_END.equals(event.getFrom().getEnvironment());
		boolean isEnd = World.Environment.THE_END.equals(event.getPlayer().getWorld().getEnvironment());

		if (wasEnd || isEnd)
		{
			this.addon.getAddonManager().checkTickTaskNeeded();
		}
	}


	/**
	 * This method handles Island Created, Resetted and Registered events.
	 * @param event Event that must be handled.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNewIsland(IslandEvent event)
	{
		if (event.getReason().equals(IslandEvent.Reason.CREATED) ||
			event.getReason().equals(IslandEvent.Reason.RESETTED) ||
			event.getReason().equals(IslandEvent.Reason.REGISTERED))
		{
			this.addon.getAddonManager().loadIslandData(event.getIsland());
		}
	}


	/**
	 * This method handles island deletion. On island deletion it should remove
	 * generator data too.
	 * @param event IslandDeletedEvent instance.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onIslandDelete(IslandDeleteEvent event)
	{
		this.addon.getAddonManager().removeIslandData(event.getIsland());
	}


	/**
	 * stores addon instance
	 */
	private final DragonFightsAddon addon;
}