package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;

public class EntityExplodeListener implements Listener
{
    public static void handleExplosion(Location location, Entity entity, List<Block> blocks)
    {
        GriefPrevention plugin = GriefPrevention.instance;
        DataStore dataStore = plugin.getDataStore();

        //only applies to claims-enabled worlds
        World world = location.getWorld();

        if (!plugin.claimsEnabledForWorld(world)) return;

        //FEATURE: explosions don't destroy surface blocks by default
        boolean isCreeper = (entity != null && entity.getType() == EntityType.CREEPER);

        boolean applySurfaceRules = world.getEnvironment() == World.Environment.NORMAL && (
                (isCreeper && plugin.config_blockSurfaceCreeperExplosions) || (!isCreeper
                                                                               &&
                                                                               plugin.config_blockSurfaceOtherExplosions));

        //special rule for creative worlds: explosions don't destroy anything
        if (plugin.creativeRulesApply(location)) {
            for (int i = 0; i < blocks.size(); i++) {
                blocks.remove(i--);
            }

            return;
        }

        //make a list of blocks which were allowed to explode
        List<Block> explodedBlocks = new ArrayList<>();
        Claim cachedClaim = null;
        for (Block block : blocks) {
            //always ignore air blocks
            if (block.getType() == Material.AIR) continue;

            //is it in a land claim?
            Claim claim = dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
            if (claim != null) {
                cachedClaim = claim;
            }

            //if yes, apply claim exemptions if they should apply
            if (claim != null && (claim.areExplosivesAllowed || !plugin.config_blockClaimExplosions)) {
                explodedBlocks.add(block);
                continue;
            }

            //if claim is under siege, allow soft blocks to be destroyed
            if (claim != null && claim.siegeData != null) {
                Material material = block.getType();
                boolean breakable = plugin.getPluginConfig().getSiegeConfiguration().isBreakableBlock(material);

                if (breakable) continue;
            }

            //if no, then also consider surface rules
            if (claim == null) {
                if (!applySurfaceRules || block.getLocation().getBlockY() < plugin.getSeaLevel(world) - 7) {
                    explodedBlocks.add(block);
                }
            }
        }

        //clear original damage list and replace with allowed damage list
        blocks.clear();
        blocks.addAll(explodedBlocks);
    }

    //when an entity explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent explodeEvent)
    {
        handleExplosion(explodeEvent.getLocation(), explodeEvent.getEntity(), explodeEvent.blockList());
    }
}
