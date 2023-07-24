package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

public class BlockFormListener implements Listener {
    private final DataStore dataStore;

    public BlockFormListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onForm(BlockFormEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (GriefPrevention.instance.creativeRulesApply(location)) {
            Material type = block.getType();
            if (type == Material.COBBLESTONE || type == Material.OBSIDIAN || type == Material.LAVA || type == Material.WATER) {
                Claim claim = dataStore.getClaimAt(location, false, null);
                if (claim == null) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
