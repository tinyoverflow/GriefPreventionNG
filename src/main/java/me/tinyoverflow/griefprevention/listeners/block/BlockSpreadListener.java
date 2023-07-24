package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;

public class BlockSpreadListener implements Listener {
    private final DataStore dataStore;

    public BlockSpreadListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockSpread(BlockSpreadEvent spreadEvent) {
        if (spreadEvent.getSource().getType() != Material.FIRE) return;

        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(spreadEvent.getBlock().getWorld())) return;

        if (!GriefPrevention.instance.config_fireSpreads) {
            spreadEvent.setCancelled(true);

            Block underBlock = spreadEvent.getSource().getRelative(BlockFace.DOWN);
            if (underBlock.getType() != Material.NETHERRACK) {
                spreadEvent.getSource().setType(Material.AIR);
            }

            return;
        }

        //never spread into a claimed area, regardless of settings
        if (dataStore.getClaimAt(spreadEvent.getBlock().getLocation(), false, null) != null) {
            if (!GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventFireSpreadEnabled())
                return;

            spreadEvent.setCancelled(true);

            //if the source of the spread is not fire on netherrack, put out that source fire to save cpu cycles
            Block source = spreadEvent.getSource();
            if (source.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                source.setType(Material.AIR);
            }
        }
    }
}
