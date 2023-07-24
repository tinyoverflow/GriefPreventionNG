package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;

public class BlockBurnListener implements Listener {
    private final DataStore dataStore;

    public BlockBurnListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent burnEvent) {
        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(burnEvent.getBlock().getWorld())) return;

        if (!GriefPrevention.instance.config_fireDestroys) {
            burnEvent.setCancelled(true);
            Block block = burnEvent.getBlock();
            Block[] adjacentBlocks = new Block[]{block.getRelative(BlockFace.UP), block.getRelative(BlockFace.DOWN), block.getRelative(
                    BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.EAST), block.getRelative(
                    BlockFace.WEST)};

            //pro-actively put out any fires adjacent the burning block, to reduce future processing here
            for (Block adjacentBlock : adjacentBlocks) {
                if (adjacentBlock.getType() == Material.FIRE && adjacentBlock.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                    adjacentBlock.setType(Material.AIR);
                }
            }

            Block aboveBlock = block.getRelative(BlockFace.UP);
            if (aboveBlock.getType() == Material.FIRE) {
                aboveBlock.setType(Material.AIR);
            }
            return;
        }

        //never burn claimed blocks, regardless of settings
        if (dataStore.getClaimAt(burnEvent.getBlock().getLocation(), false, null) != null) {
            if (!GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventFireDamageEnabled())
                return;

            burnEvent.setCancelled(true);
        }
    }
}
