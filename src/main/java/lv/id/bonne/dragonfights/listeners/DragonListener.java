//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.dragonfights.listeners;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import lv.id.bonne.custombattle.CustomDragonBattle;
import lv.id.bonne.dragonfights.DragonFightsAddon;
import lv.id.bonne.dragonfights.database.objects.DragonFightsObject;
import lv.id.bonne.dragonfights.managers.DragonFightManager;
import world.bentobox.bentobox.database.objects.Island;


/**
 * This class checks dragon related things.
 */
public class DragonListener implements Listener
{
    /**
     * @param addon - addon
     */
    public DragonListener(DragonFightsAddon addon)
    {
        this.addon = addon;
        this.addonManager = addon.getAddonManager();
    }


    /**
     * This listener manages entity spawning advancements.
     * @param event Dragon spawn event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonSummon(EntitySpawnEvent event)
    {
        if (!EntityType.ENDER_DRAGON.equals(event.getEntityType()))
        {
            // Interested only in custom dragon entity.
            return;
        }

        if (this.addon.getSettings().getSummonAdvancementList().isEmpty() &&
            this.addon.getSettings().getReSummonAdvancementList().isEmpty())
        {
            // Advancement granting is not necessary.
            return;
        }

        Optional<Island> optionalIsland =
            this.addon.getIslands().getIslandAt(event.getLocation());

        optionalIsland.ifPresent(island ->
        {
            DragonFightsObject islandData = this.addonManager.getIslandData(island);

            if (islandData != null)
            {
                long dragonsKilled = islandData.getDragonsKilled();

                if (dragonsKilled == 0)
                {
                    this.addonManager.grantAdvancements(island,
                        this.addon.getSettings().getSummonAdvancementList());
                }
                else
                {
                    this.addonManager.grantAdvancements(island,
                        this.addon.getSettings().getReSummonAdvancementList());
                }
            }
        });
    }


    /**
     * This listener manages entity death advancements.
     * @param event Dragon death event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonKilled(EntityDeathEvent event)
    {
        if (event.getEntityType() != EntityType.ENDER_DRAGON)
        {
            // Only interested in ender dragon kills.
            return;
        }

        this.addon.getIslands().getIslandAt(event.getEntity().getLocation()).
            ifPresent(island -> {
                this.addonManager.getDragonBattle(island.getUniqueId()).ifPresent(battle ->
                {
                    if (event.getEntity().getUniqueId().equals(battle.getLastDragonUUID()))
                    {
                        // Grant advancement to all players on island
                        this.addonManager.grantAdvancements(island,
                            this.addon.getSettings().getKilledAdvancementList());

                        // Grant advancements to the killer.
                        this.addonManager.grantAdvancements(event.getEntity().getKiller(),
                            this.addon.getSettings().getKillerAdvancementList());

                        this.tryDropDragonEgg(island, event, battle);
                    }
                });
            });
    }


    /**
     * Attempts to place a dragon egg on the exit portal post based on island kill count and config settings.
     * Always places on first kill (if enabled), otherwise rolls against the configured chance.
     * Prefers the block above the center pillar (air above torch); if not empty, searches 5x5x5 for a solid block with air above.
     *
     * @param island Island where the dragon was killed.
     * @param event The dragon death event.
     * @param battle The dragon battle (for portal location).
     */
    private void tryDropDragonEgg(Island island, EntityDeathEvent event, CustomDragonBattle battle)
    {
        DragonFightsObject islandData = this.addonManager.getIslandData(island);

        if (islandData == null)
        {
            return;
        }

        boolean shouldDrop;

        if (islandData.getDragonsKilled() == 0)
        {
            shouldDrop = this.addon.getSettings().isDropEggOnFirstKill();
        }
        else
        {
            shouldDrop = ThreadLocalRandom.current().nextDouble() < this.addon.getSettings().getEggDropChance();
        }

        if (!shouldDrop)
        {
            return;
        }

        long killCount = islandData.getDragonsKilled() + 1;
        Player killer = event.getEntity().getKiller();
        String playerName = killer != null ? killer.getName() : "unknown";

        org.bukkit.util.Vector portalVec = battle.getGeneratedPortalLocation();
        if (portalVec == null)
        {
            Bukkit.getLogger().warning("[DragonFights] Dragon egg not placed: no portal location (kill #" + killCount + ")");
            return;
        }

        org.bukkit.World world = event.getEntity().getWorld();
        int centerX = portalVec.getBlockX();
        int centerY = portalVec.getBlockY();
        int centerZ = portalVec.getBlockZ();

        // Prefer top of center pillar: one block above the torch (pillar is center y+1..y+4, torch at y+4, so egg at y+5)
        Block preferred = world.getBlockAt(centerX, centerY + 5, centerZ);
        Block placeAt = null;

        if (preferred.isEmpty() || preferred.getType() == Material.AIR)
        {
            placeAt = preferred;
        }
        else
        {
            // Search 5x5x5 around the pillar top for an air block with a solid block below
            int radius = 2;
            search:
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dy = -radius; dy <= radius; dy++)
                {
                    for (int dz = -radius; dz <= radius; dz++)
                    {
                        int x = centerX + dx;
                        int y = centerY + 5 + dy;
                        int z = centerZ + dz;
                        Block above = world.getBlockAt(x, y, z);
                        Block below = world.getBlockAt(x, y - 1, z);
                        if ((above.isEmpty() || above.getType() == Material.AIR) && below.getType().isSolid())
                        {
                            placeAt = above;
                            break search;
                        }
                    }
                }
            }
        }

        if (placeAt != null)
        {
            placeAt.setType(Material.DRAGON_EGG);
            Bukkit.getLogger().info("[DragonFights] Dragon egg rewarded! Player: " + playerName +
                ", island dragon kill #" + killCount + ", placed on portal post at " +
                placeAt.getX() + "," + placeAt.getY() + "," + placeAt.getZ());
        }
        else
        {
            Bukkit.getLogger().warning("[DragonFights] Dragon egg not placed: no empty air block on portal post (5x5x5 search) for kill #" + killCount);
        }
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
